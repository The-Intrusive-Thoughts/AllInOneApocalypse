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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class AioaSpawnStudioHandler {
    private static final double MAX_REQUEST_DISTANCE_SQR = 64.0D * 64.0D;

    private AioaSpawnStudioHandler() {
    }

    public static void handle(ServerPlayer player, AioaSpawnRequest request) {
        if (!player.isCreative()) {
            player.sendOverlayMessage(Component.literal("AIOA Spawn Studio requires creative mode."));
            return;
        }

        Vec3 position = new Vec3(request.x(), request.y(), request.z());
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)
                || player.distanceToSqr(position) > MAX_REQUEST_DISTANCE_SQR) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        if (position.y < level.getMinY() || position.y >= level.getMaxY()) {
            return;
        }

        Optional<EntityType<?>> resolved = AioaEntityHelper.resolveEntityType(request.entityId());
        if (resolved.isEmpty()) {
            player.sendOverlayMessage(Component.literal("Unknown mob: " + request.entityId()));
            return;
        }

        Entity entity = resolved.get().create(level, EntitySpawnReason.COMMAND);
        if (!(entity instanceof Mob mob)) {
            player.sendOverlayMessage(Component.literal("That entity is not a spawnable mob."));
            return;
        }

        float yaw = request.facePlayer()
                ? (float) (Mth.atan2(player.getZ() - position.z, player.getX() - position.x) * (180.0D / Math.PI)) - 90.0F
                : player.getYRot();
        mob.snapTo(position.x, position.y, position.z, yaw, 0.0F);
        mob.setNoAi(request.noAi());
        if (request.persistent()) {
            mob.setPersistenceRequired();
        }
        mob.addTag(AioaConstants.STUDIO_SPAWN_TAG);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.COMMAND, null);

        if (!level.noCollision(mob)) {
            mob.discard();
            player.sendOverlayMessage(Component.literal("No safe room to spawn that mob."));
            return;
        }
        level.addFreshEntityWithPassengers(mob);
        player.sendOverlayMessage(Component.literal("Spawned " + request.entityId()));
    }
}
