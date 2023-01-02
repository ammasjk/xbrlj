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
import io.datanapis.xbrl.model.Axis;
import io.datanapis.xbrl.model.Concept;
import io.datanapis.xbrl.model.RoleType;
import io.datanapis.xbrl.model.arc.DefinitionArc;
import io.datanapis.xbrl.model.link.DefinitionLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        return definitionNode.isTable() || definitionNode.getArc().getArcrole().isAll();
    }

    private static boolean isAxis(GraphNode<DefinitionArc> definitionNode) {
        return definitionNode.isAxis() || definitionNode.getArc().getArcrole().isHypercubeDimension();
    }

    private static boolean isDomain(GraphNode<DefinitionArc> definitionNode) {
        return definitionNode.isDomain() ||
                definitionNode.getArc().getArcrole().isDimensionDomain() ||
                definitionNode.getArc().getArcrole().isDimensionDefault();
    }

    private static void collectRecursive(DefinitionGraphNode graphNode, Axis axis) {
        // Add this member.
        if (graphNode.getArc().getArcrole().isDomainMember()) {
            axis.addMember(graphNode.getConcept());
        }

        // Recurse through all children
        for (GraphNode<DefinitionArc> child : graphNode.getOutLinks()) {
            assert child instanceof DefinitionGraphNode;
            DefinitionGraphNode node = (DefinitionGraphNode) child;
            DefinitionTaxonomy.collectRecursive(node, axis);
        }
    }

    private static Axis getMembers(DefinitionGraphNode graphNode) {
        assert DefinitionTaxonomy.isAxis(graphNode);

        Axis members = new Axis(graphNode.getConcept());
        for (GraphNode<DefinitionArc> domain : graphNode.getOutLinks()) {
            assert DefinitionTaxonomy.isDomain(domain);
            for (GraphNode<DefinitionArc> node : domain.getOutLinks()) {
                assert node instanceof DefinitionGraphNode;
                DefinitionGraphNode member = (DefinitionGraphNode) node;
                DefinitionTaxonomy.collectRecursive(member, members);
            }
        }

        return members;
    }

    private static void getLineItems(DefinitionGraphNode node, LineItems<DefinitionArc,DefinitionGraphNode> lineItems) {
        Concept concept = node.getConcept();
        if (!concept.isAbstractConcept()) {
            lineItems.add(node);
        }

        if (node.hasChildren()) {
            for (GraphNode<DefinitionArc> outNode : node.getOutLinks()) {
                DefinitionGraphNode child = (DefinitionGraphNode)outNode;
                DefinitionTaxonomy.getLineItems(child, lineItems);
            }
        }
    }

    private static DefinitionHypercube getHypercubeDefinition(DefinitionGraphNode root) {
        /*
         * Unlike PresentationNetworks, Table definitions in DefinitionNetwork are more straightforward.
         * DefinitionNetworks will usually have a root node with multiple children one of which is the
         * table definition with axis, domains and members. The siblings of the table node are the line items
         * The structure is not as clean for PresentationNetworks - see getTableDefinitions() in TableBasedFactCollector
         */
        log.debug("DefinitionNetwork.getHypercubeDefinition(): [{}, {}:{}]", root.getConcept().getName(),
                root.getConcept().getSubstitutionGroup().getNamespacePrefix(),
                root.getConcept().getSubstitutionGroup().getName());

        Collection<Axis> axes = new ArrayList<>();
        LineItems<DefinitionArc,DefinitionGraphNode> lineItems = new LineItems<>();

        for (GraphNode<DefinitionArc> parent : root.getOutLinks()) {
            DefinitionGraphNode definitionNode = (DefinitionGraphNode)parent;
            if (DefinitionTaxonomy.isTable(definitionNode)) {
                log.debug("Table: [{}] -> [{}] -> [{}]", root.getQualifiedName(),
                        definitionNode.getArc().getArcrole().getArcroleURI(), definitionNode.getQualifiedName());
                for (GraphNode<DefinitionArc> node : definitionNode.getOutLinks()) {
                    DefinitionGraphNode axisNode = (DefinitionGraphNode)node;
                    assert DefinitionTaxonomy.isAxis(axisNode);
                    axes.add(DefinitionTaxonomy.getMembers(axisNode));
                }
            } else {
                log.debug("LineItem: [{}] -> [{}] -> [{}]", root.getQualifiedName(),
                        definitionNode.getArc().getArcrole().getArcroleURI(), definitionNode.getQualifiedName());
                DefinitionTaxonomy.getLineItems(definitionNode, lineItems);
            }
        }

        return new DefinitionHypercube(root, axes, lineItems);
    }

    public void displayNetwork(PrintWriter writer, RoleType roleType) {
        displayNetwork(DisplayStyle.TREE, writer, roleType);
    }

    public void displayNetwork(DisplayStyle style, PrintWriter writer, RoleType roleType) {
        displayNetwork(style, writer, roleType, Integer.MAX_VALUE);
    }

    public void displayNetwork(DisplayStyle style, PrintWriter writer, RoleType roleType, int maxDepth) {
        DefinitionLink definitionLink = roleType.getDefinitionLink();
        if (definitionLink == null)
            return;

        Collection<DefinitionGraphNode> graphNodes = DefinitionTaxonomy.getRootNodes(dts, definitionLink);
        if (graphNodes.size() == 0)
            return;

        if (style == DisplayStyle.TREE) {
            writer.println("Definition: [" + roleType.getDefinition() + "]");
        }
        for (DefinitionGraphNode node : graphNodes) {
            node.displayNetwork(style, writer, maxDepth);
            writer.println();
        }
    }

    public Collection<DefinitionHypercube> getHypercubeDefinitions(RoleType roleType) {
        /*
         * Definition networks usually have the following hierarchy where -> indicates a parent-child relationship
         * RoleType -> LineItems -> (Table|Abstract)*
         *
         * The Table nodes usually have Axis and Domain / Member definitions while the Abstract nodes have the LineItems
         * that need to be displayed
         */
        List<DefinitionHypercube> tables = new ArrayList<>();

        DefinitionLink definitionLink = roleType.getDefinitionLink();
        if (definitionLink == null)
            return tables;

        Collection<DefinitionGraphNode> rootNodes = DefinitionTaxonomy.getRootNodes(dts, definitionLink);
        if (rootNodes.size() == 0)
            return tables;

        /* TODO Check if the root nodes are LineItems and log appropriately - will be useful to handle variations */
        for (DefinitionGraphNode rootNode : rootNodes) {
            tables.add(DefinitionTaxonomy.getHypercubeDefinition(rootNode));
        }

        return tables;
    }
}
