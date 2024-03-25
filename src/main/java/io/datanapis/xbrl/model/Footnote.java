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

import io.datanapis.xbrl.TagNames;
import io.datanapis.xbrl.utils.JsonUtils;
import io.datanapis.xbrl.utils.XmlUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Footnote {
    private static final Set<String> FOOTNOTE_ATTRIBUTES = new HashSet<>();
    static {
        FOOTNOTE_ATTRIBUTES.add(TagNames.ID_TAG);
        FOOTNOTE_ATTRIBUTES.add(TagNames.LABEL_TAG);
        FOOTNOTE_ATTRIBUTES.add(TagNames.ROLE_TAG);
        FOOTNOTE_ATTRIBUTES.add(TagNames.TYPE_TAG);
        FOOTNOTE_ATTRIBUTES.add(TagNames.LANG_TAG);
    }

    private static final Logger log = LoggerFactory.getLogger(Footnote.class);

    /* See FootnoteArc for a sample */
    private final String sourceUrl;
    private String id;
    private String label;
    private String role;
    private String type;
    private String lang;
    private String value;

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getRole() {
        return role;
    }

    public String getType() {
        return type;
    }

    public String getLang() {
        return lang;
    }

    public String getValue() {
        return value;
    }

    public static Footnote fromElement(String sourceUrl, Element element) {
        Footnote footnote = new Footnote(sourceUrl);

        footnote.id = element.attributeValue(TagNames.ID_TAG);
        footnote.label = element.attributeValue(TagNames.LABEL_TAG);
        footnote.role = element.attributeValue(TagNames.ROLE_TAG);
        footnote.type = element.attributeValue(TagNames.TYPE_TAG);
        footnote.lang = element.attributeValue(TagNames.LANG_TAG);
        footnote.value = element.getStringValue();

        for (Attribute attribute : element.attributes()) {
            if (!FOOTNOTE_ATTRIBUTES.contains(attribute.getName())) {
                log.info("Ignoring attribute [{}] in [{}]", attribute.getName(), element.getQualifiedName());
            }
        }

        return footnote;
    }

    public static Footnote fromElements(String sourceUrl, List<Element> elements) {
        Footnote footnote = new Footnote(sourceUrl);

        Element element = elements.get(0);
        footnote.id = element.attributeValue(TagNames.ID_TAG);
        footnote.role = element.attributeValue(TagNames.FOOTNOTE_ROLE_TAG);
        boolean escape = Boolean.parseBoolean(element.attributeValue(TagNames.ESCAPE_TAG));

        String value = XmlUtils.asXML(elements);
        if (escape) {
            value = StringEscapeUtils.unescapeHtml4(value);
        }
        footnote.value = Jsoup.clean(value, JsonUtils.relaxed());

        return footnote;
    }

    private Footnote(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
