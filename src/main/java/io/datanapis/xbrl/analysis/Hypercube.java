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

import io.datanapis.xbrl.model.Axis;
import io.datanapis.xbrl.model.Concept;
import io.datanapis.xbrl.model.arc.FromToArc;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hypercube consists of a root node (usually a table but not always one especially in the case of
 * implied hypercubes, e.g., an abstract node that contains just LineItems) and a collection of Axis and LineItems.
 * Axis may be empty and LineItems could be just a single concept. Axis, when present, provide dimensions for
 * the concepts included within the LineItems. Dimensions qualify a fact and is represented by the class
 * ExplicitMember. For example, a dimension could specify the revenue for a segment of a business.
 * The Axis collection could include multiple of them in which case, we present the various
 * combinations of these axes for the different concepts.
 */
class Hypercube<ArcType extends FromToArc<ArcType>, NodeType extends GraphNode<ArcType>, AxisType extends Hypercube.HypercubeAxis<ArcType,NodeType>> {
    private final GraphNode<ArcType> table;
    private final List<AxisType> axes;
    private final LineItems<ArcType,NodeType> lineItems;

    Hypercube(GraphNode<ArcType> table, LineItems<ArcType,NodeType> lineItems) {
        this.table = table;
        this.axes = new ArrayList<>();
        this.lineItems = lineItems;
    }

    Hypercube(GraphNode<ArcType> table, List<AxisType> axes, LineItems<ArcType,NodeType> lineItems) {
        this.table = table;
        this.axes = axes;
        this.lineItems = lineItems;
    }

    GraphNode<ArcType> getTable() {
        return table;
    }

    Concept getTableConcept() {
        return table.getConcept();
    }

    List<AxisType> getAxes() {
        return axes;
    }

    int nOfAxes() {
        return axes.size();
    }

    LineItems<ArcType,NodeType> getLineItems() {
        return lineItems;
    }

    int nOfItems() {
        return lineItems.size();
    }

    @Getter
    static class HypercubeAxis<ArcType extends FromToArc<ArcType>, NodeType extends GraphNode<ArcType>> {
        private final NodeType dimension;
        private List<NodeType> domains;
        @Setter
        private NodeType defaultDomain;
        private List<NodeType> members;

        HypercubeAxis(NodeType dimension) {
            this.dimension = dimension;
            this.domains = new ArrayList<>();
            this.members = new ArrayList<>();
        }

        boolean empty() {
            return domains.isEmpty() && members.isEmpty();
        }

        void updateDefinition(HypercubeAxis<ArcType, NodeType> axis) {
            assert empty();
            this.domains = axis.domains;
            this.members = axis.members;
            this.defaultDomain = axis.defaultDomain;
        }

        Axis toAxis() {
            Axis axis = new Axis(dimension.getConcept());
            if (defaultDomain != null) {
                axis.setDefaultDomain(defaultDomain.getConcept());
            }
            for (NodeType domain : domains) {
                axis.addDomain(domain.getConcept());
            }
            for (NodeType member : members) {
                axis.addMember(member.getConcept());
            }
            return axis;
        }

        boolean hasDefaultDomain() {
            return defaultDomain != null;
        }

        void addDomain(NodeType domain) {
            this.domains.add(domain);
        }

        void addMember(NodeType member) {
            members.add(member);
        }

        boolean hasMember(NodeType m) {
            for (NodeType member : members) {
                if (member.getConcept().equals(m.getConcept())) {
                    return true;
                }
            }
            return false;
        }

        Set<String> getMemberNames() {
            return members.stream().map(GraphNode::getQualifiedName).collect(Collectors.toSet());
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(dimension.getQualifiedName());
            if (defaultDomain != null) {
                builder.append("[").append(defaultDomain.getQualifiedName()).append("]");
            }
            builder.append("(");
            boolean first = true;
            for (NodeType member : members) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append(member.getQualifiedName());
                first = false;
            }
            builder.append(")");
            return builder.toString();
        }
    }
}
