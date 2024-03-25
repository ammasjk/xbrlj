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

import com.google.common.collect.Sets;
import io.datanapis.xbrl.XbrlInstance;
import io.datanapis.xbrl.model.*;
import io.datanapis.xbrl.model.arc.PresentationArc;
import io.datanapis.xbrl.model.link.PresentationLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public final class PresentationDataProvider {
    /* root is the root graph node in the presentation network that contains both the hypercubes */
    public record NodeCube(PresentationGraphNode root, Collection<PresentationHypercube> hypercubes) {}
    /* Returns the list of Cubes along with their root and all facts pertinent to this presentation network */
    public record CubesAndFacts(Collection<NodeCube> nodeCubes, TimeOrdered<DimensionedFact> facts) {}

    private static final Logger log = LoggerFactory.getLogger(PresentationDataProvider.class);
    private final XbrlInstance instance;

    public PresentationDataProvider(XbrlInstance instance) {
        this.instance = instance;
    }

    private static boolean equivalent(Set<String> first, Set<String> second) {
        return first.containsAll(second) && second.containsAll(first);
    }

    public CubesAndFacts getCubesAndFactsFor(RoleType roleType) {
        PresentationLink presentationLink = roleType.getPresentationLink();
        if (presentationLink == null)
            return null;

        /* We can have multiple roots for a given link */
        Collection<PresentationGraphNode> graphNodes =
                PresentationTaxonomy.getRootNodes(instance.getTaxonomy(), presentationLink);
        if (graphNodes.isEmpty())
            return null;

        /* Collect hypercubes from the DefinitionNetwork and corresponding facts from the XbrlInstance */
        Collection<DefinitionHypercube> definitionHypercubes;
        DefinitionNetwork definitionNetwork = new DefinitionNetwork(instance);
        definitionHypercubes = definitionNetwork.getHypercubeDefinitions(roleType);

        /* Collect hypercubes from the PresentationNetwork and corresponding facts from the XbrlInstance */
        Collection<PresentationHypercube> presentationHypercubes = new ArrayList<>();
        Collection<NodeCube> nodeCubes = new ArrayList<>();
        for (PresentationGraphNode root : graphNodes) {
            /* A root may have multiple hypercubes. This is an artifact of how we are creating hypercubes */
            Collection<PresentationHypercube> nodeHypercubes = getHypercubeDefinitions(root, definitionHypercubes);
            nodeCubes.add(new NodeCube(root, nodeHypercubes));
            presentationHypercubes.addAll(nodeHypercubes);
        }

        /*
         * Presentation networks sometimes reuse axis definitions implicitly - this usually happens when a network contains
         * multiple hypercubes and the one of the hypercubes implicitly refers to the axis definition in another
         * hypercube by using the axis but by not defining it. This shows up as an empty axis in a hypercube when
         * the intent is to reuse the other definition. After constructing all hypercubes, we are collecting all
         * axis definitions that are not empty and then updating the existing empty definitions with the non-empty ones
         */
        Map<Concept, PresentationHypercube.PresentationAxis> axisMap = new HashMap<>();
        for (PresentationHypercube hypercube : presentationHypercubes) {
            for (PresentationHypercube.PresentationAxis presentationAxis : hypercube.getAxes()) {
                Concept axisConcept = presentationAxis.getDimension().getConcept();
                if (!presentationAxis.empty()) {
                    PresentationHypercube.PresentationAxis existing = axisMap.putIfAbsent(axisConcept, presentationAxis);
                    if (Objects.nonNull(existing)) {
                        if (equivalent(presentationAxis.getMemberNames(), existing.getMemberNames())) {
                            log.debug("Potentially duplicate definitions for axis: [{}] - [{}] vs [{}]",
                                    axisConcept, existing, presentationAxis);
                        } else {
                            log.info("Potentially inconsistent definitions for axis: [{}] - [{}] vs [{}]",
                                    axisConcept, existing, presentationAxis);

                        }
                    }
                }
            }
        }
        for (PresentationHypercube hypercube : presentationHypercubes) {
            for (PresentationHypercube.PresentationAxis presentationAxis : hypercube.getAxes()) {
                if (presentationAxis.empty()) {
                    Concept axisConcept = presentationAxis.getDimension().getConcept();
                    PresentationHypercube.PresentationAxis pa = axisMap.get(axisConcept);
                    if (Objects.nonNull(pa)) {
                        presentationAxis.updateDefinition(pa);
                    }
                }
            }
        }

        /* Collect facts for the entire presentation network. We will separate them by hypercube later */
        TimeOrdered<DimensionedFact> facts = new TimeOrdered<>();
        collectPresentationFacts(presentationHypercubes, facts);

        return new CubesAndFacts(nodeCubes, facts);
    }

    private static void collectRecursive(PresentationGraphNode graphNode, PresentationHypercube.PresentationAxis presentationAxis) {
        // Add this member.
        log.debug("[{}] -> [{}] -> [{}]", presentationAxis.getDimension(), graphNode.getArc().getArcrole(), graphNode.getConcept());
        presentationAxis.addMember(graphNode);

        // Recurse through all children
        for (GraphNode<PresentationArc> child : graphNode.getOutLinks()) {
            assert child instanceof PresentationGraphNode;
            PresentationGraphNode node = (PresentationGraphNode) child;
            collectRecursive(node, presentationAxis);
        }
    }

    private static boolean isDomain(PresentationGraphNode node, @Nullable DefinitionHypercube.DefinitionAxis definitionAxis) {
        if (definitionAxis == null)
            return node.isDomain();

        for (DefinitionGraphNode concept : definitionAxis.getDomains()) {
            if (concept.getConcept().equals(node.getConcept()))
                return true;
        }

        return false;
    }

    private static PresentationHypercube.PresentationAxis getMembers(PresentationGraphNode graphNode, @Nullable DefinitionHypercube.DefinitionAxis definitionAxis) {
        PresentationHypercube.PresentationAxis presentationAxis = new PresentationHypercube.PresentationAxis(graphNode);
        for (GraphNode<PresentationArc> outNode : graphNode.getOutLinks()) {
            PresentationGraphNode domainOrMember = (PresentationGraphNode)outNode;
            /*
             * PresentationNetworks can have members containing other members as a grouping construct for
             * presentation purposes. e.g., ConsumerPortfolioSegmentMember
             * -- The following partial network is from JP Morgan, 20210930
             * [us-gaap:ScheduleOfFinancingReceivableAllowanceForCreditLossesTable]  [TERSE]  =  [none] [http://www.xbrl.org/2003/arcrole/parent-child] [1.00]:
             *     [us-gaap:FinancingReceivablePortfolioSegmentAxis]  [TERSE]  =  [none] [http://www.xbrl.org/2003/arcrole/parent-child] [1.00]:
             *         [us-gaap:FinancingReceivablePortfolioSegmentDomain]  [TERSE]  =  [none] [http://www.xbrl.org/2003/arcrole/parent-child] [1.00]:
             *            [us-gaap:ConsumerPortfolioSegmentMember]  [TERSE]  =  [none] [http://www.xbrl.org/2003/arcrole/parent-child] [1.00]:
             *                [jpm:ConsumerExcludingCreditCardLoanPortfolioSegmentMember]  [TERSE]  =  [none] [http://www.xbrl.org/2003/arcrole/parent-child] [1.00]:
             *                [jpm:CreditCardLoanPortfolioSegmentMember]  [VERBOSE]  =  [none] [http://www.xbrl.org/2003/arcrole/parent-child] [2.00]:
             *            [us-gaap:CommercialPortfolioSegmentMember]  [TERSE]  =  [none] [http://www.xbrl.org/2003/arcrole/parent-child] [2.00]:
             *     [srt:CumulativeEffectPeriodOfAdoptionAxis]  [TERSE]  =  [none] [http://www.xbrl.org/2003/arcrole/parent-child] [2.00]:
             *         [srt:CumulativeEffectPeriodOfAdoptionDomain]  [TERSE]  =  [none] [http://www.xbrl.org/2003/arcrole/parent-child] [1.00]:
             *            [srt:CumulativeEffectPeriodOfAdoptionAdjustmentMember]  [TERSE]  =  [none] [http://www.xbrl.org/2003/arcrole/parent-child] [1.00]:
             *     [us-gaap:FinancingReceivableRecordedInvestmentByClassOfFinancingReceivableAxis]  [TERSE]  =  [none] [http://www.xbrl.org/2003/arcrole/parent-child] [3.00]:
             *         [us-gaap:FinancingReceivableRecordedInvestmentClassOfFinancingReceivableDomain]  [TERSE]  =  [none] [http://www.xbrl.org/2003/arcrole/parent-child] [1.00]:
             *             [jpm:CollateralDependentLoansMember]  [TERSE]  =  [none] [http://www.xbrl.org/2003/arcrole/parent-child] [1.00]:
             */
            if (isDomain(domainOrMember, definitionAxis)) {
                presentationAxis.addDomain(domainOrMember);
                if (definitionAxis != null && definitionAxis.hasDefaultDomain()) {
                    if (definitionAxis.getDefaultDomain().getConcept().equals(domainOrMember.getConcept())) {
                        presentationAxis.setDefaultDomain(domainOrMember);
                    }
                }
                for (GraphNode<PresentationArc> node : domainOrMember.getOutLinks()) {
                    assert node instanceof PresentationGraphNode;
                    PresentationGraphNode member = (PresentationGraphNode) node;
                    collectRecursive(member, presentationAxis);
                }
            } else {
                collectRecursive(domainOrMember, presentationAxis);
            }
        }

        return presentationAxis;
    }

    private void getLineItems(PresentationGraphNode node, LineItems<PresentationArc, PresentationGraphNode> lineItems) {
        Concept concept = node.getConcept();
        if (!concept.isAbstractConcept()) {
            lineItems.add(node);
        }

        if (node.hasChildren()) {
            for (GraphNode<PresentationArc> outNode : node.getOutLinks()) {
                PresentationGraphNode child = (PresentationGraphNode)outNode;
                getLineItems(child, lineItems);
            }
        }
    }

    private static DefinitionHypercube getDefinitionFor(PresentationGraphNode node, Collection<DefinitionHypercube> definitionHypercubes) {
        DefinitionHypercube definitionHypercube = null;
        if (definitionHypercubes != null) {
            for (DefinitionHypercube hypercube : definitionHypercubes) {
                if (hypercube.getTableConcept().equals(node.getConcept())) {
                    definitionHypercube = hypercube;
                    break;
                }
            }
        }

        return definitionHypercube;
    }

    private static DefinitionHypercube.DefinitionAxis getAxisFor(PresentationGraphNode node, @Nullable  DefinitionHypercube definitionHypercube) {
        if (definitionHypercube == null)
            return null;

        Collection<DefinitionHypercube.DefinitionAxis> axes = definitionHypercube.getAxes();
        for (DefinitionHypercube.DefinitionAxis definitionAxis : axes) {
            if (definitionAxis.getDimension().getConcept().equals(node.getConcept())) {
                return definitionAxis;
            }
        }

        return null;
    }

    private static String deiString(XbrlInstance instance) {
        Dei dei = instance.getDei();
        if (dei != null) {
            return dei.getEntityInformation() + ":" + dei.getDocumentInformation();
        }

        return "(null dei)";
    }

    private PresentationHypercube getHypercubeForTable(PresentationGraphNode root, Collection<DefinitionHypercube> definitionHypercubes) {
        /* log if table contains other tables; this should never happen */
        if (hasTable(root, definitionHypercubes)) {
            log.error("In [{}], [{}] has table within a table", deiString(instance), root.getQualifiedName());
        }

        DefinitionHypercube definitionHypercube = getDefinitionFor(root, definitionHypercubes);

        List<PresentationHypercube.PresentationAxis> axes = new ArrayList<>();
        LineItems<PresentationArc, PresentationGraphNode> lineItems = new LineItems<>();

        for (GraphNode<PresentationArc> graphNode : root.getOutLinks()) {
            PresentationGraphNode node = (PresentationGraphNode) graphNode;
            DefinitionHypercube.DefinitionAxis definitionAxis = getAxisFor(node, definitionHypercube);
            if (definitionAxis != null || node.isAxis()) {
                axes.add(getMembers(node, definitionAxis));
            } else {
                getLineItems(node, lineItems);
            }
        }

        return new PresentationHypercube(root, axes, lineItems);
    }

    private static int countTables(PresentationGraphNode node) {
        long tableCount = node.getOutLinks().stream().filter(GraphNode::isTable).count();
        return (int)tableCount;
    }

    private static boolean hasTable(PresentationGraphNode node, @Nullable Collection<DefinitionHypercube> definitionHypercubes) {
        if (definitionHypercubes != null) {
            Set<Concept> tablesInDefinition = definitionHypercubes.stream().map(DefinitionHypercube::getTableConcept).collect(Collectors.toSet());
            Set<Concept> outConcepts = node.getOutLinks().stream().map(GraphNode::getConcept).collect(Collectors.toSet());
            return Sets.intersection(tablesInDefinition, outConcepts).size() > 0;
        }

        return countTables(node) > 0;
    }

    private static boolean isTable(PresentationGraphNode node, @Nullable Collection<DefinitionHypercube> definitionHypercubes) {
        DefinitionHypercube definitionHypercube = getDefinitionFor(node, definitionHypercubes);
        return definitionHypercube != null || node.isTable();
    }

    /*
     * We can have multiple hypercubes per node. This is an artifact of how we are creating hypercubes. When presentation networks have multiple
     * tables, each table becomes its own hypercube. This constrains the axis and members to the given table. We could also have cases
     * where one of the hypercubes is a table while the other is just a collection of line items. In this case, the dimensions only apply to
     * the table whereas the line items are all non-dimensioned.
     */
    private void collectHypercubes(PresentationGraphNode node, @Nullable Collection<DefinitionHypercube> definitionHypercubes, Collection<PresentationHypercube> presentationHypercubes) {
        /* If node is a table, construct a hypercube from it. We are assuming tables don't contain other tables */
        if (isTable(node, definitionHypercubes)) {
            PresentationHypercube hypercube = getHypercubeForTable(node, definitionHypercubes);
            presentationHypercubes.add(hypercube);
            return;
        }

        /*
         * Can have the following options:
         * 1) root node has one or more hypercubes, the hypercubes may be explicit or implicit. Explicit hypercubes
         *    will start with a table whereas implicit hypercubes will start with an abstract node. However, for an
         *    abstract node to be considered an implicit table, it needs to have tables as siblings.
         * 2) If the root node has no tables and just abstract nodes, then we have just one implicit hypercube.
         * 3) Alternatively, the node may have just one abstract node with tables. This doesn't introduce new
         *    patterns just creates the same patterns with deeper nesting.
         */
        boolean hasTables = hasTable(node, definitionHypercubes);
        LineItems<PresentationArc, PresentationGraphNode> lineItems = new LineItems<>();

        for (GraphNode<PresentationArc> child : node.getOutLinks()) {
            PresentationGraphNode graphNode = (PresentationGraphNode)child;
            /* The order of these checks is important */
            if (isTable(graphNode, definitionHypercubes)) {
                /*
                 * 1) Check if child is a table and if it is, construct a hypercube from it. We are assuming tables don't nest
                 * Multiple tables are possible at this level - Amalgamated Financial FY, 2021
                 */
                PresentationHypercube hypercube = getHypercubeForTable(graphNode, definitionHypercubes);
                presentationHypercubes.add(hypercube);
            } else if (graphNode.isAbstract() && hasTable(graphNode, definitionHypercubes)) {
                /* 2) Check if child is an abstract node with one or more tables as children, e.g., JPM Q3 2021, General Motors FY 2013 */
                collectHypercubes(graphNode, definitionHypercubes, presentationHypercubes);
            } else {
                /*
                 * Collect the remaining line items into an implicit hypercube. We will construct the hypercube below.
                 * This includes case 3) identified above. We used to handle case 3, specially before, but it is no longer
                 * needed.
                 * e.g., most income / balance sheet / cash flow statements
                 */
                getLineItems(graphNode, lineItems);
            }
        }

        if (!lineItems.isEmpty()) {
            presentationHypercubes.add(new PresentationHypercube(node, lineItems));
        }
    }

    private Collection<PresentationHypercube> getHypercubeDefinitions(PresentationGraphNode root, @Nullable Collection<DefinitionHypercube> definitionHypercubes) {
        /*
         * We can have one of the following variations:
         * - A root node containing one or more table nodes - most likely scenario
         * - A root node containing multiple children, one of which is a table
         * - A root node that is the table - Possible in older XBRL instances
         * - A root node with no tables and just line-items - Possible in older XBRL instances
         */
        log.debug("PresentationDataProvider.getHypercubeDefinitions(): [{}, {}:{}]", root.getConcept().getName(),
                root.getConcept().getSubstitutionGroup().getNamespacePrefix(),
                root.getConcept().getSubstitutionGroup().getName());

        List<PresentationHypercube> presentationHypercubes = new ArrayList<>();
        collectHypercubes(root, definitionHypercubes, presentationHypercubes);
        return presentationHypercubes;
    }

    static boolean isDebugConcept(Concept concept) {
        return "aep:TotalEstimatedFebruary2021StormRestorationExpenditures".matches(concept.getQualifiedName());
    }

    private void collectPresentationFacts(
            Collection<PresentationHypercube> tables, TimeOrdered<DimensionedFact> facts) {

        for (PresentationHypercube table : tables) {
            Collection<Concept> reportableConcepts = new ArrayList<>();
            for (PresentationGraphNode node : table.getLineItems()) {
                reportableConcepts.add(node.getConcept());
            }

            List<Axis> axes = table.getAxes().stream().map(PresentationHypercube.PresentationAxis::toAxis).toList();
            for (Concept concept : reportableConcepts) {
                if (isDebugConcept(concept)) {
                    int y = 5;
                }
                instance.getFactsFor(concept, axes, facts);
            }
        }
    }
}
