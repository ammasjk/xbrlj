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
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ArcroleType {
    private static final String DYNAMIC = "Dynamic";
    private static final String PREDEFINED = "predefined";

    /* 2003 types */
    public static final String ALL_URI = "http://xbrl.org/int/dim/arcrole/all";
    public static final String NOT_ALL_URI = "http://xbrl.org/int/dim/arcrole/notAll";
    public static final String CONCEPT_LABEL_URI = "http://www.xbrl.org/2003/arcrole/concept-label";
    public static final String DIMENSION_DEFAULT_URI = "http://xbrl.org/int/dim/arcrole/dimension-default";
    public static final String DIMENSION_DOMAIN_URI = "http://xbrl.org/int/dim/arcrole/dimension-domain";
    public static final String DOMAIN_MEMBER_URI = "http://xbrl.org/int/dim/arcrole/domain-member";
    public static final String FACT_FOOTNOTE_URI = "http://www.xbrl.org/2003/arcrole/fact-footnote";
    public static final String HYPERCUBE_DIMENSION_URI = "http://xbrl.org/int/dim/arcrole/hypercube-dimension";
    public static final String PARENT_CHILD_URI = "http://www.xbrl.org/2003/arcrole/parent-child";
    public static final String SUMMATION_ITEM_URI = "http://www.xbrl.org/2003/arcrole/summation-item";
    public static final String GENERAL_SPECIAL_URI = "http://www.xbrl.org/2003/arcrole/general-special";
    public static final String ESSENCE_ALIAS_URI = "http://www.xbrl.org/2003/arcrole/essence-alias";
    public static final String SIMILAR_TUPLES_URI = "http://www.xbrl.org/2003/arcrole/similar-tuples";
    public static final String REQUIRES_ELEMENT_URI = "http://www.xbrl.org/2003/arcrole/requires-element";

    /* 2009 types */
    public static final String DEP_AGGREGATE_CONCEPT_DEPRECATED_PART_CONCEPT_URI = "http://www.xbrl.org/2009/arcrole/dep-aggregateConcept-deprecatedPartConcept";
    public static final String DEP_CONCEPT_DEPRECATED_CONCEPT_URI = "http://www.xbrl.org/2009/arcrole/dep-concept-deprecatedConcept";
    public static final String DEP_DIMENSIONALLY_QUALIFIED_CONCEPT_DEPRECATED_CONCEPT_URI = "http://www.xbrl.org/2009/arcrole/dep-dimensionallyQualifiedConcept-deprecatedConcept";
    public static final String DEP_MUTUALLY_EXCLUSIVE_CONCEPT_DEPRECATED_CONCEPT_URI = "http://www.xbrl.org/2009/arcrole/dep-mutuallyExclusiveConcept-deprecatedConcept";
    public static final String DEP_PART_CONCEPT_DEPRECATED_AGGREGATE_CONCEPT_URI = "http://www.xbrl.org/2009/arcrole/dep-partConcept-deprecatedAggregateConcept";
    public static final String FACT_EXPLANATORY_FACT_URI = "http://www.xbrl.org/2009/arcrole/fact-explanatoryFact";

    private static final Logger log = LoggerFactory.getLogger(ArcroleType.class);

    public static final ArcroleType ALL = new ArcroleType(PREDEFINED);
    public static final ArcroleType NOT_ALL = new ArcroleType(PREDEFINED);
    public static final ArcroleType CONCEPT_LABEL = new ArcroleType(PREDEFINED);
    public static final ArcroleType DIMENSION_DEFAULT = new ArcroleType(PREDEFINED);
    public static final ArcroleType DIMENSION_DOMAIN = new ArcroleType(PREDEFINED);
    public static final ArcroleType DOMAIN_MEMBER = new ArcroleType(PREDEFINED);
    public static final ArcroleType FACT_FOOTNOTE = new ArcroleType(PREDEFINED);
    public static final ArcroleType HYPERCUBE_DIMENSION = new ArcroleType(PREDEFINED);
    public static final ArcroleType PARENT_CHILD = new ArcroleType(PREDEFINED);
    public static final ArcroleType SUMMATION_ITEM = new ArcroleType(PREDEFINED);
    public static final ArcroleType GENERAL_SPECIAL = new ArcroleType(PREDEFINED);
    public static final ArcroleType ESSENCE_ALIAS = new ArcroleType(PREDEFINED);
    public static final ArcroleType SIMILAR_TUPLES = new ArcroleType(PREDEFINED);
    public static final ArcroleType REQUIRES_ELEMENT = new ArcroleType(PREDEFINED);
    public static final ArcroleType DEPRECATED_PART_CONCEPT = new ArcroleType(PREDEFINED);
    public static final ArcroleType DEPRECATED_CONCEPT = new ArcroleType(PREDEFINED);
    public static final ArcroleType DIMENSIONALLY_QUALIFIED_DEPRECATED_CONCEPT = new ArcroleType(PREDEFINED);
    public static final ArcroleType MUTUALLY_EXCLUSIVE_DEPRECATED_CONCEPT = new ArcroleType(PREDEFINED);
    public static final ArcroleType DEPRECATED_AGGREGATE_CONCEPT = new ArcroleType(PREDEFINED);
    public static final ArcroleType EXPLANATORY_FACT = new ArcroleType(PREDEFINED);
    static {
        ALL.id = "all";
        ALL.arcroleURI = ALL_URI;
        ALL.cyclesAllowed = "undirected";
        ALL.definition = "An arc from a primary item (substitutionGroup == xbrli:item) to a hypercube";

        NOT_ALL.id = "notAll";
        NOT_ALL.arcroleURI = NOT_ALL_URI;
        NOT_ALL.cyclesAllowed = "undirected";
        NOT_ALL.definition = "Negation of all?";

        CONCEPT_LABEL.id = "concept-label";
        CONCEPT_LABEL.arcroleURI = CONCEPT_LABEL_URI;
        CONCEPT_LABEL.cyclesAllowed = "none";
        CONCEPT_LABEL.definition = "A concept label";

        DIMENSION_DEFAULT.id = "dimension-default";
        DIMENSION_DEFAULT.arcroleURI = DIMENSION_DEFAULT_URI;
        DIMENSION_DEFAULT.cyclesAllowed = "none";
        DIMENSION_DEFAULT.definition = "Dimension default";

        DIMENSION_DOMAIN.id = "dimension-domain";
        DIMENSION_DOMAIN.arcroleURI = DIMENSION_DOMAIN_URI;
        DIMENSION_DOMAIN.cyclesAllowed = "none";
        DIMENSION_DOMAIN.definition = "A dimension domain";

        DOMAIN_MEMBER.id = "domain-member";
        DOMAIN_MEMBER.arcroleURI = DOMAIN_MEMBER_URI;
        DOMAIN_MEMBER.cyclesAllowed = "undirected";
        DOMAIN_MEMBER.definition = "A domain member";

        FACT_FOOTNOTE.id = "fact-footnote";
        FACT_FOOTNOTE.arcroleURI = FACT_FOOTNOTE_URI;
        FACT_FOOTNOTE.cyclesAllowed = "none";
        FACT_FOOTNOTE.definition = "Footnote for a fact";

        HYPERCUBE_DIMENSION.id = "hypercube-dimension";
        HYPERCUBE_DIMENSION.arcroleURI = HYPERCUBE_DIMENSION_URI;
        HYPERCUBE_DIMENSION.cyclesAllowed = "none";
        HYPERCUBE_DIMENSION.definition = "A hypercube dimension";

        PARENT_CHILD.id = "parent-child";
        PARENT_CHILD.arcroleURI = PARENT_CHILD_URI;
        PARENT_CHILD.cyclesAllowed = "none";
        PARENT_CHILD.definition = "This arc role is used on generic arcs to define presentation relationships between arbitrary elements in an XBRL DTS, not limited to XBRL 2.1 concepts. Directed cycles are not allowed. Software may use the network defined by relationships having this arcrole to arrange the relative position of elements displayed to a user. The generic preferred label attribute may appear on an arc with this arcrole";

        SUMMATION_ITEM.id = "summation-item";
        SUMMATION_ITEM.arcroleURI = SUMMATION_ITEM_URI;
        SUMMATION_ITEM.cyclesAllowed = "none";
        SUMMATION_ITEM.definition = "A summation of items";

        GENERAL_SPECIAL.id = "general-special";
        GENERAL_SPECIAL.arcroleURI = GENERAL_SPECIAL_URI;
        GENERAL_SPECIAL.cyclesAllowed = "none";
        GENERAL_SPECIAL.definition = "General-special arcs connect from a generalisation concept Locator to a specialisation concept locator.";

        ESSENCE_ALIAS.id = "essence-alias";
        ESSENCE_ALIAS.arcroleURI = ESSENCE_ALIAS_URI;
        ESSENCE_ALIAS.cyclesAllowed = "none";
        ESSENCE_ALIAS.definition = "This arc role value is for use on a <definitionArc> from an Essence Concept Locator to an Alias Concept Locator.";

        SIMILAR_TUPLES.id = "similar-tuples";
        SIMILAR_TUPLES.arcroleURI = SIMILAR_TUPLES_URI;
        SIMILAR_TUPLES.cyclesAllowed = "none";
        SIMILAR_TUPLES.definition = "The similar-tuples arcs represent relationships between tuple Concepts that have equivalent definitions (as provided in the labels and references for those tuples) even when they have different XML content models";

        REQUIRES_ELEMENT.id = "requires-element";
        REQUIRES_ELEMENT.arcroleURI = REQUIRES_ELEMENT_URI;
        REQUIRES_ELEMENT.cyclesAllowed = "none";
        REQUIRES_ELEMENT.definition = "If an instance of the Concept at the source of the arc occurs in an XBRL Instance then an instance of the arc's target concept MUST also occur in the XBRL instance.";

        DEPRECATED_PART_CONCEPT.id = "dep-aggregateConcept-deprecatedPartConcept";
        DEPRECATED_PART_CONCEPT.arcroleURI = DEP_AGGREGATE_CONCEPT_DEPRECATED_PART_CONCEPT_URI;
        DEPRECATED_PART_CONCEPT.cyclesAllowed = "none";
        DEPRECATED_PART_CONCEPT.definition = "Indicates that the deprecated concept is combined with another deprecated concept to a define a new replacement concept";

        DEPRECATED_CONCEPT.id = "dep-concept-deprecatedConcept";
        DEPRECATED_CONCEPT.arcroleURI = DEP_CONCEPT_DEPRECATED_CONCEPT_URI;
        DEPRECATED_CONCEPT.cyclesAllowed = "none";
        DEPRECATED_CONCEPT.definition = "Indicates what the replacement concepts is for the deprecated concept is";

        DIMENSIONALLY_QUALIFIED_DEPRECATED_CONCEPT.id = "dep-dimensionallyQualifiedConcept-deprecatedConcept";
        DIMENSIONALLY_QUALIFIED_DEPRECATED_CONCEPT.arcroleURI = DEP_DIMENSIONALLY_QUALIFIED_CONCEPT_DEPRECATED_CONCEPT_URI;
        DIMENSIONALLY_QUALIFIED_DEPRECATED_CONCEPT.cyclesAllowed = "none";
        DIMENSIONALLY_QUALIFIED_DEPRECATED_CONCEPT.definition = "Indicates that the deprecated concept has been qualified by a dimension. The item is removed because that same value now appears in a table";

        MUTUALLY_EXCLUSIVE_DEPRECATED_CONCEPT.id = "dep-mutuallyExclusiveConcept-deprecatedConcept";
        MUTUALLY_EXCLUSIVE_DEPRECATED_CONCEPT.arcroleURI = DEP_MUTUALLY_EXCLUSIVE_CONCEPT_DEPRECATED_CONCEPT_URI;
        MUTUALLY_EXCLUSIVE_DEPRECATED_CONCEPT.cyclesAllowed = "none";
        MUTUALLY_EXCLUSIVE_DEPRECATED_CONCEPT.definition = "Indicates that the deprecated concept can be replaced by 2 or more replacement items but cannot be split between the replacement concepts";

        DEPRECATED_AGGREGATE_CONCEPT.id = "dep-partConcept-deprecatedAggregateConcept";
        DEPRECATED_AGGREGATE_CONCEPT.arcroleURI = DEP_PART_CONCEPT_DEPRECATED_AGGREGATE_CONCEPT_URI;
        DEPRECATED_AGGREGATE_CONCEPT.cyclesAllowed = "none";
        DEPRECATED_AGGREGATE_CONCEPT.definition = "Indicates that the deprecated concept can be replaced by 2 or more replacement items and can be split between the replacement concepts as the issuer deems appropriate";

        EXPLANATORY_FACT.id = "fact-explanatoryFact";
        EXPLANATORY_FACT.arcroleURI = FACT_EXPLANATORY_FACT_URI;
        EXPLANATORY_FACT.cyclesAllowed = "none";
        EXPLANATORY_FACT.definition = "Arcrole for linking fact with explanatory fact in instance document footnote link";
    }

    private final String sourceUrl;
    private String id;
    private String arcroleURI;
    private String cyclesAllowed;
    private String definition;

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getId() {
        return id;
    }

    public String getArcroleURI() {
        return arcroleURI;
    }

    public String getCyclesAllowed() {
        return cyclesAllowed;
    }

    public String getDefinition() {
        return definition;
    }

    public boolean isParentChild() {
        return arcroleURI.equals(PARENT_CHILD.getArcroleURI());
    }

    public boolean isSummationItem() {
        return arcroleURI.equals(SUMMATION_ITEM.getArcroleURI());
    }

    public boolean isConceptLabel() {
        return arcroleURI.equals(CONCEPT_LABEL.getArcroleURI());
    }

    public boolean isHypercubeDimension() {
        return arcroleURI.equals(HYPERCUBE_DIMENSION_URI);
    }

    public boolean isDimensionDomain() {
        return arcroleURI.equals(DIMENSION_DOMAIN_URI);
    }

    public boolean isDomainMember() {
        return arcroleURI.equals(DOMAIN_MEMBER_URI);
    }

    public boolean isDimensionDefault() {
        return arcroleURI.equals(DIMENSION_DEFAULT_URI);
    }

    public boolean isAll() {
        return arcroleURI.equals(ALL_URI);
    }

    public boolean isNotAll() {
        return arcroleURI.equals(NOT_ALL_URI);
    }

    public boolean isGeneralSpecial() {
        return arcroleURI.equals(GENERAL_SPECIAL_URI);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArcroleType that = (ArcroleType) o;

        return arcroleURI.equals(that.arcroleURI);
    }

    @Override
    public int hashCode() {
        return arcroleURI.hashCode();
    }

    @Override
    public String toString() {
        return arcroleURI;
    }

    public static ArcroleType fromElement(String sourceUrl, Element element) {
        ArcroleType arcroleType = new ArcroleType(sourceUrl);

        arcroleType.id = element.attributeValue(TagNames.ID_TAG);
        arcroleType.arcroleURI = element.attributeValue(TagNames.ARCROLE_URI_TAG);
        arcroleType.cyclesAllowed = element.attributeValue(TagNames.CYCLES_ALLOWED_TAG);

        Element definition = element.element(TagNames.DEFINITION_TAG);
        if (definition != null) {
            arcroleType.definition = definition.getTextTrim();
        }

        // Ignoring usedOn elements. Not sure if that will be required

        return arcroleType;
    }

    public static ArcroleType createDynamic(String arcroleURI) {
        ArcroleType arcroleType = new ArcroleType(DYNAMIC);

        arcroleType.id = arcroleURI;
        arcroleType.arcroleURI = arcroleURI;
        arcroleType.definition = arcroleURI;

        log.info("Creating arcroleType [{}] dynamically", arcroleURI);

        return arcroleType;
    }

    private ArcroleType(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
