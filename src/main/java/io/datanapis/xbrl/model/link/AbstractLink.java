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
import io.datanapis.xbrl.model.Location;
import io.datanapis.xbrl.model.Locator;
import io.datanapis.xbrl.model.RoleType;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class AbstractLink implements Locator {
    static final Logger log = LoggerFactory.getLogger(AbstractLink.class);
    static final Set<String> ABSTRACT_LINK_ATTRIBUTES = new HashSet<>();
    static {
        ABSTRACT_LINK_ATTRIBUTES.add(TagNames.ROLE_TAG);
        ABSTRACT_LINK_ATTRIBUTES.add(TagNames.TYPE_TAG);
        ABSTRACT_LINK_ATTRIBUTES.add(TagNames.TITLE_TAG);
    }

    private RoleType role;
    private String type;
    private String title;
    final Map<String, Location> locations = new HashMap<>();

    public RoleType getRole() {
        return role;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public Location getLocation(String label) {
        return locations.getOrDefault(label, null);
    }

    public Collection<Location> getAllLocations() {
        return locations.values();
    }

    void merge(AbstractLink other) {
        /* merge locations from other into this.locations */
        for (Map.Entry<String,Location> entry : other.locations.entrySet()) {
            Location value = locations.get(entry.getKey());
            assert value == null || value.equals(entry.getValue());
            locations.put(entry.getKey(), entry.getValue());
        }
    }

    void readElement(DiscoverableTaxonomySet dts, Element element) {
        String roleURI = element.attributeValue(TagNames.ROLE_TAG);
        this.role = dts.getRoleType(roleURI);
        if (this.role == null) {
            log.info("Missing roleType [{}]", roleURI);
        }
        this.type = element.attributeValue(TagNames.TYPE_TAG);
        this.title = element.attributeValue(TagNames.TITLE_TAG);
    }
}
