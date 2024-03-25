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
import io.datanapis.xbrl.model.arc.PresentationArc;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;

public class PresentationGraphNode extends GraphNode<PresentationArc> {
    PresentationGraphNode(Concept concept, PresentationArc arc) {
        super(concept, arc);
    }

    void displayNode(String prefix, int level, PrintWriter writer) {
        writer.printf("%s [%d][%s][%s]  =  [%s] [%s] [%.2f]:\n",
                prefix, level, getConcept().getQualifiedName(), getArc().getPreferredLabelType(),
                getConcept().getBalance().toString(), getArc().getArcrole().getArcroleURI(), getArc().getOrder());
    }

    /**
     * Negative flag was added in case we need to adjust the label depending on whether the fact value is negative or not.
     * However, it is not currently used.
     *
     * @param negative should the negative version of the label be used. Not used currently
     * @return Returns the label for this concept or the qualified name of the concept if the concept does not have any labels
     */
    private String conceptLabel(boolean negative) {
        Label label = getConcept().getLabel();
        if (label != null)
            return label.getValue();

        return getConcept().getQualifiedName();
    }

    public String getLabel() {
        return getLabel(false);
    }

    public String getLabel(boolean negative) {
        if (getArc() == null || getArc().getPreferredLabel() == null) {
            return conceptLabel(negative);
        }

        Label label = getConcept().getLabel(getArc().getPreferredLabel());
        if (label != null) {
            return label.getValue();
        }

        return conceptLabel(negative);
    }

    public boolean isNegated() {
        /*
         * Negated is only relevant in a presentation context. So, only consider preferred labels when determining
         * if the value should be negated
         */
        if (getArc() == null || getArc().getPreferredLabel() == null)
            return false;

        Label label = getConcept().getLabel(getArc().getPreferredLabel());
        if (label != null)
            return label.isNegated();

        return false;
    }

    public boolean isTotal() {
        /*
         * Negated is only relevant in a presentation context. So, only consider preferred labels when determining
         * if the value should be negated
         */
        if (getArc() == null || getArc().getPreferredLabel() == null)
            return false;

        Label label = getConcept().getLabel(getArc().getPreferredLabel());
        if (label != null)
            return label.isTotal();

        return false;
    }

    void walk(PresentationNetworkConsumer consumer, Deque<PresentationGraphNode> path) {
        for (GraphNode<PresentationArc> graphNode : this.getOutLinks()) {
            PresentationGraphNode node = (PresentationGraphNode)graphNode;
            consumer.nodeStart(node, path);
            if (node.hasChildren()) {
                path.push(node);
                node.walk(consumer, path);
                path.pop();
            }
            consumer.nodeEnd(node, path);
        }
    }

    public void walk(PresentationNetworkConsumer consumer) {
        Deque<PresentationGraphNode> path = new ArrayDeque<>();
        path.push(this);
        walk(consumer, path);
    }
}
