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
import io.datanapis.xbrl.XbrlInstance;
import io.datanapis.xbrl.utils.IxtTransform;
import io.datanapis.xbrl.utils.JsonUtils;
import io.datanapis.xbrl.utils.XmlUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.QName;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class Fact {
    private static final Logger log = LoggerFactory.getLogger(Fact.class);

    /* Sample Facts
       -- XBRL facts
       -- This context is available as an example in Context. This is a qualified context
       <us-gaap:FinancingReceivableRecordedInvestmentNonaccrualStatus contextRef="FI2019Q1_us-gaap_FinancingReceivablePortfolioSegmentAxis_us-gaap_ResidentialPortfolioSegmentMember_us-gaap_FinancingReceivableRecordedInvestmentByClassOfFinancingReceivableAxis_us-gaap_ConstructionLoansMember"
                decimals="-3" id="Fact-C52F5615EF0F5667820062CC0A86EFFC" unitRef="usd">0</us-gaap:FinancingReceivableRecordedInvestmentNonaccrualStatus>
       <us-gaap:LoansAndLeasesReceivableNetOfDeferredIncome contextRef="FI2019Q1_us-gaap_FinancingReceivablePortfolioSegmentAxis_us-gaap_ResidentialPortfolioSegmentMember_us-gaap_FinancingReceivableRecordedInvestmentByClassOfFinancingReceivableAxis_us-gaap_ConstructionLoansMember"
                decimals="-3" id="Fact-6D64867251455198A70C88EA8F557569" unitRef="usd">11382000</us-gaap:LoansAndLeasesReceivableNetOfDeferredIncome>

       -- This context is also available in Context. This is a simple context. This Concept is available in Concept
       <us-gaap:NoninterestIncome contextRef="FD2019Q1QTD_srt_ProductOrServiceAxis_us-gaap_DepositAccountMember"
                decimals="-3" id="Fact-B95A4FB4DDCD5782B54A6E663FAFAD61" unitRef="usd">6858000</us-gaap:NoninterestIncome>

       -- iXBRL facts
       <ix:nonNumeric contextRef="ie6ce0f9cfa43427b8eac330eab629f6a_D20210101-20210930" name="dei:EntityCentralIndexKey"
           id="id3VybDovL2RvY3MudjEvZG9jOjMyZWE2ZDUzYzJiMzQwMDFiMGMzNDExN2U5ZmY0MTBiL3NlYzozMmVhNmQ1M2MyYjM0MDAxYjBjMzQxMTdlOWZmNDEwYl80L2ZyYWc6ZWE0YTNkMzkwNzNhNDkzZjkyZjMxNjI2YWExMDhiNGIvdGFibGU6NTY0OWZjZGI1M2YwNGIwYWI0ZTY0Y2ZkYmM2NTU2ZjcvdGFibGVyYW5nZTo1NjQ5ZmNkYjUzZjA0YjBhYjRlNjRjZmRiYzY1NTZmN18zLTEtMS0xLTA_3ac96c3a-5a1b-4db2-b500-cb61f68b5272">0000019617</ix:nonNumeric>
       <ix:nonFraction unitRef="usd" contextRef="ie6ce0f9cfa43427b8eac330eab629f6a_D20210101-20210930"
           decimals="-6"  format="ixt:num-dot-decimal" name="us-gaap:InvestmentBankingRevenue" scale="6"
           id="id3VybDovL2RvY3MudjEvZG9jOjMyZWE2ZDUzYzJiMzQwMDFiMGMzNDExN2U5ZmY0MTBiL3NlYzozMmVhNmQ1M2MyYjM0MDAxYjBjMzQxMTdlOWZmNDEwYl8xNzgvZnJhZzpjMDRiNzMyNmE1Zjk0NDgwOTQ1YjhkOGU2NWQ0YTc1Yi90YWJsZTo4NjVmN2UzOTBhOTA0M2FhYTNlNjFhZDFmMTQ2OTkzMS90YWJsZXJhbmdlOjg2NWY3ZTM5MGE5MDQzYWFhM2U2MWFkMWYxNDY5OTMxXzMtNi0xLTEtMA_1f19e3e1-c2e5-4370-8d80-5ea54eff0bea">9,722</ix:nonFraction>
     */
    private Concept concept;
    private String id;
    private Context context;
    private Unit unit;
    private String value;
    private Long longValue;
    private Double doubleValue;
    private int decimals;
    private boolean nil;
    private Footnote footnote;

    public Concept getConcept() {
        return concept;
    }

    public String getId() {
        return id;
    }

    public Context getContext() {
        return context;
    }

    public Unit getUnit() {
        return unit;
    }

    public String getValue() {
        return value;
    }

    public Long getLongValue() {
        return longValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public int getDecimals() {
        return decimals;
    }

    public boolean isNil() {
        return nil;
    }

    public boolean hasExplicitMembers() {
        return context != null && context.hasDimensions();
    }

    public Footnote getFootnote() {
        return this.footnote;
    }
    public void setFootnote(Footnote footnote) {
        this.footnote = footnote;
    }

    public static int compareStringValue(Fact lhs, Fact rhs) {
        /* Sort on just the stringValue. Anything else can create an unstable sort. stringValue is always available! */
        return lhs.value.compareTo(rhs.value);
    }

    /**
     * A loose ordering of facts. Recent facts are ordered first. For facts belonging to the same period
     * facts are ordered by the fully qualified name of the concept, the decimal value and value in order
     *
     * @param lhs The left fact
     * @param rhs The right fact
     * @return less than 0, 0 or greater than 0 as lhs is less than, equal to or greater than rhs
     */
    public static int compare(Fact lhs, Fact rhs) {
        int result = lhs.getConcept().getQualifiedName().compareTo(rhs.getConcept().getQualifiedName());
        if (result != 0)
            return result;

        /* Sort by recent period first */
        result = rhs.getContext().getPeriod().compareTo(lhs.getContext().getPeriod());
        if (result != 0)
            return result;

        result = lhs.getContext().getId().compareTo(rhs.getContext().getId());
        if (result != 0)
            return result;

        /*
         * Round both to the same precision
         * Potential issue: We need to compare decimals here since we could have two values with different decimals that are duplicates.
         * For example, when numbers are large we could have the same value rounded to both millions and 100's of millions. (decimal -6 and -8)
         * But, since we are not using a consistent decimal we could end-up violating the transitive property of comparison i.e.
         * a < b and b < c => a < c
         */
        int e = Math.min(lhs.decimals, rhs.decimals);
        double multiplier = 1.0;
        if (e < 15) {
            multiplier = Math.pow(10, e);
        }

        /* Either both units are null or both have the same value */
        double v1 = Double.NaN;
        double v2 = Double.NaN;
        if (lhs.longValue != null) {
            v1 = lhs.longValue * multiplier;
        } else if (lhs.doubleValue != null) {
            v1 = lhs.doubleValue * multiplier;
        }

        if (!Double.isNaN(v1)) {
            if (rhs.longValue != null) {
                v2 = rhs.longValue * multiplier;
            } else if (rhs.doubleValue != null) {
                v2 = rhs.doubleValue * multiplier;
            }
            if (!Double.isNaN(v2)) {
                if (lhs.decimals < 0 && rhs.decimals < 0) {
                    return Long.compare(Math.round(v1), Math.round(v2));
                } else {
                    return Double.compare(v1, v2);
                }
            }
        }

        return compareStringValue(lhs, rhs);
    }

    /* Making this static handles the rare cases when fact may be null */
    public static boolean isNegative(Fact fact) {
        if (fact == null)
            return false;

        if (fact.getLongValue() != null) {
            return fact.getLongValue() < 0;
        } else if (fact.getDoubleValue() != null) {
            return fact.getDoubleValue() < 0;
        }

        return false;
    }

    /* Making this static handles the rare cases when fact may be null */
    public static String getValue(Fact fact) {
        if (fact == null)
            return "null";

        if (fact.getLongValue() != null) {
            return fact.getLongValue().toString();
        } else if (fact.getDoubleValue() != null) {
            return String.format("%.2f", fact.getDoubleValue());
        } else {
            return fact.getValue();
        }
    }

    /* Making this static handles the rare cases when fact may be null */
    public static String getValue(Fact fact, boolean negated) {
        if (fact == null)
            return "null";

        if (fact.getLongValue() != null) {
            long value = fact.getLongValue();
            if (negated) {
                value = -value;
            }
            return String.format("%d", value);
        } else if (fact.getDoubleValue() != null) {
            double value = fact.getDoubleValue();
            if (negated) {
                value = -value;
            }
            return String.format("%.2f", value);
        } else {
            return fact.getValue();
        }
    }

    public static String getUnit(Fact fact) {
        if (fact == null)
            return "";

        if (fact.getLongValue() != null || fact.getDoubleValue() != null) {
            Unit unit = fact.getUnit();
            if (unit != null)
                return unit.toString();
        }

        return "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Fact fact = (Fact) o;

        if (!concept.equals(fact.concept))
            return false;
        if (!context.equals(fact.context))
            return false;
        if (nil != fact.nil)
            return false;

        /* Round both to the same precision */
        int e = Math.min(decimals, fact.decimals);
        double multiplier = 1.0;
        if (e < 15) {
            multiplier = Math.pow(10, e);
        }

        /* Either both units are null or both have the same value */
        double v1 = Double.NaN;
        double v2 = Double.NaN;
        if (longValue != null) {
            v1 = longValue * multiplier;
        } else if (doubleValue != null) {
            v1 = doubleValue * multiplier;
        }

        if (!Double.isNaN(v1)) {
            if (fact.longValue != null) {
                v2 = fact.longValue * multiplier;
            } else if (fact.doubleValue != null) {
                v2 = fact.doubleValue * multiplier;
            }
            if (decimals < 0 && fact.decimals < 0) {
                return Math.round(v1) == Math.round(v2);
            } else {
                return Double.compare(v1, v2) == 0;
            }
        }

        return value.compareTo(fact.value) == 0;
    }

    @Override
    public int hashCode() {
        /*
         * If two objects are equals, then their hashCode must match.
         */
        int result = concept.hashCode();
        result = 31 * result + context.hashCode();
        result = 31 * result + (unit != null ? unit.hashCode() : 0);
        result = 31 * result + (nil ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        String factValue = value;
        if (longValue != null) {
            factValue = String.format("%d", longValue);
        } else if (doubleValue != null) {
            factValue = String.format("%.2f", doubleValue);
        }
        return String.format("[%s][%s] = [%s/%d] (%s.%s/%s/%s)", concept.getQualifiedName(), context.getPeriod().toString(),
                factValue, decimals, concept.getType().getName(), unit, concept.getBalance(), concept.getPeriod());
    }

    public static Fact fromElement(XbrlInstance instance, Element element) {
        return fromElement(instance, element, false);
    }

    public static Fact fromElement(XbrlInstance instance, Element element, boolean isInlineXBRL) {
        return fromElement(instance, element, null, isInlineXBRL);
    }

    public static Fact fromElement(XbrlInstance instance, Element element, Fact valueFact, boolean isInlineXBRL) {
        List<Element> elements = new ArrayList<>();
        elements.add(element);
        return fromElement(instance, elements, valueFact, isInlineXBRL);
    }

    public static Fact fromElement(XbrlInstance instance, List<Element> elements, boolean isInlineXBRL) {
        return fromElement(instance, elements, null, isInlineXBRL);
    }

    public static Fact fromElement(XbrlInstance instance, List<Element> elements, Fact valueFact, boolean isInlineXBRL) {
        Element element = elements.get(0);  /* First element will always be the starting element - see NodeChain in XbrlInstance */
        Fact fact = new Fact();
        if (!isInlineXBRL) {
            /* XBRL - Element name equals concept name */
            QName name = element.getQName();
            fact.concept = instance.getConcept(name);
            assert (fact.concept != null) : String.format("Missing concept [%s]", name);
        } else {
            /* iXBRL - Attribute name equals concept name */
            String nameValue = element.attributeValue(TagNames.NAME_TAG);
            QName name = element.getQName(nameValue);
            fact.concept = instance.getConcept(name);
            assert (fact.concept != null) : String.format("Missing concept [%s]", name);
        }
        fact.concept.addFact(fact);

        String format = null;
        boolean escape = !isInlineXBRL;
        int sign = 1;
        int scale = 0;
        for (Attribute attribute : element.attributes()) {
            switch (attribute.getName()) {
                case TagNames.ID_TAG:
                    fact.id = attribute.getValue();
                    break;
                case TagNames.CONTEXT_REF_TAG:
                    String contextId = attribute.getValue();
                    fact.context = instance.getContext(contextId);
                    fact.context.addFact(fact);
                    break;
                case TagNames.UNIT_REF_TAG:
                    String unitId = attribute.getValue();
                    fact.unit = instance.getUnit(unitId);
                    break;
                case TagNames.DECIMALS_TAG:
                    String value = attribute.getValue();
                    if (!value.equals("INF")) {
                        try {
                            fact.decimals = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            log.info("Invalid value for decimals attribute [{}]", value);
                        }
                    } else {
                        fact.decimals = Integer.MAX_VALUE;
                    }
                    break;
                case TagNames.SCALE_TAG:
                    String scaleValue = attribute.getValue();
                    try {
                        scale = Integer.parseInt(scaleValue);
                    } catch (NumberFormatException e) {
                        log.info("Invalid value for scale attribute [{}]", scaleValue);
                    }
                    break;
                case TagNames.NIL_TAG:
                    fact.nil = Boolean.parseBoolean(attribute.getValue());
                    break;
                case TagNames.FORMAT_TAG:
                    format = attribute.getValue();
                    break;
                case TagNames.NAME_TAG:
                    /* Name attribute represents the XBRL Concept and is only present for iXBRL elements but is handled separately */
                    break;
                case TagNames.SIGN_TAG:
                    if (!attribute.getValue().equals("-")) {
                        log.info("Invalid value [{}] for sign attribute. Overriding to '-'", attribute.getValue());
                    }
                    sign = -1;
                    break;
                case TagNames.ESCAPE_TAG:
                    escape = Boolean.parseBoolean(attribute.getValue());
                    break;
                case TagNames.ORDER_TAG:
                    /* This is used within an ix:tuple construct - we don't need it */
                    assert isInlineXBRL : "Order tag in non ix context";
                    break;
                case TagNames.CONTINUED_AT_TAG:
                    /* Ignore this. This is handled in NodeChain in XbrlInstance */
                    break;
                default:
                    log.info("Ignoring attribute [{}] = [{}]", attribute.getQualifiedName(), attribute.getValue());
                    break;
            }
        }

        /*
         * ids are null sometimes. Fact ids are not that critical for correctness. However, ids are used to
         * connect facts to footnotes. Having null id's means that several parts of the code will need to deal
         * with it. Hence, generating a random id for now.
         */
        if (fact.id == null) {
            fact.id = UUID.randomUUID().toString();
        }

        /* Check and initialize value if fact is nil */
        if (fact.nil) {
            fact.value = "";
            fact.longValue = 0L;
            fact.doubleValue = null;
            return fact;
        }

        String conceptName = fact.getConcept().getQualifiedName();

        /* An ix:nonFraction element can sometimes contain another ix:nonFraction element - see iXBRL specification */
        if (valueFact == null) {
            String elementContent;
            if (fact.getConcept().isText() || (elements.size() > 1 && format == null)) {
                elementContent = XmlUtils.asXML(elements);
            } else if (elements.size() > 1) {
                elementContent = XmlUtils.asString(elements);
            } else {
                elementContent = XmlUtils.asString(element);
            }
            if (escape) {
                elementContent = StringEscapeUtils.unescapeHtml4(elementContent);
            }
            if (format != null) {
                fact.value = IxtTransform.transformWithFormat(format, elementContent);
            } else {
                fact.value = Jsoup.clean(elementContent, JsonUtils.relaxed());    /* Use relaxed list to not skip tables */
            }

            if (element.getName().equals(TagNames.NON_NUMERIC_TAG)) {
                return fact;
            }

            try {
                double multiplier = Math.pow(10, scale);
                double doubleValue = Double.parseDouble(fact.getValue()) * multiplier * sign;

                /* Check if the double is actually a long and treat it as such if it is */
                if (Math.abs(doubleValue - (long)doubleValue) < 0.0001) {
                    fact.longValue = (long)doubleValue;
                } else {
                    fact.doubleValue = doubleValue;
                }
            } catch (NumberFormatException ignored) {
            }
        } else {
            /* Only the value is from the value ixElement. All other attributes are from the original element */
            fact.value = valueFact.value;
            fact.longValue = valueFact.longValue;
            fact.doubleValue = valueFact.doubleValue;
        }

        return fact;
    }

    private Fact setValue(List<Element> elements) {
        return this;
    }

    private Fact setValue(Fact fact) {
        return this;
    }

    private Fact() {
    }
}
