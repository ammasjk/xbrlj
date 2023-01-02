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
import io.datanapis.xbrl.model.Location;
import io.datanapis.xbrl.model.arc.FromArc;

import java.util.*;
import java.util.stream.Collectors;

public abstract class BipartiteLink<T extends FromArc> extends AbstractLink {
    final Map<Location,T> arcs = new HashMap<>();

    public T getArc(Location location) {
        return arcs.getOrDefault(location, null);
    }

    public Collection<T> getAllArcs() {
        return arcs.values();
    }

    public List<T> getArcsOrdered() {
        List<T> list = new ArrayList<>(arcs.values());
        list.sort(Comparator.comparing(T::getOrder));
        return list;
    }

    public Set<ArcroleType> getArcTypes() {
        return arcs.values().stream().map(T::getArcrole).collect(Collectors.toSet());
    }

    void addArc(Location location, T arc) {
        arcs.put(location, arc);
    }
}
