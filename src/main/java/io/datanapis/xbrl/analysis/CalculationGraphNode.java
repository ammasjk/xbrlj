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

import io.datanapis.xbrl.model.*;
import io.datanapis.xbrl.model.arc.CalculationArc;
import io.datanapis.xbrl.model.arc.DefinitionArc;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;

public class CalculationGraphNode extends GraphNode<CalculationArc> {
    CalculationGraphNode(Concept concept, CalculationArc arc) {
        super(concept, arc);
    }

    void displayNode(String prefix, PrintWriter writer) {
        CalculationArc arc = getArc();
        writer.printf("%s [%s] = [%s] [%4.1f] [%s] [%.2f]:\n",
                prefix, getConcept().getQualifiedName(), getConcept().getBalance().toString(),
                arc.getWeight(), arc.getArcrole().getArcroleURI(), arc.getOrder());
    }

    void walk(CalculationNetworkConsumer consumer, Deque<CalculationGraphNode> path) {
        for (GraphNode<CalculationArc> graphNode : this.getOutLinks()) {
            CalculationGraphNode node = (CalculationGraphNode)graphNode;
            consumer.nodeStart(node, path);
            if (node.hasChildren()) {
                path.push(node);
                node.walk(consumer, path);
                path.pop();
            }
            consumer.nodeEnd(node, path);
        }
    }

    public void walk(CalculationNetworkConsumer consumer) {
        Deque<CalculationGraphNode> path = new ArrayDeque<>();
        path.push(this);
        walk(consumer, path);
    }
}
