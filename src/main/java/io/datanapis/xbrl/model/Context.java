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
import io.datanapis.xbrl.DiscoverableTaxonomySet;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class Context {
    private static final Logger log = LoggerFactory.getLogger(Context.class);

    public static final String CONTEXT_TAG = "context";
    public static final String ID_TAG = "id";

    /* Sample Contexts:
       -- XBRL context
       <xbrli:context id="FD2019Q1QTD_srt_ProductOrServiceAxis_us-gaap_DepositAccountMember">
           <xbrli:entity>
               <xbrli:identifier scheme="http://www.sec.gov/CIK">0001108134</xbrli:identifier>
               <xbrli:segment>
                   <xbrldi:explicitMember dimension="srt:ProductOrServiceAxis">us-gaap:DepositAccountMember</xbrldi:explicitMember>
               </xbrli:segment>
           </xbrli:entity>
           <xbrli:period>
               <xbrli:startDate>2019-01-01</xbrli:startDate>
               <xbrli:endDate>2019-03-31</xbrli:endDate>
            </xbrli:period>
       </xbrli:context>

       <xbrli:context id="FI2019Q1_us-gaap_FinancingReceivablePortfolioSegmentAxis_us-gaap_ResidentialPortfolioSegmentMember_us-gaap_FinancingReceivableRecordedInvestmentByClassOfFinancingReceivableAxis_us-gaap_ConstructionLoansMember">
            <xbrli:entity>
                <xbrli:identifier scheme="http://www.sec.gov/CIK">0001108134</xbrli:identifier>
                <xbrli:segment>
                    <xbrldi:explicitMember dimension="us-gaap:FinancingReceivablePortfolioSegmentAxis">us-gaap:ResidentialPortfolioSegmentMember</xbrldi:explicitMember>
                    <xbrldi:explicitMember dimension="us-gaap:FinancingReceivableRecordedInvestmentByClassOfFinancingReceivableAxis">us-gaap:ConstructionLoansMember</xbrldi:explicitMember>
                </xbrli:segment>
            </xbrli:entity>
            <xbrli:period>
                <xbrli:instant>2019-03-31</xbrli:instant>
            </xbrli:period>
       </xbrli:context>

       -- iXBRL contexts. This is the same as XBRL contexts
       <xbrli:context id="ie6ce0f9cfa43427b8eac330eab629f6a_D20210101-20210930">
           <xbrli:entity>
               <xbrli:identifier scheme="http://www.sec.gov/CIK">0000019617</xbrli:identifier>
           </xbrli:entity>
           <xbrli:period>
               <xbrli:startDate>2021-01-01</xbrli:startDate>
               <xbrli:endDate>2021-09-30</xbrli:endDate>
           </xbrli:period>
       </xbrli:context>

       <xbrli:context id="i228272277e0f4f99bd938f6676010d97_D20210101-20210930">
           <xbrli:entity>
               <xbrli:identifier scheme="http://www.sec.gov/CIK">0000019617</xbrli:identifier>
               <xbrli:segment>
                   <xbrldi:explicitMember dimension="us-gaap:StatementClassOfStockAxis">us-gaap:CommonStockMember</xbrldi:explicitMember>
               </xbrli:segment>
           </xbrli:entity>
           <xbrli:period>
               <xbrli:startDate>2021-01-01</xbrli:startDate>
               <xbrli:endDate>2021-09-30</xbrli:endDate>
           </xbrli:period>
       </xbrli:context>
     */
    private final String id;
    private final String namespaceUri;
    private final Entity entity;
    private final Period period;
    private final FactList facts = new FactList();

    private Context(String namespaceUri, String id, Entity entity, Period period) {
        if (namespaceUri == null || id == null || entity == null || period == null)
            throw new NullPointerException();

        this.namespaceUri = namespaceUri;
        this.id = id;
        this.entity = entity;
        this.period = period;
    }

    public void clear() {
        facts.clear();
    }

    public String getNamespaceUri() {
        return namespaceUri;
    }

    public String getId() {
        return id;
    }

    public Entity getEntity() {
        return entity;
    }

    public Period getPeriod() {
        return period;
    }

    public List<Fact> getFacts() {
        return facts;
    }

    void addFact(Fact fact) {
        assert fact.getContext().equals(this);
        this.facts.add(fact);
    }

    public void sortFacts() {
        this.facts.sort();
    }

    public boolean hasDimensions() {
        return entity.hasDimensions();
    }

    public Set<ExplicitMember> getDimensions() {
        return entity.getDimensions();
    }

    public Set<TypedMember> getTypedMembers() {
        return entity.getTypedMembers();
    }

    private static boolean equalOrAfter(LocalDate inner, LocalDate outer) {
        return inner.isEqual(outer) || inner.isAfter(outer);
    }

    private static boolean equalOrBefore(LocalDate inner, LocalDate outer) {
        return inner.equals(outer) || inner.isBefore(outer);
    }

    private static boolean between(LocalDate date, LocalDate start, LocalDate end) {
        return equalOrAfter(date, start) && equalOrBefore(date, end);
    }

    /**
     * Check if this context is relevant to the given end date. A context is relevant if it has a period type of
     * Instant and the date of the instant matches the given end date. A context is also relevant if it has a
     * period of type Duration and the given end date falls within the duration.
     *
     * @param periodEndDate the end date to assess relevance
     * @return true if this context is relevant as described above and false otherwise.
     */
    public boolean isRelevantTo(LocalDate periodEndDate) {
        Period period = this.getPeriod();
        if (period instanceof Instant instant) {
            return instant.getDate().equals(periodEndDate);
        } else {
            assert (period instanceof Duration);
            Duration duration = (Duration)period;
            return between(periodEndDate, duration.getStartDate(), duration.getEndDate());
        }
    }

    /**
     * Get fact corresponding to Concept.
     *
     * @param concept The concept for the fact
     * @return The fact is one is available, null otherwise
     */
    public Fact getFact(Concept concept) {
        /* Look for a direct match - we should be considering the decimals of facts before returning them but are not */
        for (Fact fact : facts) {
            if (fact.getConcept().equals(concept))
                return fact;
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Context context = (Context) o;

        return id.equals(context.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public String toString() {
        final int limit = 15;
        String contextId;
        if (id.length() > limit) {
            contextId = id.substring(0, (limit - 3) >> 1) + "..." + id.substring(id.length() - ((limit - 3) >> 1));
        } else {
            contextId = id;
        }
        return String.format("Context(%s, %s, %s)", contextId, period, entity);
    }

    public static Context fromElement(DiscoverableTaxonomySet dts, Element element) {
        Namespace namespace = element.getNamespace();
        String namespaceUri = namespace.getURI();

        String id = element.attributeValue(ID_TAG);

        Entity entity = null;
        Period period = null;
        for (Element child : element.elements()) {
            String childName = child.getName();
            switch (childName) {
                case TagNames.ENTITY_TAG:
                    entity = Entity.fromElement(dts, child);
                    break;
                case TagNames.PERIOD_TAG:
                    period = Period.fromElement(child);
                    break;
                default:
                    throw new UnsupportedElementException(CONTEXT_TAG, childName);
            }
        }
        return new Context(namespaceUri, id, entity, period);
    }
}
