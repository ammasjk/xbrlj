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
import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;

public interface Period extends Comparable<Period> {
    /* For a sample see, Context */
    enum Type {
        INSTANT("instant"),
        DURATION("duration");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }

    Type getType();

    boolean equals(Object o);

    int hashCode();

    String toString();

    long durationInDays();

    boolean equalOrAtEnd(Period rhs);

    static boolean isInstant(Period period) {
        return period.getType().equals(Type.INSTANT);
    }

    static boolean isDuration(Period period) {
        return period.getType().equals(Type.DURATION);
    }

    @Override
    default int compareTo(@NotNull Period rhs) {
        switch (this.getType()) {
            case INSTANT:
                Instant i1 = (Instant)this;
                switch (rhs.getType()) {
                    case INSTANT:
                        Instant i2 = (Instant)rhs;
                        if (i1.getDate().isBefore(i2.getDate())) {
                            return -1;
                        } else if (i1.getDate().isAfter(i2.getDate())) {
                            return 1;
                        } else {
                            return 0;
                        }
                    case DURATION:
                        Duration r2 = (Duration)rhs;
                        if (i1.getDate().isBefore(r2.getStartDate())) {
                            return -1;
                        } else if (i1.getDate().isAfter(r2.getEndDate())) {
                            return 1;
                        } else {
                            /* i1 falls within r2. Prioritize duration over instant, helps with merging in PresentationNetwork */
                            return 1;
                        }
                }
                break;
            case DURATION:
                Duration r1 = (Duration)this;
                switch (rhs.getType()) {
                    case INSTANT:
                        Instant i2 = (Instant)rhs;
                        if (r1.getEndDate().isBefore(i2.getDate())) {
                            return -1;
                        } else if (r1.getStartDate().isAfter(i2.getDate())) {
                            return 1;
                        } else {
                            /* i2 falls within r1. Prioritize range over instant since instant begins later */
                            return -1;
                        }
                    case DURATION:
                        Duration r2 = (Duration)rhs;
                        if (r1.getEndDate().isBefore(r2.getEndDate())) {
                            return -1;
                        } else if (r1.getEndDate().isAfter(r2.getEndDate())) {
                            return 1;
                        }

                        /* r1 and r2 have the same end date, the shorter duration goes first */
                        if (r1.getStartDate().isBefore(r2.getStartDate())) {
                            return 1;
                        } else if (r1.getStartDate().isAfter(r2.getStartDate())) {
                            return -1;
                        }

                        return 0;
                }
                break;
        }
        return 0;
    }

    static Period fromElement(Element element) {
        String instant = null;
        String startDate = null;
        String endDate = null;
        for (Element child : element.elements()) {
            switch (child.getName()) {
                case TagNames.INSTANT_TAG:
                    instant = child.getTextTrim();
                    break;
                case TagNames.START_DATE_TAG:
                    startDate = child.getTextTrim();
                    break;
                case TagNames.END_DATE_TAG:
                    endDate = child.getTextTrim();
                    break;
                default:
                    throw new UnsupportedElementException(element.getName(), child.getName());
            }
        }

        if (instant != null) {
            return new Instant(Utils.asDate(instant));
        } else if (startDate != null && endDate != null) {
            return new Duration(Utils.asDate(startDate), Utils.asDate(endDate));
        }

        return null;
    }
}
