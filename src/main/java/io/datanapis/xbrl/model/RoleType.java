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
import io.datanapis.xbrl.model.link.CalculationLink;
import io.datanapis.xbrl.model.link.DefinitionLink;
import io.datanapis.xbrl.model.link.PresentationLink;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoleType {
    private static final String DYNAMIC = "Dynamic";
    private static final String PREDEFINED = "predefined";

    public static final String DEPRECATED_URI = "http://fasb.org/us-gaap/role/deprecated/deprecated";
    public static final String COMMON_PRACTICE_REF_URI = "http://www.xbrl.org/2009/role/commonPracticeRef";
    public static final String NON_AUTHORITATIVE_LITERATURE_REF_URI = "http://www.xbrl.org/2009/role/nonauthoritativeLiteratureRef";
    public static final String RECOGNITION_REF_URI = "http://www.xbrl.org/2009/role/recognitionRef";

    private static final Logger log = LoggerFactory.getLogger(RoleType.class);

    public static final RoleType DEPRECATED = new RoleType(PREDEFINED, false);
    public static final RoleType COMMON_PRACTICE_REF = new RoleType(PREDEFINED, false);
    public static final RoleType NON_AUTHORITATIVE_LITERATURE_REF = new RoleType(PREDEFINED, false);
    public static final RoleType RECOGNITION_REF = new RoleType(PREDEFINED, false);
    static {
        DEPRECATED.id = "deprecated";
        DEPRECATED.roleURI = DEPRECATED_URI;
        DEPRECATED.definition = "Type consisting of all deprecated calculation, definition and presentation concepts";

        COMMON_PRACTICE_REF.id = "common-practice-ref";
        COMMON_PRACTICE_REF.roleURI = COMMON_PRACTICE_REF_URI;
        COMMON_PRACTICE_REF.definition = "Reference for common practice disclosure relating to the concept. Replaces former reference to common practice (ie using part name with value IFRS-CP) and enables common practice reference to a given point in a literature (for example commonPracticeRef to Name:IAS, Number:16, Paragraph:24). The content of the common practice disclosure is the same as other references (so for example contains parts Name, Number, IssueDate, Paragraph)";

        NON_AUTHORITATIVE_LITERATURE_REF.id = "";
        NON_AUTHORITATIVE_LITERATURE_REF.roleURI = NON_AUTHORITATIVE_LITERATURE_REF_URI;
        NON_AUTHORITATIVE_LITERATURE_REF.definition = "Reference to non-authoritative literature. Can be used to relate a concept to additional literature (academic literature, accounting firm guidelines, entity-specific guidelines, etc.)";

        RECOGNITION_REF.id = "";
        RECOGNITION_REF.roleURI = RECOGNITION_REF_URI;
        RECOGNITION_REF.definition = "Reference for recognition and derecognition. Enables the expression of additional financial reporting semantic for a concept";
    }

    private final boolean reportable;
    private final String sourceUrl;
    private String id;
    private String roleURI;
    private String definition;
    private CalculationLink calculationLink = null;
    private PresentationLink presentationLink = null;
    private DefinitionLink definitionLink = null;

    public boolean isReportable() {
        return reportable;
    }

    public boolean isDeprecated() {
        return DEPRECATED_URI.equals(roleURI);
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getId() {
        return id;
    }

    public String getRoleURI() {
        return roleURI;
    }

    public String getDefinition() {
        return definition;
    }

    public CalculationLink getCalculationLink() {
        return calculationLink;
    }
    public void setCalculationLink(CalculationLink link) {
        if (calculationLink != null) {
            /* merge with existing link */
            calculationLink.merge(link);
        } else {
            calculationLink = link;
        }
    }

    public PresentationLink getPresentationLink() {
        return presentationLink;
    }
    public void setPresentationLink(PresentationLink link) {
        if (presentationLink != null) {
            /* merge with existing link */
            presentationLink.merge(link);
        } else {
            presentationLink = link;
        }
    }

    public DefinitionLink getDefinitionLink() {
        return definitionLink;
    }
    public void setDefinitionLink(DefinitionLink link) {
        if (definitionLink != null) {
            definitionLink.merge(link);
        } else {
            definitionLink = link;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RoleType roleType = (RoleType) o;

        return roleURI.equals(roleType.roleURI);
    }

    @Override
    public int hashCode() {
        return roleURI.hashCode();
    }

    @Override
    public String toString() {
        if (definition != null) {
            return definition;
        }

        return roleURI;
    }

    public static RoleType fromElement(String sourceUrl, Element element, boolean reportable) {
        RoleType roleType = new RoleType(sourceUrl, reportable);

        roleType.id = element.attributeValue(TagNames.ID_TAG);
        roleType.roleURI = element.attributeValue(TagNames.ROLE_URI_TAG);

        Element definition = element.element(TagNames.DEFINITION_TAG);
        if (definition != null) {
            roleType.definition = definition.getTextTrim();
        }

        // Ignoring usedOn elements. Just being exhaustive when linking calculation, presentation and definition links.

        return roleType;
    }

    public static RoleType createDynamic(String roleURI) {
        RoleType roleType = new RoleType(DYNAMIC, false);

        roleType.id = roleURI;
        roleType.roleURI = roleURI;
        roleType.definition = roleURI;

        log.debug("Creating roleType [{}] dynamically", roleURI);

        return roleType;
    }

    private RoleType(String sourceUrl, boolean reportable) {
        this.reportable = reportable;
        this.sourceUrl = sourceUrl;
    }
}
