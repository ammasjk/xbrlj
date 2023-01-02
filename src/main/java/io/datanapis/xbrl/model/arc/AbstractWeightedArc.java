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
import io.datanapis.xbrl.model.Locator;
import org.dom4j.Element;

public abstract class AbstractWeightedArc extends FromArc {
    private double weight;

    public double getWeight() {
        return weight;
    }

    void readElement(DiscoverableTaxonomySet dts, Locator locator, Element element) {
        super.readElement(dts, locator, element);

        String value = element.attributeValue(TagNames.WEIGHT_TAG);
        if (value != null) {
            try {
                this.weight = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                log.info("NumberFormatException [{}]", value, e);
                this.weight = 1.0;
            }
        } else {
            log.info("Defaulting weight to 1.0");
            this.weight = 1.0;
        }
    }

    AbstractWeightedArc(String sourceUrl) {
        super(sourceUrl);
    }
}
