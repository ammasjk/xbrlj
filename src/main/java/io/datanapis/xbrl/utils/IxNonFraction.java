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
package io.datanapis.xbrl.utils;

import io.datanapis.xbrl.TagNames;
import org.dom4j.Element;

public class IxNonFraction {
    private final int decimals;
    private final int scale;
    private final String format;
    private final String unitRef;
    private final String value;

    private IxNonFraction(int decimals, int scale, String format, String unitRef, String value) {
        this.decimals = decimals;
        this.scale = scale;
        this.format = format;
        this.unitRef = unitRef;
        this.value = value;
    }

    public int getDecimals() {
        return decimals;
    }

    public int getScale() {
        return scale;
    }

    public String getFormat() {
        return format;
    }

    public String getUnitRef() {
        return unitRef;
    }

    public String getValue() {
        return value;
    }

    public long asLong() {
        return Utils.asLong(value);
    }

    public long asScaledLong() {
        return asLong() * (long)Math.pow(10, scale);
    }

    public int asInt() {
        return Utils.asInt(value);
    }

    public double asDouble() {
        double doubleValue = Double.parseDouble(value);
        return doubleValue;
    }

    public double asScaledDouble() {
        return asDouble() * Math.pow(10, scale);
    }

    public static class Builder {
        private final String format;
        private final String unitRef;
        private int decimals;
        private final int scale;
        private String value;

        public Builder(Element element) {
            String name = element.getName();
            if (!name.equals(TagNames.NON_FRACTION_TAG))
                throw new RuntimeException("Invalid Element [" + name + "]. Expecting nonFraction");

            format = element.attributeValue(TagNames.FORMAT_TAG);
            unitRef = element.attributeValue(TagNames.UNIT_REF_TAG);

            String d = element.attributeValue(TagNames.DECIMALS_TAG);
            if (d != null && !d.equals("INF")) {
                try {
                    decimals = Integer.parseInt(d);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else {
                decimals = Integer.MAX_VALUE;
            }

            String s = element.attributeValue(TagNames.SCALE_TAG);
            if (s != null) {
                scale = Utils.asInt(s);
            } else {
                scale = 0;
            }

            value = element.getText();
            value = value.replace(",", "");
        }

        public IxNonFraction build() {
            return new IxNonFraction(decimals, scale, format, unitRef, value);
        }
    }
}
