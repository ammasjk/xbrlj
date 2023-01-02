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

import io.datanapis.xbrl.model.Context;
import io.datanapis.xbrl.model.Fact;
import io.datanapis.xbrl.model.RoleType;

/**
 * A callback interface to receive notifications about a calculation network. Each role-type has a distinct
 * calculation network
 */
public interface CalculationProcessor {

    /* indicates the start of a calculation network */
    default void calculationStart(RoleType roleType) {}

    /* indicates the end of a calculation network */
    default void calculationEnd(RoleType roleType) {}

    /* indicates the start of a root node within a calculation network */
    default void calculationRootStart(CalculationGraphNode root) {}

    /* indicates the end of a root node within a calculation network */
    default void calculationRootEnd(CalculationGraphNode root) {}

    /* indicates the start of a period within a root node */
    default void calculationPeriodStart(CalculationGraphNode root, Context context) {}

    /* indicates the end of a period within a root node */
    default void calculationPeriodEnd(CalculationGraphNode root, Context context) {}

    /* indicates the start of a node within a period */
    default void calculationNodeStart(int level, CalculationGraphNode node, Fact fact) {}

    /* indicates the end of a node within a period */
    default void calculationNodeEnd(int level, CalculationGraphNode node, CalculationNetwork.Result result, double computed) {}
}
