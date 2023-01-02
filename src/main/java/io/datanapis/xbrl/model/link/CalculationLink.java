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

import io.datanapis.xbrl.TagNames;
import io.datanapis.xbrl.DiscoverableTaxonomySet;
import io.datanapis.xbrl.model.Location;
import io.datanapis.xbrl.model.arc.CalculationArc;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.*;

public final class CalculationLink extends DirectedAcyclicLink<CalculationArc> {
    private static final Set<String> CALCULATION_LINK_ATTRIBUTES = new HashSet<>();
    static {
        CALCULATION_LINK_ATTRIBUTES.addAll(ABSTRACT_LINK_ATTRIBUTES);
    }

    /* Sample Calculation Link:
       <link:calculationLink xlink:role="http://www.berkshirebank.com/role/BorrowedFundsSummaryOfBorrowedFundsDetails" xlink:type="extended">
         <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_DebtAndCapitalLeaseObligations" xlink:label="loc_us-gaap_DebtAndCapitalLeaseObligations_7af59755-3d0e-afb6-d02a-69abd398a504" xlink:type="locator" />
         <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_ShortTermBorrowings" xlink:label="loc_us-gaap_ShortTermBorrowings_422d7c41-753d-7765-5880-50978d071d31" xlink:type="locator" />
         <link:calculationArc order="1" weight="1" xlink:arcrole="http://www.xbrl.org/2003/arcrole/summation-item" xlink:from="loc_us-gaap_DebtAndCapitalLeaseObligations_7af59755-3d0e-afb6-d02a-69abd398a504" xlink:to="loc_us-gaap_ShortTermBorrowings_422d7c41-753d-7765-5880-50978d071d31" xlink:type="arc" />
         <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_LongTermDebt" xlink:label="loc_us-gaap_LongTermDebt_80dca0ba-44dc-8565-fb9b-c2edfc8c357d" xlink:type="locator" />
         <link:calculationArc order="2" weight="1" xlink:arcrole="http://www.xbrl.org/2003/arcrole/summation-item" xlink:from="loc_us-gaap_DebtAndCapitalLeaseObligations_7af59755-3d0e-afb6-d02a-69abd398a504" xlink:to="loc_us-gaap_LongTermDebt_80dca0ba-44dc-8565-fb9b-c2edfc8c357d" xlink:type="arc" />
       </link:calculationLink>
       <link:calculationLink xlink:role="http://www.berkshirebank.com/role/BorrowedFundsSummaryOfMaturitiesOfFhlbDetails" xlink:type="extended">
         <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_AdvancesFromFederalHomeLoanBanks" xlink:label="loc_us-gaap_AdvancesFromFederalHomeLoanBanks_321f257b-7432-8d3b-21bd-9be1091575a8" xlink:type="locator" />
         <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_FederalHomeLoanBankAdvancesMaturitiesSummaryDueInNextRollingTwelveMonths" xlink:label="loc_us-gaap_FederalHomeLoanBankAdvancesMaturitiesSummaryDueInNextRollingTwelveMonths_6d4a4fd1-ec91-24ab-78b7-475fcfb12b38" xlink:type="locator" />
         <link:calculationArc order="1" weight="1" xlink:arcrole="http://www.xbrl.org/2003/arcrole/summation-item" xlink:from="loc_us-gaap_AdvancesFromFederalHomeLoanBanks_321f257b-7432-8d3b-21bd-9be1091575a8" xlink:to="loc_us-gaap_FederalHomeLoanBankAdvancesMaturitiesSummaryDueInNextRollingTwelveMonths_6d4a4fd1-ec91-24ab-78b7-475fcfb12b38" xlink:type="arc" />
         <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_FederalHomeLoanBankAdvancesMaturitiesSummaryDueInRollingYearTwo" xlink:label="loc_us-gaap_FederalHomeLoanBankAdvancesMaturitiesSummaryDueInRollingYearTwo_36f07adb-8449-02ae-c25b-ae95852ff329" xlink:type="locator" />
         <link:calculationArc order="2" weight="1" xlink:arcrole="http://www.xbrl.org/2003/arcrole/summation-item" xlink:from="loc_us-gaap_AdvancesFromFederalHomeLoanBanks_321f257b-7432-8d3b-21bd-9be1091575a8" xlink:to="loc_us-gaap_FederalHomeLoanBankAdvancesMaturitiesSummaryDueInRollingYearTwo_36f07adb-8449-02ae-c25b-ae95852ff329" xlink:type="arc" />
         <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_FederalHomeLoanBankAdvancesMaturitiesSummaryDueInRollingYearThree" xlink:label="loc_us-gaap_FederalHomeLoanBankAdvancesMaturitiesSummaryDueInRollingYearThree_fe6f1f23-4750-c3b9-40d3-21595c786977" xlink:type="locator" />
         <link:calculationArc order="3" weight="1" xlink:arcrole="http://www.xbrl.org/2003/arcrole/summation-item" xlink:from="loc_us-gaap_AdvancesFromFederalHomeLoanBanks_321f257b-7432-8d3b-21bd-9be1091575a8" xlink:to="loc_us-gaap_FederalHomeLoanBankAdvancesMaturitiesSummaryDueInRollingYearThree_fe6f1f23-4750-c3b9-40d3-21595c786977" xlink:type="arc" />
         <link:loc xlink:href="bhlb-20190331.xsd#bhlb_FederalHomeLoanBankAdvancesMaturitiesSummaryDueAfterRollingYearFour" xlink:label="loc_bhlb_FederalHomeLoanBankAdvancesMaturitiesSummaryDueAfterRollingYearFour_92325fca-c6f4-fc1d-05d4-880799bbd1c1" xlink:type="locator" />
         <link:calculationArc order="4" weight="1" xlink:arcrole="http://www.xbrl.org/2003/arcrole/summation-item" xlink:from="loc_us-gaap_AdvancesFromFederalHomeLoanBanks_321f257b-7432-8d3b-21bd-9be1091575a8" xlink:to="loc_bhlb_FederalHomeLoanBankAdvancesMaturitiesSummaryDueAfterRollingYearFour_92325fca-c6f4-fc1d-05d4-880799bbd1c1" xlink:type="arc" />
         <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_FederalHomeLoanBankAdvancesMaturitiesSummaryDueInRollingYearFour" xlink:label="loc_us-gaap_FederalHomeLoanBankAdvancesMaturitiesSummaryDueInRollingYearFour_2386f29a-c574-ce56-95f8-43e298cee626" xlink:type="locator" />
         <link:calculationArc order="5" weight="1" xlink:arcrole="http://www.xbrl.org/2003/arcrole/summation-item" xlink:from="loc_us-gaap_AdvancesFromFederalHomeLoanBanks_321f257b-7432-8d3b-21bd-9be1091575a8" xlink:to="loc_us-gaap_FederalHomeLoanBankAdvancesMaturitiesSummaryDueInRollingYearFour_2386f29a-c574-ce56-95f8-43e298cee626" xlink:type="arc" />
       </link:calculationLink>
     */

