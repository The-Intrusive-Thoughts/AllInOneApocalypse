package com.flubburr.aioa.behavior;

import java.util.List;
import java.util.Set;

/** Shared editor/runtime port contract. Keeping this in common prevents port drift between the UI and executor. */
public final class AioaNodeSchema {
    private static final List<String> EXEC_INPUT = List.of("exec");

    private AioaNodeSchema() {
    }

    public static List<String> inputs(AioaBehaviorGraph.NodeType type) {
        if (type == AioaBehaviorGraph.NodeType.MOB_BASE) return List.of();
        return EXEC_INPUT;
    }

    public static List<String> outputs(AioaBehaviorGraph.NodeType type) {
        return switch (type) {
            case FIND_NEAREST_PLAYER, FIND_PLAYER_NAME, FIND_NEAREST_ANIMAL, FIND_NEAREST_MOB, FIND_ENTITY_TYPE ->
                    List.of("found", "missing");
            case HAS_TARGET, TARGET_IN_RANGE, HEALTH_BELOW, CAN_SEE_TARGET, IS_DAYTIME, IS_ON_GROUND, WAS_HURT,
                    HAS_TAG, COMPARE_VARIABLE -> List.of("true", "false");
            case EVERY_TICKS, EVERY_SECONDS, DELAY_TICKS, DELAY_SECONDS, ON_FIRST_TICK -> List.of("ready", "waiting");
            case COOLDOWN -> List.of("ready", "waiting");
            case REPEAT_COUNT -> List.of("repeat", "done");
            case RANDOM_CHANCE -> List.of("success", "fail");
            case SEQUENCE -> List.of("then_1", "then_2", "then_3", "then_4");
            case PHASE_BRANCH -> List.of("phase_1", "phase_2", "phase_3", "phase_4");
            case TARGET_IS_PLAYER, TARGET_HEALTH_BELOW, IS_RAINING -> List.of("true", "false");
            case TARGET_IS_SURVIVAL, TARGET_IS_CREATIVE, TARGET_IS_ADVENTURE, TARGET_IS_SPECTATOR ->
                    List.of("true", "false");
            default -> List.of("next");
        };
    }

    public static boolean accepts(AioaBehaviorGraph.NodeType type, String input) {
        return inputs(type).contains(normalizeInput(input));
    }

    public static boolean emits(AioaBehaviorGraph.NodeType type, String output) {
        return outputs(type).contains(normalizeOutput(output));
    }

    public static String normalizeInput(String input) {
        return input == null || input.isBlank() ? "exec" : input;
    }

    public static String normalizeOutput(String output) {
        return output == null || output.isBlank() ? "next" : output;
    }

    public static Set<String> branchOutputs(AioaBehaviorGraph.NodeType type) {
        return Set.copyOf(outputs(type));
    }
}
