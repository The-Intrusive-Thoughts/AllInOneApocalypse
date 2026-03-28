package com.flubburr.aioa.spawn;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.IronGolem;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AioaZombieBehaviour {

    private static final UUID FOLLOW_RANGE_MODIFIER_ID = UUID.fromString("7dc8f5fb-15a5-4a64-a228-cb62398f2d9f");
    private static final UUID CHASE_SPEED_MODIFIER_ID = UUID.fromString("80a5998f-26f7-4a83-986e-1577145f0ed0");
    private static final AttributeModifier FOLLOW_RANGE_MODIFIER = new AttributeModifier(
            FOLLOW_RANGE_MODIFIER_ID,
            "aioa_refined_zombie_follow_range",
            10.0D,
            AttributeModifier.Operation.ADDITION
    );
    private static final AttributeModifier CHASE_SPEED_MODIFIER = new AttributeModifier(
            CHASE_SPEED_MODIFIER_ID,
            "aioa_refined_zombie_chase_speed",
            0.08D,
            AttributeModifier.Operation.MULTIPLY_TOTAL
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

    public static void refreshTargetGoals(Zombie zombie, GoalSelector targetSelector, AioaConfig.ZombieTargetMode mode) {
        targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal<?>);

        switch (mode) {
            case VANILLA -> {
                targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(zombie, Player.class, true));
                targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(zombie, AbstractVillager.class, false));
                targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(zombie, IronGolem.class, true));
                targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(zombie, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
            }
            case PLAYERS_ONLY -> targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(zombie, Player.class, true));
            case ANIMALS_ONLY -> {
                targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(zombie, Animal.class, true));
                targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(zombie, WaterAnimal.class, true));
            }
            case OTHER_MOBS_ONLY -> targetSelector.addGoal(
                    2,
                    new NearestAttackableTargetGoal<>(zombie, PathfinderMob.class, 10, true, false, target -> canTarget(zombie, target))
            );
            case EVERYTHING -> targetSelector.addGoal(
                    2,
                    new NearestAttackableTargetGoal<>(zombie, LivingEntity.class, 10, true, false, target -> canTarget(zombie, target))
            );
        }
    }

    public static boolean canTarget(Zombie zombie, LivingEntity target) {
        if (target == null || !target.isAlive() || target == zombie || target instanceof Zombie) {
            return false;
        }

        return switch (AioaConfigManager.getConfig().daySurfaceSpawns.zombieTargetMode) {
            case VANILLA -> target instanceof Player || target instanceof AbstractVillager || target instanceof IronGolem
                    || target instanceof Turtle turtle && turtle.isBaby() && !turtle.isInWater();
            case PLAYERS_ONLY -> target instanceof Player;
            case ANIMALS_ONLY -> target instanceof Animal || target instanceof WaterAnimal;
            case OTHER_MOBS_ONLY -> target instanceof Mob && !(target instanceof Player);
            case EVERYTHING -> true;
        };
    }

    public static boolean shouldClimb(Zombie zombie, boolean enabled) {
        LivingEntity target = zombie.getTarget();
        if (!enabled || target == null || !target.isAlive()) {
            return false;
        }

        return zombie.horizontalCollision
                && zombie.distanceToSqr(target) <= 144.0D
                && target.getY() >= zombie.getY() - 0.5D;
    }

    public static void applyRefinedAi(Zombie zombie, boolean refinedAiEnabled) {
        AttributeInstance followRange = zombie.getAttribute(Attributes.FOLLOW_RANGE);
        AttributeInstance movementSpeed = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
        if (followRange == null || movementSpeed == null) {
            return;
        }

        if (refinedAiEnabled) {
            if (!followRange.hasModifier(FOLLOW_RANGE_MODIFIER)) {
                followRange.addTransientModifier(FOLLOW_RANGE_MODIFIER);
            }
            if (zombie.getTarget() != null) {
                if (!movementSpeed.hasModifier(CHASE_SPEED_MODIFIER)) {
                    movementSpeed.addTransientModifier(CHASE_SPEED_MODIFIER);
                }
            } else if (movementSpeed.hasModifier(CHASE_SPEED_MODIFIER)) {
                movementSpeed.removeModifier(CHASE_SPEED_MODIFIER);
            }
            return;
        }

        if (followRange.hasModifier(FOLLOW_RANGE_MODIFIER)) {
            followRange.removeModifier(FOLLOW_RANGE_MODIFIER);
        }
        if (movementSpeed.hasModifier(CHASE_SPEED_MODIFIER)) {
            movementSpeed.removeModifier(CHASE_SPEED_MODIFIER);
        }
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
