package com.flubburr.aioa.client.config;

import com.flubburr.aioa.behavior.AioaBehaviorGraph;
import com.flubburr.aioa.behavior.AioaNodeSchema;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic, side-effect-free graph sandbox used by the editor viewport. */
final class AioaViewportSimulation {
    private String graphId = "";
    private long lastNanos = System.nanoTime();
    private double tickAccumulator;
    private long tick;
    private float mobX = -2.5F;
    private float mobZ;
    private float targetX = 2.5F;
    private float targetZ;
    private float yaw = 90.0F;
    private float headYaw = 90.0F;
    private float pitch;
    private float walk;
    private float health = 20.0F;
    private float maxHealth = 20.0F;
    private float targetHealth = 20.0F;
    private float attackDamage = 3.0F;
    private int phase = 1;
    private int attackTicks;
    private boolean hasTarget;
    private boolean glowing;
    private boolean targetGlowing;
    private boolean stopped = true;
    private String activeNode = "Ready";
    private String emittedPort = "next";
    private final Map<String, Long> deadlines = new HashMap<>();
    private final Map<String, Integer> repeats = new HashMap<>();
    private final Map<String, Double> variables = new HashMap<>();
    private final Set<String> firedOnce = new HashSet<>();

    void reset(AioaBehaviorGraph graph) {
        this.graphId = graph == null ? "" : graph.id;
        this.lastNanos = System.nanoTime();
        this.tickAccumulator = 0.0D;
        this.tick = 0L;
        this.mobX = -2.5F;
        this.mobZ = 0.0F;
        this.targetX = 2.5F;
        this.targetZ = 0.0F;
        this.yaw = 90.0F;
        this.headYaw = 90.0F;
        this.pitch = 0.0F;
        this.walk = 0.0F;
        this.health = 20.0F;
        this.maxHealth = 20.0F;
        this.targetHealth = 20.0F;
        this.attackDamage = 3.0F;
        this.phase = 1;
        this.attackTicks = 0;
        this.hasTarget = false;
        this.glowing = false;
        this.targetGlowing = false;
        this.stopped = true;
        this.activeNode = "Ready";
        this.emittedPort = "next";
        this.deadlines.clear();
        this.repeats.clear();
        this.variables.clear();
        this.firedOnce.clear();
    }

    void resume() {
        this.stopped = false;
        this.lastNanos = System.nanoTime();
    }

    void pause() {
        this.lastNanos = System.nanoTime();
    }

    boolean stopped() {
        return this.stopped;
    }

    void advance(AioaBehaviorGraph graph, boolean playing, int speed) {
        if (graph == null) return;
        if (!graph.id.equals(this.graphId)) reset(graph);
        long now = System.nanoTime();
        double elapsed = Math.min(0.25D, Math.max(0.0D, (now - this.lastNanos) / 1_000_000_000.0D));
        this.lastNanos = now;
        if (!playing) return;
        this.stopped = false;
        this.tickAccumulator += elapsed * 20.0D * Math.max(1, speed);
        int budget = 12;
        while (this.tickAccumulator >= 1.0D && budget-- > 0) {
            simulateTick(graph);
            this.tickAccumulator -= 1.0D;
        }
    }

    AioaScreenUtil.ViewportFrame frame(boolean playing) {
        String state = this.stopped ? "STOPPED" : playing ? "PLAYING" : "PAUSED";
        String label = state + "  t=" + this.tick + "  " + this.activeNode + " -> " + this.emittedPort;
        return new AioaScreenUtil.ViewportFrame(label, this.yaw, this.headYaw, this.pitch, this.walk,
                this.attackTicks > 0, this.glowing, this.hasTarget, this.targetGlowing,
                this.mobX, this.mobZ, this.targetX, this.targetZ, this.health, this.maxHealth,
                this.targetHealth, this.phase);
    }

