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
package io.datanapis.xbrl.model.arc;

import io.datanapis.xbrl.DiscoverableTaxonomySet;
import io.datanapis.xbrl.TagNames;
import io.datanapis.xbrl.model.Label;
import io.datanapis.xbrl.model.link.PresentationLink;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.HashSet;
import java.util.Set;

public final class PresentationArc extends FromToArc<PresentationArc> {
    private static final Set<String> PRESENTATION_ARC_ATTRIBUTES = new HashSet<>();
    static {
        PRESENTATION_ARC_ATTRIBUTES.addAll(ABSTRACT_ARC_ATTRIBUTES);
        PRESENTATION_ARC_ATTRIBUTES.add(TagNames.PREFERRED_LABEL_TAG);
        PRESENTATION_ARC_ATTRIBUTES.add(TagNames.TO_TAG);
    }

    /* For a sample, see PresentationLink */
    private String preferredLabel;

    public String getPreferredLabel() {
        return preferredLabel;
    }

    public String getPreferredLabelType() {
        if (preferredLabel == null || preferredLabel.length() == 0)
            return Label.defaultLabelType();

        String type = Label.asLabelType(preferredLabel);
        if (type != null)
            return type;

        return preferredLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PresentationArc p = (PresentationArc)o;

        if (!getFrom().equals(p.getFrom())) return false;
        if (!getTo().equals(p.getTo())) return false;
        if (!getArcrole().equals(p.getArcrole())) return false;
        if (!getType().equals(p.getType())) return false;
        return preferredLabel == null || preferredLabel.equals(p.getPreferredLabel());
    }

    @Override
    public int hashCode() {
        int result = getFrom().hashCode();
        result = 31 * result + getTo().hashCode();
        result = 31 * result + getArcrole().hashCode();
        result = 31 * result + getType().hashCode();
        if (preferredLabel != null) {
            result = 31 * result + preferredLabel.hashCode();
        }
        return result;
    }

    public static PresentationArc fromElement(DiscoverableTaxonomySet dts, String sourceUrl, PresentationLink link, Element element) {
        PresentationArc arc = new PresentationArc(sourceUrl);

        arc.readElement(dts, link, element);
        arc.preferredLabel = element.attributeValue(TagNames.PREFERRED_LABEL_TAG);

        for (Attribute attribute : element.attributes()) {
            if (!PRESENTATION_ARC_ATTRIBUTES.contains(attribute.getName())) {
                log.debug("Ignoring attribute [{}] in [{}]", attribute.getName(), element.getQualifiedName());
            }
        }

        return arc;
    }

    private PresentationArc(String sourceUrl) {
        super(sourceUrl);
    }
}
