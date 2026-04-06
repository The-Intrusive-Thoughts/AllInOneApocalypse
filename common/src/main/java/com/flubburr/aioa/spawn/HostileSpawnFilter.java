package com.flubburr.aioa.spawn;

import com.flubburr.aioa.compat.AioaEntityHelper;
import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.EnumSet;
import java.util.List;

public final class HostileSpawnFilter {

    private static final EnumSet<EntitySpawnReason> SPECIAL_SPAWN_TYPES = EnumSet.of(
            EntitySpawnReason.BREEDING,
            EntitySpawnReason.BUCKET,
            EntitySpawnReason.COMMAND,
            EntitySpawnReason.CONVERSION,
            EntitySpawnReason.DISPENSER,
            EntitySpawnReason.EVENT,
            EntitySpawnReason.JOCKEY,
            EntitySpawnReason.MOB_SUMMONED,
            EntitySpawnReason.PATROL,
            EntitySpawnReason.REINFORCEMENT,
            EntitySpawnReason.SPAWN_ITEM_USE,
            EntitySpawnReason.TRIGGERED
    );

    private HostileSpawnFilter() {
    }

    public static boolean shouldCancelSpawn(EntityType<?> entityType, ServerLevelAccessor levelAccessor, EntitySpawnReason spawnType, BlockPos pos) {
        AioaConfig.HostileSpawnControl settings = AioaConfigManager.getConfig().hostileSpawnControl;
        if (!settings.enabled) {
            return false;
        }

        ServerLevel level = levelAccessor.getLevel();
        if (settings.overworldOnly && !level.dimension().equals(Level.OVERWORLD)) {
            return false;
        }
        if (spawnType == EntitySpawnReason.STRUCTURE && settings.ignoreStructureSpawns) {
            return false;
        }
        if (spawnType == EntitySpawnReason.SPAWNER && settings.ignoreSpawnerSpawns) {
            return false;
        }
        if (settings.ignoreSpecialSpawns && SPECIAL_SPAWN_TYPES.contains(spawnType)) {
            return false;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (isWhitelisted(entityId, settings.whitelistEntityIds)) {
            return false;
        }

        return AioaEntityHelper.isHostileMob(entityType, level);
    }

    private static boolean isWhitelisted(ResourceLocation entityId, List<String> rawWhitelist) {
        for (String rawId : rawWhitelist) {
            java.util.Optional<ResourceLocation> configuredId = AioaEntityHelper.resolveEntityId(
                    rawId,
                    warning -> AioaConfigManager.warnOnce("invalid-whitelist-id:" + rawId, warning)
            );
            if (configuredId.isEmpty()) {
                continue;
            }

            if (configuredId.get().equals(entityId)) {
                return true;
            }
        }

        return false;
    }
}