    private void simulateTick(AioaBehaviorGraph graph) {
        this.tick++;
        if (this.attackTicks > 0) this.attackTicks--;
        this.walk *= 0.72F;
        Map<String, AioaBehaviorGraph.Node> nodes = new HashMap<>();
        graph.nodes.forEach(node -> nodes.put(node.id, node));
        Map<String, List<AioaBehaviorGraph.Edge>> outgoing = graph.edges.stream()
                .collect(java.util.stream.Collectors.groupingBy(edge -> edge.from));
        ArrayDeque<AioaBehaviorGraph.Node> queue = new ArrayDeque<>();
        graph.nodes.stream().filter(node -> node.type == AioaBehaviorGraph.NodeType.MOB_BASE).forEach(queue::add);
        if (queue.isEmpty()) graph.nodes.stream().filter(node -> node.type == AioaBehaviorGraph.NodeType.ON_TICK).forEach(queue::add);
        Set<String> visited = new HashSet<>();
        int budget = 128;
        while (!queue.isEmpty() && budget-- > 0) {
            AioaBehaviorGraph.Node node = queue.removeFirst();
            if (!visited.add(node.id)) continue;
            String output = run(node);
            this.activeNode = friendly(node.type);
            this.emittedPort = output;
            Set<String> emitted = node.type == AioaBehaviorGraph.NodeType.SEQUENCE
                    ? AioaNodeSchema.branchOutputs(node.type) : Set.of(AioaNodeSchema.normalizeOutput(output));
            for (AioaBehaviorGraph.Edge edge : outgoing.getOrDefault(node.id, List.of())) {
                if (emitted.contains(AioaNodeSchema.normalizeOutput(edge.output))) {
                    AioaBehaviorGraph.Node next = nodes.get(edge.to);
                    if (next != null) queue.addLast(next);
                }
            }
        }
    }

