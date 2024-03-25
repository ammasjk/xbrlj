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

import io.datanapis.xbrl.DiscoverableTaxonomySet;
import io.datanapis.xbrl.SubstitutionGroup;
import io.datanapis.xbrl.model.Concept;
import io.datanapis.xbrl.model.RoleType;
import io.datanapis.xbrl.model.arc.DefinitionArc;
import io.datanapis.xbrl.model.link.DefinitionLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.*;

public class DefinitionTaxonomy {
    private static final Logger log = LoggerFactory.getLogger(DefinitionNetwork.class);
    protected final DiscoverableTaxonomySet dts;

    public DefinitionTaxonomy(DiscoverableTaxonomySet dts) {
        this.dts = dts;
    }

    public static Collection<DefinitionGraphNode> getRootNodes(DiscoverableTaxonomySet dts, DefinitionLink link) {
        return GraphNode.getRootNodes(dts, link, DefinitionGraphNode::new);
    }

    private static boolean isTable(GraphNode<DefinitionArc> definitionNode) {
        boolean result = definitionNode.getArc().getArcrole().isAll() || definitionNode.getArc().getArcrole().isNotAll() || definitionNode.isTable();
        if (result) {
            if (definitionNode.getArc().getArcrole().isNotAll()) {
                log.error("Definition arc role is notAll which is not handled correctly!");
            }
            Concept hypercubeItem = definitionNode.getConcept();
            if (!SubstitutionGroup.XBRLDT_HYPERCUBE_ITEM.equals(hypercubeItem.getSubstitutionGroup())) {
                log.info("Definition node [{}] is a table but does not belong to the substitutionGroup [xbrldt:hypercubeItem]", hypercubeItem);
            }
        }
        return result;
    }

    private static boolean isAxis(GraphNode<DefinitionArc> definitionNode) {
        boolean result = definitionNode.getArc().getArcrole().isHypercubeDimension() || definitionNode.isAxis();
        if (result) {
            Concept axisConcept = definitionNode.getConcept();
            if (!SubstitutionGroup.XBRLDT_DIMENSION_ITEM.equals(axisConcept.getSubstitutionGroup())) {
                log.info("Definition node [{}] is an axis but does not belong to the substitutionGroup [xbrldt:dimensionItem]", axisConcept);
            }
        }
        return result;
    }

    private static boolean isDomain(GraphNode<DefinitionArc> definitionNode) {
        return definitionNode.getArc().getArcrole().isDimensionDomain() ||
                definitionNode.getArc().getArcrole().isDimensionDefault() ||
                definitionNode.isDomain();
    }

    private static void collectRecursive(DefinitionGraphNode graphNode, DefinitionHypercube.DefinitionAxis definitionAxis) {
        // Add this member.
        if (graphNode.getArc().getArcrole().isDomainMember()) {
            definitionAxis.addMember(graphNode);
        }

        // Recurse through all children
        for (GraphNode<DefinitionArc> child : graphNode.getOutLinks()) {
            if (child instanceof DefinitionGraphNode node) {
                DefinitionTaxonomy.collectRecursive(node, definitionAxis);
            } else {
                log.error("graphNode.getOutLinks() contains an object that is not a DefinitionGraphNode");
            }
        }
    }

    private static DefinitionHypercube.DefinitionAxis collectMembers(DefinitionGraphNode graphNode) {
        DefinitionHypercube.DefinitionAxis definitionAxis = new DefinitionHypercube.DefinitionAxis(graphNode);

        for (GraphNode<DefinitionArc> node : graphNode.getOutLinks()) {
            DefinitionGraphNode domain = (DefinitionGraphNode) node;
            assert DefinitionTaxonomy.isDomain(domain);
            if (domain.getArc().getArcrole().isDimensionDefault()) {
                definitionAxis.setDefaultDomain(domain);
                continue;
            }
            definitionAxis.addDomain(domain);
            for (GraphNode<DefinitionArc> memberNode : domain.getOutLinks()) {
                assert memberNode instanceof DefinitionGraphNode;
                DefinitionGraphNode member = (DefinitionGraphNode) memberNode;
                DefinitionTaxonomy.collectRecursive(member, definitionAxis);
            }
        }

        return definitionAxis;
    }

