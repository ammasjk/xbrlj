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

import io.datanapis.xbrl.TagNames;
import io.datanapis.xbrl.DiscoverableTaxonomySet;
import io.datanapis.xbrl.model.link.LabelLink;
import io.datanapis.xbrl.model.RoleLabelMap;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.HashSet;
import java.util.Set;

public final class LabelArc extends FromArc {
    private static final Set<String> LABEL_ARC_ATTRIBUTES = new HashSet<>();
    static {
        LABEL_ARC_ATTRIBUTES.addAll(ABSTRACT_ARC_ATTRIBUTES);
        LABEL_ARC_ATTRIBUTES.add(TagNames.TO_TAG);
    }

    /* Sample LabelArc:
       <link:labelArc order="1"
                      xlink:arcrole="http://www.xbrl.org/2003/arcrole/concept-label"
                      xlink:from="loc_us-gaap_DerivativeInstrumentRiskAxis_7D962C24A702A711624F94B0083A458D"
                      xlink:to="lab_us-gaap_DerivativeInstrumentRiskAxis_7D962C24A702A711624F94B0083A458D"
                      xlink:type="arc" />

       Sample from location:
       <link:loc xlink:href="http://xbrl.fasb.org/us-gaap/2018/elts/us-gaap-2018-01-31.xsd#us-gaap_DerivativeInstrumentRiskAxis"
                 xlink:label="loc_us-gaap_DerivativeInstrumentRiskAxis_7D962C24A702A711624F94B0083A458D"
                 xlink:type="locator" />

       Sample to labels:
       <link:label id="lab_us-gaap_DerivativeInstrumentRiskAxis_7D962C24A702A711624F94B0083A458D_terseLabel_en-US"
                   xlink:label="lab_us-gaap_DerivativeInstrumentRiskAxis_7D962C24A702A711624F94B0083A458D"
                   xlink:role="http://www.xbrl.org/2003/role/terseLabel"
                   xlink:type="resource" xml:lang="en-US">Derivative Instrument [Axis]</link:label>
       <link:label id="lab_us-gaap_DerivativeInstrumentRiskAxis_7D962C24A702A711624F94B0083A458D_label_en-US"
                   xlink:label="lab_us-gaap_DerivativeInstrumentRiskAxis_7D962C24A702A711624F94B0083A458D"
                   xlink:role="http://www.xbrl.org/2003/role/label"
                   xlink:type="resource" xml:lang="en-US">Derivative Instrument [Axis]</link:label>
     */
    private RoleLabelMap to;

    public RoleLabelMap getTo() {
        return to;
    }

    public static LabelArc fromElement(DiscoverableTaxonomySet dts, String sourceUrl, LabelLink link, Element element) {
        LabelArc arc = new LabelArc(sourceUrl);

        arc.readElement(dts, link, element);

        String to = element.attributeValue(TagNames.TO_TAG);
        arc.to = link.getLabel(to);
        if (arc.to == null) {
            log.info("(To) Label not found [{}]", to);
        }

        for (Attribute attribute : element.attributes()) {
            if (!LABEL_ARC_ATTRIBUTES.contains(attribute.getName())) {
                log.debug("Ignoring attribute [{}] in [{}]", attribute.getName(), element.getQualifiedName());
            }
        }

        return arc;
    }

    private LabelArc(String sourceUrl) {
        super(sourceUrl);
    }
}