    private String run(AioaBehaviorGraph.Node node) {
        return switch (node.type) {
            case MOB_BASE -> {
                this.maxHealth = (float) number(node, "health", 20.0D);
                this.health = Math.min(this.health, this.maxHealth);
                this.attackDamage = (float) number(node, "damage", 3.0D);
                yield "next";
            }
            case ON_FIRST_TICK -> this.firedOnce.add(node.id) ? "ready" : "waiting";
            case EVERY_TICKS -> timer(node, Math.round(number(node, "ticks", 20.0D)), false);
            case EVERY_SECONDS -> timer(node, Math.round(number(node, "seconds", 1.0D) * 20.0D), false);
            case DELAY_TICKS, COOLDOWN -> timer(node, Math.round(number(node, "ticks", 20.0D)), true);
            case DELAY_SECONDS -> timer(node, Math.round(number(node, "seconds", 1.0D) * 20.0D), true);
            case REPEAT_COUNT -> repeat(node);
            case RANDOM_CHANCE -> ((this.tick * 1103515245L + node.id.hashCode()) & 0x7fffffffL) / (double) Integer.MAX_VALUE
                    <= number(node, "chance", 0.5D) ? "success" : "fail";
            case FIND_NEAREST_PLAYER, FIND_PLAYER_NAME, FIND_NEAREST_ANIMAL, FIND_NEAREST_MOB, FIND_ENTITY_TYPE -> {
                this.hasTarget = true;
                yield "found";
            }
            case HAS_TARGET, CAN_SEE_TARGET, TARGET_IS_PLAYER -> this.hasTarget ? "true" : "false";
            case TARGET_IN_RANGE -> this.hasTarget && distance() <= number(node, "range", 3.0D) ? "true" : "false";
            case HEALTH_BELOW -> this.health / Math.max(1.0F, this.maxHealth) <= number(node, "percent", 0.5D) ? "true" : "false";
            case TARGET_HEALTH_BELOW -> this.targetHealth / 20.0F <= number(node, "percent", 0.5D) ? "true" : "false";
            case IS_DAYTIME, IS_ON_GROUND -> "true";
            case WAS_HURT, IS_RAINING, HAS_TAG -> "false";
            case COMPARE_VARIABLE -> compareVariable(node) ? "true" : "false";
            case PHASE_BRANCH -> "phase_" + this.phase;
            case SET_TARGET, TARGET_ATTACKER -> { this.hasTarget = true; yield "next"; }
            case CLEAR_TARGET -> { this.hasTarget = false; yield "next"; }
            case MOVE_TO_TARGET -> { moveToward((float) number(node, "speed", 1.0D) * 0.055F); yield "next"; }
            case FLEE_TARGET -> { moveAway((float) number(node, "speed", 1.0D) * 0.055F); yield "next"; }
            case WALK_BLOCKS, STRAFE, WANDER, SET_VELOCITY, DASH_TO_TARGET, ORBIT_TARGET -> { movePreview(node); yield "next"; }
            case TELEPORT_RELATIVE -> { this.mobX += number(node, "x", 0.0D); this.mobZ += number(node, "z", 0.0D); yield "next"; }
            case TELEPORT_TO_TARGET -> { this.mobX = this.targetX + (float) number(node, "offsetX", 0.0D); this.mobZ = this.targetZ + (float) number(node, "offsetZ", 0.0D); yield "next"; }
            case ROTATE_DEGREES -> { this.yaw += number(node, "degrees", 90.0D); this.headYaw = this.yaw; yield "next"; }
            case LOOK_AT_TARGET -> { faceTarget(); yield "next"; }
            case STOP_MOVING -> { this.walk = 0.0F; yield "next"; }
            case ATTACK_TARGET, DAMAGE_TARGET, AREA_DAMAGE -> { attack(node); yield "next"; }
            case HEAL_SELF -> { this.health = Math.min(this.maxHealth, this.health + (float) number(node, "amount", 4.0D)); yield "next"; }
            case HEAL_TARGET -> { this.targetHealth = Math.min(20.0F, this.targetHealth + (float) number(node, "amount", 4.0D)); yield "next"; }
            case SET_GLOWING -> { this.glowing = flag(node, "value", true); yield "next"; }
            case SET_TARGET_GLOWING -> { this.targetGlowing = flag(node, "value", true); yield "next"; }
            case SET_MAX_HEALTH -> { this.maxHealth = (float) number(node, "value", 20.0D); this.health = Math.min(this.health, this.maxHealth); yield "next"; }
            case SET_ATTACK_DAMAGE -> { this.attackDamage = (float) number(node, "value", 3.0D); yield "next"; }
            case SET_BODY_ROTATION -> { this.yaw = (float) number(node, "degrees", 0.0D); yield "next"; }
            case SET_HEAD_ROTATION -> { this.headYaw = (float) number(node, "degrees", 0.0D); yield "next"; }
            case SET_PHASE -> { this.phase = Math.max(1, Math.min(4, (int) number(node, "phase", 1.0D))); yield "next"; }
            case SET_VARIABLE -> { this.variables.put(node.parameters.getOrDefault("name", "value"), number(node, "value", 0.0D)); yield "next"; }
            case MATH_VARIABLE -> { mutateVariable(node); yield "next"; }
            case DESPAWN_SELF -> { this.health = 0.0F; yield "next"; }
            default -> "next";
        };
    }

    private String timer(AioaBehaviorGraph.Node node, long duration, boolean waitBeforeFirst) {
        Long deadline = this.deadlines.get(node.id);
        if (deadline == null) {
            this.deadlines.put(node.id, this.tick + Math.max(1L, duration));
            return waitBeforeFirst ? "waiting" : "ready";
        }
        if (this.tick < deadline) return "waiting";
        this.deadlines.put(node.id, this.tick + Math.max(1L, duration));
        return "ready";
    }

    private String repeat(AioaBehaviorGraph.Node node) {
        int value = this.repeats.getOrDefault(node.id, 0) + 1;
        int limit = Math.max(1, Math.min(1024, (int) number(node, "count", 3.0D)));
        if (value < limit) { this.repeats.put(node.id, value); return "repeat"; }
        this.repeats.remove(node.id);
        return "done";
    }

