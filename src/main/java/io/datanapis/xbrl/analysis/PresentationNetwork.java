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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PresentationNetwork extends PresentationTaxonomy {
    private static final Logger log = LoggerFactory.getLogger(PresentationNetwork.class);

    private final Set<String> factsUsed = new HashSet<>();
    private final XbrlInstance instance;
    private final PresentationProcessor processor;
    private final PresentationDataProvider provider;
    private final Set<Period> mrqPeriods;
    private final Set<Period> ytdPeriods;

    public PresentationNetwork(XbrlInstance instance, PresentationProcessor processor) {
        super(instance.getTaxonomy());

        this.instance = instance;
        this.processor = processor;
        this.provider = new PresentationDataProvider(instance);

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
     * Returns true if the arc coming into this node is of type PERIOD_START. Root
     * nodes will not have any arc coming into the node.
     *
     * @param node The node to test
     * @return True if node has an arc of type PERIOD_START, false otherwise.
     */
    private static boolean hasStartLabel(GraphNode<PresentationArc> node) {
        if (node.getArc() == null)
            return false;

        return node.getArc().getPreferredLabelType().contains(Label.PERIOD_START);
    }

    /**
     * Returns true if the arc coming into this node is of type PERIOD_END. Root
     * nodes will not have any arc coming into the node.
     *
     * @param node The node to test
     * @return True if node has an arc of type PERIOD_END, false otherwise.
     */
    private static boolean hasEndLabel(GraphNode<PresentationArc> node) {
        if (node.getArc() == null)
            return false;

        return node.getArc().getPreferredLabelType().contains(Label.PERIOD_END);
    }

    /**
     * Collects all nodes with an arc of type PERIOD_START or PERIOD_END rooted into respective collections.
     *
     * @param graphNodes The collection of nodes that define the subtrees to check
     */
    private static void collectStartEndNodes(
            Collection<GraphNode<PresentationArc>> graphNodes,
            Collection<Concept> startingConcepts, Collection<Concept> endingConcepts) {
        for (GraphNode<PresentationArc> node : graphNodes) {
            if (hasStartLabel(node))
                startingConcepts.add(node.getConcept());
            else if (hasEndLabel(node))
                endingConcepts.add(node.getConcept());

            if (node.hasChildren()) {
                collectStartEndNodes(node.getOutLinks(), startingConcepts, endingConcepts);
            }
        }
    }

    private static boolean shouldMerge(
            Collection<PresentationGraphNode> graphNodes,
            Collection<Concept> startingConcepts, Collection<Concept> endingConcepts) {
        Collection<GraphNode<PresentationArc>> nodes = new ArrayList<>(graphNodes);
        collectStartEndNodes(nodes, startingConcepts, endingConcepts);
        return !startingConcepts.isEmpty() || !endingConcepts.isEmpty();
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
        if (instants.isEmpty() || durations.isEmpty()) {
            facts.forEach((key, value) -> groups.add(newGroup(key, value)));
            return groups;
        }

        Set<Concept> startingConcepts = new HashSet<>();
        Set<Concept> endingConcepts = new HashSet<>();
        boolean merge = shouldMerge(graphNodes, startingConcepts, endingConcepts);
        if (!merge) {
            /* There is nothing to merge, straightforward */
            assert startingConcepts.isEmpty() && endingConcepts.isEmpty();
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

        /* Construct maps of start, end and current (called pairs) to make it easier to construct the final merged list */
        Map<LocalDate,Map.Entry<Period,Collection<DimensionedFact>>> starts = new HashMap<>();
        Map<LocalDate,Map.Entry<Period,Collection<DimensionedFact>>> ends = new HashMap<>();
        Map<Duration,Map.Entry<Period,Collection<DimensionedFact>>> pairs = new HashMap<>();
        for (Map.Entry<Period,Collection<DimensionedFact>> entry : facts.entrySet()) {
            Period key = entry.getKey();
            if (Period.isInstant(key)) {
                Instant instant = (Instant)key;
                /* Instants get considered for both start and end, as they should */
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

            LocalDate startDate = duration.getStartDate().plusDays(-1);
            Map.Entry<Period,Collection<DimensionedFact>> start = starts.get(startDate);
            Map.Entry<Period,Collection<DimensionedFact>> end = ends.get(duration.getEndDate());

            PeriodGroup.Builder builder = new PeriodGroup.Builder().current(pair.getKey()).currentFacts(pair.getValue());
            if (start != null) {
                builder.start((Instant)start.getKey()).startingFacts(start.getValue(), startingConcepts);
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

    private static boolean isDebugRoleType(RoleType roleType) {
        return roleType != null && roleType.getDefinition() != null &&
                (roleType.getDefinition().contains("157 - Disclosure") || roleType.getDefinition().contains("2412405"));
    }

    public void process(RoleType roleType) {
        PresentationLink presentationLink = roleType.getPresentationLink();
        if (presentationLink == null)
            return;

        String title = roleType.getDefinition();
        if (isDebugRoleType(roleType)) {
            int y = 5;
        }

        /* We are assuming a given link can have multiple roots */
        Collection<PresentationGraphNode> graphNodes =
                PresentationTaxonomy.getRootNodes(instance.getTaxonomy(), presentationLink);
        if (graphNodes.isEmpty())
            return;

        /* Log RoleTypes that have more than one root node - this is quite common even though it doesn't make much sense */
        if (graphNodes.size() > 1)
            log.debug("[{}] has [{}] root nodes", title, graphNodes.size());

        PresentationDataProvider.CubesAndFacts cubesAndFacts = provider.getCubesAndFactsFor(roleType);
        if (cubesAndFacts == null)
            return;

        Collection<PresentationDataProvider.NodeCube> nodeCubes = cubesAndFacts.nodeCubes();
        TimeOrdered<DimensionedFact> facts = cubesAndFacts.facts();

        processor.start(roleType, facts);
        for (var nodeCube : nodeCubes) {
            List<PresentationGraphNode> lineItems = new ArrayList<>();
            for (PresentationHypercube hypercube : nodeCube.hypercubes()) {
                lineItems.addAll(hypercube.getLineItems());
            }
            final Set<Concept> hypercubeConcepts = lineItems.stream().map(GraphNode::getConcept).collect(Collectors.toSet());
            Predicate<DimensionedFact> filter = g -> g.getFact() != null && hypercubeConcepts.contains(g.getFact().getConcept());

            TimeOrdered<DimensionedFact> hypercubeFacts = facts.filter(filter);
            /* The time periods are not independent of each other. Regroup so we will process the facts correctly */
            Collection<PeriodGroup> groups = asGroups(graphNodes, hypercubeFacts);
            visit(nodeCube.root(), nodeCube.hypercubes(), groups);
        }
        processor.end(roleType);
    }

    private static final Function<String,Long> DELTA_MAPPER = a -> switch (a) {
        case "Q1" -> 90L;
        case "Q2" -> 180L;
        case "Q3" -> 270L;
        default -> 365L;
    };

    private final int DAYS_ALLOWANCE = 10;

    /**
     * Map the given period to an appropriate reporting period type. Period will neither be an MRQ nor a YTD.
     * Instants can only be classified as either BOP (beginning of period) or NOT_CLASSIFIED. Durations can be
     * classified as P_MRQ, P_YTD, PQ or NOT_CLASSIFIED. The core idea is to compare the day-difference between
     * the given period and the end date of the reporting period to understand where period occurs in the
     * calendar.
     *
     * @param period the period to classify
     * @return One of BOP, PQ, P_MRQ, P_YTD or NOT_CLASSIFIED
     */
    private PresentationProcessor.ReportingPeriodType classify(Period period) {
        if (period instanceof Instant p) {
            long days = ChronoUnit.DAYS.between(p.getDate(), instance.getPeriodEndDate());
            long delta = DELTA_MAPPER.apply(instance.getFiscalPeriod());
            if (Math.abs(days - delta) < DAYS_ALLOWANCE) {
                return PresentationProcessor.ReportingPeriodType.BEGINNING_OF_PERIOD;
            } else if (p.getDate().isAfter(instance.getPeriodEndDate())) {
                /* Share count values are generally updated after the reporting period ends and before the statement is filed */
                return PresentationProcessor.ReportingPeriodType.MOST_RECENT_VALUE;
            } else {
                return PresentationProcessor.ReportingPeriodType.NOT_CLASSIFIED;
            }
        } else if (period instanceof Duration d) {
            /* Interval between end of the duration and the period-end-date => duration is interesting when end to end is roughly 365 days */
            long daysBetweenEnds = ChronoUnit.DAYS.between(d.getEndDate(), instance.getPeriodEndDate());
            LocalDate startGuess = instance.getPeriodEndDate().minusDays(DELTA_MAPPER.apply(instance.getFiscalPeriod()));
            long endToStart = ChronoUnit.DAYS.between(d.getEndDate(), startGuess);

            if (Math.abs(daysBetweenEnds - 365L) < DAYS_ALLOWANCE) {
                if (Math.abs(d.durationInDays() - 90L) < DAYS_ALLOWANCE) {
                    return PresentationProcessor.ReportingPeriodType.PRIOR_YEAR_QUARTER;
                } else if (Math.abs(d.durationInDays() - 365L) < DAYS_ALLOWANCE) {
                    return PresentationProcessor.ReportingPeriodType.PFY;
                } else {
                    /* We are not validating duration length i.e. Math.abs(d.durationInDays() - DELTA_MAPPER.apply(instance.getFiscalPeriod())) < DAYS_ALLOWANCE */
                    return PresentationProcessor.ReportingPeriodType.PRIOR_YEAR_TO_DATE;
                }
            } else if (Math.abs(daysBetweenEnds - 90) < DAYS_ALLOWANCE && Math.abs(d.durationInDays() - 90L) < DAYS_ALLOWANCE) {
                return PresentationProcessor.ReportingPeriodType.PRIOR_QUARTER;
            } else if (endToStart < DAYS_ALLOWANCE && Math.abs(d.durationInDays() - 365L) < DAYS_ALLOWANCE) {
                /* d ends at the start of the instance's reporting period and is ~365 days long => Previous Fiscal Year */
                return PresentationProcessor.ReportingPeriodType.PFY;
            } else {
                return PresentationProcessor.ReportingPeriodType.NOT_CLASSIFIED;
            }
        }

        return PresentationProcessor.ReportingPeriodType.NOT_CLASSIFIED;
    }

    private void visit(PresentationGraphNode root, Collection<PresentationHypercube> hypercubes, Collection<PeriodGroup> groups) {
        PresentationInfoProvider infoProvider = new PresentationInfoProviderImpl(hypercubes);
        processor.rootStart(root, infoProvider);

        for (PeriodGroup group : groups) {
            /* skip groups that don't have any facts */
            if (group.getTotalFacts() == 0)
                continue;

            Period period = group.getCurrent();

            PresentationProcessor.ReportingPeriodType rpType;
            if (!Period.isInstant(period) && mrqPeriods.contains(period)) {
                rpType = PresentationProcessor.ReportingPeriodType.MRQ;
            } else if (ytdPeriods.contains(period)) {
                if (Period.isInstant(period)) {
                    rpType = PresentationProcessor.ReportingPeriodType.MOST_RECENT_VALUE;
                } else {
                    Duration d = (Duration) period;
                    if (Math.abs(d.durationInDays() - 365L) < DAYS_ALLOWANCE) {
                        rpType = PresentationProcessor.ReportingPeriodType.FY;
                    } else {
                        rpType = PresentationProcessor.ReportingPeriodType.YTD;
                    }
                }
            } else {
                rpType = classify(period);
            }

            processor.periodStart(root, period, rpType);
            visit(root, 1, group, infoProvider);
            processor.periodEnd(root, period, rpType);
        }

        processor.rootEnd(root, infoProvider);
    }

    private static void addMatchingFacts(PresentationGraphNode graphNode, List<DimensionedFact> to, Collection<DimensionedFact> from) {
        if (from == null)
            return;

        Concept concept = graphNode.getConcept();
        for (DimensionedFact df : from) {
            if (df.getFact().getConcept().equals(concept)) {
                to.add(df);
            }
        }
    }

    private static List<DimensionedFact> getFactsFor(PresentationGraphNode graphNode, PeriodGroup group) {
        List<DimensionedFact> conceptFacts = new ArrayList<>();
        if (PresentationDataProvider.isDebugConcept(graphNode.getConcept())) {
            int y = 5;
        }

        addMatchingFacts(graphNode, conceptFacts, group.getStartingFacts());
        addMatchingFacts(graphNode, conceptFacts, group.getCurrentFacts());
        addMatchingFacts(graphNode, conceptFacts, group.getEndingFacts());

        List<DimensionedFact> facts = DimensionedFact.getDistinctFacts(conceptFacts);
        if (conceptFacts.size() > facts.size()) {
            log.debug("Filtering [{}] duplicate facts for [{}] for Period [{}/{}/{}]",
                    conceptFacts.size() - facts.size(), graphNode.getConcept().getQualifiedName(),
                    group.getStart(), group.getCurrent(), group.getEnd());
        }
        return facts;
    }

    private static boolean isMonetaryFact(Fact fact) {
        Concept concept = fact.getConcept();
        return concept.isMonetaryConcept();
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

        if (basic.isEmpty()) {
            if (processor.groupDimensionedFacts()) {
                //
                // DimensionedFacts have a lot of information - the name of the node, the different axis and dimensions, etc.
                // However, all DimensionedFacts for one invocation share the same node. By creating a fictitious internal
                // node, we can group the information better.
                //
                processor.internalNodeStart(graphNode, level);
            }
        } else {
            //
            // Some XBRL instances have a node with the same name as the dimensioned data but without any dimensions.
            // This could be a summary across the different dimensions. In these cases, there is no need to create a
            // fictitious internal node. The node without dimensions can provide the grouping construct. We are only
            // using the first node, since the expectation is that the different instances are equivalent
            // representations of the same fact. We are validating this in the block above, but are only logging failures
            //
            for (DimensionedFact g : basic) {
                processor.lineItem(graphNode, level, g.getFact());
                factsUsed.add(g.getFact().getId());
            }
        }

        infoProvider.order(graphNode, qualified);
        for (DimensionedFact f : qualified) {
            assert f.isQualified();
            processor.lineItem(graphNode, level, f, infoProvider);
            factsUsed.add(f.getFact().getId());
        }

        if (basic.isEmpty()) {
            if (processor.groupDimensionedFacts()) {
                processor.internalNodeEnd(graphNode, level);
            }
        }
    }

    private static int dayInterval(Instant one, Period two, Function<Period,LocalDate> mapper) {
        long daysInterval = Math.abs(ChronoUnit.DAYS.between(one.getDate(), mapper.apply(two)));
        return (int)daysInterval;
    }

    private void processFact(PresentationGraphNode graphNode, int level, PeriodGroup group, PresentationInfoProvider infoProvider) {
        List<DimensionedFact> facts = getFactsFor(graphNode, group);
        /* Filter out any facts that cannot appear in this network */
        List<DimensionedFact> conceptFacts = new ArrayList<>();
        for (DimensionedFact fact : facts) {
            /* Dimensions don't match with the hypercube */
            if (fact.getDimensions() != null && !infoProvider.contains(graphNode, fact.getDimensions()))
                continue;

            String labelType = graphNode.getArc().getPreferredLabelType();
            if (labelType.contains(Label.PERIOD_START)) {
                /* For a period start label, fact must belong to an instant context */
                if (fact.getFact().getContext().getPeriod() instanceof Instant instant) {
                    int daysInterval = dayInterval(instant, (group.getStart() != null) ? group.getStart() : group.getCurrent(), p -> switch (p.getType()) {
                        case INSTANT -> ((Instant)p).getDate();
                        case DURATION -> ((Duration)p).getStartDate();
                    });
                    if (daysInterval > 3)
                        continue;
                }
            } else if (labelType.contains(Label.PERIOD_END)) {
                /* For a period end label, fact must belong to an instant context */
                if (fact.getFact().getContext().getPeriod() instanceof Instant instant) {
                    int daysInterval = dayInterval(instant, (group.getEnd() != null) ? group.getEnd() : group.getCurrent(), p -> switch (p.getType()) {
                        case INSTANT -> ((Instant)p).getDate();
                        case DURATION -> ((Duration)p).getEndDate();
                    });
                    if (daysInterval > 3)
                        continue;
                }
            }

            conceptFacts.add(fact);
        }

        // Nothing to do
        if (conceptFacts.isEmpty())
            return;

        processDimensionedFacts(graphNode, level, conceptFacts, infoProvider);
    }

    private static boolean shouldSkip(PresentationGraphNode node) {
        /* The LineItems node in a Table is only a grouping construct, not a presentation construct */
        boolean result = node.getQualifiedName().endsWith("LineItems") &&
                node.getParent() != null && node.getParent().getQualifiedName().endsWith("Table");
        if (result)
            return true;

        /* Similarly, a Table node within an Abstract node is only a grouping construct */
        result = node.getQualifiedName().endsWith("Table") && node.getParent() != null && node.getParent().isAbstract();
        return result;
    }

    private void visit(PresentationGraphNode graphNode, int level, PeriodGroup group, PresentationInfoProvider infoProvider) {
        List<PresentationGraphNode> lineItems = new ArrayList<>();
        for (GraphNode<PresentationArc> outLink : graphNode.getOutLinks()) {
            PresentationGraphNode node = (PresentationGraphNode) outLink;
            if (!node.isAxis()) {
                lineItems.add(node);
            }
        }

        int increment = 1;
        boolean skipNode = false;
        if (graphNode.isAbstract()) {
            skipNode = shouldSkip(graphNode);
            if (!skipNode) {
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
            if (!skipNode) {
                processor.internalNodeEnd(graphNode, level);
            }
        }
    }
}
