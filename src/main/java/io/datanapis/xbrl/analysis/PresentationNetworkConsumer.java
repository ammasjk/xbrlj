package io.datanapis.xbrl.analysis;

import io.datanapis.xbrl.model.RoleType;

import java.util.Deque;

public interface PresentationNetworkConsumer {
    default void start(RoleType roleType) {}

    default void end(RoleType roleType) {}

    default void rootStart(PresentationGraphNode root) {}

    default void rootEnd(PresentationGraphNode root) {}

    default void nodeStart(PresentationGraphNode node, Deque<PresentationGraphNode> path) {}

    default void nodeEnd(PresentationGraphNode node, Deque<PresentationGraphNode> path) {}
}
