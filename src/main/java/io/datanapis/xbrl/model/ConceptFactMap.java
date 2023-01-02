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
package io.datanapis.xbrl.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ConceptFactMap {
    private final Map<Concept,FactList> conceptFactListMap = new HashMap<>();

    public ConceptFactMap() {
    }

    public void clear() {
        conceptFactListMap.forEach((k, v) -> v.clear());
        conceptFactListMap.clear();
    }

    public void add(Fact fact) {
        FactList factList = conceptFactListMap.computeIfAbsent(fact.getConcept(), k -> new FactList());
        factList.add(fact);
    }

    public Collection<Fact> getFactsFor(Concept concept) {
        return conceptFactListMap.get(concept);
    }

    public void sort() {
        for (Map.Entry<Concept,FactList> entry : conceptFactListMap.entrySet()) {
            entry.getValue().sort();
        }
    }
}