    private void moveToward(float amount) {
        float dx = this.targetX - this.mobX, dz = this.targetZ - this.mobZ;
        float length = Math.max(0.001F, (float) Math.sqrt(dx * dx + dz * dz));
        this.mobX += dx / length * Math.min(amount, length);
        this.mobZ += dz / length * Math.min(amount, length);
        this.walk = 1.0F;
        faceTarget();
    }

    private void moveAway(float amount) {
        float dx = this.mobX - this.targetX, dz = this.mobZ - this.targetZ;
        float length = Math.max(0.001F, (float) Math.sqrt(dx * dx + dz * dz));
        this.mobX += dx / length * amount;
        this.mobZ += dz / length * amount;
        this.walk = 1.0F;
    }

    private void movePreview(AioaBehaviorGraph.Node node) {
        if (node.type == AioaBehaviorGraph.NodeType.DASH_TO_TARGET) { moveToward((float) number(node, "strength", 1.25D) * 0.16F); return; }
        if (node.type == AioaBehaviorGraph.NodeType.ORBIT_TARGET) {
            double angle = Math.atan2(this.mobZ - this.targetZ, this.mobX - this.targetX) + Math.toRadians(number(node, "degrees", 35.0D)) * 0.05D;
            float radius = (float) number(node, "radius", 5.0D);
            this.mobX = this.targetX + (float) Math.cos(angle) * radius;
            this.mobZ = this.targetZ + (float) Math.sin(angle) * radius;
        } else {
            double radians = Math.toRadians(this.yaw);
            this.mobX += (float) Math.sin(radians) * 0.08F;
            this.mobZ += (float) Math.cos(radians) * 0.08F;
        }
        this.walk = 1.0F;
    }

    private void faceTarget() {
        this.yaw = (float) Math.toDegrees(Math.atan2(this.targetX - this.mobX, this.targetZ - this.mobZ));
        this.headYaw = this.yaw;
    }

    private void attack(AioaBehaviorGraph.Node node) {
        this.attackTicks = 6;
        float amount = node.type == AioaBehaviorGraph.NodeType.ATTACK_TARGET ? this.attackDamage : (float) number(node, "amount", 4.0D);
        if (this.hasTarget) this.targetHealth = Math.max(0.0F, this.targetHealth - amount);
    }

    private void mutateVariable(AioaBehaviorGraph.Node node) {
        String name = node.parameters.getOrDefault("name", "value");
        double current = this.variables.getOrDefault(name, 0.0D);
        double value = number(node, "value", 1.0D);
        double result = switch (node.parameters.getOrDefault("operation", "add")) {
            case "subtract" -> current - value;
            case "multiply" -> current * value;
            case "divide" -> value == 0.0D ? current : current / value;
            case "min" -> Math.min(current, value);
            case "max" -> Math.max(current, value);
            default -> current + value;
        };
        this.variables.put(name, result);
    }

    private boolean compareVariable(AioaBehaviorGraph.Node node) {
        double current = this.variables.getOrDefault(node.parameters.getOrDefault("name", "value"), 0.0D);
        double value = number(node, "value", 0.0D);
        return switch (node.parameters.getOrDefault("comparison", ">=")) {
            case ">" -> current > value;
            case "<" -> current < value;
            case "<=" -> current <= value;
            case "==" -> current == value;
            case "!=" -> current != value;
            default -> current >= value;
        };
    }

    private double distance() { return Math.hypot(this.targetX - this.mobX, this.targetZ - this.mobZ); }
    private static boolean flag(AioaBehaviorGraph.Node node, String key, boolean fallback) { return Boolean.parseBoolean(node.parameters.getOrDefault(key, Boolean.toString(fallback))); }
    private static double number(AioaBehaviorGraph.Node node, String key, double fallback) {
        try { return Double.parseDouble(node.parameters.getOrDefault(key, Double.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }
    private static String friendly(AioaBehaviorGraph.NodeType type) {
        String text = type.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
