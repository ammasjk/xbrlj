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

public interface PresentationInfoProvider {
    /**
     * Order a list of dimensioned facts by the presentation order of the network
     *
     * @param facts list of facts to order
     */
    void order(List<DimensionedFact> facts);

    /**
     * Returns true or false depending on whether all members of dimensions are contained in this instance
     *
     * @param dimensions the list of ExplicitMember's to consider
     * @return true or false depending on whether all members are contained or not
     */
    boolean contains(List<ExplicitMember> dimensions);

    /**
     * Return the level of a member in the presentation hierarchy
     *
     * @param member the member whose level should be returned
     * @return the level of the member
     */
    int getLevel(ExplicitMember member);

    /**
     * Return the label for the given axis for the current presentation network.
     *
     * @param axis the axis whose label is to be returned
     * @return the label when available or the qualified name of the concept
     */
    String getAxisLabel(Concept axis);

    /**
     * Return labels for the (axis, member) combination defined by pair for the current presentation network.
     *
     * @param pair the (axis, member) pair whose labels are to be returned
     * @return a pair of labels corresponding to the axis and member
     */
    Pair<String,String> getLabel(ExplicitMember pair);
}
