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

public class PresentationTaxonomy {
    private static final Logger log = LoggerFactory.getLogger(PresentationTaxonomy.class);
    private final DiscoverableTaxonomySet dts;

    public PresentationTaxonomy(DiscoverableTaxonomySet dts) {
        this.dts = dts;
    }

    public static Collection<PresentationGraphNode> getRootNodes(DiscoverableTaxonomySet dts, PresentationLink link) {
        return GraphNode.getRootNodes(dts, link, PresentationGraphNode::new);
    }

    public void displayNetwork(PrintWriter writer, RoleType roleType) {
        displayNetwork(writer, roleType, Integer.MAX_VALUE);
    }

    public void displayNetwork(PrintWriter writer, RoleType roleType, int maxDepth) {
        PresentationLink presentationLink = roleType.getPresentationLink();
        if (presentationLink == null)
            return;

        Collection<PresentationGraphNode> graphNodes = PresentationTaxonomy.getRootNodes(dts, presentationLink);
        if (graphNodes.size() == 0)
            return;

        writer.println("Presentation: [" + roleType.getDefinition() + "]");
        for (PresentationGraphNode node : graphNodes) {
            node.displayNetwork(writer);
            writer.println();
        }
    }
}
