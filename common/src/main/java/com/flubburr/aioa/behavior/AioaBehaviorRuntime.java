package com.flubburr.aioa.behavior;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.compat.AioaEntityHelper;
import com.flubburr.aioa.config.AioaConfigManager;
import com.flubburr.aioa.spawn.AioaZombieBehaviour;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AioaBehaviorRuntime {
    private AioaBehaviorRuntime() {
    }

    public static void tick(Mob mob) {
        var engine = AioaConfigManager.getConfig().behaviorEngine;
        if (mob.level().isClientSide() || !engine.enabled || mob.tickCount % engine.tickInterval != 0) {
            return;
        }
        int executed = 0;
        for (AioaBehaviorGraph graph : AioaConfigManager.getConfig().behaviorGraphs) {
            if (executed >= engine.maxGraphsPerMob) break;
            if (graph.enabled && matches(graph, mob)) {
                execute(graph, mob, engine.maxStepsPerGraph);
                executed++;
            }
        }
    }

    public static boolean allowsWallClimbing(Mob mob) {
        return AioaConfigManager.getConfig().behaviorGraphs.stream()
                .filter(graph -> graph.enabled && matches(graph, mob))
                .anyMatch(graph -> graph.nodes.stream().anyMatch(node -> node.type == AioaBehaviorGraph.NodeType.WALL_CLIMB));
    }

    private static boolean matches(AioaBehaviorGraph graph, Mob mob) {
        return switch (graph.scope) {
            case ALL_MOBS -> true;
            case MANAGED_MOBS -> mob.getTags().contains(AioaConstants.DAY_SPAWN_TAG)
                    || mob.getTags().contains(AioaConstants.STUDIO_SPAWN_TAG);
            case ENTITY_TAG -> !graph.selector.isBlank() && mob.getTags().contains(graph.selector);
            case SINGLE_ENTITY -> mob.getUUID().toString().equalsIgnoreCase(graph.selector);
            case ENTITY_TYPE -> AioaEntityHelper.resolveEntityId(graph.selector, ignored -> { })
                    .map(id -> id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType())))
                    .orElse(false);
        };
    }

    private static void execute(AioaBehaviorGraph graph, Mob mob, int maxSteps) {
        Map<String, AioaBehaviorGraph.Node> nodes = new HashMap<>();
        graph.nodes.forEach(node -> nodes.put(node.id, node));
        Map<String, List<AioaBehaviorGraph.Edge>> outgoing = graph.edges.stream()
                .collect(java.util.stream.Collectors.groupingBy(edge -> edge.from));
        ArrayDeque<AioaBehaviorGraph.Node> queue = new ArrayDeque<>();
        graph.nodes.stream().filter(node -> node.type == AioaBehaviorGraph.NodeType.MOB_BASE).forEach(queue::add);
        if (queue.isEmpty()) graph.nodes.stream().filter(node -> node.type == AioaBehaviorGraph.NodeType.ON_TICK).forEach(queue::add);
        Set<String> visited = new HashSet<>();
        ExecutionContext context = new ExecutionContext(mob.getTarget());
        int steps = 0;

        while (!queue.isEmpty() && steps++ < maxSteps) {
            AioaBehaviorGraph.Node node = queue.removeFirst();
            if (!visited.add(node.id)) continue;
            String output = runNode(node, mob, context);
            for (AioaBehaviorGraph.Edge edge : outgoing.getOrDefault(node.id, List.of())) {
                if (edge.output == null || edge.output.equals("next") || edge.output.equals(output)) {
                    AioaBehaviorGraph.Node next = nodes.get(edge.to);
                    if (next != null) queue.addLast(next);
                }
            }
        }
    }

    private static String runNode(AioaBehaviorGraph.Node node, Mob mob, ExecutionContext context) {
        double range = number(node, "range", 24.0D, 1.0D, 64.0D);
        switch (node.type) {
            case MOB_BASE -> {
                setAttribute(mob, Attributes.MAX_HEALTH, number(node, "health", 20, 1, 2048));
                setAttribute(mob, Attributes.ATTACK_DAMAGE, number(node, "damage", 3, 0, 2048));
                setAttribute(mob, Attributes.MOVEMENT_SPEED, number(node, "speed", 0.23, 0.01, 2));
                if (mob.getHealth() > mob.getMaxHealth()) mob.setHealth(mob.getMaxHealth());
                return "next";
            }
            case ON_TICK, COMMENT -> { return "next"; }
            case ON_FIRST_TICK -> { return mob.tickCount <= 1 ? "ready" : "waiting"; }
            case EVERY_TICKS -> { return mob.tickCount % (int) number(node, "ticks", 20, 1, 12000) == 0 ? "ready" : "waiting"; }
            case EVERY_SECONDS -> { return mob.tickCount % Math.max(1, (int) Math.round(number(node, "seconds", 1, 0.05, 600) * 20)) == 0 ? "ready" : "waiting"; }
            case DELAY_TICKS -> { return mob.tickCount % (int) number(node, "ticks", 20, 1, 12000) == 0 ? "ready" : "waiting"; }
            case RANDOM_CHANCE -> { return mob.getRandom().nextDouble() <= number(node, "chance", 0.5, 0, 1) ? "success" : "fail"; }
            case HAS_TARGET -> { return mob.getTarget() != null && mob.getTarget().isAlive() ? "true" : "false"; }
            case TARGET_IN_RANGE -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                return target != null && mob.distanceToSqr(target) <= range * range ? "true" : "false";
            }
            case HEALTH_BELOW -> { return mob.getHealth() / mob.getMaxHealth() <= number(node, "percent", 0.5, 0, 1) ? "true" : "false"; }
            case CAN_SEE_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                return target != null && mob.getSensing().hasLineOfSight(target) ? "true" : "false";
            }
            case IS_DAYTIME -> { return mob.level().isDay() ? "true" : "false"; }
            case IS_ON_GROUND -> { return mob.onGround() ? "true" : "false"; }
            case WAS_HURT -> { return mob.hurtTime > 0 ? "true" : "false"; }
            case FIND_NEAREST_PLAYER -> context.target = nearest(mob, Player.class, range, candidate -> !candidate.isSpectator());
            case FIND_NEAREST_ANIMAL -> context.target = nearest(mob, Animal.class, range, LivingEntity::isAlive);
            case FIND_NEAREST_MOB -> context.target = nearest(mob, Mob.class, range, candidate -> candidate != mob && candidate.isAlive());
            case FIND_ENTITY_TYPE -> context.target = findEntityType(node, mob, range);
            case SET_TARGET -> {
                if (context.target != null && AioaZombieBehaviour.canTarget(mob, context.target)) mob.setTarget(context.target);
            }
            case CLEAR_TARGET -> { mob.setTarget(null); context.target = null; }
            case MOVE_TO_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null && mob instanceof PathfinderMob pathfinder) {
                    pathfinder.getNavigation().moveTo(target, number(node, "speed", 1.0D, 0.1D, 3.0D));
                }
            }
            case FLEE_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null && mob instanceof PathfinderMob pathfinder) {
                    Vec3 away = mob.position().subtract(target.position()).normalize().scale(number(node, "distance", 12, 2, 32));
                    pathfinder.getNavigation().moveTo(mob.getX() + away.x, mob.getY(), mob.getZ() + away.z,
                            number(node, "speed", 1.1D, 0.1D, 3.0D));
                }
            }
            case WALL_CLIMB -> AioaZombieBehaviour.tickWallClimbing(mob, true, true);
            case WALK_BLOCKS -> {
                if (mob instanceof PathfinderMob pathfinder) {
                    Vec3 destination = mob.position().add(mob.getLookAngle().multiply(1, 0, 1).normalize()
                            .scale(number(node, "blocks", 4, 0.25, 32)));
                    pathfinder.getNavigation().moveTo(destination.x, destination.y, destination.z, number(node, "speed", 1, 0.1, 3));
                }
            }
            case WANDER -> {
                if (mob instanceof PathfinderMob pathfinder) {
                    double radius = number(node, "radius", 8, 1, 32);
                    pathfinder.getNavigation().moveTo(mob.getX() + (mob.getRandom().nextDouble() - 0.5) * radius * 2,
                            mob.getY(), mob.getZ() + (mob.getRandom().nextDouble() - 0.5) * radius * 2,
                            number(node, "speed", 0.9, 0.1, 3));
                }
            }
            case ROTATE_DEGREES -> {
                float yaw = mob.getYRot() + (float) number(node, "degrees", 90, -360, 360);
                mob.setYRot(yaw);
                mob.setYHeadRot(yaw);
                mob.setYBodyRot(yaw);
            }
            case STRAFE -> mob.getMoveControl().strafe((float) number(node, "forward", 0, -1, 1), (float) number(node, "sideways", 1, -1, 1));
            case JUMP -> {
                if (mob.onGround()) mob.setDeltaMovement(mob.getDeltaMovement().x, number(node, "strength", 0.42, 0.1, 1.5), mob.getDeltaMovement().z);
            }
            case TELEPORT_RELATIVE -> mob.teleportTo(mob.getX() + number(node, "x", 0, -16, 16),
                    mob.getY() + number(node, "y", 0, -16, 16), mob.getZ() + number(node, "z", 0, -16, 16));
            case LOOK_AT_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null) mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            case ATTACK_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null && mob.level() instanceof ServerLevel serverLevel
                        && mob.distanceToSqr(target) <= number(node, "range", 3, 1, 8) * number(node, "range", 3, 1, 8)) {
                    mob.doHurtTarget(serverLevel, target);
                }
            }
            case KNOCKBACK_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null) target.knockback(number(node, "strength", 0.6, 0, 4), mob.getX() - target.getX(), mob.getZ() - target.getZ());
            }
            case SET_AGGRESSIVE -> mob.setAggressive(flag(node, "value", true));
            case SHARE_TARGET -> AioaZombieBehaviour.coordinateNearbyMobs(mob, AioaConfigManager.getConfig().daySurfaceSpawns);
            case STOP_MOVING -> mob.getNavigation().stop();
            case SET_NO_AI -> mob.setNoAi(flag(node, "value", true));
            case SET_PERSISTENT -> { if (flag(node, "value", true)) mob.setPersistenceRequired(); }
            case SET_GLOWING -> mob.setGlowingTag(flag(node, "value", true));
            case SET_SILENT -> mob.setSilent(flag(node, "value", true));
            case SET_INVULNERABLE -> mob.setInvulnerable(flag(node, "value", true));
            case SET_CUSTOM_NAME -> {
                mob.setCustomName(Component.literal(node.parameters.getOrDefault("name", "AIOA Mob")));
                mob.setCustomNameVisible(flag(node, "visible", true));
            }
            case SET_MAX_HEALTH -> {
                setAttribute(mob, Attributes.MAX_HEALTH, number(node, "value", 20, 1, 2048));
                if (mob.getHealth() > mob.getMaxHealth()) mob.setHealth(mob.getMaxHealth());
            }
            case SET_ATTACK_DAMAGE -> setAttribute(mob, Attributes.ATTACK_DAMAGE, number(node, "value", 3, 0, 2048));
            case SET_MOVEMENT_SPEED -> setAttribute(mob, Attributes.MOVEMENT_SPEED, number(node, "value", 0.23, 0.01, 2));
            case EQUIP_ITEM -> equipItem(node, mob);
            case SPAWN_MOB -> {
                if (AioaConfigManager.getConfig().behaviorEngine.allowWorldNodes) spawnMob(node, mob);
            }
            case PLAY_SOUND -> playSound(node, mob);
            case SAY_IN_CHAT -> sayInChat(node, mob);
            case PARTICLE_PATTERN -> particlePattern(node, mob);
            case HEAL_SELF -> mob.heal((float) number(node, "amount", 4, 0, 2048));
            case SET_VELOCITY -> mob.setDeltaMovement(number(node, "x", 0, -8, 8),
                    number(node, "y", 0, -8, 8), number(node, "z", 0, -8, 8));
            case ADD_TAG -> mob.addTag(node.parameters.getOrDefault("tag", "aioa_custom"));
            case REMOVE_TAG -> mob.removeTag(node.parameters.getOrDefault("tag", "aioa_custom"));
            case HAS_TAG -> { return mob.getTags().contains(node.parameters.getOrDefault("tag", "aioa_custom")) ? "true" : "false"; }
            case SET_VARIABLE -> context.variables.put(variableName(node), number(node, "value", 0, -1_000_000, 1_000_000));
            case MATH_VARIABLE -> applyVariableMath(node, context);
            case COMPARE_VARIABLE -> { return compareVariable(node, context) ? "true" : "false"; }
            case SCRIPT -> runCreatorScript(node, mob);
            case SET_BODY_ROTATION -> mob.setYBodyRot((float) number(node, "degrees", mob.yBodyRot, -360, 360));
            case SET_HEAD_ROTATION -> mob.setYHeadRot((float) number(node, "degrees", mob.getYHeadRot(), -360, 360));
        }
        return context.target == null ? "missing" : "found";
    }

    private static void sayInChat(AioaBehaviorGraph.Node node, Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        String message = node.parameters.getOrDefault("message", "AIOA event");
        double range = number(node, "range", 32, 1, 256);
        level.players().stream().filter(player -> player.distanceToSqr(mob) <= range * range)
                .forEach(player -> player.sendSystemMessage(Component.literal(message.replace("{mob}", mob.getName().getString()))));
    }

    private static void particlePattern(AioaBehaviorGraph.Node node, Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        int points = (int) number(node, "points", 16, 3, 64);
        double radius = number(node, "radius", 1.5, 0.1, 8);
        String pattern = node.parameters.getOrDefault("pattern", "circle").toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            double progress = pattern.equals("spiral") ? (i + 1.0D) / points : 1.0D;
            double x = mob.getX() + Math.cos(angle) * radius * progress;
            double z = mob.getZ() + Math.sin(angle) * radius * progress;
            double y = mob.getY() + 0.2D + (pattern.equals("spiral") ? progress * 2.0D : 0.0D);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    private static void runCreatorScript(AioaBehaviorGraph.Node node, Mob mob) {
        String script = node.parameters.getOrDefault("script", "");
        for (String raw : script.split(";")) {
            String command = raw.trim();
            if (command.isEmpty()) continue;
            String[] parts = command.split("=", 2);
            String name = parts[0].trim().toLowerCase(java.util.Locale.ROOT);
            String value = parts.length > 1 ? parts[1].trim() : "true";
            switch (name) {
                case "say" -> sayInChat(new AioaBehaviorGraph.Node("script-say", AioaBehaviorGraph.NodeType.SAY_IN_CHAT, 0, 0).parameter("message", value), mob);
                case "rotate" -> { try { mob.setYRot(mob.getYRot() + Float.parseFloat(value)); } catch (NumberFormatException ignored) { } }
                case "glow" -> mob.setGlowingTag(Boolean.parseBoolean(value));
                case "stop" -> mob.getNavigation().stop();
                case "aggressive" -> mob.setAggressive(Boolean.parseBoolean(value));
                default -> { }
            }
        }
    }

    private static String variableName(AioaBehaviorGraph.Node node) {
        String name = node.parameters.getOrDefault("name", "value").trim();
        return name.isEmpty() ? "value" : name.substring(0, Math.min(48, name.length()));
    }

    private static void applyVariableMath(AioaBehaviorGraph.Node node, ExecutionContext context) {
        String name = variableName(node);
        double current = context.variables.getOrDefault(name, 0.0D);
        double operand = number(node, "value", 1, -1_000_000, 1_000_000);
        double result = switch (node.parameters.getOrDefault("operation", "add").toLowerCase(java.util.Locale.ROOT)) {
            case "subtract" -> current - operand;
            case "multiply" -> current * operand;
            case "divide" -> operand == 0.0D ? current : current / operand;
            case "min" -> Math.min(current, operand);
            case "max" -> Math.max(current, operand);
            default -> current + operand;
        };
        context.variables.put(name, Math.max(-1_000_000, Math.min(1_000_000, result)));
    }

    private static boolean compareVariable(AioaBehaviorGraph.Node node, ExecutionContext context) {
        double current = context.variables.getOrDefault(variableName(node), 0.0D);
        double value = number(node, "value", 0, -1_000_000, 1_000_000);
        return switch (node.parameters.getOrDefault("comparison", ">=").trim()) {
            case ">" -> current > value;
            case "<" -> current < value;
            case "<=" -> current <= value;
            case "==" -> Math.abs(current - value) < 1.0E-9D;
            case "!=" -> Math.abs(current - value) >= 1.0E-9D;
            default -> current >= value;
        };
    }

    private static LivingEntity findEntityType(AioaBehaviorGraph.Node node, Mob mob, double range) {
        Optional<EntityType<?>> type = AioaEntityHelper.resolveEntityType(node.parameters.getOrDefault("entity", "minecraft:zombie"));
        if (type.isEmpty()) return null;
        return mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(range),
                        candidate -> candidate != mob && candidate.getType() == type.get() && candidate.isAlive()).stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
    }

    private static void spawnMob(AioaBehaviorGraph.Node node, Mob source) {
        int cooldown = (int) number(node, "cooldown", 200, 20, 12000);
        if (source.tickCount % cooldown != 0 || !(source.level() instanceof ServerLevel level)) return;
        double radius = number(node, "capRadius", 16, 4, 64);
        int configuredCap = AioaConfigManager.getConfig().behaviorEngine.maxNodeSpawnedMobsNearby;
        int cap = Math.min(configuredCap, (int) number(node, "nearbyCap", 8, 1, 64));
        if (level.getEntitiesOfClass(Mob.class, source.getBoundingBox().inflate(radius)).size() >= cap) return;
        Optional<EntityType<?>> type = AioaEntityHelper.resolveEntityType(node.parameters.getOrDefault("entity", "minecraft:zombie"));
        if (type.isEmpty()) return;
        Entity created = type.get().create(level, EntitySpawnReason.EVENT);
        if (!(created instanceof Mob spawned)) return;
        spawned.moveTo(source.getX() + number(node, "offsetX", 1, -8, 8), source.getY() + number(node, "offsetY", 0, -8, 8),
                source.getZ() + number(node, "offsetZ", 1, -8, 8), source.getYRot(), 0);
        if (!level.noCollision(spawned)) return;
        spawned.finalizeSpawn(level, level.getCurrentDifficultyAt(spawned.blockPosition()), EntitySpawnReason.EVENT, null);
        spawned.addTag(AioaConstants.STUDIO_SPAWN_TAG);
        level.addFreshEntityWithPassengers(spawned);
    }

    private static <T extends LivingEntity> T nearest(Mob mob, Class<T> type, double range, java.util.function.Predicate<T> predicate) {
        AABB box = mob.getBoundingBox().inflate(range);
        return mob.level().getEntitiesOfClass(type, box, predicate).stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
    }

    private static void playSound(AioaBehaviorGraph.Node node, Mob mob) {
        Optional<ResourceLocation> id = Optional.ofNullable(AioaEntityHelper.parseResourceLocation(node.parameters.getOrDefault("sound", "minecraft:entity.zombie.ambient")));
        SoundEvent sound = id.map(BuiltInRegistries.SOUND_EVENT::getValue).orElse(null);
        if (sound != null) {
            mob.level().playSound(null, mob.blockPosition(), sound, SoundSource.HOSTILE,
                    (float) number(node, "volume", 1, 0, 4), (float) number(node, "pitch", 1, 0.25, 2));
        }
    }

    private static void setAttribute(Mob mob, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    private static void equipItem(AioaBehaviorGraph.Node node, Mob mob) {
        ResourceLocation id = AioaEntityHelper.parseResourceLocation(node.parameters.getOrDefault("item", "minecraft:air"));
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return;
        EquipmentSlot slot;
        try {
            slot = EquipmentSlot.valueOf(node.parameters.getOrDefault("slot", "MAINHAND").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return;
        }
        BuiltInRegistries.ITEM.get(id).ifPresent(item -> mob.setItemSlot(slot, new ItemStack(item)));
        mob.setDropChance(slot, (float) number(node, "dropChance", 0, 0, 1));
    }

    private static boolean flag(AioaBehaviorGraph.Node node, String key, boolean fallback) {
        return Boolean.parseBoolean(node.parameters.getOrDefault(key, Boolean.toString(fallback)));
    }

    private static double number(AioaBehaviorGraph.Node node, String key, double fallback, double min, double max) {
        try {
            return Math.max(min, Math.min(max, Double.parseDouble(node.parameters.getOrDefault(key, Double.toString(fallback)))));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static final class ExecutionContext {
        private LivingEntity target;
        private final Map<String, Double> variables = new HashMap<>();

        private ExecutionContext(LivingEntity target) {
            this.target = target;
        }
    }
}
