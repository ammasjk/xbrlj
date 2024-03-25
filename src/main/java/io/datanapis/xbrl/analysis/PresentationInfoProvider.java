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

import io.datanapis.xbrl.model.Concept;
import io.datanapis.xbrl.model.DimensionedFact;
import io.datanapis.xbrl.model.ExplicitMember;

import java.util.List;

/**
 * Provides a mechanism to interrogate a presentation hypercube. Normally, there is a 1:1 correspondence between
 * a presentation hypercube and a presentation network. However, this isn't always the case. In the case where
 * a presentation network contains multiple presentation hypercubes, there will be a different PresentationInfoProvider
 * for each hypercube
 */
public interface PresentationInfoProvider {
    /**
     * Compare the position of two presentation graph nodes in this presentation hypercube. We do this by comparing
     * the arcs of the nodes. The arcs have a defined order
     *
     * @param lhs the left presentation graph node
     * @param rhs the right presentation graph node
     * @return -1, 0 or 1 depending on whether lhs occurs before, is the same as or occurs after rhs in
     * this presentation hypercube
     */
    int compare(PresentationGraphNode lhs, PresentationGraphNode rhs);

    /**
     * Compare the position of two axis in this presentation hypercube
     *
     * @param node the presentation node that lhsAxis and rhsAxis are dimensioning
     * @param lhsAxis the left axis
     * @param rhsAxis the right axis
     * @return -1, 0, or 1 depending on whether lhsAxis occurs before, is the same as or occurs after rhsAxis in
     * this presentation hypercube
     */
    int compare(PresentationGraphNode node, Concept lhsAxis, Concept rhsAxis);

    /**
     * Compare the position of two explicit members in this presentation hypercube.
     *
     * @param node the presentation node that lhs and rhs are dimensioning
     * @param lhs the left explicit member
     * @param rhs the right explicit member
     * @return -1, 0 or 1 depending on whether lhs occurs before, is the same as or occurs after rhs in this
     * presentation hypercube
     */
    int compare(PresentationGraphNode node, ExplicitMember lhs, ExplicitMember rhs);

    /**
     * Order a list of dimensioned facts by the presentation order of the network. All facts must belong to the same
     * concept
     *
     * @param node the presentation node that facts are representing
     * @param facts list of facts to order
     */
    void order(PresentationGraphNode node, List<DimensionedFact> facts);

    /**
     * Returns true or false depending on whether all members of dimensions are contained in this instance
     *
     * @param node the presentation node that dimensions is qualifying
     * @param dimensions the list of ExplicitMember's to consider
     * @return true or false depending on whether all members are contained or not
     */
    boolean contains(PresentationGraphNode node, List<ExplicitMember> dimensions);

    /**
     * Return the label for the given axis for the current presentation network. Usually used for TypedMembers.
     * Use getLabel for ExplicitMembers
     *
     * @param node the presentation node that axis is qualifying.
     * @param axis the axis whose label is to be returned
     * @return the label when available or the qualified name of the concept
     */
    String getAxisLabel(PresentationGraphNode node, Concept axis);

    /**
     * Return labels for the (axis, member) combination defined by pair for the current presentation network.
     *
     * @param node the presentation node that pair is qualifying
     * @param pair the (axis, member) pair whose labels are to be returned
     * @return a pair of labels corresponding to the axis and member
     */
    LabelPair getLabel(PresentationGraphNode node, ExplicitMember pair);

    /**
     * Return the line items in this info provider. When there are multiple hypercubes, the line items are combined
     * into a single list
     *
     * @return the line items in this info provider
     */
    List<PresentationGraphNode> getLineItems();

    /**
     * Return the nesting level of the last dimension in dimensions
     *
     * @param node the presentation node that dimensions is qualifying
     * @param dimensions the dimensions
     * @return the nesting level
     */
    int level(PresentationGraphNode node, List<ExplicitMember> dimensions);
}
