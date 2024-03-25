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
import io.datanapis.xbrl.model.arc.CalculationArc;
import io.datanapis.xbrl.model.link.CalculationLink;
import io.datanapis.xbrl.model.link.DefinitionLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Deque;

public class CalculationTaxonomy {
    private static final Logger log = LoggerFactory.getLogger(CalculationTaxonomy.class);
    private final DiscoverableTaxonomySet dts;

    public CalculationTaxonomy(DiscoverableTaxonomySet dts) {
        this.dts = dts;
    }

    public static Collection<CalculationGraphNode> getRootNodes(DiscoverableTaxonomySet dts, CalculationLink link) {
        return GraphNode.getRootNodes(dts, link, CalculationGraphNode::new);
    }

    public void walk(RoleType roleType, CalculationNetworkConsumer consumer) {
        CalculationLink calculationLink = roleType.getCalculationLink();
        if (calculationLink == null)
            return;

        Collection<CalculationGraphNode> graphNodes = CalculationTaxonomy.getRootNodes(dts, calculationLink);
        if (graphNodes.isEmpty())
            return;

        consumer.start(roleType);
        for (CalculationGraphNode node : graphNodes) {
            consumer.rootStart(node);
            node.walk(consumer);
            consumer.rootEnd(node);
        }
        consumer.end(roleType);
    }

    public static class WriterConsumer implements CalculationNetworkConsumer {
        private final PrintWriter writer;

        public WriterConsumer(PrintWriter writer) {
            this.writer = writer;
        }

        @Override
        public void start(RoleType roleType) {
            writer.println("Definition: [" + roleType.getDefinition() + "]");
        }

        @Override
        public void rootStart(CalculationGraphNode root) {
            String prefix = " ".repeat(4);
            writer.printf("%s [%s] = [%s]:\n",
                    prefix, root.getConcept().getQualifiedName(), root.getConcept().getBalance().toString());
        }

        @Override
        public void rootEnd(CalculationGraphNode root) {
            writer.println();
        }

        @Override
        public void nodeStart(CalculationGraphNode node, Deque<CalculationGraphNode> path) {
            String prefix = " ".repeat((path.size() + 1) * 4);
            CalculationArc arc = node.getArc();
            writer.printf("%s [%s] = [%s] [%4.1f] [%s] [%.2f]:\n",
                    prefix, node.getConcept().getQualifiedName(), node.getConcept().getBalance().toString(),
                    arc.getWeight(), arc.getArcrole().getArcroleURI(), arc.getOrder());
        }
    }
}
