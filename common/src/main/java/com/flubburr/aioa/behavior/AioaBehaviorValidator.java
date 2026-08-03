package com.flubburr.aioa.behavior;

import com.flubburr.aioa.compat.AioaEntityHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;

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
        long bases = graph.nodes.stream().filter(node -> node.type == AioaBehaviorGraph.NodeType.MOB_BASE).count();
        if (bases != 1) issues.add("Every graph needs exactly one Base Mob node.");
        if (!graph.nodes.isEmpty() && graph.nodes.get(0).type != AioaBehaviorGraph.NodeType.MOB_BASE) issues.add("Base Mob must be the first node.");
        Set<String> ids = new HashSet<>();
        for (AioaBehaviorGraph.Node node : graph.nodes) {
            if (!ids.add(node.id)) issues.add("Duplicate node id: " + node.id);
            if (node.id.length() > 80) issues.add("Node ids are limited to 80 characters.");
            if (node.parameters.size() > 32) issues.add("A node cannot have more than 32 parameters.");
            validateParameters(node, issues);
        }
        for (AioaBehaviorGraph.Edge edge : graph.edges) {
            if (!ids.contains(edge.from) || !ids.contains(edge.to)) issues.add("A link points to a missing node.");
            if (edge.from.equals(edge.to)) issues.add("A node cannot link to itself: " + edge.from);
        }
        validateReachability(graph, issues);
        if (graph.edges.stream().noneMatch(edge -> graph.nodes.stream().anyMatch(node -> node.id.equals(edge.from)
                && node.type == AioaBehaviorGraph.NodeType.MOB_BASE))) issues.add("Connect Base Mob to the graph's first event.");
        if ((graph.scope == AioaBehaviorGraph.Scope.ENTITY_TYPE
                || graph.scope == AioaBehaviorGraph.Scope.ENTITY_TAG
                || graph.scope == AioaBehaviorGraph.Scope.SINGLE_ENTITY) && graph.selector.isBlank()) {
            issues.add("The selected scope needs a selector.");
        }
        return issues;
    }

    private static void validateParameters(AioaBehaviorGraph.Node node, List<String> issues) {
        Set<String> allowed = allowedParameters(node.type);
        node.parameters.keySet().stream().filter(key -> !allowed.contains(key))
                .forEach(key -> issues.add(node.type.name().replace('_', ' ') + " does not use parameter '" + key + "'."));
        switch (node.type) {
            case MOB_BASE -> {
                number(node, "health", 1, 2048, issues);
                number(node, "damage", 0, 2048, issues);
                number(node, "speed", 0.01, 2, issues);
            }
            case SET_MAX_HEALTH -> number(node, "value", 1, 2048, issues);
            case SET_ATTACK_DAMAGE -> number(node, "value", 0, 2048, issues);
            case SET_MOVEMENT_SPEED -> number(node, "value", 0.01, 2, issues);
            case EQUIP_ITEM -> {
                ResourceLocation id = AioaEntityHelper.parseResourceLocation(node.parameters.get("item"));
                if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) issues.add("Equip Item needs a valid registered item id.");
                String slot = node.parameters.getOrDefault("slot", "MAINHAND").toUpperCase(java.util.Locale.ROOT);
                if (!Set.of("MAINHAND", "OFFHAND", "FEET", "LEGS", "CHEST", "HEAD").contains(slot)) issues.add("Equip Item has an invalid slot.");
            }
            case FIND_ENTITY_TYPE, SPAWN_MOB -> {
                if (AioaEntityHelper.resolveEntityType(node.parameters.get("entity")).isEmpty()) issues.add(node.type + " needs a valid mob selection.");
                if (node.type == AioaBehaviorGraph.NodeType.FIND_ENTITY_TYPE) number(node, "range", 1, 64, issues);
                else {
                    number(node, "cooldown", 20, 12000, issues);
                    number(node, "nearbyCap", 1, 64, issues);
                }
            }
            case EVERY_TICKS, DELAY_TICKS -> number(node, "ticks", 1, 12000, issues);
            case EVERY_SECONDS -> number(node, "seconds", 0.05, 600, issues);
            case RANDOM_CHANCE -> number(node, "chance", 0, 1, issues);
            case FIND_NEAREST_PLAYER, FIND_NEAREST_ANIMAL, FIND_NEAREST_MOB -> number(node, "range", 1, 64, issues);
            case TARGET_IN_RANGE, ATTACK_TARGET -> number(node, "range", 1, 64, issues);
            case HEALTH_BELOW -> number(node, "percent", 0, 1, issues);
            case MOVE_TO_TARGET -> number(node, "speed", 0.1, 3, issues);
            case FLEE_TARGET -> { number(node, "distance", 2, 32, issues); number(node, "speed", 0.1, 3, issues); }
            case WALK_BLOCKS -> { number(node, "blocks", 0.25, 32, issues); number(node, "speed", 0.1, 3, issues); }
            case WANDER -> { number(node, "radius", 1, 32, issues); number(node, "speed", 0.1, 3, issues); }
            case ROTATE_DEGREES -> number(node, "degrees", -360, 360, issues);
            case STRAFE -> { number(node, "forward", -1, 1, issues); number(node, "sideways", -1, 1, issues); }
            case JUMP -> number(node, "strength", 0.1, 1.5, issues);
            case KNOCKBACK_TARGET -> number(node, "strength", 0, 4, issues);
            case PLAY_SOUND -> { number(node, "volume", 0, 4, issues); number(node, "pitch", 0.25, 2, issues); }
            case SAY_IN_CHAT -> number(node, "range", 1, 256, issues);
            case PARTICLE_PATTERN -> { number(node, "points", 3, 64, issues); number(node, "radius", 0.1, 8, issues); }
            case HEAL_SELF -> number(node, "amount", 0, 2048, issues);
            case SET_VELOCITY -> { number(node, "x", -8, 8, issues); number(node, "y", -8, 8, issues); number(node, "z", -8, 8, issues); }
            case SET_VARIABLE, MATH_VARIABLE, COMPARE_VARIABLE -> number(node, "value", -1_000_000, 1_000_000, issues);
            case SET_BODY_ROTATION, SET_HEAD_ROTATION -> number(node, "degrees", -360, 360, issues);
            case SCRIPT -> {
                String script = node.parameters.getOrDefault("script", "");
                if (script.length() > 1024) issues.add("Script nodes are limited to 1,024 characters.");
            }
            default -> { }
        }
    }

    private static Set<String> allowedParameters(AioaBehaviorGraph.NodeType type) {
        return switch (type) {
            case MOB_BASE -> Set.of("entity", "health", "damage", "speed");
            case EVERY_TICKS, DELAY_TICKS -> Set.of("ticks");
            case EVERY_SECONDS -> Set.of("seconds");
            case RANDOM_CHANCE -> Set.of("chance");
            case FIND_NEAREST_PLAYER, FIND_NEAREST_ANIMAL, FIND_NEAREST_MOB -> Set.of("range");
            case FIND_ENTITY_TYPE -> Set.of("entity", "range");
            case TARGET_IN_RANGE, ATTACK_TARGET -> Set.of("range");
            case HEALTH_BELOW -> Set.of("percent");
            case MOVE_TO_TARGET -> Set.of("speed");
            case FLEE_TARGET -> Set.of("distance", "speed");
            case WALK_BLOCKS -> Set.of("blocks", "speed");
            case WANDER -> Set.of("radius", "speed");
            case ROTATE_DEGREES -> Set.of("degrees");
            case STRAFE -> Set.of("forward", "sideways");
            case JUMP, KNOCKBACK_TARGET -> Set.of("strength");
            case TELEPORT_RELATIVE -> Set.of("x", "y", "z");
            case SET_AGGRESSIVE, SET_NO_AI, SET_PERSISTENT, SET_GLOWING, SET_SILENT, SET_INVULNERABLE -> Set.of("value");
            case SET_CUSTOM_NAME -> Set.of("name", "visible");
            case SET_MAX_HEALTH, SET_ATTACK_DAMAGE, SET_MOVEMENT_SPEED -> Set.of("value");
            case EQUIP_ITEM -> Set.of("item", "slot", "dropChance");
            case SPAWN_MOB -> Set.of("entity", "cooldown", "nearbyCap", "capRadius", "offsetX", "offsetY", "offsetZ");
            case PLAY_SOUND -> Set.of("sound", "volume", "pitch");
            case SAY_IN_CHAT -> Set.of("message", "range");
            case PARTICLE_PATTERN -> Set.of("pattern", "points", "radius");
            case HEAL_SELF -> Set.of("amount");
            case SET_VELOCITY -> Set.of("x", "y", "z");
            case ADD_TAG, REMOVE_TAG, HAS_TAG -> Set.of("tag");
            case SET_VARIABLE -> Set.of("name", "value");
            case MATH_VARIABLE -> Set.of("name", "operation", "value");
            case COMPARE_VARIABLE -> Set.of("name", "comparison", "value");
            case SET_BODY_ROTATION, SET_HEAD_ROTATION -> Set.of("degrees");
            case SCRIPT -> Set.of("script");
            case COMMENT -> Set.of("text");
            default -> Set.of();
        };
    }

    private static void validateReachability(AioaBehaviorGraph graph, List<String> issues) {
        Map<String, List<String>> outgoing = new HashMap<>();
        graph.edges.forEach(edge -> outgoing.computeIfAbsent(edge.from, key -> new ArrayList<>()).add(edge.to));
        ArrayDeque<String> queue = new ArrayDeque<>();
        graph.nodes.stream().filter(node -> node.type == AioaBehaviorGraph.NodeType.MOB_BASE).map(node -> node.id).forEach(queue::add);
        Set<String> reached = new HashSet<>();
        while (!queue.isEmpty()) {
            String id = queue.removeFirst();
            if (reached.add(id)) queue.addAll(outgoing.getOrDefault(id, List.of()));
        }
        graph.nodes.stream().filter(node -> node.type != AioaBehaviorGraph.NodeType.COMMENT && !reached.contains(node.id))
                .limit(3).forEach(node -> issues.add(node.type.name().replace('_', ' ') + " is not connected to Base Mob."));
    }

    private static void number(AioaBehaviorGraph.Node node, String key, double min, double max, List<String> issues) {
        String raw = node.parameters.get(key);
        try {
            double value = Double.parseDouble(raw == null ? "" : raw);
            if (!Double.isFinite(value) || value < min || value > max) throw new NumberFormatException();
        } catch (NumberFormatException ignored) {
            issues.add(node.type.name().replace('_', ' ') + " needs " + key + " between " + min + " and " + max + ".");
        }
    }
}
