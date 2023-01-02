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
import io.datanapis.xbrl.model.arc.DefinitionArc;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.Set;
import java.util.HashSet;

public final class DefinitionLink extends DirectedAcyclicLink<DefinitionArc> {
    private static final Set<String> DEFINITION_LINK_ATTRIBUTES = new HashSet<>();
    static {
        DEFINITION_LINK_ATTRIBUTES.addAll(ABSTRACT_LINK_ATTRIBUTES);
        DEFINITION_LINK_ATTRIBUTES.add(TagNames.ID_TAG);
    }

    /* Sample Definition Link:
       <link:definitionLink xlink:role="http://www.berkshirebank.com/role/BasisOfPresentationAdditionalInformationDetails" xlink:type="extended">
           <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_NewAccountingPronouncementsOrChangeInAccountingPrincipleLineItems" xlink:label="loc_us-gaap_NewAccountingPronouncementsOrChangeInAccountingPrincipleLineItems_D19431EE021C4A30DB789445DB7EAA4F" xlink:type="locator" />
           <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_NewAccountingPronouncementsOrChangeInAccountingPrincipleTable" xlink:label="loc_us-gaap_NewAccountingPronouncementsOrChangeInAccountingPrincipleTable_027FBC25E0ECF0C8C0029445DB7D3C79" xlink:type="locator" />
           <link:definitionArc order="1" xlink:arcrole="http://xbrl.org/int/dim/arcrole/all" xlink:from="loc_us-gaap_NewAccountingPronouncementsOrChangeInAccountingPrincipleLineItems_D19431EE021C4A30DB789445DB7EAA4F" xlink:to="loc_us-gaap_NewAccountingPronouncementsOrChangeInAccountingPrincipleTable_027FBC25E0ECF0C8C0029445DB7D3C79" xlink:type="arc" xbrldt:closed="true" xbrldt:contextElement="segment" />
           <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_AdjustmentsForNewAccountingPronouncementsAxis" xlink:label="loc_us-gaap_AdjustmentsForNewAccountingPronouncementsAxis_2FA51922C052818B2DE19445DB7EFBAC" xlink:type="locator" />
           <link:definitionArc order="1" xlink:arcrole="http://xbrl.org/int/dim/arcrole/hypercube-dimension" xlink:from="loc_us-gaap_NewAccountingPronouncementsOrChangeInAccountingPrincipleTable_027FBC25E0ECF0C8C0029445DB7D3C79" xlink:to="loc_us-gaap_AdjustmentsForNewAccountingPronouncementsAxis_2FA51922C052818B2DE19445DB7EFBAC" xlink:type="arc" />
           <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_TypeOfAdoptionMember" xlink:label="loc_us-gaap_TypeOfAdoptionMember_CB0593B58B02046A436E9445DB7EA902_default" xlink:type="locator" />
           <link:definitionArc order="1" xlink:arcrole="http://xbrl.org/int/dim/arcrole/dimension-default" xlink:from="loc_us-gaap_AdjustmentsForNewAccountingPronouncementsAxis_2FA51922C052818B2DE19445DB7EFBAC" xlink:to="loc_us-gaap_TypeOfAdoptionMember_CB0593B58B02046A436E9445DB7EA902_default" xlink:type="arc" />
           <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_TypeOfAdoptionMember" xlink:label="loc_us-gaap_TypeOfAdoptionMember_CB0593B58B02046A436E9445DB7EA902" xlink:type="locator" />
           <link:definitionArc order="1" xlink:arcrole="http://xbrl.org/int/dim/arcrole/dimension-domain" xlink:from="loc_us-gaap_AdjustmentsForNewAccountingPronouncementsAxis_2FA51922C052818B2DE19445DB7EFBAC" xlink:to="loc_us-gaap_TypeOfAdoptionMember_CB0593B58B02046A436E9445DB7EA902" xlink:type="arc" />
           <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_AccountingStandardsUpdate201602Member" xlink:label="loc_us-gaap_AccountingStandardsUpdate201602Member_9D046BF588CF91BEF513946DACC28851" xlink:type="locator" />
           <link:definitionArc order="1" xlink:arcrole="http://xbrl.org/int/dim/arcrole/domain-member" xlink:from="loc_us-gaap_TypeOfAdoptionMember_CB0593B58B02046A436E9445DB7EA902" xlink:to="loc_us-gaap_AccountingStandardsUpdate201602Member_9D046BF588CF91BEF513946DACC28851" xlink:type="arc" />
           <link:loc xlink:href="bhlb-20190331.xsd#bhlb_LeaseRightofUseAsset" xlink:label="loc_bhlb_LeaseRightofUseAsset_5830480373A7EFDB50F9946ED6C2D239" xlink:type="locator" />
           <link:definitionArc order="1" xlink:arcrole="http://xbrl.org/int/dim/arcrole/domain-member" xlink:from="loc_us-gaap_NewAccountingPronouncementsOrChangeInAccountingPrincipleLineItems_D19431EE021C4A30DB789445DB7EAA4F" xlink:to="loc_bhlb_LeaseRightofUseAsset_5830480373A7EFDB50F9946ED6C2D239" xlink:type="arc" />
           <link:loc xlink:href="bhlb-20190331.xsd#bhlb_LeaseLiability" xlink:label="loc_bhlb_LeaseLiability_F82AFC5756577B96F919946F04BD5E17" xlink:type="locator" />
           <link:definitionArc order="2" xlink:arcrole="http://xbrl.org/int/dim/arcrole/domain-member" xlink:from="loc_us-gaap_NewAccountingPronouncementsOrChangeInAccountingPrincipleLineItems_D19431EE021C4A30DB789445DB7EAA4F" xlink:to="loc_bhlb_LeaseLiability_F82AFC5756577B96F919946F04BD5E17" xlink:type="arc" />
       </link:definitionLink>
     */
    private String id;

    public String getId() {
        return id;
    }

    public static DefinitionLink fromElement(String sourceUrl, DiscoverableTaxonomySet dts, Element element) {
        DefinitionLink link = new DefinitionLink();

        link.readElement(dts, element);
        link.id = element.attributeValue(TagNames.ID_TAG);

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
            if (TagNames.DEFINITION_ARC_TAG.equals(childName)) {
                DefinitionArc arc = DefinitionArc.fromElement(dts, sourceUrl, link, child);
                link.addArc(arc);
            } else if (!TagNames.LOC_TAG.equals(childName)) {
                log.info("Ignoring element [{}] in [{}}]", childName, element.getQualifiedName());
            }
        }

        for (Attribute attribute : element.attributes()) {
            if (!DEFINITION_LINK_ATTRIBUTES.contains(attribute.getName())) {
                log.info("Ignoring attribute [{}] in [{}}]", attribute.getName(), element.getQualifiedName());
            }
        }

        link.getRole().setDefinitionLink(link);
        return link;
    }

    private DefinitionLink() {
    }
}
