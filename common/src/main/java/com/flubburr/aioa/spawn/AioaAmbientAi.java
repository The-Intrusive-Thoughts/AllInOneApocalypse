package com.flubburr.aioa.spawn;

import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.mixin.accessor.MobAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Live settings for mobs that still use their native goal selectors. */
public final class AioaAmbientAi {
    private final boolean refined;
    private final boolean climbs;
    private long nextListen;
    private final AioaConfig.ZombieTargetMode mode;
    private final List<WrappedGoal> originalTargets;
    private final List<WrappedGoal> replacements;
    private OpenDoorGoal doorGoal;
    private boolean couldOpenDoors;
    private boolean couldPassDoors;

    public static AioaAmbientAi configure(Mob mob, AioaConfig.DaySurfaceSpawns settings) {
        boolean refined = AioaZombieBehaviour.usesRefinedAi(mob, settings);
        boolean climbs = AioaZombieBehaviour.usesWallClimbing(mob, settings);
        var mode = AioaZombieBehaviour.getTargetMode(mob, settings);
        if (!refined && !climbs && (mode == null || mode == AioaConfig.ZombieTargetMode.VANILLA)) return null;
        return new AioaAmbientAi(mob, settings, refined, climbs, mode);
    }

    private AioaAmbientAi(Mob mob, AioaConfig.DaySurfaceSpawns settings, boolean refined, boolean climbs, AioaConfig.ZombieTargetMode mode) {
        this.refined = refined;
        this.climbs = climbs;
        this.mode = mode;
        var selectors = (MobAccessor) mob;
        var targets = selectors.aioa$getTargetSelector();
        if (mode != null && mode != AioaConfig.ZombieTargetMode.VANILLA) {
            originalTargets = targets.getAvailableGoals().stream().filter(g -> g.getGoal() instanceof NearestAttackableTargetGoal<?>).toList();
            originalTargets.forEach(g -> targets.removeGoal(g.getGoal()));
            AioaZombieBehaviour.refreshTargetGoals(mob, targets, mode);
            replacements = targets.getAvailableGoals().stream().filter(g -> g.getGoal() instanceof NearestAttackableTargetGoal<?>).toList();
        } else {
            originalTargets = replacements = List.of();
        }
        if (refined && settings.refinedPathfindingOpensDoors && mob.getNavigation() instanceof GroundPathNavigation ground) {
            var evaluator = ground.getNodeEvaluator();
            couldOpenDoors = evaluator.canOpenDoors();
            couldPassDoors = evaluator.canPassDoors();
            evaluator.setCanOpenDoors(true);
            evaluator.setCanPassDoors(true);
            doorGoal = new OpenDoorGoal(mob, true);
            selectors.aioa$getGoalSelector().addGoal(1, doorGoal);
        }
    }

    public void tick(Mob mob, AioaConfig.DaySurfaceSpawns settings) {
        var target = mob.getTarget();
        if (target != null && mode != null && mode != AioaConfig.ZombieTargetMode.VANILLA
                && !AioaZombieBehaviour.canTarget(mob, target, mode)) mob.setTarget(null);
        if (refined) {
            if ((mob.tickCount + mob.getId()) % 5 == 0) AioaZombieBehaviour.applyRefinedAi(mob, true);
            AioaZombieBehaviour.coordinateNearbyMobs(mob, settings);
            if (mob instanceof Zombie && target == null && mob.getNavigation().isDone()) investigateFootsteps(mob);
        }
        if (climbs) AioaZombieBehaviour.tickWallClimbing(mob, true, refined);
    }

    private void investigateFootsteps(Mob mob) {
        if (mode != null && mode != AioaConfig.ZombieTargetMode.VANILLA
                && mode != AioaConfig.ZombieTargetMode.PLAYERS_ONLY && mode != AioaConfig.ZombieTargetMode.EVERYTHING) return;
        long now = mob.level().getGameTime();
        if (now < nextListen) return;
        nextListen = now + 24 + Math.floorMod(mob.getId(), 17);
        Player heard = null;
        double nearest = 12 * 12;
        for (Player player : mob.level().players()) {
            if (!player.isSprinting() || !player.onGround() || player.isSpectator() || player.isCreative() || !player.isAlive()) continue;
            double distance = mob.distanceToSqr(player);
            if (distance < nearest && mob.canAttack(player)) {
                heard = player;
                nearest = distance;
            }
        }
        if (heard != null && !mob.getSensing().hasLineOfSight(heard)) {
            // Walk to the sound once. Only the normal targeting goals can acquire the player.
            mob.getNavigation().moveTo(heard.getX(), heard.getY(), heard.getZ(), 0.9);
            nextListen = now + 80;
        }
    }

    public void restore(Mob mob) {
        var selectors = (MobAccessor) mob;
        var targets = selectors.aioa$getTargetSelector();
        replacements.forEach(g -> targets.removeGoal(g.getGoal()));
        originalTargets.forEach(g -> targets.addGoal(g.getPriority(), g.getGoal()));
        if (doorGoal != null) {
            selectors.aioa$getGoalSelector().removeGoal(doorGoal);
            var evaluator = mob.getNavigation().getNodeEvaluator();
            evaluator.setCanOpenDoors(couldOpenDoors);
            evaluator.setCanPassDoors(couldPassDoors);
        }
        if (refined) AioaZombieBehaviour.applyRefinedAi(mob, false);
    }
}
