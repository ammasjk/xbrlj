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
import io.datanapis.xbrl.utils.XmlUtils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class Unit {
    private static final Logger log = LoggerFactory.getLogger(Unit.class);
    public static final Unit USD = new Unit("usd", new Measure("iso4217:USD"));
    public static final Unit CAD = new Unit("cad", new Measure("iso4217:CAD"));
    public static final Unit EUR = new Unit("eur", new Measure("iso4217:EUR"));
    public static final Unit GBP = new Unit("gbp", new Measure("iso4217:GBP"));
    public static final Unit JPY = new Unit("jpy", new Measure("iso4217:JPY"));

    /* Sample Unit
       <xbrli:unit id="usdPerShare">
               <xbrli:divide>
                       <xbrli:unitNumerator>
                               <xbrli:measure>iso4217:USD</xbrli:measure>
                       </xbrli:unitNumerator>
                       <xbrli:unitDenominator>
                               <xbrli:measure>xbrli:shares</xbrli:measure>
                       </xbrli:unitDenominator>
               </xbrli:divide>
       </xbrli:unit>
       <xbrli:unit id="usd">
               <xbrli:measure>iso4217:USD</xbrli:measure>
       </xbrli:unit>
     */
    private final String id;
    private final Type type;
    private final List<Measure> measures;
    private final Fraction fraction;

    public enum Type {
        MEASURE,
        FRACTION
    }

    private Unit(String id, Measure measure) {
        this.id = id;
        type = Type.MEASURE;
        measures = new ArrayList<>();
        measures.add(measure);
        fraction = null;
    }

    private Unit(String id, List<Measure> measures) {
        if (measures.size() > 1) {
            log.info("Constructing unit with [{}] measures", measures.size());
        }
        this.id = id;
        type = Type.MEASURE;
        this.measures = measures;
        fraction = null;
    }

    private Unit(String id, Fraction fraction) {
        this.id = id;
        type = Type.FRACTION;
        this.fraction = fraction;
        this.measures = null;
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public Measure getMeasure() {
        return this.measures.get(0);
    }

    public List<Measure> getMeasures() {
        return this.measures;
    }

    public Fraction getFraction() {
        return this.fraction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Unit unit = (Unit) o;

        if (type != unit.type) return false;
        if (type == Type.FRACTION) {
            return fraction.equals(unit.fraction);
        } else {
            if (measures.size() != unit.measures.size()) {
                return false;
            } else if (measures.size() == 1) {
                return measures.get(0).equals(unit.measures.get(0));
            } else if (measures.size() == 2) {
                return (measures.get(0).equals(unit.measures.get(0)) && measures.get(1).equals(unit.measures.get(1))) ||
                        (measures.get(0).equals(unit.measures.get(1)) && measures.get(1).equals(unit.measures.get(0)));
            } else {
                throw new RuntimeException("Not Implemented");
            }
        }
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (measures != null ? measures.hashCode() : 0);
        result = 31 * result + (fraction != null ? fraction.hashCode() : 0);
        return result;
    }

    public String toString() {
        if (type == Type.FRACTION) {
            return fraction.toString();
        } else if (type == Type.MEASURE) {
            StringBuilder builder = new StringBuilder();
            for (Measure measure : measures) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(measure.toString());
            }
            return builder.toString();
        }
        return "Unit";
    }

    public static Unit fromElement(Element element) {
        String id = element.attributeValue(TagNames.ID_TAG);

        Fraction fraction = null;
        List<Measure> measures = new ArrayList<>();
        for (Element child : element.elements()) {
            String childName = child.getName();
            switch (childName) {
                case TagNames.MEASURE_TAG:
                    Measure measure = new Measure(child.getText());
                    measures.add(measure);
                    break;
                case TagNames.DIVIDE_TAG:
                    Measure numerator = new Measure(XmlUtils.getChild(child, TagNames.UNIT_NUMERATOR_TAG, TagNames.MEASURE_TAG).getText());
                    Measure denominator = new Measure(XmlUtils.getChild(child, TagNames.UNIT_DENOMINATOR_TAG, TagNames.MEASURE_TAG).getText());
                    fraction = new Fraction(numerator, denominator);
                    break;
                default:
                    log.info("Ignoring element [{}] in [{}]", childName, element.getQualifiedName());
                    throw new UnsupportedElementException(TagNames.UNIT_TAG, child.getName());
            }
        }

        if (fraction != null) {
            return new Unit(id, fraction);
        } else {
            return new Unit(id, measures);
        }
    }

    static public class Measure {
        private final String unit;
        private final String trimmedUnit;

        private Measure(String unit) {
            this.unit = unit;
            int index = unit.indexOf(':');
            if (index > 0) {
                trimmedUnit = unit.substring(index + 1);
            } else {
                trimmedUnit = unit;
            }
        }

        public String getUnit() {
            return unit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Measure measure = (Measure) o;

            return unit.equals(measure.unit);
        }

        @Override
        public int hashCode() {
            return unit.hashCode();
        }

        public String toString() {
            return trimmedUnit;
        }
    }
    static public class Fraction {
        private final Measure numerator;
        private final Measure denominator;

        public Measure getNumerator() {
            return numerator;
        }

        public Measure getDenominator() {
            return denominator;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Fraction fraction = (Fraction) o;

            if (!numerator.equals(fraction.numerator)) return false;
            return denominator.equals(fraction.denominator);
        }

        @Override
        public int hashCode() {
            int result = numerator.hashCode();
            result = 31 * result + denominator.hashCode();
            return result;
        }

        public String toString() {
            return numerator.toString() + "/" + denominator.toString();
        }

        private Fraction(Measure numerator, Measure denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }
    }
}
