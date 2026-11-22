package com.flubburr.aioa.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

/** Per-mob pursuit memory. Keep useful paths and investigate the last visible position. */
final class AioaPursuit {
    private int targetId = -1;
    private Vec3 lastSeen;
    private Vec3 destination;
    private long seenAt;
    private long nextAttempt;
    private int failedAttempts;

    void follow(PathfinderMob mob, LivingEntity target, double stopDistance, double speed) {
        var navigation = mob.getNavigation();
        long now = mob.level().getGameTime();
        boolean changed = target.getId() != targetId;
        if (changed) {
            targetId = target.getId();
            lastSeen = null;
            destination = null;
            failedAttempts = 0;
            nextAttempt = now;
            navigation.stop();
        }
        if (mob.getSensing().hasLineOfSight(target)) {
            lastSeen = target.position();
            seenAt = now;
        }
        if (!target.isAlive() || lastSeen == null || now - seenAt > 60
                || mob.position().distanceToSqr(lastSeen) <= stopDistance * stopDistance) {
            if (!navigation.isDone()) navigation.stop();
            return;
        }
        navigation.setSpeedModifier(speed);
        if (now < nextAttempt) return;
        if (!navigation.isDone() && destination != null && destination.distanceToSqr(lastSeen) < 2.25D) return;
        destination = lastSeen;
        boolean moving = navigation.moveTo(destination.x, destination.y, destination.z, speed);
        failedAttempts = moving ? 0 : Math.min(4, failedAttempts + 1);
        nextAttempt = now + 6 + Math.floorMod(mob.getId(), 5) + failedAttempts * 8;
    }
}
