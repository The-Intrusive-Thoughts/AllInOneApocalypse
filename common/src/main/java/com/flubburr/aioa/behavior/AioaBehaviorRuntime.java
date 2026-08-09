package com.flubburr.aioa.behavior;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.api.AioaBehaviorApi;
import com.flubburr.aioa.compat.AioaEntityHelper;
import com.flubburr.aioa.compat.AioaRegistryCompat;
import com.flubburr.aioa.config.AioaConfigManager;
import com.flubburr.aioa.mixin.accessor.MobAccessor;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AioaBehaviorRuntime {
    private static final Map<RuntimeKey, RuntimeState> STATES = new ConcurrentHashMap<>();
    private static long lastCleanupTick;

    private AioaBehaviorRuntime() {
    }

    public static void tick(Mob mob) {
        var engine = AioaConfigManager.getConfig().behaviorEngine;
        if (mob.level().isClientSide() || !engine.enabled || mob.tickCount % engine.tickInterval != 0) {
            return;
        }
        int executed = 0;
        cleanupStates(mob.level().getGameTime());
        List<AioaBehaviorGraph> runtimeGraphs = new java.util.ArrayList<>(AioaConfigManager.getConfig().behaviorGraphs);
        runtimeGraphs.addAll(AioaBehaviorApi.registeredGraphs());
        for (AioaBehaviorGraph graph : runtimeGraphs) {
            if (executed >= engine.maxGraphsPerMob) break;
            if (graph.enabled && matches(graph, mob)) {
                stripVanillaAi(mob);
                RuntimeKey key = new RuntimeKey(graph.id, mob.getUUID());
                RuntimeState state = STATES.computeIfAbsent(key, ignored -> new RuntimeState());
                state.lastTouchedTick = mob.level().getGameTime();
                execute(graph, mob, engine.maxStepsPerGraph, state);
                executed++;
            }
        }
    }

    /** Graph-controlled mobs keep navigation/controllers but lose every vanilla behavior and target goal. */
    private static void stripVanillaAi(Mob mob) {
        MobAccessor accessor = (MobAccessor) mob;
        accessor.aioa$getGoalSelector().removeAllGoals(goal -> true);
        accessor.aioa$getTargetSelector().removeAllGoals(goal -> true);
        mob.setNoAi(false);
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

    private static void execute(AioaBehaviorGraph graph, Mob mob, int maxSteps, RuntimeState state) {
        Map<String, AioaBehaviorGraph.Node> nodes = new HashMap<>();
        graph.nodes.forEach(node -> nodes.put(node.id, node));
        Map<String, List<AioaBehaviorGraph.Edge>> outgoing = graph.edges.stream()
                .collect(java.util.stream.Collectors.groupingBy(edge -> edge.from));
        ArrayDeque<AioaBehaviorGraph.Node> queue = new ArrayDeque<>();
        graph.nodes.stream().filter(node -> node.type == AioaBehaviorGraph.NodeType.MOB_BASE).forEach(queue::add);
        if (queue.isEmpty()) graph.nodes.stream().filter(node -> node.type == AioaBehaviorGraph.NodeType.ON_TICK).forEach(queue::add);
        Set<String> visited = new HashSet<>();
        ExecutionContext context = new ExecutionContext(graph, mob.getTarget(), state);
        int steps = 0;

        while (!queue.isEmpty() && steps++ < maxSteps) {
            AioaBehaviorGraph.Node node = queue.removeFirst();
            if (!visited.add(node.id)) continue;
            String output = runNode(node, mob, context);
            Set<String> emitted = node.type == AioaBehaviorGraph.NodeType.SEQUENCE
                    ? AioaNodeSchema.branchOutputs(node.type)
                    : Set.of(AioaNodeSchema.normalizeOutput(output));
            for (AioaBehaviorGraph.Edge edge : outgoing.getOrDefault(node.id, List.of())) {
                if (emitted.contains(AioaNodeSchema.normalizeOutput(edge.output))) {
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
            case ON_FIRST_TICK -> {
                if (context.state.firedOnce.add(node.id)) return "ready";
                return "waiting";
            }
            case EVERY_TICKS -> { return timerReady(node, mob, context.state, (long) number(node, "ticks", 20, 1, 12000), false) ? "ready" : "waiting"; }
            case EVERY_SECONDS -> { return timerReady(node, mob, context.state,
                    Math.max(1L, Math.round(number(node, "seconds", 1, 0.05, 600) * 20.0D)), false) ? "ready" : "waiting"; }
            case DELAY_TICKS -> { return timerReady(node, mob, context.state, (long) number(node, "ticks", 20, 1, 12000), true) ? "ready" : "waiting"; }
            case DELAY_SECONDS -> { return timerReady(node, mob, context.state,
                    Math.max(1L, Math.round(number(node, "seconds", 1, 0.05, 600) * 20.0D)), true) ? "ready" : "waiting"; }
            case RANDOM_CHANCE -> { return mob.getRandom().nextDouble() <= number(node, "chance", 0.5, 0, 1) ? "success" : "fail"; }
            case COOLDOWN -> { return timerReady(node, mob, context.state,
                    (long) number(node, "ticks", 100, 1, 12000), true) ? "ready" : "waiting"; }
            case SEQUENCE -> { return "then_1"; }
            case REPEAT_COUNT -> {
                String key = "repeat:" + node.id;
                double count = context.state.variables.getOrDefault(key, 0.0D) + 1.0D;
                double limit = number(node, "count", 3, 1, 1024);
                if (count < limit) {
                    context.state.variables.put(key, count);
                    return "repeat";
                }
                context.state.variables.remove(key);
                return "done";
            }
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
            case TARGET_IS_PLAYER -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                return target instanceof Player ? "true" : "false";
            }
            case TARGET_HEALTH_BELOW -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                return target != null && target.getHealth() / Math.max(1.0F, target.getMaxHealth())
                        <= number(node, "percent", 0.5, 0, 1) ? "true" : "false";
            }
            case IS_RAINING -> { return mob.level().isRainingAt(mob.blockPosition()) ? "true" : "false"; }
            case TARGET_IS_SURVIVAL -> { return targetGameMode(mob, context, GameType.SURVIVAL); }
            case TARGET_IS_CREATIVE -> { return targetGameMode(mob, context, GameType.CREATIVE); }
            case TARGET_IS_ADVENTURE -> { return targetGameMode(mob, context, GameType.ADVENTURE); }
            case TARGET_IS_SPECTATOR -> { return targetGameMode(mob, context, GameType.SPECTATOR); }
            case FIND_NEAREST_PLAYER -> {
                context.target = nearest(mob, Player.class, range, candidate -> !candidate.isSpectator());
                return context.target == null ? "missing" : "found";
            }
            case FIND_PLAYER_NAME -> {
                String playerName = node.parameters.getOrDefault("name", "");
                context.target = nearest(mob, Player.class, range, candidate -> !candidate.isSpectator()
                        && candidate.getGameProfile().getName().equalsIgnoreCase(playerName));
                return context.target == null ? "missing" : "found";
            }
            case FIND_NEAREST_ANIMAL -> {
                context.target = nearest(mob, Animal.class, range, LivingEntity::isAlive);
                return context.target == null ? "missing" : "found";
            }
            case FIND_NEAREST_MOB -> {
                context.target = nearest(mob, Mob.class, range, candidate -> candidate != mob && candidate.isAlive());
                return context.target == null ? "missing" : "found";
            }
            case FIND_ENTITY_TYPE -> {
                context.target = findEntityType(node, mob, range);
                return context.target == null ? "missing" : "found";
            }
            case SET_TARGET -> {
                if (context.target != null && AioaZombieBehaviour.canTarget(mob, context.target)) mob.setTarget(context.target);
            }
            case CLEAR_TARGET -> { mob.setTarget(null); context.target = null; }
            case TARGET_ATTACKER -> {
                LivingEntity attacker = mob.getLastHurtByMob();
                if (attacker != null && attacker.isAlive() && AioaZombieBehaviour.canTarget(mob, attacker)) {
                    context.target = attacker;
                    mob.setTarget(attacker);
                }
            }
            case MOVE_TO_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null && mob instanceof PathfinderMob pathfinder) {
                    double stopDistance = number(node, "stopDistance", 1.5D, 0.1D, 16.0D);
                    pathfinder.getNavigation().stop();
                    if (mob.distanceToSqr(target) > stopDistance * stopDistance) {
                        pathfinder.getNavigation().moveTo(target, number(node, "speed", 1.0D, 0.1D, 3.0D));
                    }
                }
            }
            case FLEE_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null && mob instanceof PathfinderMob pathfinder) {
                    double distance = number(node, "distance", 12, 2, 32);
                    pathfinder.getNavigation().stop();
                    if (mob.distanceToSqr(target) < distance * distance) {
                        Vec3 away = mob.position().subtract(target.position()).normalize().scale(distance);
                        pathfinder.getNavigation().moveTo(mob.getX() + away.x, mob.getY(), mob.getZ() + away.z,
                                number(node, "speed", 1.1D, 0.1D, 3.0D));
                    }
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
            case TELEPORT_TO_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null) mob.teleportTo(target.getX() + number(node, "offsetX", 0, -16, 16),
                        target.getY() + number(node, "offsetY", 0, -16, 16),
                        target.getZ() + number(node, "offsetZ", 0, -16, 16));
            }
            case ORBIT_TARGET -> orbitTarget(node, mob, context);
            case DASH_TO_TARGET -> dashToTarget(node, mob, context);
            case ATTACK_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null && mob.distanceToSqr(target) <= number(node, "range", 3, 1, 8) * number(node, "range", 3, 1, 8)) {
                    mob.getNavigation().stop();
                    mob.getLookControl().setLookAt(target, 60.0F, 60.0F);
                    mob.doHurtTarget(target);
                }
            }
            case KNOCKBACK_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null) target.knockback(number(node, "strength", 0.6, 0, 4), mob.getX() - target.getX(), mob.getZ() - target.getZ());
            }
            case DAMAGE_TARGET -> damageTarget(node, mob, context);
            case HEAL_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null) target.heal((float) number(node, "amount", 4, 0, 2048));
            }
            case AREA_DAMAGE -> areaDamage(node, mob);
            case SET_FIRE_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null) target.igniteForSeconds((int) number(node, "seconds", 4, 0, 60));
            }
            case LAUNCH_TARGET -> launchTarget(node, mob, context);
            case SET_AGGRESSIVE -> mob.setAggressive(flag(node, "value", true));
            case SHARE_TARGET -> AioaZombieBehaviour.coordinateNearbyMobs(mob, AioaConfigManager.getConfig().daySurfaceSpawns);
            case STOP_MOVING -> {
                mob.getNavigation().stop();
                mob.getMoveControl().strafe(0.0F, 0.0F);
                mob.setDeltaMovement(0.0D, mob.getDeltaMovement().y, 0.0D);
            }
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
            case SET_ARMOR -> setAttribute(mob, Attributes.ARMOR, number(node, "value", 0, 0, 2048));
            case SET_FOLLOW_RANGE -> setAttribute(mob, Attributes.FOLLOW_RANGE, number(node, "value", 32, 1, 2048));
            case SET_KNOCKBACK_RESISTANCE -> setAttribute(mob, Attributes.KNOCKBACK_RESISTANCE, number(node, "value", 0, 0, 1));
            case EQUIP_ITEM, EQUIP_ARMOR -> equipItem(node, mob);
            case SPAWN_MOB -> {
                if (AioaConfigManager.getConfig().behaviorEngine.allowWorldNodes) spawnMob(node, mob, context.state);
            }
            case PLAY_SOUND -> playSound(node, mob);
            case APPLY_EFFECT_SELF -> applyEffect(node, mob, mob);
            case APPLY_EFFECT_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null) applyEffect(node, mob, target);
            }
            case CLEAR_EFFECTS_SELF -> mob.removeAllEffects();
            case CLEAR_EFFECTS_TARGET -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null) target.removeAllEffects();
            }
            case SET_TARGET_GLOWING -> {
                LivingEntity target = context.target != null ? context.target : mob.getTarget();
                if (target != null) target.setGlowingTag(flag(node, "value", true));
            }
            case SUMMON_LIGHTNING -> summonLightning(node, mob, context);
            case EXPLOSION -> explosion(node, mob, context);
            case SAY_IN_CHAT -> sayInChat(node, mob);
            case ACTION_BAR -> actionBar(node, mob);
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
            case SET_PHASE -> context.variables.put("__phase", number(node, "phase", 1, 1, 4));
            case PHASE_BRANCH -> { return "phase_" + Math.max(1, Math.min(4,
                    (int) Math.round(context.variables.getOrDefault("__phase", 1.0D)))); }
            case SCRIPT -> runCreatorScript(node, mob, context);
            case SET_BODY_ROTATION -> mob.setYBodyRot((float) number(node, "degrees", mob.yBodyRot, -360, 360));
            case SET_HEAD_ROTATION -> mob.setYHeadRot((float) number(node, "degrees", mob.getYHeadRot(), -360, 360));
            case DESPAWN_SELF -> mob.discard();
            case FUNCTION_GROUP -> executeFunctionGroup(node, mob, context);
        }
        return "next";
    }

    private static String targetGameMode(Mob mob, ExecutionContext context, GameType expected) {
        LivingEntity target = context.target != null ? context.target : mob.getTarget();
        if (!(target instanceof ServerPlayer player)) return "false";
        return player.gameMode.getGameModeForPlayer() == expected ? "true" : "false";
    }

    private static void executeFunctionGroup(AioaBehaviorGraph.Node groupNode, Mob mob, ExecutionContext context) {
        if (context.groupDepth >= 4) return;
        String groupId = groupNode.parameters.getOrDefault("_groupId", "");
        if (groupId.isBlank()) return;
        Map<String, AioaBehaviorGraph.Node> members = context.graph.nodes.stream()
                .filter(node -> groupId.equals(node.parameters.get("_group")))
                .collect(java.util.stream.Collectors.toMap(node -> node.id, node -> node, (first, ignored) -> first));
        if (members.isEmpty()) return;
        Map<String, List<AioaBehaviorGraph.Edge>> outgoing = context.graph.edges.stream()
                .filter(edge -> members.containsKey(edge.from) && members.containsKey(edge.to))
                .collect(java.util.stream.Collectors.groupingBy(edge -> edge.from));
        Set<String> hasIncoming = context.graph.edges.stream()
                .filter(edge -> members.containsKey(edge.from) && members.containsKey(edge.to))
                .map(edge -> edge.to).collect(java.util.stream.Collectors.toSet());
        ArrayDeque<AioaBehaviorGraph.Node> queue = new ArrayDeque<>();
        members.values().stream().filter(member -> !hasIncoming.contains(member.id)).forEach(queue::addLast);
        Set<String> visited = new HashSet<>();
        context.groupDepth++;
        int budget = 48;
        while (!queue.isEmpty() && budget-- > 0) {
            AioaBehaviorGraph.Node member = queue.removeFirst();
            if (!visited.add(member.id) || member.type == AioaBehaviorGraph.NodeType.FUNCTION_GROUP) continue;
            String output = runNode(member, mob, context);
            Set<String> emitted = member.type == AioaBehaviorGraph.NodeType.SEQUENCE
                    ? AioaNodeSchema.branchOutputs(member.type) : Set.of(AioaNodeSchema.normalizeOutput(output));
            for (AioaBehaviorGraph.Edge edge : outgoing.getOrDefault(member.id, List.of())) {
                if (emitted.contains(AioaNodeSchema.normalizeOutput(edge.output))) queue.addLast(members.get(edge.to));
            }
        }
        context.groupDepth--;
    }

    private static void orbitTarget(AioaBehaviorGraph.Node node, Mob mob, ExecutionContext context) {
        LivingEntity target = context.target != null ? context.target : mob.getTarget();
        if (target == null || !(mob instanceof PathfinderMob pathfinder)) return;
        double radius = number(node, "radius", 5, 1, 24);
        double angle = Math.atan2(mob.getZ() - target.getZ(), mob.getX() - target.getX())
                + Math.toRadians(number(node, "degrees", 35, -180, 180)) * 0.05D;
        pathfinder.getNavigation().stop();
        pathfinder.getNavigation().moveTo(target.getX() + Math.cos(angle) * radius, target.getY(),
                target.getZ() + Math.sin(angle) * radius, number(node, "speed", 1.1, 0.1, 3));
    }

    private static void dashToTarget(AioaBehaviorGraph.Node node, Mob mob, ExecutionContext context) {
        LivingEntity target = context.target != null ? context.target : mob.getTarget();
        if (target == null) return;
        mob.getNavigation().stop();
        Vec3 direction = target.position().add(0, target.getBbHeight() * 0.35, 0)
                .subtract(mob.position()).normalize();
        double strength = number(node, "strength", 1.25, 0.1, 4);
        mob.setDeltaMovement(direction.x * strength, direction.y * strength + number(node, "lift", 0.15, -1, 2),
                direction.z * strength);
        mob.hasImpulse = true;
    }

    private static void damageTarget(AioaBehaviorGraph.Node node, Mob mob, ExecutionContext context) {
        LivingEntity target = context.target != null ? context.target : mob.getTarget();
        if (target != null && target.isAlive()) {
            target.hurt(mob.damageSources().mobAttack(mob), (float) number(node, "amount", 6, 0, 2048));
        }
    }

    private static void areaDamage(AioaBehaviorGraph.Node node, Mob mob) {
        double radius = number(node, "radius", 4, 0.5, 32);
        float amount = (float) number(node, "amount", 4, 0, 2048);
        boolean includeAllies = flag(node, "includeAllies", false);
        mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(radius), target ->
                        target != mob && target.isAlive() && (includeAllies || AioaZombieBehaviour.canTarget(mob, target)))
                .forEach(target -> target.hurt(mob.damageSources().mobAttack(mob), amount));
    }

    private static void launchTarget(AioaBehaviorGraph.Node node, Mob mob, ExecutionContext context) {
        LivingEntity target = context.target != null ? context.target : mob.getTarget();
        if (target == null) return;
        Vec3 outward = target.position().subtract(mob.position()).multiply(1, 0, 1).normalize();
        double horizontal = number(node, "horizontal", 0.7, 0, 4);
        target.setDeltaMovement(outward.x * horizontal, number(node, "vertical", 0.65, -1, 4), outward.z * horizontal);
        target.hasImpulse = true;
    }

    private static void applyEffect(AioaBehaviorGraph.Node node, Mob source, LivingEntity target) {
        ResourceLocation id = AioaEntityHelper.parseResourceLocation(node.parameters.getOrDefault("effect", "minecraft:speed"));
        if (id == null || !BuiltInRegistries.MOB_EFFECT.containsKey(id)) return;
        Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
        if (effect == null) return;
        target.addEffect(new MobEffectInstance(effect,
                (int) number(node, "duration", 200, 1, 72000),
                (int) number(node, "amplifier", 0, 0, 255),
                flag(node, "ambient", false), flag(node, "particles", true)), source);
    }

    private static void summonLightning(AioaBehaviorGraph.Node node, Mob mob, ExecutionContext context) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        LivingEntity target = context.target != null ? context.target : mob.getTarget();
        Vec3 position = flag(node, "atTarget", true) && target != null ? target.position() : mob.position();
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning == null) return;
        lightning.moveTo(position.x, position.y, position.z);
        lightning.setVisualOnly(flag(node, "visualOnly", true));
        level.addFreshEntity(lightning);
    }

    private static void explosion(AioaBehaviorGraph.Node node, Mob mob, ExecutionContext context) {
        LivingEntity target = context.target != null ? context.target : mob.getTarget();
        Vec3 position = flag(node, "atTarget", false) && target != null ? target.position() : mob.position();
        Level.ExplosionInteraction interaction = flag(node, "breakBlocks", false)
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        mob.level().explode(mob, position.x, position.y, position.z,
                (float) number(node, "power", 2, 0, 12), flag(node, "fire", false), interaction);
    }

    private static void actionBar(AioaBehaviorGraph.Node node, Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        String message = node.parameters.getOrDefault("message", "{mob}")
                .replace("{mob}", mob.getName().getString());
        double range = number(node, "range", 32, 1, 256);
        level.players().stream().filter(player -> player.distanceToSqr(mob) <= range * range)
                .forEach(player -> player.displayClientMessage(Component.literal(message), true));
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

    private static void runCreatorScript(AioaBehaviorGraph.Node node, Mob mob, ExecutionContext context) {
        String script = node.parameters.getOrDefault("script", "").replace(';', '\n');
        ArrayDeque<Boolean> parents = new ArrayDeque<>();
        ArrayDeque<Boolean> conditions = new ArrayDeque<>();
        boolean active = true;
        int executed = 0;
        for (String raw : script.split("\\R")) {
            if (executed++ >= 128) break;
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) continue;
            if (line.startsWith("if ")) {
                boolean condition = scriptCondition(line.substring(3).replace("{", "").trim(), mob, context);
                parents.push(active);
                conditions.push(condition);
                active = active && condition;
                continue;
            }
            if (line.equals("else") || line.equals("else {") || line.equals("} else {")) {
                if (!parents.isEmpty() && !conditions.isEmpty()) {
                    boolean inverted = !conditions.pop();
                    conditions.push(inverted);
                    active = parents.peek() && inverted;
                }
                continue;
            }
            if (line.equals("}") || line.equalsIgnoreCase("end")) {
                if (!parents.isEmpty()) active = parents.pop();
                if (!conditions.isEmpty()) conditions.pop();
                continue;
            }
            if (!active) continue;
            if (line.startsWith("let ")) {
                String[] assignment = line.substring(4).split("=", 2);
                if (assignment.length == 2) context.variables.put(safeScriptName(assignment[0]),
                        scriptValue(assignment[1], mob, context));
                continue;
            }
            runScriptCall(line, mob, context);
        }
    }

    private static void runScriptCall(String line, Mob mob, ExecutionContext context) {
        String name;
        String argument;
        int open = line.indexOf('(');
        if (open >= 0 && line.endsWith(")")) {
            name = line.substring(0, open).trim().toLowerCase(java.util.Locale.ROOT);
            argument = line.substring(open + 1, line.length() - 1).trim();
        } else {
            String[] legacy = line.split("=", 2);
            name = legacy[0].trim().toLowerCase(java.util.Locale.ROOT);
            argument = legacy.length > 1 ? legacy[1].trim() : "true";
        }
        LivingEntity target = context.target != null ? context.target : mob.getTarget();
        switch (name) {
            case "say" -> sayInChat(new AioaBehaviorGraph.Node("script-say", AioaBehaviorGraph.NodeType.SAY_IN_CHAT, 0, 0)
                    .parameter("message", scriptText(argument, mob, context)), mob);
            case "actionbar" -> actionBar(new AioaBehaviorGraph.Node("script-actionbar", AioaBehaviorGraph.NodeType.ACTION_BAR, 0, 0)
                    .parameter("message", scriptText(argument, mob, context)), mob);
            case "rotate" -> {
                float yaw = mob.getYRot() + (float) scriptValue(argument, mob, context);
                mob.setYRot(yaw); mob.setYHeadRot(yaw); mob.setYBodyRot(yaw);
            }
            case "glow" -> mob.setGlowingTag(scriptBoolean(argument, mob, context));
            case "aggressive" -> mob.setAggressive(scriptBoolean(argument, mob, context));
            case "no_ai" -> mob.setNoAi(scriptBoolean(argument, mob, context));
            case "stop" -> mob.getNavigation().stop();
            case "heal" -> mob.heal((float) Math.max(0, Math.min(2048, scriptValue(argument, mob, context))));
            case "damage_target" -> {
                if (target != null && target.isAlive()) target.hurt(mob.damageSources().mobAttack(mob),
                        (float) Math.max(0, Math.min(2048, scriptValue(argument, mob, context))));
            }
            case "move_to_target" -> {
                if (target != null && mob instanceof PathfinderMob pathfinder) pathfinder.getNavigation().moveTo(target,
                        Math.max(0.1, Math.min(3, scriptValue(argument, mob, context))));
            }
            case "set_phase" -> context.variables.put("__phase", Math.max(1, Math.min(4, scriptValue(argument, mob, context))));
            case "tag" -> mob.addTag(safeScriptName(stripScriptQuotes(argument)));
            default -> { }
        }
    }

    private static boolean scriptCondition(String expression, Mob mob, ExecutionContext context) {
        String value = expression.trim();
        if (value.equals("target_exists")) return (context.target != null ? context.target : mob.getTarget()) != null;
        if (value.equals("!target_exists")) return (context.target != null ? context.target : mob.getTarget()) == null;
        for (String operator : List.of(">=", "<=", "==", "!=", ">", "<")) {
            int at = value.indexOf(operator);
            if (at < 0) continue;
            double left = scriptValue(value.substring(0, at), mob, context);
            double right = scriptValue(value.substring(at + operator.length()), mob, context);
            return switch (operator) {
                case ">=" -> left >= right; case "<=" -> left <= right; case "==" -> Math.abs(left - right) < 1.0E-9;
                case "!=" -> Math.abs(left - right) >= 1.0E-9; case ">" -> left > right; default -> left < right;
            };
        }
        return scriptValue(value, mob, context) != 0;
    }

    private static double scriptValue(String raw, Mob mob, ExecutionContext context) {
        String value = raw.trim();
        LivingEntity target = context.target != null ? context.target : mob.getTarget();
        return switch (value) {
            case "health" -> mob.getHealth();
            case "max_health" -> mob.getMaxHealth();
            case "health_percent" -> mob.getHealth() / Math.max(1.0F, mob.getMaxHealth());
            case "target_health" -> target == null ? 0 : target.getHealth();
            case "target_distance" -> target == null ? 1_000_000 : Math.sqrt(mob.distanceToSqr(target));
            case "phase" -> context.variables.getOrDefault("__phase", 1.0D);
            case "tick" -> (double) mob.level().getGameTime();
            default -> {
                try { yield Double.parseDouble(value); }
                catch (NumberFormatException ignored) { yield context.variables.getOrDefault(safeScriptName(value), 0.0D); }
            }
        };
    }

    private static boolean scriptBoolean(String raw, Mob mob, ExecutionContext context) {
        String value = raw.trim();
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        return scriptValue(value, mob, context) != 0;
    }

    private static String scriptText(String raw, Mob mob, ExecutionContext context) {
        String value = stripScriptQuotes(raw).replace("{mob}", mob.getName().getString());
        for (Map.Entry<String, Double> variable : context.variables.entrySet()) {
            value = value.replace("{" + variable.getKey() + "}", Double.toString(variable.getValue()));
        }
        return value.substring(0, Math.min(256, value.length()));
    }

    private static String stripScriptQuotes(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'")))) return trimmed.substring(1, trimmed.length() - 1);
        return trimmed;
    }

    private static String safeScriptName(String value) {
        String safe = value.trim().replaceAll("[^A-Za-z0-9_]", "_");
        if (safe.isBlank()) safe = "value";
        return safe.substring(0, Math.min(48, safe.length()));
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

    private static void spawnMob(AioaBehaviorGraph.Node node, Mob source, RuntimeState state) {
        int cooldown = (int) number(node, "cooldown", 200, 20, 12000);
        long gameTime = source.level().getGameTime();
        long nextAllowed = state.cooldowns.getOrDefault(node.id, 0L);
        if (gameTime < nextAllowed || !(source.level() instanceof ServerLevel level)) return;
        state.cooldowns.put(node.id, gameTime + cooldown);
        double radius = number(node, "capRadius", 16, 4, 64);
        int configuredCap = AioaConfigManager.getConfig().behaviorEngine.maxNodeSpawnedMobsNearby;
        int cap = Math.min(configuredCap, (int) number(node, "nearbyCap", 8, 1, 64));
        if (level.getEntitiesOfClass(Mob.class, source.getBoundingBox().inflate(radius)).size() >= cap) return;
        Optional<EntityType<?>> type = AioaEntityHelper.resolveEntityType(node.parameters.getOrDefault("entity", "minecraft:zombie"));
        if (type.isEmpty()) return;
        Entity created = type.get().create(level);
        if (!(created instanceof Mob spawned)) return;
        spawned.moveTo(source.getX() + number(node, "offsetX", 1, -8, 8), source.getY() + number(node, "offsetY", 0, -8, 8),
                source.getZ() + number(node, "offsetZ", 1, -8, 8), source.getYRot(), 0);
        if (!level.noCollision(spawned)) return;
        spawned.finalizeSpawn(level, level.getCurrentDifficultyAt(spawned.blockPosition()), MobSpawnType.EVENT, null);
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
        SoundEvent sound = id.map(AioaRegistryCompat::getSoundEvent).orElse(null);
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
        mob.setItemSlot(slot, new ItemStack(BuiltInRegistries.ITEM.get(id)));
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

    private static boolean timerReady(AioaBehaviorGraph.Node node, Mob mob, RuntimeState state, long delay, boolean waitBeforeFirst) {
        long now = mob.level().getGameTime();
        Long readyAt = state.timers.get(node.id);
        if (readyAt == null) {
            state.timers.put(node.id, now + delay);
            return !waitBeforeFirst;
        }
        if (now < readyAt) return false;
        state.timers.put(node.id, now + delay);
        return true;
    }

    private static void cleanupStates(long gameTime) {
        if (gameTime - lastCleanupTick < 1200L) return;
        lastCleanupTick = gameTime;
        STATES.entrySet().removeIf(entry -> gameTime - entry.getValue().lastTouchedTick > 2400L);
    }

    private static final class ExecutionContext {
        private final AioaBehaviorGraph graph;
        private LivingEntity target;
        private final RuntimeState state;
        private final Map<String, Double> variables;

        private int groupDepth;

        private ExecutionContext(AioaBehaviorGraph graph, LivingEntity target, RuntimeState state) {
            this.graph = graph;
            this.target = target;
            this.state = state;
            this.variables = state.variables;
        }
    }

    private record RuntimeKey(String graphId, UUID entityId) { }

    private static final class RuntimeState {
        private final Map<String, Double> variables = new HashMap<>();
        private final Map<String, Long> timers = new HashMap<>();
        private final Map<String, Long> cooldowns = new HashMap<>();
        private final Set<String> firedOnce = new HashSet<>();
        private long lastTouchedTick;
    }
}
