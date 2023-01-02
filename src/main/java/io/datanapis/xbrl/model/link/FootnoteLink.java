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
import io.datanapis.xbrl.model.Footnote;
import io.datanapis.xbrl.model.Location;
import io.datanapis.xbrl.model.arc.FootnoteArc;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FootnoteLink extends BipartiteLink<FootnoteArc> {
    private static final Set<String> FOOTNOTE_LINK_ATTRIBUTES = new HashSet<>();
    static {
        FOOTNOTE_LINK_ATTRIBUTES.addAll(ABSTRACT_LINK_ATTRIBUTES);
    }

    private final Map<String, Footnote> footnotes = new HashMap<>();

    public Footnote getFootnote(String label) {
        return footnotes.getOrDefault(label, null);
    }

    public Footnote getFootnote(Location from) {
        FootnoteArc arc = getArc(from);
        if (arc == null)
            return null;

        return arc.getTo();
    }

    public static FootnoteLink fromElement(String sourceUrl, DiscoverableTaxonomySet dts, Element element) {
        FootnoteLink link = new FootnoteLink();

        link.readElement(dts, element);

        //
        // Process locations and footnotes first
        //
        for (Element child : element.elements()) {
            String childName = child.getName();
            switch (childName) {
                case TagNames.LOC_TAG:
                    Location location = Location.fromElement(sourceUrl, child);
                    link.locations.put(location.getLabel(), location);
                    break;
                case TagNames.FOOTNOTE_TAG:
                    Footnote footnote = Footnote.fromElement(sourceUrl, child);
                    link.footnotes.put(footnote.getLabel(), footnote);
                    break;
            }
        }

        //
        // Process footnote arcs last
        //
        for (Element child : element.elements()) {
            String childName = child.getName();
            switch (childName) {
                case TagNames.LOC_TAG:
                case TagNames.FOOTNOTE_TAG:
                    break;
                case TagNames.FOOTNOTE_ARC_TAG:
                    FootnoteArc arc = FootnoteArc.fromElement(dts, sourceUrl, link, child);
                    link.addArc(arc.getFrom(), arc);
                    break;
                default:
                    log.info("Ignoring child [{}] in [{}]", childName, element.getQualifiedName());
                    break;
            }
        }

        for (Attribute attribute : element.attributes()) {
            if (!FOOTNOTE_LINK_ATTRIBUTES.contains(attribute.getName())) {
                log.info("Ignoring attribute [{}] in [{}]", attribute.getName(), element.getQualifiedName());
            }
        }

        return link;
    }

    private FootnoteLink() {
    }
}
