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
import io.datanapis.xbrl.model.Location;
import io.datanapis.xbrl.model.Locator;
import io.datanapis.xbrl.utils.Utils;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public abstract class FromToArc<ArcType extends FromToArc<ArcType>> extends FromArc {
    private Location to;
    boolean hasParent = false;
    private final List<ArcType> children = new ArrayList<>();

    public boolean hasParent() {
        return hasParent;
    }

    public Location getTo() {
        return to;
    }

    public Collection<ArcType> getChildren() {
        return children;
    }

    public void addChild(ArcType child) {
        // Insert in sorted order
        Utils.insert(children, child, Comparator.comparing(FromArc::getOrder));
        child.hasParent = true;
    }

    void readElement(DiscoverableTaxonomySet dts, Locator locator, Element element) {
        super.readElement(dts, locator, element);

        String to = element.attributeValue(TagNames.TO_TAG);
        this.to = locator.getLocation(to);
        if (this.to == null) {
            log.info("(To) Location not found [{}]", to);
        }
    }

    FromToArc(String sourceUrl) {
        super(sourceUrl);
    }
}
