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

import java.util.HashMap;
import java.util.Map;

public class RoleLabelMap extends HashMap<String,Label> {
    public RoleLabelMap() {
    }

    public RoleLabelMap(RoleLabelMap other) {
        super(other);
    }

    public void addAll(RoleLabelMap other) {
        super.putAll(other);
    }

    public Label getLabel() {
        /* If map has only one entry, return that */
        if (this.size() == 1) {
            for (Map.Entry<String,Label> entry : this.entrySet()) {
                return entry.getValue();
            }
        }

        Label label = this.get(Label.ROLE_TYPE_LABEL);
        if (label != null)
            return label;

        label = this.get(Label.ROLE_TYPE_TERSE_LABEL);
        return label;
    }

    public Label getLabel(String roleType) {
        return this.get(roleType);
    }

    public Label getTerseLabel() {
        return this.get(Label.ROLE_TYPE_TERSE_LABEL);
    }

    public Label getDocumentation() {
        return this.get(Label.ROLE_TYPE_DOCUMENTATION);
    }
}
