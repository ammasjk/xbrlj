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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.function.Predicate;

public class Counter<T> {
    private final Set<T> ignored;
    private final HashMap<T,Integer> counter = new HashMap<>();

    public Counter() {
        ignored = null;
    }

    public Counter(Set<T> ignored) {
        this.ignored = ignored;
    }

    public int size() {
        return counter.size();
    }

    public Map<T,Integer> map() {
        return counter;
    }

    public void clear() {
        if (ignored != null)
            ignored.clear();
        counter.clear();
    }

    private boolean consider(T key) {
        return (ignored == null || !ignored.contains(key));
    }

    private void add(T key, int value) {
        if (consider(key)) {
            counter.merge(key, value, Integer::sum);
        }
    }

    public void add(T key) {
        add(key, 1);
    }

    public void subtract(T key) {
        add(key, -1);
    }

    public void add(Collection<T> keys, int value) {
        for (T key : keys) {
            add(key, value);
        }
    }

    public void subtract(Collection<T> keys, int value) {
        for (T key : keys) {
            add(key, -value);
        }
    }

    public void add(Counter<T> other) {
        for (Map.Entry<T,Integer> entry : other.getEntries()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    public void subtract(Counter<T> other) {
        for (Map.Entry<T,Integer> entry : other.getEntries()) {
            add(entry.getKey(), -entry.getValue());
        }
    }

    public List<Map.Entry<T,Integer>> getEntries() {
        ArrayList<Map.Entry<T,Integer>> entries = new ArrayList<>(counter.entrySet());
        return entries;
    }

    public List<Map.Entry<T,Integer>> getEntriesSorted() {
        ArrayList<Map.Entry<T,Integer>> entries = new ArrayList<>(counter.entrySet());
        entries.sort((lhs, rhs) -> Integer.compare(rhs.getValue(), lhs.getValue()));
        return entries;
    }

    public JsonElement asJson(Predicate<Map.Entry<T,Integer>> predicate) {
        JsonArray array = new JsonArray();
        for (Map.Entry<T,Integer> entry : getEntriesSorted()) {
            if (predicate.test(entry)) {
                JsonObject object = new JsonObject();
                object.addProperty("name", entry.getKey().toString());
                object.addProperty("value", entry.getValue());
                array.add(object);
            }
        }
        return array;
    }
}
