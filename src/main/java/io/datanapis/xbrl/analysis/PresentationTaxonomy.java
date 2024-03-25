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

import io.datanapis.xbrl.DiscoverableTaxonomySet;
import io.datanapis.xbrl.model.RoleType;
import io.datanapis.xbrl.model.link.PresentationLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Deque;
import java.util.function.BiConsumer;

public class PresentationTaxonomy {
    private static final Logger log = LoggerFactory.getLogger(PresentationTaxonomy.class);
    private final DiscoverableTaxonomySet dts;

    public PresentationTaxonomy(DiscoverableTaxonomySet dts) {
        this.dts = dts;
    }

    public static Collection<PresentationGraphNode> getRootNodes(DiscoverableTaxonomySet dts, PresentationLink link) {
        return GraphNode.getRootNodes(dts, link, PresentationGraphNode::new);
    }

    public void walk(RoleType roleType, PresentationNetworkConsumer consumer) {
        PresentationLink presentationLink = roleType.getPresentationLink();
        if (presentationLink == null)
            return;

        Collection<PresentationGraphNode> graphNodes = PresentationTaxonomy.getRootNodes(dts, presentationLink);
        if (graphNodes.isEmpty())
            return;

        consumer.start(roleType);
        for (PresentationGraphNode node : graphNodes) {
            consumer.rootStart(node);
            node.walk(consumer);
            consumer.rootEnd(node);
        }
        consumer.end(roleType);
    }

    public static class WriterConsumer implements PresentationNetworkConsumer {
        private final PrintWriter writer;

        public WriterConsumer(PrintWriter writer) {
            this.writer = writer;
        }

        @Override
        public void start(RoleType roleType) {
            writer.println("Presentation: [" + roleType.getDefinition() + "]");
        }

        @Override
        public void rootStart(PresentationGraphNode root) {
            String prefix = " ".repeat(4);
            writer.printf("%s [%s] = [%s]:\n",
                    prefix, root.getConcept().getQualifiedName(), root.getConcept().getBalance().toString());
        }

        @Override
        public void rootEnd(PresentationGraphNode root) {
            writer.println();
        }

        @Override
        public void nodeStart(PresentationGraphNode node, Deque<PresentationGraphNode> path) {
            String prefix = " ".repeat((path.size() + 1) * 4);
            writer.printf("%s [%s]  [%s]  =  [%s] [%s] [%.2f]:\n",
                    prefix, node.getConcept().getQualifiedName(), node.getArc().getPreferredLabelType(),
                    node.getConcept().getBalance().toString(), node.getArc().getArcrole().getArcroleURI(), node.getArc().getOrder());
        }
    }
}
