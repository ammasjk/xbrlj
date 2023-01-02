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
import io.datanapis.xbrl.model.*;
import io.datanapis.xbrl.model.arc.CalculationArc;
import io.datanapis.xbrl.utils.EdgarUtils;

import java.util.Collection;

public class CalculationSerializer extends AbstractSerializer implements CalculationProcessor {
    private final JsonArray calculations;

    public CalculationSerializer() {
        this.calculations = new JsonArray();
    }

    public JsonArray asJson() {
        return this.calculations;
    }

    public void calculationStart(RoleType roleType) {
        super.start();
    }

    public void calculationEnd(RoleType roleType) {
        String[] groups = EdgarUtils.splitDefinition(roleType);
        if (groups != null) {
            String sortCode = groups[0];
            String title = groups[2];

            currentRoleType.addProperty(SORT_CODE, sortCode);
            currentRoleType.addProperty(TITLE, title);
            calculations.add(currentRoleType);
        }

        super.end();
    }

    public void calculationPeriodStart(CalculationGraphNode root, Context context) {
        super.periodStart(context.getPeriod());
        JsonArray array = new JsonArray();
        if (context.getDimensions() != null) {
            Collection<ExplicitMember> dimensions = context.getDimensions();
            for (ExplicitMember member : dimensions) {
                JsonObject qualifier = new JsonObject();
                qualifier.addProperty(AXIS, member.getDimension().getQualifiedName());
                qualifier.addProperty(MEMBER, member.getMember().getQualifiedName());
                array.add(qualifier);
            }
        } else if (context.getTypedMembers() != null) {
            for (TypedMember member : context.getTypedMembers()) {
                JsonObject qualifier = new JsonObject();
                qualifier.addProperty(AXIS, member.getDimension().getQualifiedName());
                qualifier.addProperty(MEMBER, member.getMember());
                array.add(qualifier);
            }
        }
        if (!array.isEmpty()) {
            currentPeriod.add(DIMENSIONS, array);
        }
    }

    public void calculationPeriodEnd(CalculationGraphNode root, Context context) {
        super.periodEnd(context.getPeriod());
    }

    private static final String ARC_WEIGHT = "arcWeight";
    private static final String BALANCE = "balance";
    private static final String COMPUTED_VALUE = "computedValue";
    private static final String FACT_VALUE = "factValue";
    private static final String RESULT = "result";

    private static void addNumericProperty(JsonObject object, String property, double value) {
        if (value - (long)value < 0.0001) {
            object.addProperty(property, (long)value);
        } else {
            object.addProperty(property, value);
        }
    }

    private static void addProperties(JsonObject object, CalculationGraphNode node, Fact fact) {
        Concept concept = node.getConcept();
        object.addProperty(NAME, concept.getQualifiedName());
        CalculationArc arc = node.getArc();
        if (arc != null) {
            object.addProperty(ARC_WEIGHT, arc.getWeight());
        }
        object.addProperty(BALANCE, concept.getBalance().toString());
        object.addProperty(DECIMALS, fact.getDecimals());
        if (fact.getLongValue() != null) {
            object.addProperty(FACT_VALUE, fact.getLongValue());
        } else if (fact.getDoubleValue() != null) {
            object.addProperty(FACT_VALUE, fact.getDoubleValue());
        }
        object.addProperty(TYPE, concept.getTypeName());
        Unit unit = fact.getUnit();
        if (unit != null)
            object.addProperty(UNIT, unit.toString());
    }

    public void calculationNodeStart(int level, CalculationGraphNode node, Fact fact) {
        JsonObject object = super.nodeStart();
        addProperties(object, node, fact);
    }

    public void calculationNodeEnd(int level, CalculationGraphNode node, CalculationNetwork.Result result, double computedValue) {
        JsonObject object = top();
        if (object.has(COMPONENTS)) {
            object.addProperty(RESULT, result.name());
            addNumericProperty(object, COMPUTED_VALUE, computedValue);
        }
        super.nodeEnd();
    }
}
