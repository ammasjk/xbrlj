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

import com.google.gson.*;
import io.datanapis.xbrl.XbrlInstance;
import io.datanapis.xbrl.model.Dei;
import io.datanapis.xbrl.model.Fact;
import lombok.Getter;
import org.dom4j.Namespace;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Map;
import java.util.Objects;

public class JsonSerializer {
    private static final String NUMBER_OF_CONTEXTS = "numberOfContexts";
    private static final String NUMBER_OF_UNITS = "numberOfUnits";
    private static final String NUMBER_OF_FACTS = "numberOfFacts";
    private static final String NUMBER_OF_UNIQUE_FACTS = "numberOfUniqueFacts";
    private static final String NUMBER_OF_FACT_CONCEPTS = "numberOfFactConcepts";
    private static final String NAMESPACE_FACT_COUNTS = "namespaceFactCounts";
    private static final String NAMESPACE = "namespace";
    private static final String PREFIX = "prefix";
    private static final String URI = "uri";
    private static final String COUNT = "count";
    private static final String UNUSED_FACTS = "unusedFacts";

    private static final String CALCULATIONS = "calculations";
    private static final String PRESENTATION = "presentation";
    private static final String DEI = "dei";
    private static final String META = "meta";
    private static final String STATISTICS = "statistics";
    private static final String UNUSED_STATISTICS = "unusedStatistics";

    private boolean prettyPrint = true;
    private boolean serializeNulls = false;
    private JsonObject presentation;
    private JsonArray calculations;
    private Dei dei;
    private XbrlInstance.Statistics statistics;
    private XbrlInstance.UnusedStatistics unusedStatistics;

    public JsonSerializer() {
    }

    public JsonSerializer prettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
        return this;
    }

    public JsonSerializer serializeNulls(boolean serializeNulls) {
        this.serializeNulls = serializeNulls;
        return this;
    }

    public JsonSerializer presentation(JsonObject presentation) {
        this.presentation = presentation;
        return this;
    }

    public JsonSerializer calculations(JsonArray calculations) {
        this.calculations = calculations;
        return this;
    }

    public JsonSerializer dei(Dei dei) {
        this.dei = dei;
        return this;
    }

    public JsonSerializer statistics(XbrlInstance.Statistics statistics) {
        this.statistics = statistics;
        return this;
    }

    public JsonSerializer unusedStatistics(XbrlInstance.UnusedStatistics unusedStatistics) {
        this.unusedStatistics = unusedStatistics;
        return this;
    }

    public String serialize() {
        JsonObject meta = new JsonObject();
        meta.add(STATISTICS, asJson(statistics));
        meta.add(UNUSED_STATISTICS, asJson(unusedStatistics));

        JsonObject root = new JsonObject();
        root.add(DEI, asJson(dei));
        root.add(PRESENTATION, presentation);
        root.add(CALCULATIONS, calculations);
        root.add(META, meta);

        GsonBuilder gsonBuilder = new GsonBuilder();
        if (prettyPrint) {
            gsonBuilder.setPrettyPrinting();
        }
        if (serializeNulls) {
            gsonBuilder.serializeNulls();
        }
        Gson gson = gsonBuilder.create();

        StringBuilder builder = new StringBuilder(1024 * 1024);
        gson.toJson(root, builder);

        return builder.toString();
    }

    private static JsonElement asJson(XbrlInstance.Statistics statistics) {
        JsonObject object = new JsonObject();
        if (statistics != null) {
            object.addProperty(NUMBER_OF_CONTEXTS, statistics.nOfContexts);
            object.addProperty(NUMBER_OF_FACTS, statistics.nOfFacts);
            object.addProperty(NUMBER_OF_UNITS, statistics.nOfUnits);
            object.addProperty(NUMBER_OF_UNIQUE_FACTS, statistics.nOfUniqueFacts);
            object.addProperty(NUMBER_OF_FACT_CONCEPTS, statistics.nOfFactConcepts);

            JsonArray array = new JsonArray();
            for (Map.Entry<Namespace,Integer> entry : statistics.namespaceFactCount.entrySet()) {
                JsonObject namespace = new JsonObject();
                namespace.addProperty(PREFIX, entry.getKey().getPrefix());
                namespace.addProperty(URI, entry.getKey().getURI());

                JsonObject count = new JsonObject();
                count.add(NAMESPACE, namespace);
                count.addProperty(COUNT, entry.getValue());
                array.add(count);
            }
            object.add(NAMESPACE_FACT_COUNTS, array);
        }

        return object;
    }

    private static JsonElement asJson(XbrlInstance.UnusedStatistics unusedStatistics) {
        JsonObject object = new JsonObject();
        if (unusedStatistics != null) {
            object.addProperty(NUMBER_OF_CONTEXTS, unusedStatistics.nOfContexts);
            object.addProperty(NUMBER_OF_FACTS, unusedStatistics.nOfFacts);
            object.addProperty(NUMBER_OF_FACT_CONCEPTS, unusedStatistics.nOfFactConcepts);

            JsonArray array = new JsonArray();
            for (Fact fact : unusedStatistics.unusedFacts) {
                array.add(fact.toString());
            }
            object.add(UNUSED_FACTS, array);
        }

        return object;
    }

    private static final String CIK = "cik";
    private static final String REGISTRANT_NAME = "registrantName";
    private static final String FILER_CATEGORY = "filerCategory";
    private static final String DATE_FILED = "dateFiled";
    private static final String FISCAL_YEAR = "fiscalYear";
    private static final String FISCAL_PERIOD = "fiscalPeriod";
    private static final String DOCUMENT_TYPE = "documentType";
    private static final String PERIOD_END_DATE = "periodEndDate";
    private static final String AMENDMENT_FLAG = "amendmentFlag";
    private static final String YEAR_END_DATE = "yearEndDate";

    private static JsonElement asJson(Dei dei) {
        JsonObject object = new JsonObject();
        if (dei != null) {
            Dei.DocumentInformation di = dei.getDocumentInformation();
            Dei.EntityInformation ei = dei.getEntityInformation();
            object.addProperty(CIK, ei.getCentralIndexKey());
            object.addProperty(REGISTRANT_NAME, ei.getRegistrantName());
            object.addProperty(FILER_CATEGORY, ei.getFilerCategory());
            if (dei.getDateFiled() != null) {
                object.addProperty(DATE_FILED, dei.getDateFiled().toString());
            } else {
                object.addProperty(DATE_FILED, (String)null);
            }
            LocalDate periodEndDate = dei.getEstimatedPeriodEndDate();
            object.addProperty(FISCAL_YEAR, Dei.guessFiscalYear(dei, periodEndDate));
            object.addProperty(FISCAL_PERIOD, Dei.guessFiscalPeriod(dei, periodEndDate));
            object.addProperty(DOCUMENT_TYPE, di.getDocumentType());
            if (dei.getPeriodEndDate() != null) {
                object.addProperty(PERIOD_END_DATE, di.getPeriodEndDate().toString());
            } else {
                object.addProperty(PERIOD_END_DATE, (String)null);
            }
            object.addProperty(AMENDMENT_FLAG, dei.isAmendmentFlag());
            MonthDay yearEndDate = dei.getYearEndDate();
            object.addProperty(YEAR_END_DATE, Objects.nonNull(yearEndDate) ? yearEndDate.toString() : null);
        }
        return object;
    }

    @Getter
    public static final class ValidationDetails {
        private final String definition;
        private final String calculationDetails;

        public ValidationDetails(String definition, String calculationDetails) {
            this.definition = definition;
            this.calculationDetails = calculationDetails;
        }
    }
}