    private static void collectLineItems(DefinitionGraphNode node, LineItems<DefinitionArc,DefinitionGraphNode> lineItems) {
        Concept concept = node.getConcept();
        if (!concept.isAbstractConcept()) {
            lineItems.add(node);
        }

        if (node.hasChildren()) {
            for (GraphNode<DefinitionArc> outNode : node.getOutLinks()) {
                DefinitionGraphNode child = (DefinitionGraphNode)outNode;
                DefinitionTaxonomy.collectLineItems(child, lineItems);
            }
        }
    }

    private static DefinitionHypercube getHypercubeDefinition(DefinitionGraphNode root) {
        /*
         * Unlike PresentationNetworks, Table definitions in DefinitionNetwork are more straightforward.
         * DefinitionNetworks will usually have a root node with multiple children one of which is the
         * table definition with axis aka dimensions, domains and members. The siblings of the table node
         * are the line items. The line items are the target of a domain-member arc as well even though
         * they are not part of any domain.
         *
         * See PresentationDataProvider.getHypercubeDefinitions() for the structure of PresentationNetworks.
         */
        log.debug("DefinitionNetwork.getHypercubeDefinition(): [{}, {}]",
                root.getQualifiedName(), root.getSubstitutionGroup());

        List<DefinitionHypercube.DefinitionAxis> axes = new ArrayList<>();
        List<DefinitionGraphNode> tables = new ArrayList<>();
        LineItems<DefinitionArc,DefinitionGraphNode> lineItems = new LineItems<>();

        for (GraphNode<DefinitionArc> node : root.getOutLinks()) {
            DefinitionGraphNode definitionNode = (DefinitionGraphNode)node;
            if (DefinitionTaxonomy.isTable(definitionNode)) {
                log.debug("Table: [{}:{}] -> [{}] -> [{}:{}]",
                        root.getQualifiedName(), root.getSubstitutionGroup(),
                        definitionNode.getArc().getArcrole().getArcroleURI(),
                        definitionNode.getQualifiedName(), definitionNode.getSubstitutionGroup());
                tables.add(definitionNode);

                for (GraphNode<DefinitionArc> child : definitionNode.getOutLinks()) {
                    DefinitionGraphNode axisNode = (DefinitionGraphNode)child;
                    if (DefinitionTaxonomy.isAxis(axisNode)) {
                        axes.add(DefinitionTaxonomy.collectMembers(axisNode));
                    } else if (axisNode.getArc().getArcrole().isGeneralSpecial()) {
                        /* Special case, e.g., IMAX FY 2017 */
                        log.info("Ignoring general-special arc");
                    } else {
                        log.info("Ignoring [{}] arc", axisNode.getArc().getArcrole());
                    }
                }
            } else {
                log.debug("LineItem: [{}:{}] -> [{}] -> [{}:{}]",
                        root.getQualifiedName(), root.getSubstitutionGroup(),
                        definitionNode.getArc().getArcrole().getArcroleURI(),
                        definitionNode.getQualifiedName(), definitionNode.getSubstitutionGroup());

                DefinitionTaxonomy.collectLineItems(definitionNode, lineItems);
            }
        }

        if (tables.isEmpty()) {
            /*
             * This just means we don't have any tables and therefore no axes/dimensions. Just line items.
             */
            log.info("DefinitionNetwork.getHypercubeDefinition(): Found zero table nodes");
            return new DefinitionHypercube(root, axes, lineItems);
        } else if (tables.size() > 1) {
            /* TODO We are not currently handling this. This pattern probably occurs in the data. Fortunately, we are not relying on DefinitionHypercubes as much */
            log.error("DefinitionNetwork.getHypercubeDefinition(): Expected exactly one table, found {}", tables.size());
        }

        /* Expecting the common case to be tables.size() == 1 which will be the actual table */
        return new DefinitionHypercube(tables.get(0), axes, lineItems);
    }

    private static boolean equivalent(Set<String> first, Set<String> second) {
        return first.containsAll(second) && second.containsAll(first);
    }

