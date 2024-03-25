package io.datanapis.xbrl.analysis;

import io.datanapis.xbrl.model.RoleType;

import java.util.Deque;

public interface DefinitionNetworkConsumer {
    default void start(RoleType roleType) {}

    default void end(RoleType roleType) {}

    default void rootStart(DefinitionGraphNode root) {}

    default void rootEnd(DefinitionGraphNode root) {}

    default void nodeStart(DefinitionGraphNode node, Deque<DefinitionGraphNode> path) {}

    default void nodeEnd(DefinitionGraphNode node, Deque<DefinitionGraphNode> path) {}
}
