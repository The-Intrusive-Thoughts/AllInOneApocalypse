package com.flubburr.aioa.spawn;

import com.flubburr.aioa.compat.AioaEntityHelper;
import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.EnumSet;
import java.util.List;

public final class HostileSpawnFilter {

    private static final EnumSet<MobSpawnType> SPECIAL_SPAWN_TYPES = EnumSet.of(
            MobSpawnType.BREEDING,
            MobSpawnType.BUCKET,
            MobSpawnType.COMMAND,
            MobSpawnType.CONVERSION,
            MobSpawnType.DISPENSER,
            MobSpawnType.EVENT,
            MobSpawnType.JOCKEY,
            MobSpawnType.MOB_SUMMONED,
            MobSpawnType.PATROL,
            MobSpawnType.REINFORCEMENT,
            MobSpawnType.SPAWN_EGG,
            MobSpawnType.TRIGGERED
    );

    private HostileSpawnFilter() {
    }

    public static boolean shouldCancelSpawn(EntityType<?> entityType, ServerLevelAccessor levelAccessor, MobSpawnType spawnType, BlockPos pos) {
        AioaConfig.HostileSpawnControl settings = AioaConfigManager.getConfig().hostileSpawnControl;
        if (!settings.enabled) {
            return false;
        }

        ServerLevel level = levelAccessor.getLevel();
        if (settings.overworldOnly && !level.dimension().equals(Level.OVERWORLD)) {
            return false;
        }
        if (spawnType == MobSpawnType.STRUCTURE && settings.ignoreStructureSpawns) {
            return false;
        }
        if (spawnType == MobSpawnType.SPAWNER && settings.ignoreSpawnerSpawns) {
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
