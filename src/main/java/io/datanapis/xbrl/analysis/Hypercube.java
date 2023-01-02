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
import io.datanapis.xbrl.model.arc.FromToArc;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Hypercube consists of a collection of Axis and LineItems. Axis may be empty and LineItems
 * could be just a single concept. Axis when present provide dimensions for the concepts
 * included within the LineItems. Dimensions qualify a fact and is represented by the class
 * ExplicitMember. For example, a dimension could specify the revenue for a segment of a business.
 * The Axis collection could include multiple of them in which case, we present the various
 * combinations of these axes for the different concepts.
 */
class Hypercube<ArcType extends FromToArc<ArcType>, NodeType extends GraphNode<ArcType>> {
    private final GraphNode<ArcType> root;
    private final Collection<Axis> axes;
    private final LineItems<ArcType,NodeType> lineItems;

    Hypercube(GraphNode<ArcType> root, LineItems<ArcType,NodeType> lineItems) {
        this.root = root;
        this.axes = new ArrayList<>();
        this.lineItems = lineItems;
    }

    Hypercube(GraphNode<ArcType> root, Collection<Axis> axes, LineItems<ArcType,NodeType> lineItems) {
        this.root = root;
        this.axes = axes;
        this.lineItems = lineItems;
    }

    GraphNode<ArcType> getRoot() {
        return root;
    }

    Collection<Axis> getAxes() {
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
}
