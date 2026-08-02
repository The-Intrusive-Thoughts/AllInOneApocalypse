package com.flubburr.aioa.spawn;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.compat.AioaEntityHelper;
import com.flubburr.aioa.network.AioaSpawnRequest;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class AioaSpawnStudioHandler {
    private static final double MAX_REQUEST_DISTANCE_SQR = 64.0D * 64.0D;

    private AioaSpawnStudioHandler() {
    }

    public static void handle(ServerPlayer player, AioaSpawnRequest request) {
        if (!player.isCreative()) {
            player.displayClientMessage(Component.literal("AIOA Spawn Studio requires creative mode."), true);
            return;
        }

        Vec3 position = new Vec3(request.x(), request.y(), request.z());
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)
                || player.distanceToSqr(position) > MAX_REQUEST_DISTANCE_SQR) {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (position.y < level.getMinBuildHeight() || position.y >= level.getMaxBuildHeight()) {
            return;
        }

        Optional<EntityType<?>> resolved = AioaEntityHelper.resolveEntityType(request.entityId());
        if (resolved.isEmpty()) {
            player.displayClientMessage(Component.literal("Unknown mob: " + request.entityId()), true);
            return;
        }

        Entity entity = resolved.get().create(level);
        if (!(entity instanceof Mob mob)) {
            player.displayClientMessage(Component.literal("That entity is not a spawnable mob."), true);
            return;
        }

        float yaw = request.facePlayer()
                ? (float) (Mth.atan2(player.getZ() - position.z, player.getX() - position.x) * (180.0D / Math.PI)) - 90.0F
                : player.getYRot();
        mob.moveTo(position.x, position.y, position.z, yaw, 0.0F);
        mob.setNoAi(request.noAi());
        if (request.persistent()) {
            mob.setPersistenceRequired();
        }
        mob.addTag(AioaConstants.STUDIO_SPAWN_TAG);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null);

        if (!level.noCollision(mob)) {
            mob.discard();
            player.displayClientMessage(Component.literal("No safe room to spawn that mob."), true);
            return;
        }
        level.addFreshEntityWithPassengers(mob);
        player.displayClientMessage(Component.literal("Spawned " + request.entityId()), true);
    }
}