    public Collection<DefinitionHypercube> getHypercubeDefinitions(RoleType roleType) {
        /*
         * Definition networks usually have the following hierarchy where -> indicates a parent-child relationship
         * RoleType -> LineItems -> (Table|Abstract)*
         *
         * The Table nodes usually have Axis and Domain / Member definitions while the Abstract nodes have the LineItems
         * that need to be displayed
         */
        DefinitionLink definitionLink = roleType.getDefinitionLink();
        if (definitionLink == null)
            return null;

        Collection<DefinitionGraphNode> rootNodes = DefinitionTaxonomy.getRootNodes(dts, definitionLink);
        if (rootNodes.isEmpty())
            return null;

        List<DefinitionHypercube> definitionHypercubes = new ArrayList<>();
        for (DefinitionGraphNode rootNode : rootNodes) {
            definitionHypercubes.add(DefinitionTaxonomy.getHypercubeDefinition(rootNode));
        }

        /*
         * Definition networks sometimes reuse axis definitions implicitly - this usually happens when a network contains
         * multiple hypercubes and the one of the hypercubes implicitly refers to the axis definition in another
         * hypercube by using the axis but by not defining it. This shows up as an empty axis in a hypercube when
         * the intent is to reuse the other definition. After constructing all hypercubes, we are collecting all
         * axis definitions that are not empty and then updating the existing empty definitions with the non-empty ones
         */
        Map<Concept, DefinitionHypercube.DefinitionAxis> axisMap = new HashMap<>();
        for (DefinitionHypercube hypercube : definitionHypercubes) {
            for (DefinitionHypercube.DefinitionAxis definitionAxis : hypercube.getAxes()) {
                Concept axisConcept = definitionAxis.getDimension().getConcept();
                if (!definitionAxis.empty()) {
                    DefinitionHypercube.DefinitionAxis existing = axisMap.putIfAbsent(axisConcept, definitionAxis);
                    if (Objects.nonNull(existing)) {
                        if (equivalent(definitionAxis.getMemberNames(), existing.getMemberNames())) {
                            log.debug("Potentially duplicate definitions for axis: [{}] - [{}] vs [{}]",
                                    axisConcept, existing, definitionAxis);
                        } else {
                            log.info("Potentially inconsistent definitions for axis: [{}] - [{}] vs [{}]",
                                    axisConcept, existing, definitionAxis);
                        }
                    }
                }
            }
        }
        for (DefinitionHypercube hypercube : definitionHypercubes) {
            for (DefinitionHypercube.DefinitionAxis definitionAxis : hypercube.getAxes()) {
                if (definitionAxis.empty()) {
                    Concept axisConcept = definitionAxis.getDimension().getConcept();
                    DefinitionHypercube.DefinitionAxis pa = axisMap.get(axisConcept);
                    if (Objects.nonNull(pa)) {
                        definitionAxis.updateDefinition(pa);
                    }
                }
            }
        }

        return definitionHypercubes;
    }

    public void walk(RoleType roleType, DefinitionNetworkConsumer consumer) {
        DefinitionLink definitionLink = roleType.getDefinitionLink();
        if (definitionLink == null)
            return;

        Collection<DefinitionGraphNode> graphNodes = DefinitionTaxonomy.getRootNodes(dts, definitionLink);
        if (graphNodes.isEmpty())
            return;

        consumer.start(roleType);
        for (DefinitionGraphNode node : graphNodes) {
            consumer.rootStart(node);
            node.walk(consumer);
            consumer.rootEnd(node);
        }
        consumer.end(roleType);
    }

    public static class WriterConsumer implements DefinitionNetworkConsumer {
        private final PrintWriter writer;

        public WriterConsumer(PrintWriter writer) {
            this.writer = writer;
        }

        @Override
        public void start(RoleType roleType) {
            writer.println("Definition: [" + roleType.getDefinition() + "]");
        }

        @Override
        public void rootStart(DefinitionGraphNode root) {
            String prefix = " ".repeat(4);
            writer.printf("%s [%s] = [%s]:\n",
                    prefix, root.getConcept().getQualifiedName(), root.getConcept().getBalance().toString());
        }

        @Override
        public void rootEnd(DefinitionGraphNode root) {
            writer.println();
        }

        @Override
        public void nodeStart(DefinitionGraphNode node, Deque<DefinitionGraphNode> path) {
            String prefix = " ".repeat((path.size() + 1) * 4);
            writer.printf("%s [%s] = [%s] [%s] [%.2f]:\n",
                    prefix, node.getConcept().getQualifiedName(), node.getConcept().getBalance().toString(),
                    node.getArc().getArcrole().getArcroleURI(), node.getArc().getOrder());
        }
    }
}
