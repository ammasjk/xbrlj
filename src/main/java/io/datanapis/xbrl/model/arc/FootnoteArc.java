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
package io.datanapis.xbrl.model.arc;

import io.datanapis.xbrl.DiscoverableTaxonomySet;
import io.datanapis.xbrl.TagNames;
import io.datanapis.xbrl.model.Footnote;
import io.datanapis.xbrl.model.link.FootnoteLink;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.HashSet;
import java.util.Set;

public final class FootnoteArc extends FromArc {
    private static final Set<String> FOOTNOTE_ARC_ATTRIBUTES = new HashSet<>();
    static {
        FOOTNOTE_ARC_ATTRIBUTES.addAll(ABSTRACT_ARC_ATTRIBUTES);
        FOOTNOTE_ARC_ATTRIBUTES.add(TagNames.TO_TAG);
    }

    /* Sample FootnoteArc
       <us-gaap:Liabilities contextRef="FI2019Q4" decimals="-6" id="d40416998e2881-wk-Fact-CBD62550305A54368407322E8ECA8DF0"
                unitRef="usd">2426049000000</us-gaap:Liabilities>
       <link:footnote id="TextSelection-615160EDDB8555F88D02BB49EA009EB8-0-wk-Footnote-615160EDDB8555F88D02BB49EA009EB8_lbl"
             xlink:label="TextSelection-615160EDDB8555F88D02BB49EA009EB8-0-wk-Footnote-615160EDDB8555F88D02BB49EA009EB8_lbl"
             xlink:role="http://www.xbrl.org/2003/role/footnote" xlink:type="resource" xml:lang="en-US">
                The following table presents information on assets and liabilities related to VIEs ... </link:footnote>
       <link:loc
         xlink:href="#d40416998e2881-wk-Fact-CBD62550305A54368407322E8ECA8DF0"
         xlink:label="d40416998e2881-wk-Fact-CBD62550305A54368407322E8ECA8DF0"
         xlink:type="locator"/>
       <link:footnoteArc
         xlink:arcrole="http://www.xbrl.org/2003/arcrole/fact-footnote"
         xlink:from="d40416998e2881-wk-Fact-CBD62550305A54368407322E8ECA8DF0"  -- This references a fact through a location
         xlink:to="TextSelection-615160EDDB8555F88D02BB49EA009EB8-0-wk-Footnote-615160EDDB8555F88D02BB49EA009EB8_lbl"  -- This is a footnote
         xlink:type="arc"/>
     */
    private Footnote to;

    public Footnote getTo() {
        return to;
    }

    public static FootnoteArc fromElement(DiscoverableTaxonomySet dts, String sourceUrl, FootnoteLink link, Element element) {
        FootnoteArc arc = new FootnoteArc(sourceUrl);

        arc.readElement(dts, link, element);

        String to = element.attributeValue(TagNames.TO_TAG);
        arc.to = link.getFootnote(to);
        if (arc.to == null) {
            log.info("(To) Footnote not found [{}]", to);
        }

        for (Attribute attribute : element.attributes()) {
            if (!FOOTNOTE_ARC_ATTRIBUTES.contains(attribute.getName())) {
                log.debug("Ignoring attribute [{}] in [{}]", attribute.getName(), element.getQualifiedName());
            }
        }

        return arc;
    }

    private FootnoteArc(String sourceUrl) {
        super(sourceUrl);
    }
}
