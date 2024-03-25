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
import io.datanapis.xbrl.utils.Utils;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

public final class Concept {
    private static final Set<String> CONCEPT_ATTRIBUTES = new HashSet<>();
    static {
        CONCEPT_ATTRIBUTES.add(TagNames.ABSTRACT_TAG);
        CONCEPT_ATTRIBUTES.add(TagNames.BALANCE_TAG);
        CONCEPT_ATTRIBUTES.add(TagNames.DEPRECATED_DATE_TAG);
        CONCEPT_ATTRIBUTES.add(TagNames.DOMAIN_TAG);
        CONCEPT_ATTRIBUTES.add(TagNames.FIXED_TAG);
        CONCEPT_ATTRIBUTES.add(TagNames.HEAD_USABLE_TAG);
        CONCEPT_ATTRIBUTES.add(TagNames.ID_TAG);
        CONCEPT_ATTRIBUTES.add(TagNames.LINKROLE_TAG);
        CONCEPT_ATTRIBUTES.add(TagNames.NAME_TAG);
        CONCEPT_ATTRIBUTES.add(TagNames.NILLABLE_TAG);
        CONCEPT_ATTRIBUTES.add(TagNames.PERIOD_TYPE_TAG);
        CONCEPT_ATTRIBUTES.add(TagNames.SUBSTITUTION_GROUP_TAG);
        CONCEPT_ATTRIBUTES.add(TagNames.TYPE_TAG);
        CONCEPT_ATTRIBUTES.add(TagNames.TYPED_DOMAIN_REF_TAG);
    }
    private static final String MONETARY_ITEM_TYPE = "monetaryItemType";
    private static final String PERCENT_ITEM_TYPE = "percentItemType";
    private static final String TEXT_BLOCK_ITEM_TYPE = "textBlockItemType";
    private static final String XML_ITEM_TYPE = "xmlItemType";
    private static final String XML_NODES_ITEM_TYPE = "xmlNodesItemType";

    public enum Balance {
        NONE("none"),
        DEBIT("debit"),
        CREDIT("credit");

        private final String balance;

        Balance(String balance) {
            this.balance = balance;
        }

        public String toString() {
            return balance;
        }
    }

    public enum Period {
        NONE("none"),
        DURATION("duration"),
        INSTANT("instant");

        private final String period;

        Period(String period) {
            this.period = period;
        }

