/*
 * Copyright (C) 2020 Jayakumar Muthukumarasamy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datanapis.xbrl.analysis;

import io.datanapis.xbrl.XbrlInstance;
import io.datanapis.xbrl.model.*;
import io.datanapis.xbrl.model.arc.PresentationArc;
import io.datanapis.xbrl.model.link.PresentationLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PresentationNetwork extends PresentationTaxonomy {
    private static final Logger log = LoggerFactory.getLogger(PresentationNetwork.class);

    private final Set<String> factsUsed = new HashSet<>();
    private final XbrlInstance instance;
    private final PresentationProcessor processor;
    private final HypercubeDataProvider provider;
    private final Set<Period> mrqPeriods;
    private final Set<Period> ytdPeriods;

    public PresentationNetwork(XbrlInstance instance, PresentationProcessor processor) {
        super(instance.getTaxonomy());

        this.instance = instance;
        this.processor = processor;
        this.provider = new PresentationDataProvider(instance, false);

        mrqPeriods = new HashSet<>();
        Collection<Context> reportingPeriodContexts = instance.getMRQContexts();
        for (Context context : reportingPeriodContexts) {
            mrqPeriods.add(context.getPeriod());
        }

        ytdPeriods = new HashSet<>();
        Collection<Context> yearToDateContexts = instance.getYTDContexts();
        for (Context context : yearToDateContexts) {
            ytdPeriods.add(context.getPeriod());
        }
    }

    public Set<String> getFactsUsed() {
        return factsUsed;
    }

    public int nOfFactsUsed() {
        return factsUsed.size();
    }

    public void complete() {
        processor.complete();
    }

    /**
     * Returns true if the arc coming into this node is of type PERIOD_START or PERIOD_END. Root
     * nodes will not have any arc coming into the node.
     *
     * @param node The node to test
     * @return True if node has an arc of type PERIOD_START or PERIOD_END, false otherwise.
     */
    private static boolean hasStartOrEndLabel(GraphNode<PresentationArc> node) {
        if (node.getArc() == null)
            return false;

        return node.getArc().getPreferredLabelType().equals(Label.PERIOD_START) ||
                node.getArc().getPreferredLabelType().equals(Label.PERIOD_END);
    }

    /**
     * Returns true if any of the nodes belonging to the trees rooted at graphNodes contains an
     * arc of type PERIOD_START or PERIOD_END.
     *
     * @param graphNodes The collection of nodes that define the subtrees to check
     * @return True if any of the nodes has an arc of type PERIOD_START or PERIOD_END, false otherwise.
     */
    private static boolean hasStartOrEndLabel(Collection<GraphNode<PresentationArc>> graphNodes) {
        for (GraphNode<PresentationArc> node : graphNodes) {
            if (hasStartOrEndLabel(node))
                return true;

            if (node.hasChildren()) {
                if (hasStartOrEndLabel(node.getOutLinks()))
                    return true;
            }
        }

        return false;
    }

    /* Only exists because Java doesn't automatically infer type equivalence for Collections */
    private static boolean shouldMerge(Collection<PresentationGraphNode> graphNodes) {
        Collection<GraphNode<PresentationArc>> nodes = new ArrayList<>(graphNodes);
        return hasStartOrEndLabel(nodes);
    }

    private static PeriodGroup newGroup(Period current, Collection<DimensionedFact> facts) {
        return new PeriodGroup.Builder().current(current).currentFacts(facts).build();
    }

    private static boolean juxtaposed(Instant current, Duration next) {
        LocalDate d = current.getDate();
        LocalDate n = next.getStartDate();
        return ChronoUnit.DAYS.between(d, n) == 1;
    }

    private static boolean juxtaposed(Duration current, Instant next) {
        LocalDate d = current.getEndDate();
        LocalDate n = next.getDate();
        return ChronoUnit.DAYS.between(d, n) == 0;  // SIC
    }

    /**
     * Regroup the time periods, so we will process the facts correctly. Presentation networks will frequently combine
     * duration with instants at the beginning and end of the duration. E.g. Cash at beginning of a period and
     * Cash at the end of a period are related to the net-income a company earns during the period. We can only
     * present things correctly if we are able to take this into consideration.
     *
     * @param graphNodes The network
     * @param facts The time ordered facts that needs to be regrouped
     * @return A time-ordered and grouped set of facts
     */
    private static Collection<PeriodGroup> asGroups(Collection<PresentationGraphNode> graphNodes, TimeOrdered<DimensionedFact> facts) {
        List<PeriodGroup> groups = new ArrayList<>();

        List<Instant> instants = new ArrayList<>();
        List<Duration> durations = new ArrayList<>();
        for (Period period : facts.keySet()) {
            if (Period.isInstant(period)) {
                instants.add((Instant)period);
            } else {
                assert Period.isDuration(period);
                durations.add((Duration)period);
            }
        }

        /* If we have only instants or durations, the logic is straightforward */
        if (instants.size() == 0 || durations.size() == 0) {
            facts.forEach((key, value) -> groups.add(newGroup(key, value)));
            return groups;
        }

        boolean merge = shouldMerge(graphNodes);
        if (!merge) {
            /* There is nothing to merge, straightforward */
            facts.forEach((key, value) -> groups.add(newGroup(key, value)));
            return groups;
        }

        /* Need to merge */
        /* Sort the duration list by start time first and then by duration. This will prioritize longer durations for merging over shorter ones */
        durations.sort((lhs, rhs) -> {
            if (lhs.getStartDate().isBefore(rhs.getStartDate())) {
                return -1;
            } else if (lhs.getStartDate().isAfter(rhs.getStartDate())) {
                return 1;
            }

            long lhsDays = ChronoUnit.DAYS.between(lhs.getStartDate(), lhs.getEndDate());
            long rhsDays = ChronoUnit.DAYS.between(rhs.getStartDate(), rhs.getEndDate());
            return Long.compare(rhsDays, lhsDays);
        });

        /* Construct maps of start, end and current (named pairs) to make it easier to construct the final merged list */
        Map<LocalDate,Map.Entry<Period,Collection<DimensionedFact>>> starts = new HashMap<>();
        Map<LocalDate,Map.Entry<Period,Collection<DimensionedFact>>> ends = new HashMap<>();
        Map<Duration,Map.Entry<Period,Collection<DimensionedFact>>> pairs = new HashMap<>();
        for (Map.Entry<Period,Collection<DimensionedFact>> entry : facts.entrySet()) {
            Period key = entry.getKey();
            if (Period.isInstant(key)) {
                Instant instant = (Instant)key;
                /* Instants get considered for both start and end, and they should */
                starts.put(instant.getDate(), entry);
                ends.put(instant.getDate(), entry);
            } else {
                assert Period.isDuration(key);
                Duration duration = (Duration)key;
                pairs.put(duration, entry);
            }
        }

        Set<LocalDate> startsUsed = new HashSet<>();
        Set<LocalDate> endsUsed = new HashSet<>();

        /* Now merge. Each instant will be considered multiple times as long as it is valid for a given duration */
        for (Duration duration : durations) {
            Map.Entry<Period,Collection<DimensionedFact>> pair = pairs.get(duration);

            LocalDate startDate = duration.getStartDate().plus(-1, ChronoUnit.DAYS);
            Map.Entry<Period,Collection<DimensionedFact>> start = starts.get(startDate);
            Map.Entry<Period,Collection<DimensionedFact>> end = ends.get(duration.getEndDate());

            PeriodGroup.Builder builder = new PeriodGroup.Builder().current(pair.getKey()).currentFacts(pair.getValue());
            if (start != null) {
                builder.start((Instant)start.getKey()).startingFacts(start.getValue());
                startsUsed.add(startDate);
            }
            if (end != null) {
                builder.end((Instant)end.getKey()).endingFacts(end.getValue());
                endsUsed.add(duration.getEndDate());
            }

            groups.add(builder.build());
        }

        /* Remove the start and end dates used before looking for any new groups to add */
        for (LocalDate startDate : startsUsed) {
            starts.remove(startDate);
        }
        for (LocalDate endDate : endsUsed) {
            ends.remove(endDate);
        }

        // Add any instant that did not merge with any of the durations i.e. instants available in both starts and ends
        // The starting instant at the end and the ending instant at the start will always be unused.
        for (Map.Entry<LocalDate,Map.Entry<Period,Collection<DimensionedFact>>> entry : starts.entrySet()) {
            if (ends.containsKey(entry.getKey())) {
                groups.add(newGroup(entry.getValue().getKey(), entry.getValue().getValue()));
            }
        }
        groups.sort(PeriodGroup::compareTo);

        return groups;
    }

    public void process(RoleType roleType) {
        PresentationLink presentationLink = roleType.getPresentationLink();
        if (presentationLink == null)
            return;

        String title = roleType.getDefinition();
        if (title.startsWith("00000005 - Statement - Consolidated Statement of Stockholders")) {
            int y = 5;
        }

        /* We are assuming a given link can have multiple roots */
        Collection<PresentationGraphNode> graphNodes =
                PresentationTaxonomy.getRootNodes(instance.getTaxonomy(), presentationLink);
        if (graphNodes.size() == 0)
            return;

        /* Log RoleTypes that have more than one root node - this is quite common even though it doesn't make much sense */
        if (graphNodes.size() > 1)
            log.debug("[{}] has [{}] root nodes", title, graphNodes.size());

        TimeOrdered<DimensionedFact> facts = provider.getFactsFor(roleType);
        Collection<PresentationHypercube> hypercubes = provider.getPresentationHypercubes(roleType);

        processor.start(roleType, facts);
        for (final PresentationHypercube hypercube : hypercubes) {
            PresentationGraphNode root = (PresentationGraphNode) hypercube.getRoot();
            List<PresentationGraphNode> lineItems = hypercube.getLineItems();
            final Set<Concept> hypercubeConcepts = lineItems.stream().map(GraphNode::getConcept).collect(Collectors.toSet());
            Predicate<DimensionedFact> filter = g -> g.getFact() != null && hypercubeConcepts.contains(g.getFact().getConcept());

            TimeOrdered<DimensionedFact> hypercubeFacts = facts.filter(filter);
            /* The time periods are not independent of each other. Regroup so we will process the facts correctly */
            Collection<PeriodGroup> groups = asGroups(graphNodes, hypercubeFacts);
            visit(root, groups);
        }
        processor.end(roleType);
    }

    private void visit(PresentationGraphNode root, Collection<PeriodGroup> groups) {
        processor.rootStart(root);

        Period mrq = null, ytd = null;

        /* Mark MRQ and YTD - TODO - Complete this */
        for (PeriodGroup group : groups) {
            Period period = group.getCurrent();
            if (mrqPeriods.contains(period)) {
                mrq = period;
            } else if (ytdPeriods.contains(period)) {
                ytd = period;
            }
        }

        for (PeriodGroup group : groups) {
            /* skip groups that don't have any facts */
            if (group.getTotalFacts() == 0)
                continue;

            Period period = group.getCurrent();

            PresentationProcessor.ReportingPeriodType rpType;
            if (mrqPeriods.contains(period)) {
                rpType = PresentationProcessor.ReportingPeriodType.MRQ;
            } else if (ytdPeriods.contains(period)) {
                rpType = PresentationProcessor.ReportingPeriodType.YTD;
            } else {
                /* This is too coarse. Need to improve this */
                rpType = PresentationProcessor.ReportingPeriodType.NOT_CLASSIFIED;
            }

            processor.periodStart(root, period, rpType);
            visit(root, 1, group, new PresentationInfoProviderImpl());
            processor.periodEnd(root, period);
        }

        processor.rootEnd(root);
    }

    private static List<DimensionedFact> getFactsFor(PresentationGraphNode graphNode, PeriodGroup group) {
        List<DimensionedFact> conceptFacts = new ArrayList<>();

        Concept concept = graphNode.getConcept();
        Collection<DimensionedFact> facts;
        if (graphNode.getArc() == null) {
            facts = group.getCurrentFacts();
        } else {
            switch (graphNode.getArc().getPreferredLabelType()) {
                case Label.PERIOD_START:
                    facts = group.getStartingFacts();
                    break;

                case Label.PERIOD_END:
                    facts = group.getEndingFacts();
                    if (facts == null) {
                        /* Ending facts is null, but we can use currentFacts provided current is an Instant */
                        if (group.getCurrentFacts() != null && Period.isInstant(group.getCurrent())) {
                            facts = group.getCurrentFacts();
                        }
                    }
                    break;

                default:
                    facts = group.getCurrentFacts();
                    break;
            }
        }

        if (facts == null) {
            return DimensionedFact.getDistinctFacts(conceptFacts);
        }

        for (DimensionedFact fact : facts) {
            if (!fact.getFact().getConcept().equals(concept))
                continue;

            conceptFacts.add(fact);
        }

        if (conceptFacts.size() == 0 && !hasStartOrEndLabel(graphNode) && group.getEndingFacts() != null) {
            for (DimensionedFact fact : group.getEndingFacts()) {
                if (!fact.getFact().getConcept().equals(concept))
                    continue;

                conceptFacts.add(fact);
            }
        }

        return DimensionedFact.getDistinctFacts(conceptFacts);
    }

    private static boolean isMonetaryFact(Fact fact) {
        Concept concept = fact.getConcept();
        return concept.isMonetaryConcept();
    }

    private boolean defaultCurrencyFilter(DimensionedFact f) {
        Fact fact = f.getFact();
        return fact.getUnit() == null || !isMonetaryFact(fact) || fact.getUnit().equals(instance.getDefaultCurrency());
    }

    /**
     * We are using DimensionedFacts for presentation purposes. However, the information is incomplete for presentation
     * purposes. For example, PresentationNetworks define the different Axis and Members for the Hypercube (a hypercube
     * is just another name for a table). But, they also define the labels that should be used for a specific hypercube.
     * We are ignoring this when presenting the table. One possible solution is to collect the labels for the different
     * axis and members during the traversal and refer to it when required. This will enable us to still make use of
     * DimensionedFacts while still fixing the presentation.
     *
     * @param graphNode The presentation line item to be presented
     * @param level The traversal level
     * @param conceptFacts The relevant facts to present
     */
    private void processDimensionedFacts(PresentationGraphNode graphNode, int level,
                                         List<DimensionedFact> conceptFacts, PresentationInfoProvider infoProvider) {
        // Separate facts into those that don't have dimensions and those that have
        List<DimensionedFact> basic = new ArrayList<>();
        List<DimensionedFact> qualified = new ArrayList<>();
        for (DimensionedFact f : conceptFacts) {
            if (!f.isQualified()) {
                basic.add(f);
            } else {
                qualified.add(f);
            }
        }
        if (basic.size() > 1) {
            // This can happen when the same fact has multiple values with different decimals. Let's validate this!
            Set<Integer> decimals = basic.stream().map(df -> df.getFact().getDecimals()).collect(Collectors.toSet());
            if (decimals.size() != basic.size()) {
                log.info("Found [{}] facts for Concept [{}] but not they may not all be the same. Decimals [{}]",
                        basic.size(), graphNode.getConcept().toString(), decimals.size());
            } else {
                /* Mark all facts as used, since they are all equivalent */
                basic.forEach(f -> factsUsed.add(f.getFact().getId()));
            }
        }
        int groupingIncrement = 0;
        if (basic.size() == 0) {
            if (processor.groupDimensionedFacts()) {
                //
                // DimensionedFacts have a lot of information - the name of the node, the different axis and dimensions, etc.
                // However, all DimensionedFacts for one invocation share the same node. By creating a fictitious internal
                // node, we can group the information better.
                //
                processor.internalNodeStart(graphNode, level);
                groupingIncrement = 1;
            }
        } else {
            //
            // Some XBRL instances have a node with the same name as the dimensioned data but without any dimensions.
            // This could be a summary across the different dimensions. In these cases, there is no need to create a
            // fictitious internal node. The node without dimensions can provide the grouping construct. We are only
            // using the first node, since the expectation is that the different instances are equivalent
            // representations of the same fact. We are validating this in the block above, but are only logging failures
            //
            DimensionedFact g = basic.get(0);
            processor.lineItem(graphNode, level, g.getFact());
            factsUsed.add(g.getFact().getId());
            groupingIncrement = 1;
        }

        infoProvider.order(qualified);
        int basis = 0;
        for (DimensionedFact f : qualified) {
            assert f.isQualified();
            int lineItemLevel = level + groupingIncrement;
            if (f.getDimensions() != null && f.getDimensions().size() > 0) {
                int memberLevel = infoProvider.getLevel(f.getDimensions().get(f.getDimensions().size() - 1));
                if (basis == 0) {
                    /* first item becomes the basis, other items are relative to this */
                    basis = memberLevel;
                } else {
                    lineItemLevel = level + groupingIncrement + memberLevel - basis;
                }
            }
            processor.lineItem(graphNode, lineItemLevel, f, infoProvider);
            factsUsed.add(f.getFact().getId());
        }

        if (basic.size() == 0) {
            if (processor.groupDimensionedFacts()) {
                processor.internalNodeEnd(graphNode, level);
            }
        }
    }

    private void processFact(PresentationGraphNode graphNode, int level, PeriodGroup group, PresentationInfoProvider infoProvider) {
        List<DimensionedFact> conceptFacts = getFactsFor(graphNode, group);
        /* Filter out any dimensional fact that cannot appear in this network */
        Predicate<DimensionedFact> checkDimensions = f -> f.getDimensions() == null || infoProvider.contains(f.getDimensions());
        conceptFacts = conceptFacts.stream().filter(checkDimensions).collect(Collectors.toList());

        // Nothing to do
        if (conceptFacts.size() == 0)
            return;

        if (conceptFacts.size() == 1) {
            DimensionedFact fact = conceptFacts.iterator().next();
            if (!fact.isQualified()) {
                processor.lineItem(graphNode, level, fact.getFact());
                factsUsed.add(fact.getFact().getId());
            } else {
                processDimensionedFacts(graphNode, level, conceptFacts, infoProvider);
            }
        } else {
            processDimensionedFacts(graphNode, level, conceptFacts, infoProvider);
        }
    }

    private static boolean shouldSkip(PresentationGraphNode node) {
        if (node.getQualifiedName().endsWith("LineItems") &&
                node.getParent() != null && node.getParent().getQualifiedName().endsWith("Table")) {
            return true;
        }

        return false;
    }

    private void visit(PresentationGraphNode graphNode, int level, PeriodGroup group, PresentationInfoProviderImpl infoProvider) {
        List<PresentationGraphNode> lineItems = new ArrayList<>();
        for (GraphNode<PresentationArc> outLink : graphNode.getOutLinks()) {
            PresentationGraphNode node = (PresentationGraphNode) outLink;
            if (node.isAxis()) {
                infoProvider.addAxis(node);
                continue;
            }

            lineItems.add(node);
        }

        int increment = 1;
        boolean shouldSkip = false;
        if (graphNode.isAbstract()) {
            shouldSkip = shouldSkip(graphNode);
            if (!shouldSkip) {
                processor.internalNodeStart(graphNode, level);
            } else {
                increment = 0;
            }
        } else {
            processFact(graphNode, level, group, infoProvider);
        }

        for (PresentationGraphNode node : lineItems) {
            visit(node,level + increment, group, infoProvider);
        }

        if (graphNode.isAbstract()) {
            if (!shouldSkip) {
                processor.internalNodeEnd(graphNode, level);
            }
        }
    }

    private static class PresentationInfoProviderImpl implements PresentationInfoProvider {
        private final Map<Concept,Label> axisMap = new HashMap<>();
        private final Map<ExplicitMember,Label> memberMap = new HashMap<>();
        private final Map<ExplicitMember,int[]> memberOrder = new HashMap<>();
        private final List<PresentationGraphNode> axisNodes = new ArrayList<>();
        private int order;

        private PresentationInfoProviderImpl() {
            order = 0;
        }

        private static String parentName(PresentationGraphNode parent) {
            if (parent == null)
                return "";
            return parent.getQualifiedName();
        }

        private static void logIfDuplicate(PresentationGraphNode node, Label label, Label previous) {
            if (previous != null && !previous.getRole().equals(label.getRole())) {
                PresentationGraphNode parent = (PresentationGraphNode) node.getParent();
                log.info("Concept [{}, {}] already had label [{}]. Replaced with [{}]",
                        node.getConcept().getQualifiedName(), parentName(parent), previous.getRole(), label.getRole());
            }
        }

        private static void logMissingLabel(PresentationArc arc, Concept concept) {
            /* The taxonomy has a preferred arc. However, the concept does not have a corresponding label */
            /* Only log if the label is an uncommon one */
            String preferredLabel = arc.getPreferredLabel();
            if (!preferredLabel.equals(Label.ROLE_TYPE_TERSE_LABEL) && !preferredLabel.equals(Label.ROLE_TYPE_VERBOSE_LABEL)) {
                log.info("Label [{}] missing for Concept [{}]", arc.getPreferredLabel(), concept.getQualifiedName());
            }
        }

        private void addMemberLabels(int level, PresentationGraphNode axisNode, PresentationGraphNode member) {
            Concept concept = member.getConcept();
            ExplicitMember em = new ExplicitMember(axisNode.getConcept(), concept);
            memberOrder.put(em, new int[] { level, order });
            ++order;

            PresentationArc arc = member.getArc();
            if (arc != null && arc.getPreferredLabel() != null) {
                String preferredLabel = arc.getPreferredLabel();
                Label label = concept.getLabel(preferredLabel);
                if (label != null) {
                    Label previous = memberMap.put(em, label);
                    logIfDuplicate(member, label, previous);
                } else {
                    logMissingLabel(arc, concept);
                }
            }

            for (GraphNode<PresentationArc> child : member.getOutLinks()) {
                PresentationGraphNode childNode = (PresentationGraphNode) child;
                addMemberLabels(level + 1, axisNode, childNode);
            }
        }

        private void addAxis(PresentationGraphNode axisNode) {
            assert axisNode.isAxis();
            axisNodes.add(axisNode);

            PresentationArc arc = axisNode.getArc();
            if (arc != null && arc.getPreferredLabel() != null) {
                String preferredLabel = arc.getPreferredLabel();
                Concept concept = axisNode.getConcept();
                Label label = concept.getLabel(arc.getPreferredLabel());
                if (label != null) {
                    Label previous = axisMap.put(concept, label);
                    logIfDuplicate(axisNode, label, previous);
                } else {
                    logMissingLabel(arc, concept);
                }
            }

            for (GraphNode<PresentationArc> child : axisNode.getOutLinks()) {
                PresentationGraphNode childNode = (PresentationGraphNode) child;
                addMemberLabels(1, axisNode, childNode);
            }
        }

        private int compare(ExplicitMember lhs, ExplicitMember rhs) {
            int[] left = memberOrder.get(lhs);
            int[] right = memberOrder.get(rhs);
            assert (left != null && right != null);

            int result = Integer.compare(left[1], right[1]);
            return result;
        }

        private int compare(DimensionedFact lhs, DimensionedFact rhs) {
            List<ExplicitMember> left = lhs.getDimensions();
            List<ExplicitMember> right = rhs.getDimensions();

            int i = 0, j = 0;
            while (i < left.size() && j < right.size()) {
                int result = compare(left.get(i++), right.get(j++));
                if (result != 0)
                    return result;
            }

            if (i == left.size() && j == right.size()) {
                return 0;
            } else if (i == left.size()) {
                return -1;
            }

            return 1;
        }

        @Override
        public void order(List<DimensionedFact> facts) {
            /* check if all qualified facts are dimensional facts before ordering, else leave things as they are */
            int count = (int)facts.stream().filter(f -> f.getDimensions() != null).count();
            if (count == facts.size())
                facts.sort(this::compare);
        }

        @Override
        public boolean contains(List<ExplicitMember> dimensions) {
            for (ExplicitMember dimension : dimensions) {
                if (!memberOrder.containsKey(dimension))
                    return false;
            }

            return true;
        }

        @Override
        public int getLevel(ExplicitMember dimension) {
            int[] levelIndex = memberOrder.get(dimension);
            assert levelIndex != null;
            return levelIndex[0];
        }

        @Override
        public String getAxisLabel(Concept axis) {
            Label label = axisMap.get(axis);
            if (label != null)
                return label.getValue();

            label = axis.getLabel(Label.ROLE_TYPE_TERSE_LABEL);
            if (label != null)
                return label.getValue();

            label = axis.getLabel();
            if (label != null)
                return label.getValue();

            log.info("Label missing for Axis [{}]", axis.getQualifiedName());
            return axis.getQualifiedName();
        }

        @Override
        public Pair<String,String> getLabel(ExplicitMember pair) {
            String axisLabel = getAxisLabel(pair.getDimension());

            Label label = memberMap.get(pair);
            if (label != null)
                return new Pair<>(axisLabel, label.getValue());

            Concept concept = pair.getMember();
            label = concept.getLabel(Label.ROLE_TYPE_TERSE_LABEL);
            if (label != null)
                return new Pair<>(axisLabel, label.getValue());

            label = concept.getLabel();
            if (label != null)
                return new Pair<>(axisLabel, label.getValue());

            log.info("Label missing for Member [{}]", concept.getQualifiedName());
            return new Pair<>(axisLabel, concept.getQualifiedName());
        }
    }
}
