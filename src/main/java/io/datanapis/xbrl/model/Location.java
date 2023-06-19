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
package io.datanapis.xbrl.model;

import com.google.common.hash.HashCode;
import io.datanapis.xbrl.TagNames;
import io.datanapis.xbrl.utils.Utils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dom4j.Element;

import java.util.Objects;

public final class Location {
    /**
     * The source url where this location was defined.
     */
    private final String sourceUrl;

    /**
     * The relative href for this location. Relative href includes the filename and the hashtag.
     */
    private String href;
    private String label;
    private String type;

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getHref() {
        assert href != null;
        return href;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }

    public String getHashTag() {
        int index = href.indexOf('#');
        if (index > 0) {
            return href.substring(index + 1);
        }

        return href;
    }

    //
    // Ideally, equals and hashCode should be comparing on "label". However, several of the XBRL files have bugs
    // with inconsistent labels which causes definition, presentation and calculation networks to be incomplete.
    // Therefore, using href.
    //
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Location location = (Location) o;

        return Objects.equals(href, location.href) && Objects.equals(label, location.label);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(href).append(label).hashCode();
    }

    public static Location fromElement(String sourceUrl, Element element) {
        Location location = new Location(sourceUrl);

        String value;

        value = element.attributeValue(TagNames.HREF_TAG);
        if (value.startsWith("#")) {
            /* In some instances, the HREF is relative to the file. Prepend sourceUrl to get the right key */
            location.href = Utils.getKey(sourceUrl + value);
        } else {
            location.href = Utils.getKey(value);
        }
        location.label = element.attributeValue(TagNames.LABEL_TAG);
        location.type = element.attributeValue(TagNames.TYPE_TAG);

        return location;
    }

    private Location(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
