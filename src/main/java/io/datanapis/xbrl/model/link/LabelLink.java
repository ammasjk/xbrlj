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
package io.datanapis.xbrl.model.link;

import io.datanapis.xbrl.DiscoverableTaxonomySet;
import io.datanapis.xbrl.TagNames;
import io.datanapis.xbrl.model.Label;
import io.datanapis.xbrl.model.Location;
import io.datanapis.xbrl.model.RoleLabelMap;
import io.datanapis.xbrl.model.arc.LabelArc;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.*;

public final class LabelLink extends BipartiteLink<LabelArc> {
    private static final Set<String> LABEL_LINK_ATTRIBUTES = new HashSet<>();
    static {
        LABEL_LINK_ATTRIBUTES.addAll(ABSTRACT_LINK_ATTRIBUTES);
    }

    /* For a sample, see LabelArc. LabelLinks are usually long since they contain all labels in an instance */
    private final Map<String, RoleLabelMap> labels = new HashMap<>();

    public RoleLabelMap getLabel(String label) {
        RoleLabelMap map = labels.get(label);
        return map;
    }

    public Label getLabel(String label, String role) {
        RoleLabelMap map = labels.get(label);
        if (map == null)
            return null;

        return map.get(role);
    }

    public RoleLabelMap getLabel(Location from) {
        LabelArc arc = getArc(from);
        if (arc == null)
            return null;

        return arc.getTo();
    }

    private void addLabel(Label label) {
        RoleLabelMap map = labels.computeIfAbsent(label.getLabel(), k -> new RoleLabelMap());
        map.put(label.getRole(), label);
    }

    public static LabelLink fromElement(String sourceUrl, DiscoverableTaxonomySet dts, Element element) {
        LabelLink link = new LabelLink();

        link.readElement(dts, element);

        //
        // Process location and label tags first
        //
        for (Element child : element.elements()) {
            String childName = child.getName();
            switch (childName) {
                case TagNames.LOC_TAG:
                    Location location = Location.fromElement(sourceUrl, child);
                    link.locations.put(location.getLabel(), location);
                    break;
                case TagNames.LABEL_TAG:
                    Label label = Label.fromElement(sourceUrl, child);
                    link.addLabel(label);
                    break;
            }
        }

        //
        // Process label arcs last
        //
        for (Element child : element.elements()) {
            String childName = child.getName();
            switch (childName) {
                case TagNames.LOC_TAG:
                case TagNames.LABEL_TAG:
                    break;
                case TagNames.LABEL_ARC_TAG:
                    LabelArc arc = LabelArc.fromElement(dts, sourceUrl, link, child);
                    link.addArc(arc.getFrom(), arc);
                    break;
                default:
                    log.info("Ignoring child [{}] in [{}]", childName, element.getQualifiedName());
                    break;
            }
        }

        for (Attribute attribute : element.attributes()) {
            if (!LABEL_LINK_ATTRIBUTES.contains(attribute.getName())) {
                log.info("Ignoring attribute [{}] in [{}]", attribute.getName(), element.getQualifiedName());
            }
        }

        return link;
    }

    private LabelLink() {
    }
}
