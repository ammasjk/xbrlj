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
import io.datanapis.xbrl.model.ArcroleType;
import io.datanapis.xbrl.model.Location;
import io.datanapis.xbrl.model.Locator;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public abstract class FromArc implements Comparable<FromArc> {
    static final Set<String> ABSTRACT_ARC_ATTRIBUTES = new HashSet<>();
    static {
        ABSTRACT_ARC_ATTRIBUTES.add(TagNames.ORDER_TAG);
        ABSTRACT_ARC_ATTRIBUTES.add(TagNames.ARCROLE_TAG);
        ABSTRACT_ARC_ATTRIBUTES.add(TagNames.FROM_TAG);
        ABSTRACT_ARC_ATTRIBUTES.add(TagNames.TYPE_TAG);
        ABSTRACT_ARC_ATTRIBUTES.add(TagNames.PRIORITY_TAG);
        ABSTRACT_ARC_ATTRIBUTES.add(TagNames.USE_TAG);
        ABSTRACT_ARC_ATTRIBUTES.add(TagNames.TITLE_TAG);
    }
    static final Logger log = LoggerFactory.getLogger(FromArc.class);

    private final String sourceUrl;
    private float order;
    private ArcroleType arcrole;
    private Location from;
    private String type;
    private int priority;
    private String use;
    private String title;

    public String getSourceUrl() {
        return sourceUrl;
    }

    public float getOrder() {
        return order;
    }

    public ArcroleType getArcrole() {
        return arcrole;
    }

    public Location getFrom() {
        return from;
    }

    public String getType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }

    public String getUse() {
        return use;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public int compareTo(FromArc that) {
        return Float.compare(order, that.order);
    }

    /**
     * Read basic attributes from Element. Dts is used to resolve references to Arcroles and locator
     * is used to resolve references to locations. The locator is usually the DefinitionLink, PresentationLink or
     * CalculationLink that surrounds this arc.
     *
     * @param dts
     * @param locator
     * @param element
     */
    void readElement(DiscoverableTaxonomySet dts, Locator locator, Element element) {
        String value;

        value = element.attributeValue(TagNames.ORDER_TAG);
        if (value != null) {
            try {
                this.order = Float.parseFloat(value);
            } catch (NumberFormatException e) {
                this.order = 1;
                log.info("NumberFormatException [{}]", value, e);
            }
        } else {
            this.order = 1;
        }

        String arcroleURI = element.attributeValue(TagNames.ARCROLE_TAG);
        this.arcrole = dts.getArcRoleType(arcroleURI);
        if (this.arcrole == null) {
            log.info("Missing arcroleType [{}]", arcroleURI);
        }

        String from = element.attributeValue(TagNames.FROM_TAG);
        this.from = locator.getLocation(from);
        if (this.from == null) {
            log.info("(From) Location not found [{}]", from);
        }

        this.type = element.attributeValue(TagNames.TYPE_TAG);

        this.use = element.attributeValue(TagNames.USE_TAG);
        if (this.use == null) {
            this.use = "optional";
        }

        value = element.attributeValue(TagNames.PRIORITY_TAG);
        if (value != null) {
            try {
                this.priority = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                this.priority = 0;
                log.info("NumberFormatException [{}]", value, e);
            }
        } else {
            this.priority = 0;
        }

        this.title = element.attributeValue(TagNames.TITLE_TAG);
    }

    FromArc(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
