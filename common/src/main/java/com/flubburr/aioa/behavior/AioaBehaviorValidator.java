package com.flubburr.aioa.behavior;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AioaBehaviorValidator {
    private AioaBehaviorValidator() {
    }

    public static List<String> validate(AioaBehaviorGraph graph) {
        List<String> issues = new ArrayList<>();
        if (graph.name.length() > 80) issues.add("Graph names are limited to 80 characters.");
        if (graph.selector.length() > 160) issues.add("Graph selectors are limited to 160 characters.");
        if (graph.nodes.size() > 128) issues.add("Graph exceeds the 128-node safety limit.");
        if (graph.edges.size() > 256) issues.add("Graph exceeds the 256-link safety limit.");
        long entries = graph.nodes.stream().filter(node -> node.type == AioaBehaviorGraph.NodeType.ON_TICK).count();
        if (entries == 0) issues.add("Add an On Tick event node.");
        Set<String> ids = new HashSet<>();
        for (AioaBehaviorGraph.Node node : graph.nodes) {
            if (!ids.add(node.id)) issues.add("Duplicate node id: " + node.id);
            if (node.id.length() > 80) issues.add("Node ids are limited to 80 characters.");
            if (node.parameters.size() > 32) issues.add("A node cannot have more than 32 parameters.");
        }
        for (AioaBehaviorGraph.Edge edge : graph.edges) {
            if (!ids.contains(edge.from) || !ids.contains(edge.to)) issues.add("A link points to a missing node.");
            if (edge.from.equals(edge.to)) issues.add("A node cannot link to itself: " + edge.from);
        }
        if ((graph.scope == AioaBehaviorGraph.Scope.ENTITY_TYPE
                || graph.scope == AioaBehaviorGraph.Scope.ENTITY_TAG
                || graph.scope == AioaBehaviorGraph.Scope.SINGLE_ENTITY) && graph.selector.isBlank()) {
            issues.add("The selected scope needs a selector.");
        }
        return issues;
    }
}