    public Location getLocation(String label) {
        return locations.getOrDefault(label, null);
    }

    public static CalculationLink fromElement(String sourceUrl, DiscoverableTaxonomySet dts, Element element) {
        CalculationLink link = new CalculationLink();

        link.readElement(dts, element);

        /* Process all locations first */
        for (Element child : element.elements()) {
            String childName = child.getName();
            if (TagNames.LOC_TAG.equals(childName)) {
                Location location = Location.fromElement(sourceUrl, child);
                link.locations.put(location.getLabel(), location);
            }
        }
        for (Element child : element.elements()) {
            String childName = child.getName();
            if (TagNames.CALCULATION_ARC_TAG.equals(childName)) {
                CalculationArc arc = CalculationArc.fromElement(dts, sourceUrl, link, child);
                link.addArc(arc);
            } else if (!TagNames.LOC_TAG.equals(childName)) {
                log.info("Ignoring element [{}] in [{}}]", childName, element.getQualifiedName());
            }
        }

        for (Attribute attribute : element.attributes()) {
            if (!CALCULATION_LINK_ATTRIBUTES.contains(attribute.getName())) {
                log.info("Ignoring attribute [{}] in [{}}]", attribute.getName(), element.getQualifiedName());
            }
        }

        link.getRole().setCalculationLink(link);
        return link;
    }

    private CalculationLink() {
    }
}
