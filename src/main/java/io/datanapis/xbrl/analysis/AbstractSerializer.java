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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.datanapis.xbrl.model.Duration;
import io.datanapis.xbrl.model.Instant;
import io.datanapis.xbrl.model.Period;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public abstract sealed class AbstractSerializer permits PresentationSerializer, CalculationSerializer {
    protected static final String AXIS = "axis";
    protected static final String AXIS_VALUE = "axisValue";
    protected static final String COMPONENTS = "components";
    protected static final String DECIMALS = "decimals";
    protected static final String DIMENSIONS = "dimensions";
    protected static final String END_DATE = "endDate";
    protected static final String MEMBER = "member";
    protected static final String MEMBER_VALUE = "memberValue";
    protected static final String NAME = "name";
    protected static final String PERIOD = "period";
    protected static final String REPORTING_PERIODS = "reportingPeriods";
    protected static final String RP_TYPE = "rpType";
    protected static final String START_DATE = "startDate";
    protected static final String SORT_CODE = "sortCode";
    protected static final String TITLE = "title";
    protected static final String DATA = "data";
    protected static final String TYPE = "type";
    protected static final String UNIT = "unit";

    protected JsonObject currentRoleType = null;
    protected JsonArray reportingPeriods = null;
    protected JsonObject currentPeriod = null;

    protected final boolean nest;
    private final List<JsonObject> data = new ArrayList<>();


    protected AbstractSerializer(boolean nest) {
        this.nest = nest;
    }

    protected void start() {
        currentRoleType = new JsonObject();
        reportingPeriods = new JsonArray();
    }

    protected void end() {
        currentRoleType.add(REPORTING_PERIODS, reportingPeriods);
        reportingPeriods = null;
        currentRoleType = null;
    }

    protected JsonObject asJson(Period period) {
        JsonObject p = new JsonObject();
        p.addProperty(TYPE, period.getType().toString());

        if (period.getType() == Period.Type.INSTANT) {
            p.addProperty(END_DATE, ((Instant)period).getDate().toString());
        } else {
            p.addProperty(START_DATE, ((Duration)period).getStartDate().toString());
            p.addProperty(END_DATE, ((Duration)period).getEndDate().toString());
        }

        return p;
    }

    protected JsonObject asJson(Period period, PresentationProcessor.ReportingPeriodType rpType) {
        JsonObject p = asJson(period);
        p.addProperty(RP_TYPE, rpType.toString());
        return p;
    }

    protected void periodStart(Period period, PresentationProcessor.ReportingPeriodType rpType) {
        currentPeriod = new JsonObject();
        currentPeriod.add(PERIOD, asJson(period, rpType));
    }

    protected void periodStart(Period period) {
        currentPeriod = new JsonObject();
        currentPeriod.add(PERIOD, asJson(period));
    }

    protected void periodEnd(Period period) {
        if (!nest) {
            JsonArray array = new JsonArray();
            for (JsonObject obj : data) {
                array.add(obj);
            }
            currentPeriod.add(DATA, array);
            data.clear();
        }
        reportingPeriods.add(currentPeriod);
        currentPeriod = null;
    }

    protected JsonObject nodeStart() {
        JsonObject object = new JsonObject();
        data.add(object);
        return object;
    }

    protected void nodeEnd() {
        if (!nest)
            return;

        JsonObject node = data.remove(data.size() - 1);
        if (data.isEmpty()) {
            currentPeriod.add(DATA, node);
        } else {
            /* Add child as a child of the top node. Create the components array if not already created */
            JsonArray components = top().getAsJsonArray(COMPONENTS);
            if (components == null) {
                components = new JsonArray();
                top().add(COMPONENTS, components);
            }
            components.add(node);
        }
    }

    protected JsonObject top() {
        return data.isEmpty() ? null : data.get(data.size()- 1);
    }
}
