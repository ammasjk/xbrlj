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
import io.datanapis.xbrl.model.arc.DefinitionArc;
import io.datanapis.xbrl.model.arc.PresentationArc;
import io.datanapis.xbrl.model.link.PresentationLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PresentationDataProvider implements HypercubeDataProvider {
    private static final Logger log = LoggerFactory.getLogger(PresentationDataProvider.class);
    protected final XbrlInstance instance;

    private final boolean includeDefinition;

    public PresentationDataProvider(XbrlInstance instance) {
        this(instance, true);
    }

    public PresentationDataProvider(XbrlInstance instance, boolean includeDefinition) {
        this.instance = instance;
        this.includeDefinition = includeDefinition;
    }

    private static void collectRecursive(PresentationGraphNode graphNode, Axis axis) {
        // Add this member.
        log.debug("[{}] -> [{}] -> [{}]", axis.getDimension(), graphNode.getArc().getArcrole(), graphNode.getConcept());
        axis.addMember(graphNode.getConcept());

        // Recurse through all children
        for (GraphNode<PresentationArc> child : graphNode.getOutLinks()) {
            assert child instanceof PresentationGraphNode;
            PresentationGraphNode node = (PresentationGraphNode) child;
            collectRecursive(node, axis);
        }
    }

    protected static Axis getMembers(PresentationGraphNode graphNode) {
        assert graphNode.isAxis();

        Axis axis = new Axis(graphNode.getConcept());
        for (GraphNode<PresentationArc> outNode : graphNode.getOutLinks()) {
            PresentationGraphNode domainOrMember = (PresentationGraphNode)outNode;
            /* PresentationNetworks can have members containing other members as a grouping construct for
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
            if (domainOrMember.isDomain()) {
                for (GraphNode<PresentationArc> node : domainOrMember.getOutLinks()) {
                    assert node instanceof PresentationGraphNode;
                    PresentationGraphNode member = (PresentationGraphNode) node;
                    collectRecursive(member, axis);
                }
            } else {
                collectRecursive(domainOrMember, axis);
            }
        }

        return axis;
    }

    @Override
    public Collection<PresentationHypercube> getPresentationHypercubes(RoleType roleType) {
        Collection<PresentationHypercube> hypercubes = new ArrayList<>();

        PresentationLink presentationLink = roleType.getPresentationLink();
        if (presentationLink == null)
            return hypercubes;

        /* We can have multiple roots for a given link */
        Collection<PresentationGraphNode> graphNodes =
                PresentationTaxonomy.getRootNodes(instance.getTaxonomy(), presentationLink);
        if (graphNodes.size() == 0)
            return hypercubes;

        /* Collect hypercubes from the PresentationNetwork */
        for (PresentationGraphNode root : graphNodes) {
            hypercubes.addAll(getHypercubeDefinitions(root));
        }

        return hypercubes;
    }

    @Override
    public TimeOrdered<DimensionedFact> getFactsFor(RoleType roleType) {
        TimeOrdered<DimensionedFact> facts = new TimeOrdered<>();

        PresentationLink presentationLink = roleType.getPresentationLink();
        if (presentationLink == null)
            return facts;

        /* We can have multiple roots for a given link */
        Collection<PresentationGraphNode> graphNodes =
                PresentationTaxonomy.getRootNodes(instance.getTaxonomy(), presentationLink);
        if (graphNodes.size() == 0)
            return facts;

        /* Collect hypercubes from the DefinitionNetwork and corresponding facts from the XbrlInstance */
        if (includeDefinition) {
            DefinitionNetwork definitionNetwork = new DefinitionNetwork(instance);
            Collection<DefinitionHypercube> definitionHypercubes = definitionNetwork.getHypercubeDefinitions(roleType);
            collectDefinitionFacts(definitionHypercubes, facts, instance);
        }

        /* Collect hypercubes from the PresentationNetwork and corresponding facts from the XbrlInstance */
        Collection<PresentationHypercube> presentationHypercubes = new ArrayList<>();
        for (PresentationGraphNode root : graphNodes) {
            presentationHypercubes.addAll(getHypercubeDefinitions(root));
        }
        collectPresentationFacts(presentationHypercubes, facts);

        return facts;
    }

    private static void collectDefinitionFacts(Collection<DefinitionHypercube> tables,
                                               TimeOrdered<DimensionedFact> facts, XbrlInstance instance) {

        for (Hypercube<DefinitionArc,DefinitionGraphNode> table : tables) {
            Collection<Concept> reportableConcepts = new ArrayList<>();
            for (DefinitionGraphNode node : table.getLineItems()) {
                reportableConcepts.add(node.getConcept());
            }

            for (Concept concept : reportableConcepts) {
                instance.getFactsForConcept(concept, table.getAxes(), facts);
            }
        }
    }

    protected void getLineItems(PresentationGraphNode node, LineItems<PresentationArc, PresentationGraphNode> lineItems) {
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

    private PresentationHypercube getHypercubeForTable(PresentationGraphNode root) {
        /* log if table contains other tables */
        if (hasTable(root)) {
            log.info("In [{}], [{}] has table within a table", deiString(instance), root.getQualifiedName());
        }

        Collection<Axis> axes = new ArrayList<>();
        LineItems<PresentationArc, PresentationGraphNode> lineItems = new LineItems<>();

        assert root.isTable() || root.hasAxis();
        for (GraphNode<PresentationArc> graphNode : root.getOutLinks()) {
            PresentationGraphNode node = (PresentationGraphNode) graphNode;
            if (node.isAxis()) {
                axes.add(getMembers(node));
            } else {
                getLineItems(node, lineItems);
            }
        }

        return new PresentationHypercube(root, axes, lineItems);
    }


    private PresentationHypercube getDefinitionForHypercube(PresentationGraphNode root) {
        Collection<Axis> axes = new ArrayList<>();
        LineItems<PresentationArc, PresentationGraphNode> lineItems = new LineItems<>();

        if (root.hasAxis()) {
            for (GraphNode<PresentationArc> graphNode : root.getOutLinks()) {
                PresentationGraphNode node = (PresentationGraphNode) graphNode;
                if (node.isAxis()) {
                    axes.add(getMembers(node));
                } else {
                    getLineItems(node, lineItems);
                }
            }
        } else {
            getLineItems(root, lineItems);
        }

        return new PresentationHypercube(root, axes, lineItems);
    }

    private static boolean abstractOrHasTable(PresentationGraphNode node) {
        if (node.isAbstract())
            return true;

        return hasTable(node);
    }

    private static int countTables(PresentationGraphNode node) {
        long tableCount = node.getOutLinks().stream().filter(GraphNode::isTable).count();
        return (int)tableCount;
    }

    private static boolean hasTable(PresentationGraphNode node) {
        return countTables(node) > 0;
    }

    private static boolean allTables(PresentationGraphNode node) {
        return countTables(node) == node.getOutLinks().size();
    }

    private static String deiString(XbrlInstance instance) {
        Dei dei = instance.getDei();
        if (dei != null) {
            return dei.getEntityInformation() + ":" + dei.getDocumentInformation();
        }

        return "(null dei)";
    }

    private void collectHypercubes(PresentationGraphNode node, Collection<PresentationHypercube> hypercubes) {
        /* If node is a table, construct a hypercube from it. We are assuming tables don't contain other tables */
        if (node.isTable()) {
            PresentationHypercube hypercube = getHypercubeForTable(node);
            hypercubes.add(hypercube);
            return;
        }

        /* Node is not a table but may contain other tables, abstract children with tables or line items. Need to handle each differently */
        LineItems<PresentationArc, PresentationGraphNode> lineItems = new LineItems<>();

        for (GraphNode<PresentationArc> child : node.getOutLinks()) {
            PresentationGraphNode graphNode = (PresentationGraphNode)child;
            if (graphNode.isTable()) {
                PresentationHypercube hypercube = getHypercubeForTable(graphNode);
                hypercubes.add(hypercube);
            } else if (graphNode.isAbstract() && hasTable(graphNode)) {
                collectHypercubes(graphNode, hypercubes);
            } else {
                getLineItems(graphNode, lineItems);
            }
        }

        if (lineItems.size() > 0) {
            PresentationHypercube hypercube = new PresentationHypercube(node, lineItems);
            hypercubes.add(hypercube);
        }
    }

    private Collection<PresentationGraphNode> getHypercubes(PresentationGraphNode node) {
        List<PresentationGraphNode> hypercubes = new ArrayList<>();

        /* a table is a hypercube */
        if (node.isTable()) {
            /* hopefully, a table does not contain other tables */
            if (hasTable(node)) {
                log.info("In [{}], [{}] has table within a table", deiString(instance), node.getQualifiedName());
            }
            if (!node.hasAxis()) {
                log.info("In [{}], [{}] is a table without any axis", deiString(instance), node.getQualifiedName());
            }

            hypercubes.add(node);
            return hypercubes;
        }

        /* if node has no tables, treat node itself as a hypercube and return it */
        if (!hasTable(node)) {
            hypercubes.add(node);
            return hypercubes;
        }

        /* node has tables. each child becomes a separate hypercube */
        for (GraphNode<PresentationArc> child : node.getOutLinks()) {
            PresentationGraphNode graphNode = (PresentationGraphNode)child;
            if (graphNode.isTable()) {
                hypercubes.add(graphNode);
            }
            if (!graphNode.isTable()) {
                log.info("In [{}], [{}] is a child of [{}] with sibling tables but is not a table",
                        deiString(instance), graphNode.getQualifiedName(), node.getQualifiedName());
            }
        }

        return hypercubes;
    }

    private
    Collection<PresentationHypercube> getHypercubeDefinitionsV2(PresentationGraphNode root) {
        Collection<PresentationHypercube> definitions = new ArrayList<>();

        Collection<PresentationGraphNode> hypercubes = getHypercubes(root);
        for (PresentationGraphNode hypercube : hypercubes) {
            PresentationHypercube definition = getDefinitionForHypercube(hypercube);
            definitions.add(definition);
        }
        return definitions;
    }

    protected
    Collection<PresentationHypercube> getHypercubeDefinitions(PresentationGraphNode root) {
        /*
         * We can have one of the following variations:
         * - A root node containing one or more table nodes - most likely scenario
         * - A root node containing multiple children, one of which is a table
         * - A root node that is the table - Possible in older XBRL instances
         * - A root node with no tables and just line-items - Possible in older XBRL instances
         */
        log.debug("TableBasedFactCollector.getTableDefinitions(): [{}, {}:{}]", root.getConcept().getName(),
                root.getConcept().getSubstitutionGroup().getNamespacePrefix(),
                root.getConcept().getSubstitutionGroup().getName());

        List<PresentationHypercube> hypercubes = new ArrayList<>();
        collectHypercubes(root, hypercubes);
        return hypercubes;
    }

    protected void collectPresentationFacts(
            Collection<PresentationHypercube> tables, TimeOrdered<DimensionedFact> facts) {

        for (Hypercube<PresentationArc,PresentationGraphNode> table : tables) {
            Collection<Concept> reportableConcepts = new ArrayList<>();
            for (PresentationGraphNode node : table.getLineItems()) {
                reportableConcepts.add(node.getConcept());
            }

            for (Concept concept : reportableConcepts) {
                instance.getFactsForConcept(concept, table.getAxes(), facts);
            }
        }
    }
}
