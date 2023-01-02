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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class Duration implements Period {
    private final LocalDate startDate, endDate;

    Duration(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Instant getEndDateAsInstant() {
        return new Instant(endDate);
    }

    public Type getType() {
        return Type.DURATION;
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

    public int compareTo(Duration rhs) {
        /*
         * Possible scenarios:
         * 1) |-----------| (lhs)
         *                         |-----------| (rhs)      Result: -1 i.e. lhs is before rhs
         *
         * 2) |-----------| (rhs)
         *                         |-----------| (lhs)      Result: 1 i.e. rhs is before lhs
         *
         * 3) |-----------| (rhs)
         *         |-----------| (lhs)                      Result: 1 since rhs starts before lhs
         *    We are treating the semantics where lhs is completely within rhs as the same as this.
         *
         * 4) |-----------| (lhs)
         *         |-----------| (rhs)                      Result: -1 since lhs starts before rhs
         *    We are treating the semantics where rhs is completely within lhs as the same as this.
         *
         * 5) If none of the above match, then the scenario is one of the following
         *    a)   |-----------| (lhs)
         *         |-----------| (rhs)
         *    b)   |-------|     (lhs)
         *         |-----------| (rhs)
         *    c)   |-----------| (lhs)
         *         |-------|     (rhs)
         */
        Duration lhs = this;
        if (lhs.endDate.isBefore(rhs.startDate)) {
            return -1;
        } else if (rhs.endDate.isBefore(lhs.startDate)) {
            return 1;
        } else if (between(lhs.startDate, rhs.startDate, rhs.endDate)) {
            /* We are not comparing end dates here. Assuming the semantics are consistent with this. */
            return 1;
        } else if (between(rhs.startDate, lhs.startDate, lhs.endDate)) {
            /* We are not comparing end dates here. Assuming, the semantics are consistent with this. */
            return -1;
        }

        assert lhs.startDate.equals(rhs.startDate);
        return lhs.endDate.compareTo(rhs.endDate);
    }

    @Override
    public long durationInDays() {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    @Override
    public boolean equalOrAtEnd(Period rhs) {
        if (rhs.getType() != Type.DURATION)
            return false;

        Duration outer = (Duration)rhs;
        return (this.startDate.equals(outer.startDate) || between(this.startDate, outer.startDate, outer.endDate)) &&
                this.endDate.equals(outer.endDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Duration duration = (Duration) o;

        if (!startDate.equals(duration.startDate)) return false;
        return endDate.equals(duration.endDate);
    }

    @Override
    public int hashCode() {
        int result = startDate.hashCode();
        result = 31 * result + endDate.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "(startDate: " + startDate + ", endDate: " + endDate + ')';
    }
}
