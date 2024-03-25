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

import io.datanapis.xbrl.model.arc.DefinitionArc;

import java.util.List;

public class DefinitionHypercube extends Hypercube<DefinitionArc, DefinitionGraphNode, DefinitionHypercube.DefinitionAxis> {
    DefinitionHypercube(DefinitionGraphNode table, LineItems<DefinitionArc, DefinitionGraphNode> lineItems) {
        super(table, lineItems);
    }

    DefinitionHypercube(DefinitionGraphNode table, List<DefinitionAxis> axes, LineItems<DefinitionArc, DefinitionGraphNode> lineItems) {
        super(table, axes, lineItems);
    }

    static class DefinitionAxis extends HypercubeAxis<DefinitionArc,DefinitionGraphNode> {
        DefinitionAxis(DefinitionGraphNode dimension) {
            super(dimension);
        }
    }
}