        public String toString() {
            return period;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(Concept.class);

    /* Sample concepts
       -- Extension concepts
       <xsd:element id="bhlb_CertainLoansAcquiredInTransferNotAccountedForAsDebtSecuritiesNoteBalanceNet"
                    name="CertainLoansAcquiredInTransferNotAccountedForAsDebtSecuritiesNoteBalanceNet"
                    nillable="true" substitutionGroup="xbrli:item"
                    type="xbrli:monetaryItemType" xbrli:balance="debit" xbrli:periodType="instant" />
       <xsd:element id="bhlb_DerivativeWeightedAverageRateReceived"
                    ame="DerivativeWeightedAverageRateReceived"
                    nillable="true" substitutionGroup="xbrli:item"
                    type="num:percentItemType" xbrli:periodType="instant" />
       <xsd:element id="bhlb_DisclosureOfTradingSecuritiesAndCertainTradingAssetsTextBlock"
                    name="DisclosureOfTradingSecuritiesAndCertainTradingAssetsTextBlock"
                    nillable="true" substitutionGroup="xbrli:item"
                    type="nonnum:textBlockItemType" xbrli:periodType="duration" />
       <xsd:element abstract="true" id="bhlb_FairValueAssetsAndLiabilitiesMeasuredOnRecurringBasisUnobservableInputReconciliationTable"
                    name="FairValueAssetsAndLiabilitiesMeasuredOnRecurringBasisUnobservableInputReconciliationTable"
                    nillable="true" substitutionGroup="xbrldt:hypercubeItem"
                    type="xbrli:stringItemType" xbrli:periodType="duration" />

       -- us-gaap concepts
       <xs:element id="us-gaap_NoninterestIncome" name="NoninterestIncome"
                   nillable="true" substitutionGroup="xbrli:item"
                   type="xbrli:monetaryItemType" xbrli:balance="credit" xbrli:periodType="duration"/>
       <xs:element id="us-gaap_CostOfRevenue" name="CostOfRevenue"
                   nillable="true" substitutionGroup="xbrli:item"
                   type="xbrli:monetaryItemType" xbrli:balance="debit" xbrli:periodType="duration"/>
       <xs:element id="us-gaap_NetIncomeLossFromContinuingOperationsAvailableToCommonShareholdersBasic"
                   name="NetIncomeLossFromContinuingOperationsAvailableToCommonShareholdersBasic"
                   nillable="true" substitutionGroup="xbrli:item"
                   type="xbrli:monetaryItemType" xbrli:balance="credit" xbrli:periodType="duration"/>
       <xs:element abstract="true" id="us-gaap_CapitalRequirementsOnForeignFinancialInstitutionsTable"
                   name="CapitalRequirementsOnForeignFinancialInstitutionsTable"
                   nillable="true" substitutionGroup="xbrldt:hypercubeItem"
                   type="xbrli:stringItemType" xbrli:periodType="duration"/>
     */
    private final String sourceUrl;
    private Namespace namespace;
    private String prefix;
    private String key;
    private String id;
    private String name;
    private String qualifiedName;

    // Only used in very specific Concepts, e.g. dei:NoTradingSymbolFlag
    private String fixed;
    private boolean abstractConcept;
    private boolean nillable;
    private QName substitutionGroup;
    private QName type;
    private Balance balance;
    private Period period;
    private String typedDomainRef;
    private LocalDate deprecatedDate;
    private final RoleLabelMap labelMap = new RoleLabelMap();
    private List<Reference> references = null;
    private final List<Fact> facts = new ArrayList<>();

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getKey() {
        return key;
    }

    public String getFixed() {
        return fixed;
    }

    public boolean isAbstractConcept() {
        return isAbstract();
    }

    public boolean isAbstract() {
        return abstractConcept;
    }

    public boolean isNillable() {
        return nillable;
    }

    public QName getSubstitutionGroup() {
        return substitutionGroup;
    }

    public String getSubstitutionGroupName() {
        return substitutionGroup.getName();
    }

    public QName getType() {
        return type;
    }

    public String getTypeName() {
        return type.getName();
    }

    public boolean isMonetaryConcept() {
        return isMonetary();
    }

    public boolean isMonetary() {
        return this.getTypeName().equals(MONETARY_ITEM_TYPE);
    }

    public boolean isPercent() {
        return this.getTypeName().equals(PERCENT_ITEM_TYPE);
    }

    private static final Set<String> TEXT_ITEM_TYPES =
            Set.of(TEXT_BLOCK_ITEM_TYPE.toLowerCase(), XML_NODES_ITEM_TYPE.toLowerCase(), XML_ITEM_TYPE.toLowerCase());
    private static boolean isTextType(String type) {
        return Objects.nonNull(type) && TEXT_ITEM_TYPES.contains(type);
    }

    public boolean isText() {
        return isTextType(getTypeName()) || name.endsWith("Text") || name.endsWith("TextBlock");
    }

    public Balance getBalance() {
        return balance;
    }

    public Period getPeriod() {
        return period;
    }

    public String getTypedDomainRef() {
        return typedDomainRef;
    }

    public LocalDate getDeprecatedDate() {
        return deprecatedDate;
    }

    public Label getLabel() {
        return labelMap.getLabel();
    }

    public Label getLabel(String roleType) {
        return labelMap.getLabel(roleType);
    }

    public void addLabels(RoleLabelMap labelMap) {
        this.labelMap.addAll(labelMap);
    }

    public void addReference(Reference reference) {
        if (Objects.isNull(this.references)) {
            this.references = new ArrayList<>();
        }
        this.references.add(reference);
    }
    public List<String> getReplacements() {
        if (Objects.isNull(this.references))
            return null;

        return this.references.stream().map(Reference::getReplacements)
                .filter(Objects::nonNull).flatMap(Collection::stream).toList();
    }

    public Collection<Label> getAllLabels() {
        return labelMap.values();
    }

    public List<Fact> getFacts() {
        return facts;
    }

    public void clear() {
        labelMap.clear();
        facts.clear();
    }

    void addFact(Fact fact) {
        assert fact.getConcept().equals(this);
        this.facts.add(fact);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Concept concept = (Concept) o;

        return id.equals(concept.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getQualifiedName();
    }

    public static Concept fromElement(String sourceUrl, String targetNamespace, Element element) {
        String value;

        value = element.attributeValue(TagNames.ID_TAG);
        if (value == null)
            return null;

        Concept concept = new Concept(sourceUrl);
        concept.id = value;

        concept.key = Utils.getKey(sourceUrl + "#" + concept.id);
        concept.name = element.attributeValue(TagNames.NAME_TAG);
        concept.fixed = element.attributeValue(TagNames.FIXED_TAG);

        concept.abstractConcept = false;
        value = element.attributeValue(TagNames.ABSTRACT_TAG);
        if (value != null) {
            concept.abstractConcept = Boolean.parseBoolean(value);
        }

        concept.nillable = false;
        value = element.attributeValue(TagNames.NILLABLE_TAG);
        if (value != null) {
            concept.nillable = Boolean.parseBoolean(value);
        }

        // Sample substitutionGroups:
        //     xbrli:item, xbrldt:hypercubeItem, xbrldt:dimensionItem
        value = element.attributeValue(TagNames.SUBSTITUTION_GROUP_TAG);
        if (value != null) {
            concept.substitutionGroup = element.getQName(value);
        }

        // Sample types:
        //     xbrli:monetaryItemType, xbrli:sharesItemType, xbrli:gYearItemType, nonnum:textBlockItemType,
        //     xbrli:durationItemType, num:perShareItemType, xbrli:pureItemType, num:percentItemType,
        //     xbrli:integerItemType, xbrli:nonNegativeIntegerItemType, xbrli:positiveIntegerItemType,
        //     xbrli:nonPositiveIntegerItemType, xbrli:negativeIntegerItemType, xbrli:stringItemType, xbrli:dateTimeItemType, etc.
        value = element.attributeValue(TagNames.TYPE_TAG);
        if (value != null) {
            concept.type = element.getQName(value);
        }

        // Balance is either debit or credit or null
        value = element.attributeValue(TagNames.BALANCE_TAG);
        if (value == null) {
            concept.balance = Balance.NONE;
        } else if (value.equals("debit")) {
            concept.balance = Balance.DEBIT;
        } else if (value.equals("credit")) {
            concept.balance = Balance.CREDIT;
        }

        value = element.attributeValue(TagNames.PERIOD_TYPE_TAG);
        if (value == null) {
            concept.period = Period.NONE;
        } else if (value.equals("duration")) {
            concept.period = Period.DURATION;
        } else if (value.equals("instant")) {
            concept.period = Period.INSTANT;
        }

        concept.typedDomainRef = element.attributeValue(TagNames.TYPED_DOMAIN_REF_TAG);

        value = element.attributeValue(TagNames.DEPRECATED_DATE_TAG);
        if (value != null) {
            concept.deprecatedDate = LocalDate.parse(value);
        }

        concept.namespace = element.getNamespaceForURI(targetNamespace);
        if (concept.namespace == null) {
            log.info("Namespace is null for Concept, id=[{}], name=[{}]", concept.id, concept.name);
        }
        concept.prefix = concept.namespace.getPrefix();
        if (concept.prefix == null) {
            log.info("Prefix is null for Concept, id=[{}], name=[{}]", concept.id, concept.name);
        }

        concept.qualifiedName = concept.namespace.getPrefix() + ":" + concept.name;

        // TODO - need to store this in the Concept
        String domain = element.attributeValue(TagNames.DOMAIN_TAG);
        String headUsable = element.attributeValue(TagNames.HEAD_USABLE_TAG);
        String linkrole = element.attributeValue(TagNames.LINKROLE_TAG);

        for (Attribute attribute : element.attributes()) {
            if (!CONCEPT_ATTRIBUTES.contains(attribute.getName())) {
                log.info("Unhandled attribute [{}] in Concept", attribute.getName());
            }
        }

        return concept;
    }

    private Concept(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
