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
package io.datanapis.xbrl.model.link;

import io.datanapis.xbrl.model.ArcroleType;
import io.datanapis.xbrl.model.arc.FromToArc;

import java.util.*;

public abstract class DirectedAcyclicLink<ArcType extends FromToArc<ArcType>> extends AbstractLink {
    final List<ArcType> arcs = new ArrayList<>();

    public void merge(DirectedAcyclicLink<ArcType> other) {
        /* merge the AbstractLink */
        super.merge(other);

        /* Construct a map of the existing arcs so we can check the new arcs against this */
        Map<ArcType,ArcType> arcMap = new HashMap<>();
        for (ArcType arc : arcs) {
            arcMap.put(arc, arc);
        }

        for (ArcType arc : other.arcs) {
            ArcType value = arcMap.get(arc);
            /* Net new arc */
            if (value == null) {
                arcs.add(arc);
                continue;
            }

            /* arc potentially already exists in arcs */
            log.debug("Ignoring potentially duplicate arc [{} -> {}]. Existing arc [{} -> {}]",
                    arc.getFrom().getLabel(), arc.getTo().getLabel(),
                    value.getFrom().getLabel(), value.getTo().getLabel());
        }
    }

    public Collection<ArcType> getAllArcs() {
        return arcs;
    }

    public List<ArcType> getArcsOrdered() {
        List<ArcType> list = new ArrayList<>(arcs);
        list.sort(Comparator.comparing(ArcType::getOrder));
        return list;
    }

    public Set<ArcroleType> getArcTypes() {
        Set<ArcroleType> arcroleTypes = new HashSet<>();
        for (ArcType arc : arcs) {
            arcroleTypes.add(arc.getArcrole());
        }
        return arcroleTypes;
    }

    void addArc(ArcType arc) {
        arcs.add(arc);
    }
}
