package com.flubburr.aioa.spawn;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.compat.AioaEntityHelper;
import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AioaZombieBehaviour {

    private static final Identifier FOLLOW_RANGE_MODIFIER_ID = Identifier.of("aioa", "refined_zombie_follow_range");
    private static final Identifier CHASE_SPEED_MODIFIER_ID = Identifier.of("aioa", "refined_zombie_chase_speed");
    private static final AttributeModifier FOLLOW_RANGE_MODIFIER = new AttributeModifier(
            FOLLOW_RANGE_MODIFIER_ID,
            10.0D,
            AttributeModifier.Operation.ADD_VALUE
    );
    private static final AttributeModifier CHASE_SPEED_MODIFIER = new AttributeModifier(
            CHASE_SPEED_MODIFIER_ID,
            0.08D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );
    private static final ConcurrentHashMap<Class<?>, Optional<Method>> SET_BABY_METHOD_CACHE = new ConcurrentHashMap<>();

    private AioaZombieBehaviour() {
    }

    public static boolean applyVariantMode(Mob mob, AioaConfig.DaySurfaceSpawns settings) {
        if (!(mob instanceof Zombie)) {
            return true;
        }

        return switch (settings.zombieVariantMode) {
            case REGULAR_ONLY -> setBabyState(mob, false);
            case REGULAR_AND_BABY -> true;
            case BABY_ONLY -> setBabyState(mob, true);
        };
    }

    public static AioaConfig.ZombieTargetMode getTargetMode(Mob mob, AioaConfig.DaySurfaceSpawns settings) {
        if (isConfiguredMob(mob, settings.playerOnlyTargetEntityIds)) {
            return AioaConfig.ZombieTargetMode.PLAYERS_ONLY;
        }
        if (isConfiguredMob(mob, settings.animalTargetEntityIds)) {
            return AioaConfig.ZombieTargetMode.ANIMALS_ONLY;
        }
        if (isConfiguredMob(mob, settings.otherMobTargetEntityIds)) {
            return AioaConfig.ZombieTargetMode.OTHER_MOBS_ONLY;
        }
        if (isConfiguredMob(mob, settings.everythingTargetEntityIds)) {
            return AioaConfig.ZombieTargetMode.EVERYTHING;
        }
        if (mob instanceof Zombie) {
            return settings.zombieTargetMode;
        }
        if (isConfiguredMob(mob, settings.refinedAiEntityIds) || mob.getTags().contains(AioaConstants.DAY_SPAWN_TAG)) {
            return settings.zombieTargetMode;
        }
        return null;
    }

    public static boolean usesRefinedAi(Mob mob, AioaConfig.DaySurfaceSpawns settings) {
        return settings.refinedZombieAi
                && (mob instanceof Zombie || isConfiguredMob(mob, settings.refinedAiEntityIds));
    }

    public static boolean usesWallClimbing(Mob mob, AioaConfig.DaySurfaceSpawns settings) {
        return settings.zombiesCanClimbWalls
                && (mob instanceof Zombie || isConfiguredMob(mob, settings.wallClimbingEntityIds));
    }

    public static void refreshTargetGoals(Mob mob, GoalSelector targetSelector, AioaConfig.ZombieTargetMode mode) {
        targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal<?>);

        switch (mode) {
            case VANILLA -> {
                if (!(mob instanceof Zombie zombie)) {
                    return;
                }
                targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(zombie, Player.class, true));
                targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(zombie, AbstractVillager.class, false));
                targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(zombie, IronGolem.class, true));
                targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(zombie, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
            }
            case PLAYERS_ONLY -> targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, true));
            case ANIMALS_ONLY -> {
                targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Animal.class, true));
                targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob, WaterAnimal.class, true));
            }
            case OTHER_MOBS_ONLY -> targetSelector.addGoal(
                    2,
                    new NearestAttackableTargetGoal<>(mob, PathfinderMob.class, 10, true, false, target -> canTarget(mob, target, mode))
            );
            case EVERYTHING -> targetSelector.addGoal(
                    2,
                    new NearestAttackableTargetGoal<>(mob, LivingEntity.class, 10, true, false, target -> canTarget(mob, target, mode))
            );
        }
    }

    public static boolean canTarget(Mob mob, LivingEntity target) {
        AioaConfig.ZombieTargetMode mode = getTargetMode(mob, AioaConfigManager.getConfig().daySurfaceSpawns);
        return mode == null || canTarget(mob, target, mode);
    }

    public static boolean canTarget(Mob mob, LivingEntity target, AioaConfig.ZombieTargetMode mode) {
        if (target == null || !target.isAlive() || target == mob || target instanceof Zombie) {
            return false;
        }

        return switch (mode) {
            case VANILLA -> target instanceof Player || target instanceof AbstractVillager || target instanceof IronGolem
                    || target instanceof Turtle turtle && turtle.isBaby() && !turtle.isInWater();
            case PLAYERS_ONLY -> target instanceof Player;
            case ANIMALS_ONLY -> target instanceof Animal || target instanceof WaterAnimal;
            case OTHER_MOBS_ONLY -> target instanceof Mob && !(target instanceof Player);
            case EVERYTHING -> true;
        };
    }

    public static boolean shouldClimb(Mob mob) {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        return mob.horizontalCollision
                && mob.distanceToSqr(target) <= 144.0D
                && target.getY() >= mob.getY() - 0.5D;
    }

    public static void tickWallClimbing(Mob mob, boolean enabled, boolean refinedAiEnabled) {
        if (!enabled || !shouldClimb(mob)) {
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }

        Vec3 currentVelocity = mob.getDeltaMovement();
        Vec3 chaseVector = new Vec3(target.getX() - mob.getX(), 0.0D, target.getZ() - mob.getZ());
        if (chaseVector.lengthSqr() > 1.0E-4D) {
            chaseVector = chaseVector.normalize().scale(0.08D);
        } else {
            chaseVector = Vec3.ZERO;
        }

        double climbBoost = target.getY() > mob.getEyeY() + 0.75D ? 0.24D : 0.16D;
        mob.setDeltaMovement(
                (currentVelocity.x * 0.92D) + chaseVector.x,
                Math.max(currentVelocity.y, climbBoost),
                (currentVelocity.z * 0.92D) + chaseVector.z
        );
        mob.fallDistance = 0.0F;
        mob.hasImpulse = true;

        if (mob instanceof PathfinderMob pathfinderMob) {
            double speed = refinedAiEnabled ? 1.15D : 1.0D;
            pathfinderMob.getNavigation().moveTo(target, speed);
        }
    }

    public static void applyRefinedAi(Mob mob, boolean refinedAiEnabled, boolean openDoors) {
        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        AttributeInstance movementSpeed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (followRange == null || movementSpeed == null) {
            return;
        }

        if (refinedAiEnabled) {
            if (!followRange.hasModifier(FOLLOW_RANGE_MODIFIER_ID)) {
                followRange.addTransientModifier(FOLLOW_RANGE_MODIFIER);
            }
            if (mob.getTarget() != null) {
                if (!movementSpeed.hasModifier(CHASE_SPEED_MODIFIER_ID)) {
                    movementSpeed.addTransientModifier(CHASE_SPEED_MODIFIER);
                }
                if (mob instanceof PathfinderMob pathfinderMob) {
                    if (openDoors && pathfinderMob.getNavigation() instanceof GroundPathNavigation groundNavigation) {
                        groundNavigation.setCanOpenDoors(true);
                    }
                    if (mob.tickCount % 10 == 0) {
                        pathfinderMob.getNavigation().moveTo(mob.getTarget(), 1.15D);
                    }
                }
            } else if (movementSpeed.hasModifier(CHASE_SPEED_MODIFIER_ID)) {
                movementSpeed.removeModifier(CHASE_SPEED_MODIFIER_ID);
            }
            return;
        }

        if (followRange.hasModifier(FOLLOW_RANGE_MODIFIER_ID)) {
            followRange.removeModifier(FOLLOW_RANGE_MODIFIER_ID);
        }
        if (movementSpeed.hasModifier(CHASE_SPEED_MODIFIER_ID)) {
            movementSpeed.removeModifier(CHASE_SPEED_MODIFIER_ID);
        }
        if (mob instanceof PathfinderMob pathfinderMob
                && pathfinderMob.getNavigation() instanceof GroundPathNavigation groundNavigation) {
            groundNavigation.setCanOpenDoors(false);
        }
    }

    private static boolean isConfiguredMob(Mob mob, List<String> rawEntityIds) {
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        for (String rawEntityId : rawEntityIds) {
            Optional<Identifier> configuredId = AioaEntityHelper.resolveEntityId(
                    rawEntityId,
                    warning -> AioaConfigManager.warnOnce("invalid-ai-selector:" + rawEntityId, warning)
            );
            if (configuredId.isPresent() && configuredId.get().equals(entityId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean setBabyState(Mob mob, boolean baby) {
        if (mob instanceof AgeableMob ageableMob && !baby) {
            ageableMob.setAge(0);
        }

        Optional<Method> setBabyMethod = resolveSetBabyMethod(mob.getClass());
        if (setBabyMethod.isPresent()) {
            try {
                setBabyMethod.get().invoke(mob, baby);
            } catch (Exception exception) {
                AioaConstants.LOG.debug("AIOA could not switch '{}' baby state to '{}'.", mob.getType(), baby, exception);
            }
        }

        if (mob.isBaby() != baby) {
            AioaConfigManager.warnOnce(
                    "zombie-variant-unsupported:" + mob.getType() + ":" + baby,
                    "AIOA skipped spawning '" + mob.getType() + "' because it could not apply the configured zombie age mode."
            );
            return false;
        }

        return true;
    }

    private static Optional<Method> resolveSetBabyMethod(Class<?> type) {
        return SET_BABY_METHOD_CACHE.computeIfAbsent(type, key -> {
            try {
                Method method = key.getMethod("setBaby", boolean.class);
                method.setAccessible(true);
                return Optional.of(method);
            } catch (Exception ignored) {
                return Optional.empty();
            }
        });
    }
}
