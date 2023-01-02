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

import io.datanapis.xbrl.TagNames;
import io.datanapis.xbrl.DiscoverableTaxonomySet;
import io.datanapis.xbrl.model.link.DefinitionLink;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.HashSet;
import java.util.Set;

public final class DefinitionArc extends FromToArc<DefinitionArc> {
    private static final Set<String> DEFINITION_ARC_ATTRIBUTES = new HashSet<>();
    static {
        DEFINITION_ARC_ATTRIBUTES.addAll(ABSTRACT_ARC_ATTRIBUTES);
        DEFINITION_ARC_ATTRIBUTES.add(TagNames.CLOSED_TAG);
        DEFINITION_ARC_ATTRIBUTES.add(TagNames.CONTEXT_ELEMENT_TAG);
        DEFINITION_ARC_ATTRIBUTES.add(TagNames.TO_TAG);
    }

    /* For a sample, see DefinitionLink */
    private boolean closed;
    private String contextElement;

    public boolean isClosed() {
        return closed;
    }

    public String getContextElement() {
        return contextElement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefinitionArc d = (DefinitionArc) o;

        if (!getFrom().equals(d.getFrom())) return false;
        if (!getTo().equals(d.getTo())) return false;
        if (!getArcrole().equals(d.getArcrole())) return false;
        if (!getType().equals(d.getType())) return false;
        return closed == d.closed;
    }

    @Override
    public int hashCode() {
        int result = getFrom().hashCode();
        result = 31 * result + getTo().hashCode();
        result = 31 * result + getArcrole().hashCode();
        result = 31 * result + getType().hashCode();
        result = 31 * result + (closed ? 1 : 0);
        return result;
    }

    public static DefinitionArc fromElement(DiscoverableTaxonomySet dts, String sourceUrl, DefinitionLink link, Element element) {
        DefinitionArc arc = new DefinitionArc(sourceUrl);

        arc.readElement(dts, link, element);

        arc.contextElement = element.attributeValue(TagNames.CONTEXT_ELEMENT_TAG);

        String value = element.attributeValue(TagNames.CLOSED_TAG);
        arc.closed = Boolean.parseBoolean(value);

        for (Attribute attribute : element.attributes()) {
            if (!DEFINITION_ARC_ATTRIBUTES.contains(attribute.getName())) {
                log.debug("Ignoring attribute [{}] in [{}]", attribute.getName(), element.getQualifiedName());
            }
        }

        return arc;
    }

    private DefinitionArc(String sourceUrl) {
        super(sourceUrl);
    }
}
