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

import java.util.ArrayList;

public class FactList extends ArrayList<Fact> {
    public void sort() {
        this.sort(Fact::compare);
    }

    private void debugSort() {
        // Useful when debugging contract violated exceptions which can happen
        try {
            this.sort(Fact::compare);
        } catch (Exception e) {
            System.out.printf("Size: [%d]\n", this.size());
            for (Fact fact : this) {
                System.out.printf("%s\n", fact.toString());
            }
            throw e;
        }
    }
}
