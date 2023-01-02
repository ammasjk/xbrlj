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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import io.datanapis.xbrl.TagNames;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Label {
    /* Non-standard labels */
    public static final String ROLE_TYPE_NEGATED_US = "http://xbrl.us/us-gaap/role/label/negated";
    public static final String ROLE_TYPE_NEGATED_PERIOD_END_US = "http://xbrl.us/us-gaap/role/label/negatedPeriodEnd";
    public static final String ROLE_TYPE_NEGATED_PERIOD_START_US = "http://xbrl.us/us-gaap/role/label/negatedPeriodStart";
    public static final String ROLE_TYPE_NEGATED_TOTAL_US = "http://xbrl.us/us-gaap/role/label/negatedTotal";

    /* 2003 types */
    public static final String ROLE_TYPE_LABEL = "http://www.xbrl.org/2003/role/label";
    public static final String ROLE_TYPE_TERSE_LABEL = "http://www.xbrl.org/2003/role/terseLabel";
    public static final String ROLE_TYPE_DOCUMENTATION = "http://www.xbrl.org/2003/role/documentation";
    public static final String ROLE_TYPE_TOTAL_LABEL = "http://www.xbrl.org/2003/role/totalLabel";
    public static final String ROLE_TYPE_VERBOSE_LABEL = "http://www.xbrl.org/2003/role/verboseLabel";
    public static final String ROLE_TYPE_PERIOD_START_LABEL = "http://www.xbrl.org/2003/role/periodStartLabel";
    public static final String ROLE_TYPE_PERIOD_END_LABEL = "http://www.xbrl.org/2003/role/periodEndLabel";

    /* 2009 types */
    public static final String ROLE_TYPE_DEPRECATED_LABEL = "http://www.xbrl.org/2009/role/deprecatedLabel";
    public static final String ROLE_TYPE_DEPRECATED_DATE_LABEL = "http://www.xbrl.org/2009/role/deprecatedDateLabel";
    public static final String ROLE_TYPE_NEGATED_LABEL = "http://www.xbrl.org/2009/role/negatedLabel";
    public static final String ROLE_TYPE_NEGATED_NET_LABEL = "http://www.xbrl.org/2009/role/negatedNetLabel";
    public static final String ROLE_TYPE_NEGATED_PERIOD_END_LABEL = "http://www.xbrl.org/2009/role/negatedPeriodEndLabel";
    public static final String ROLE_TYPE_NEGATED_PERIOD_START_LABEL = "http://www.xbrl.org/2009/role/negatedPeriodStartLabel";
    public static final String ROLE_TYPE_NEGATED_TERSE_LABEL = "http://www.xbrl.org/2009/role/negatedTerseLabel";
    public static final String ROLE_TYPE_NEGATED_TOTAL_LABEL = "http://www.xbrl.org/2009/role/negatedTotalLabel";
    public static final String ROLE_TYPE_NEGATIVE_PERIOD_END_LABEL = "http://www.xbrl.org/2009/role/negativePeriodEndLabel";
    public static final String ROLE_TYPE_NEGATIVE_PERIOD_END_TOTAL_LABEL = "http://www.xbrl.org/2009/role/negativePeriodEndTotalLabel";
    public static final String ROLE_TYPE_NEGATIVE_PERIOD_START_LABEL = "http://www.xbrl.org/2009/role/negativePeriodStartLabel";
    public static final String ROLE_TYPE_NEGATIVE_PERIOD_START_TOTAL_LABEL = "http://www.xbrl.org/2009/role/negativePeriodStartTotalLabel";
    public static final String ROLE_TYPE_NET_LABEL = "http://www.xbrl.org/2009/role/netLabel";
    public static final String ROLE_TYPE_POSITIVE_PERIOD_START_LABEL = "http://www.xbrl.org/2009/role/positivePeriodStartLabel";
    public static final String ROLE_TYPE_POSITIVE_PERIOD_END_LABEL = "http://www.xbrl.org/2009/role/positivePeriodEndLabel";
    public static final String ROLE_TYPE_POSITIVE_PERIOD_START_TOTAL_LABEL = "http://www.xbrl.org/2009/role/positivePeriodStartTotalLabel";
    public static final String ROLE_TYPE_POSITIVE_PERIOD_END_TOTAL_LABEL = "http://www.xbrl.org/2009/role/positivePeriodEndTotalLabel";
    public static final String ROLE_TYPE_RESTATED_LABEL = "http://www.xbrl.org/2006/role/restatedLabel";

    public static final String LABEL = "LABEL";
    public static final String TERSE = "TERSE";
    public static final String TERSE_LABEL = "TERSE";
    public static final String DEPRECATED = "DEPRECATED";
    public static final String DOCUMENTATION = "DOCUMENTATION";
    public static final String TOTAL = "TOTAL";
    public static final String NEGATED = "NEGATED";
    public static final String NEGATIVE = "NEGATIVE";
    public static final String NET = "NET";
    public static final String VERBOSE = "VERBOSE";
    public static final String PERIOD_START = "PERIOD-START";
    public static final String PERIOD_END = "PERIOD-END";
    public static final String RESTATED = "RESTATED";

    public static final String NEGATED_TERSE_LABEL = join(NEGATED, TERSE);
    public static final String NEGATED_TOTAL_LABEL = join(NEGATED, TOTAL);

    private static String join(String ... labels) {
        return Joiner.on(',').join(labels);
    }

    public static final Map<String,String> labelToType =
            new ImmutableMap.Builder<String,String>()
                    .put(ROLE_TYPE_LABEL,                             LABEL)
                    .put(ROLE_TYPE_TERSE_LABEL,                       TERSE)
                    .put(ROLE_TYPE_DOCUMENTATION,                     DOCUMENTATION)
                    .put(ROLE_TYPE_TOTAL_LABEL,                       TOTAL)
                    .put(ROLE_TYPE_NEGATED_LABEL,                     NEGATED)
                    .put(ROLE_TYPE_NET_LABEL,                         NET)
                    .put(ROLE_TYPE_NEGATED_NET_LABEL,                 join(NEGATED, NET))
                    .put(ROLE_TYPE_VERBOSE_LABEL,                     VERBOSE)
                    .put(ROLE_TYPE_NEGATED_TERSE_LABEL,               join(NEGATED, TERSE))
                    .put(ROLE_TYPE_PERIOD_START_LABEL,                PERIOD_START)
                    .put(ROLE_TYPE_NEGATED_PERIOD_START_LABEL,        join(NEGATED, PERIOD_START))
                    .put(ROLE_TYPE_NEGATIVE_PERIOD_START_LABEL,       join(NEGATIVE, PERIOD_START))
                    .put(ROLE_TYPE_POSITIVE_PERIOD_START_LABEL,       PERIOD_START)
                    .put(ROLE_TYPE_PERIOD_END_LABEL,                  PERIOD_END)
                    .put(ROLE_TYPE_NEGATED_PERIOD_END_LABEL,          join(NEGATED, PERIOD_END))
                    .put(ROLE_TYPE_NEGATIVE_PERIOD_END_LABEL,         join(NEGATIVE, PERIOD_END))
                    .put(ROLE_TYPE_POSITIVE_PERIOD_END_LABEL,         PERIOD_END)
                    .put(ROLE_TYPE_NEGATED_TOTAL_LABEL,               join(NEGATED, TOTAL))
                    .put(ROLE_TYPE_NEGATED_US,                        NEGATED)
                    .put(ROLE_TYPE_NEGATIVE_PERIOD_END_TOTAL_LABEL,   join(NEGATIVE, PERIOD_END, TOTAL))
                    .put(ROLE_TYPE_NEGATIVE_PERIOD_START_TOTAL_LABEL, join(NEGATIVE, PERIOD_START, TOTAL))
                    .put(ROLE_TYPE_POSITIVE_PERIOD_START_TOTAL_LABEL, join(PERIOD_START, TOTAL))
                    .put(ROLE_TYPE_POSITIVE_PERIOD_END_TOTAL_LABEL,   join(PERIOD_END, TOTAL))
                    .put(ROLE_TYPE_RESTATED_LABEL,                    RESTATED)
                    .put(ROLE_TYPE_DEPRECATED_LABEL,                  DEPRECATED)
                    .put(ROLE_TYPE_NEGATED_PERIOD_END_US,             join(NEGATED, PERIOD_END))
                    .put(ROLE_TYPE_NEGATED_PERIOD_START_US,           join(NEGATED, PERIOD_START))
                    .put(ROLE_TYPE_NEGATED_TOTAL_US,                  join(NEGATED, TOTAL))
                    .build();

    public static String asLabelType(String roleType) {
        return Label.labelToType.get(roleType);
    }

    public static String defaultLabelType() {
        return LABEL;
    }

    private static final Set<String> LABEL_ATTRIBUTES = new HashSet<>();
    static {
        LABEL_ATTRIBUTES.add(TagNames.ID_TAG);
        LABEL_ATTRIBUTES.add(TagNames.LABEL_TAG);
        LABEL_ATTRIBUTES.add(TagNames.TITLE_TAG);
        LABEL_ATTRIBUTES.add(TagNames.ROLE_TAG);
        LABEL_ATTRIBUTES.add(TagNames.TYPE_TAG);
        LABEL_ATTRIBUTES.add(TagNames.LANG_TAG);
    }
    private static final Logger log = LoggerFactory.getLogger(Label.class);

    /* Sample Label:
       <link:label id="lab_us-gaap_LesseeLeasesPolicyTextBlock_4d64ee50-718a-4a52-88cd-5148655ae8de_terseLabel_en-US"
                   xlink:label="lab_us-gaap_LesseeLeasesPolicyTextBlock"
                   xlink:role="http://www.xbrl.org/2003/role/terseLabel"
                   xlink:type="resource"
                   xmlns:xml="http://www.w3.org/XML/1998/namespace" xml:lang="en-US">Leases</link:label>
        id = "lab_us-gaap_LesseeLeasesPolicyTextBlock_4d64ee50-718a-4a52-88cd-5148655ae8de_terseLabel_en-US"
        label = "lab_us-gaap_LesseeLeasesPolicyTextBlock"
        role = "http://www.xbrl.org/2003/role/terseLabel"
        type = "resource"
        lang = "en-US"
        value = "Leases"
     */
    private final String sourceUrl;
    private String id;
    private String label;
    private String title;
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

    public String getTitle() {
        return title;
    }

    public String getRole() {
        return role;
    }

    public boolean isLabel() {
        return role.equals(ROLE_TYPE_LABEL);
    }

    public boolean is(String attribute) {
        String type = asLabelType(role);
        if (type == null)
            return false;

        return type.contains(attribute);
    }

    public boolean isPeriodStartLabel() {
        return is(PERIOD_START);
    }

    public boolean isPeriodEndLabel() {
        return is(PERIOD_END);
    }

    public boolean isTerseLabel() {
        return is(TERSE);
    }

    public boolean isDocumentation() {
        return role.equals(ROLE_TYPE_DOCUMENTATION);
    }

    public boolean isNegated() {
        return is(NEGATED);
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

    public static Label fromElement(String sourceUrl, Element element) {
        Label label = new Label(sourceUrl);

        label.id = element.attributeValue(TagNames.ID_TAG);
        label.label = element.attributeValue(TagNames.LABEL_TAG);
        label.title = element.attributeValue(TagNames.TITLE_TAG);
        label.role = element.attributeValue(TagNames.ROLE_TAG);
        label.type = element.attributeValue(TagNames.TYPE_TAG);
        label.lang = element.attributeValue(TagNames.LANG_TAG);
        label.value = element.getText();

        for (Attribute attribute : element.attributes()) {
            if (!LABEL_ATTRIBUTES.contains(attribute.getName())) {
                log.info("Ignoring attribute [{}] in [{}]", attribute.getName(), element.getQualifiedName());
            }
        }

        return label;
    }

    private Label(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
