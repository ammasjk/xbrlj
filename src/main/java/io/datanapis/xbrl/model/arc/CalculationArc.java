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
import io.datanapis.xbrl.model.link.CalculationLink;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.HashSet;
import java.util.Set;

public final class CalculationArc extends FromToArc<CalculationArc> {
    private static final Set<String> CALCULATION_ARC_ATTRIBUTES = new HashSet<>();
    static {
        CALCULATION_ARC_ATTRIBUTES.addAll(ABSTRACT_ARC_ATTRIBUTES);
        CALCULATION_ARC_ATTRIBUTES.add(TagNames.TO_TAG);
        CALCULATION_ARC_ATTRIBUTES.add(TagNames.WEIGHT_TAG);
    }

    /* For a sample, see CalculationLink */
    private double weight;

    public double getWeight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CalculationArc c = (CalculationArc)o;

        if (!getFrom().equals(c.getFrom())) return false;
        if (!getTo().equals(c.getTo())) return false;
        if (!getArcrole().equals(c.getArcrole())) return false;
        if (!getType().equals(c.getType())) return false;
        return Double.compare(weight, c.weight) == 0;
    }

    @Override
    public int hashCode() {
        int result = getFrom().hashCode();
        result = 31 * result + getTo().hashCode();
        result = 31 * result + getArcrole().hashCode();
        result = 31 * result + getType().hashCode();
        result = 31 * result + (int)weight;
        return result;
    }

    public static CalculationArc fromElement(DiscoverableTaxonomySet dts, String sourceUrl, CalculationLink link, Element element) {
        CalculationArc arc = new CalculationArc(sourceUrl);

        arc.readElement(dts, link, element);

        String value = element.attributeValue(TagNames.WEIGHT_TAG);
        if (value != null) {
            try {
                arc.weight = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                log.info("NumberFormatException [{}]", value, e);
                arc.weight = 1.0;
            }
        } else {
            log.info("Defaulting weight to 1.0");
            arc.weight = 1.0;
        }

        for (Attribute attribute : element.attributes()) {
            if (!CALCULATION_ARC_ATTRIBUTES.contains(attribute.getName())) {
                log.debug("Ignoring attribute [{}] in [{}]", attribute.getName(), element.getQualifiedName());
            }
        }

        return arc;
    }

    private CalculationArc(String sourceUrl) {
        super(sourceUrl);
    }
}
