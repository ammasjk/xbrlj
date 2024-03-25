package io.datanapis.xbrl.analysis;

import io.datanapis.xbrl.model.RoleType;

import java.util.Deque;

public interface CalculationNetworkConsumer {
    default void start(RoleType roleType) {}

    default void end(RoleType roleType) {}

    default void rootStart(CalculationGraphNode root) {}

    default void rootEnd(CalculationGraphNode root) {}

    default void nodeStart(CalculationGraphNode node, Deque<CalculationGraphNode> path) {}

    default void nodeEnd(CalculationGraphNode node, Deque<CalculationGraphNode> path) {}
}
