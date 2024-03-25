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
import io.datanapis.xbrl.model.*;
import io.datanapis.xbrl.model.arc.FromToArc;
import io.datanapis.xbrl.model.link.DirectedAcyclicLink;
import io.datanapis.xbrl.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.*;

public abstract class GraphNode<ArcType extends FromToArc<ArcType>> implements Comparable<GraphNode<ArcType>> {
    private static final String ABSTRACT = "Abstract";
    private static final String TABLE = "Table";
    private static final String AXIS = "Axis";
    private static final String DOMAIN = "Domain";
    private static final String LINE_ITEMS = "LineItems";

    private static final Logger log = LoggerFactory.getLogger(GraphNode.class);

    /* The concept for this node */
    private final Concept concept;

    /* This is the arc from the parent. Meaning (arc == null (i.e. no parent)) || (arc.getTo() == this.concept) */
    private final ArcType arc;

    /* Parent of this node. null if this is the root */
    private GraphNode<ArcType> parent;

    /* The outLinks from this node */
    private final List<GraphNode<ArcType>> outLinks = new ArrayList<>();

    GraphNode(Concept concept, ArcType incoming) {
        this.concept = concept;
        this.arc = incoming;
        this.parent = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode<?> graphNode = (GraphNode<?>) o;
        return Objects.equals(concept, graphNode.concept) &&
                Objects.equals(parent, graphNode.parent) &&
                Objects.equals(arc, graphNode.arc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(concept, parent, arc);
    }

    @Override
    public String toString() {
        return concept.getQualifiedName();
    }

    public Concept getConcept() {
        return concept;
    }

    public String getQualifiedName() {
        return concept.getQualifiedName();
    }

    public String getSubstitutionGroup() {
        return concept.getSubstitutionGroup().getQualifiedName();
    }

    public ArcType getArc() {
        return arc;
    }

    public GraphNode<ArcType> getParent() {
        return parent;
    }

    @Override
    public int compareTo(GraphNode<ArcType> that) {
        return arc.compareTo(that.arc);
    }

    public boolean hasChildren() {
        return !outLinks.isEmpty();
    }

    public Collection<GraphNode<ArcType>> getOutLinks() {
        return outLinks;
    }

    void addOutLink(GraphNode<ArcType> graphNode) {
        // Insert in sorted order
        Utils.insert(outLinks, graphNode, Comparator.comparing(node -> node.getArc().getOrder()));
        graphNode.parent = this;
    }

    <NodeType extends GraphNode<ArcType>>
    void buildSubgraph(DiscoverableTaxonomySet dts, NodeBuilder<ArcType, NodeType> maker) {
        Collection<ArcType> arcs = this.getArc().getChildren();
        for (ArcType arc : arcs) {
            Concept target = dts.getConcept(arc.getTo().getHref());
            GraphNode<ArcType> node = maker.makeNode(target, arc);
            node.buildSubgraph(dts, maker);

            this.addOutLink(node);
        }
    }

    /**
     * Find all root nodes in link and return a collection of trees rooted at the root nodes.
     *
     * @param dts the taxonomy
     * @param link the relevant PresentationLink, CalculationLink and DefinitionLink
     * @param maker the factory for creating instances of type NodeType
     * @param <ArcType> the type of the arc contained within the link object i.e. PresentationArc, CalculationArc, DefinitionArc
     * @param <NodeType> the type of the node i.e. PresentationGraphNode, CalculationGraphNode, DefinitionGraphNode
     * @return a collection of trees rooted at the root nodes
     */
    static <ArcType extends FromToArc<ArcType>, NodeType extends GraphNode<ArcType>>
    Collection<NodeType> getRootNodes(DiscoverableTaxonomySet dts,
                                      DirectedAcyclicLink<ArcType> link,
                                      NodeBuilder<ArcType, NodeType> maker) {
        Map<Concept,NodeType> nodeMap = new HashMap<>();
        Collection<ArcType> arcs = link.getAllArcs();
        for (ArcType arc : arcs) {
            /* ignore arcs that have parents */
            if (arc.hasParent())
                continue;

            if (Objects.isNull(arc.getFrom())) {
                log.info("From attribute of an Arc is null! This is not valid according to the XBRL 2.1 specification. Ignoring Arc!");
                continue;
            }

            Concept source = dts.getConcept(arc.getFrom().getHref());
            if (source == null) {
                log.info("Source Concept is null [{}]", arc.getFrom().getHref());
                throw new NullPointerException("Source Concept is null");
            }
            NodeType root = nodeMap.computeIfAbsent(source, k -> maker.makeNode(k, null));

            Concept target = dts.getConcept(arc.getTo().getHref());
            if (target == null) {
                log.info("Target Concept is null [{}]", arc.getTo().getHref());
                throw new NullPointerException("Target Concept is null");
            }
            NodeType child = maker.makeNode(target, arc);
            child.buildSubgraph(dts, maker);

            root.addOutLink(child);
        }

        return fixRoots(nodeMap);
    }

    public boolean isAbstract() {
        /* A concept that ends with Abstract should ideally have been marked as an abstract concept */
        return getConcept().isAbstractConcept() || getConcept().getName().endsWith(ABSTRACT);
    }

    boolean hasAxis() {
        long axisCount = outLinks.stream().filter(GraphNode::isAxis).count();
        return axisCount > 0;
    }

    /* The following functions check if node is of a given type. Don't know if there is a better way than checking names */
    boolean isTable() {
        return getConcept().getName().endsWith(TABLE);
    }

    boolean isAxis() {
        return getConcept().getName().endsWith(AXIS);
    }

    boolean isDomain() {
        return getConcept().getName().endsWith(DOMAIN);
    }

    boolean isLineItems() {
        return getConcept().getName().endsWith(LINE_ITEMS);
    }

    static class ConnectAction<ArcType extends FromToArc<ArcType>> {
        final GraphNode<ArcType> node;
        final List<GraphNode<ArcType>> outLinks;

        ConnectAction(GraphNode<ArcType> node, Collection<GraphNode<ArcType>> outLinks) {
            this.node = node;
            this.outLinks = new ArrayList<>(outLinks);
        }
    }

    /**
     * Sometimes arcs in a link use different labels which creates a disconnected graph. This logic will attempt
     * to reconnect the graph. Only those nodes at the root level are candidates for reconnection.
     *
     * @param nodeMap root-level nodes indexed using their concept
     * @param <ArcType>
     * @param <NodeType>
     * @return
     */
    static <ArcType extends FromToArc<ArcType>, NodeType extends GraphNode<ArcType>>
    Collection<NodeType> fixRoots(Map<Concept,NodeType> nodeMap) {
        Collection<ConnectAction<ArcType>> connectList = new ArrayList<>();

        // Make a copy to avoid potential issues with making changes while iterating.
        List<NodeType> rootNodes = new ArrayList<>(nodeMap.values());

        // Iterate through the root nodes looking for potential connections. Any valid connection is recorded
        // in connectList for processing once all iteration is complete.
        for (GraphNode<ArcType> root : rootNodes) {
            fixRoots(root, nodeMap, connectList);
        }

        for (ConnectAction<ArcType> action : connectList) {
            for (GraphNode<ArcType> child : action.outLinks) {
                action.node.addOutLink(child);
            }
            nodeMap.remove(action.node.getConcept());
        }

        rootNodes = new ArrayList<>();
        for (NodeType node : nodeMap.values()) {
            if (node.getParent() == null) {
                rootNodes.add(node);
            }
        }

        return rootNodes;
    }

    static <ArcType extends FromToArc<ArcType>, NodeType extends GraphNode<ArcType>>
    void fixRoots(GraphNode<ArcType> parent, Map<Concept,NodeType> nodeMap, Collection<ConnectAction<ArcType>> connectList) {
        for (GraphNode<ArcType> child : parent.getOutLinks()) {
            Concept concept = child.getConcept();
            NodeType node = nodeMap.get(concept);
            if (node != null) {
                connectList.add(new ConnectAction<>(child, node.getOutLinks()));
            }

            // Recurse
            fixRoots(child, nodeMap, connectList);
        }
    }

    /**
     * Get all non-abstract concepts reachable from a collection of concepts.
     * Facts will be reported against non-abstract concepts.
     *
     * @param concepts collection of concepts that serve as the starting point
     */
    void getLeafConcepts(Collection<Concept> concepts) {
        Concept concept = getConcept();
        if (!concept.isAbstractConcept()) {
            concepts.add(concept);
        }

        if (hasChildren()) {
            for (GraphNode<ArcType> node : getOutLinks()) {
                node.getLeafConcepts(concepts);
            }
        }
    }

    interface NodeProcessor<ArcType extends FromToArc<ArcType>> {
        void node(int level, GraphNode<ArcType> node);
    }

    void displayNode(String prefix, int level, PrintWriter writer) {
        writer.printf("%s [%d][%s] = [%s] [%s] [%.2f]:\n",
                prefix, level, getConcept().getQualifiedName(),
                getConcept().getBalance().toString(), getArc().getArcrole().getArcroleURI(), getArc().getOrder());
    }

    void displayNetwork(Deque<GraphNode<ArcType>> parents, int level, PrintWriter writer, int maxDepth) {
        if (level > maxDepth)
            return;

        String prefix = " ".repeat(level * 4);
        Concept.Balance balance = getConcept().getBalance();

        if (getArc() == null) {
            writer.printf("%s [%d][%s] = [%s]:\n",
                    prefix, level, getConcept().getQualifiedName(), balance.toString());
        } else {
            displayNode(prefix, level, writer);
        }

        for (GraphNode<ArcType> node : this.getOutLinks()) {
            parents.push(node);
            node.displayNetwork(parents, level + 1, writer, maxDepth);
            parents.pop();
        }
    }

    public void displayNetwork(PrintWriter writer) {
        Deque<GraphNode<ArcType>> deque = new ArrayDeque<>();
        deque.push(this);
        displayNetwork(deque, 1, writer, Integer.MAX_VALUE);
    }
}
