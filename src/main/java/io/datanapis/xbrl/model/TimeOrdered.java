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

import java.util.*;
import java.util.function.Predicate;

public final class TimeOrdered<T> extends TreeMap<Period,Collection<T>> {
    public TimeOrdered() {
    }

    public void add(Period key, T element) {
        Collection<T> value = this.computeIfAbsent(key, k -> new ArrayList<>());
        value.add(element);
    }

    public TimeOrdered<T> filter(Predicate<T> predicate) {
        TimeOrdered<T> filteredMap = new TimeOrdered<>();
        for (Map.Entry<Period,Collection<T>> entry : this.entrySet()) {
            List<T> l = new ArrayList<>();
            for (T element : entry.getValue()) {
                if (predicate.test(element)) {
                    l.add(element);
                }
            }
            if (l.size() > 0) {
                filteredMap.put(entry.getKey(), l);
            }
        }

        return filteredMap;
    }
}