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
package io.datanapis.xbrl.analysis;

import io.datanapis.xbrl.model.*;

/**
 * A callback interface to receive notifications about a presentation network. Each role-type will have a
 * distinct presentation network.
 */
public interface PresentationProcessor {
    enum ReportingPeriodType {
        MRQ("MRQ"),                      // Most recent quarter
        PRIOR_QUARTER("PQ"),             // Prior quarter
        YTD("YTD"),                      // Year to date, used only for year-to-date results
        FY("FY"),                        // Financial year, used only for full-year duration results
        MOST_RECENT_VALUE("MRV"),        // Most recent value - for point-in-time values such as balance sheets
        PRIOR_YEAR_QUARTER("PYQ"),       // Same quarter results for previous year
        PRIOR_YEAR_TO_DATE("PYTD"),      // YTD for the previous year
        PFY("PFY"),                      // Previous financial year
        BEGINNING_OF_PERIOD("BOP"),      // Beginning of period - only valid for instants
        NOT_CLASSIFIED("Unclassified");

        private final String value;

        ReportingPeriodType(String value) {
            this.value = value;
        }

        public String toString() {
            return this.value;
        }
    }

    /* Should we create a fictitious internal node for grouping dimensioned facts - makes presentation pretty in some cases */
    default boolean groupDimensionedFacts() {
        return true;
    }

    default boolean skipTables() {
        return true;
    }

    /* Start of a presentation network */
    default void start(RoleType roleType, TimeOrdered<DimensionedFact> facts) {}

    /* End of a presentation network */
    default void end(RoleType roleType) {}

    /* Start of a root within a presentation network */
    default void rootStart(PresentationGraphNode root) {}

    default void rootStart(PresentationGraphNode root, PresentationInfoProvider infoProvider) {
        rootStart(root);
    }

    /* End of a root within a presentation network */
    default void rootEnd(PresentationGraphNode root) {}

    default void rootEnd(PresentationGraphNode root, PresentationInfoProvider infoProvider) {
        rootEnd(root);
    }

    /* Start of a period within a root */
    default void periodStart(PresentationGraphNode root, Period period, ReportingPeriodType rpType) {}

    /* End of a period within a root */
    default void periodEnd(PresentationGraphNode root, Period period) {}

    default void periodEnd(PresentationGraphNode root, Period period, ReportingPeriodType rpType) {
        periodEnd(root, period);
    }

    /* Start of an internal node within the network */
    default void internalNodeStart(PresentationGraphNode node, int level) {}

    /* End of an internal node within the network */
    default void internalNodeEnd(PresentationGraphNode node, int level) {}

    /* A fact within the network */
    default void lineItem(PresentationGraphNode node, int level, Fact fact) {}

    /* Exists for backward compatibility. Not called directly */
    default void lineItem(PresentationGraphNode node, int level, DimensionedFact fact) {}

    /* A dimensioned fact within the network */
    default void lineItem(PresentationGraphNode node, int level, DimensionedFact fact, PresentationInfoProvider infoProvider) {
        lineItem(node, level, fact);
    }

    /* invoked when all role-types have been walked */
    default void complete() {}
}
