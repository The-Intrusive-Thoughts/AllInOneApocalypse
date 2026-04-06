package com.flubburr.aioa.spawn;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.compat.AioaEntityHelper;
import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import com.flubburr.aioa.config.AioaSpawnEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ApocalypseSpawnManager {

    private ApocalypseSpawnManager() {
    }

    public static void tick(ServerLevel level) {
        AioaConfig.DaySurfaceSpawns settings = AioaConfigManager.getConfig().daySurfaceSpawns;
        if (!settings.enabled) {
            return;
        }
        if (settings.overworldOnly && !level.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        if (settings.requireDaytime && !level.isDay()) {
            return;
        }
        if (level.getGameTime() % settings.spawnIntervalTicks != 0L) {
            return;
        }

        List<ResolvedSpawnEntry> pool = resolveSpawnPool(level, settings);
        if (pool.isEmpty()) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }

            int nearbyManaged = countNearbyManagedMobs(level, player.blockPosition(), settings.maxSpawnDistance);
            if (nearbyManaged >= settings.maxNearbyManagedMobs) {
                continue;
            }

            for (int attempt = 0; attempt < settings.spawnAttemptsPerPlayer && nearbyManaged < settings.maxNearbyManagedMobs; attempt++) {
                ResolvedSpawnEntry chosenEntry = chooseWeighted(pool, level.getRandom());
                if (chosenEntry == null || level.getRandom().nextDouble() > chosenEntry.entry().chance()) {
                    continue;
                }

                BlockPos basePosition = findSpawnPosition(level, player, chosenEntry.entityType(), settings, level.getRandom());
                if (basePosition == null) {
                    continue;
                }

                nearbyManaged += spawnGroup(level, basePosition, chosenEntry, settings, level.getRandom());
            }
        }
    }

    private static List<ResolvedSpawnEntry> resolveSpawnPool(ServerLevel level, AioaConfig.DaySurfaceSpawns settings) {
        List<ResolvedSpawnEntry> resolvedEntries = new ArrayList<>();

        for (String rawEntry : settings.spawnPoolEntries) {
            Optional<AioaSpawnEntry> parsedEntry = AioaSpawnEntry.parse(
                    rawEntry,
                    warning -> AioaConfigManager.warnOnce("spawn-entry:" + rawEntry, warning)
            );
            if (parsedEntry.isEmpty()) {
                continue;
            }

            AioaSpawnEntry entry = parsedEntry.get();
            if (!entry.enabled()) {
                continue;
            }

            Optional<EntityType<?>> entityType = AioaEntityHelper.resolveEntityType(entry.entityId());
            if (entityType.isEmpty()) {
                AioaConfigManager.warnOnce(
                        "missing-entity:" + entry.entityId(),
                        "AIOA ignored configured day spawn entity '" + entry.entityId() + "' because nothing is registered under that id."
                );
                continue;
            }

            if (!AioaEntityHelper.isConfigurableMob(entityType.get(), level)) {
                AioaConfigManager.warnOnce(
                        "unsupported-entity:" + entry.entityId(),
                        "AIOA ignored configured day spawn entity '" + entry.entityId() + "' because it is not a living mob with normal AI behavior."
                );
                continue;
            }

            resolvedEntries.add(new ResolvedSpawnEntry(entry, entityType.get()));
        }

        return resolvedEntries;
    }

    private static ResolvedSpawnEntry chooseWeighted(List<ResolvedSpawnEntry> pool, RandomSource random) {
        int totalWeight = 0;
        for (ResolvedSpawnEntry entry : pool) {
            totalWeight += Math.max(1, entry.entry().weight());
        }

        if (totalWeight <= 0) {
            return null;
        }

        int roll = random.nextInt(totalWeight);
        for (ResolvedSpawnEntry entry : pool) {
            roll -= Math.max(1, entry.entry().weight());
            if (roll < 0) {
                return entry;
            }
        }

        return pool.get(pool.size() - 1);
    }

    private static BlockPos findSpawnPosition(
            ServerLevel level,
            ServerPlayer player,
            EntityType<?> entityType,
            AioaConfig.DaySurfaceSpawns settings,
            RandomSource random
    ) {
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = player.getBlockX() + randomOffset(random, settings.minSpawnDistance, settings.maxSpawnDistance);
            int z = player.getBlockZ() + randomOffset(random, settings.minSpawnDistance, settings.maxSpawnDistance);
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));

            if (!level.getWorldBorder().isWithinBounds(surface)) {
                continue;
            }
            if (settings.requireClearSky && !level.canSeeSky(surface)) {
                continue;
            }
            if (!isAllowedBiome(level, surface, settings)) {
                continue;
            }
            if (player.distanceToSqr(Vec3.atBottomCenterOf(surface)) < (double) (settings.minSpawnDistance * settings.minSpawnDistance)) {
                continue;
            }
            if (!isPotentialSpawnPosition(level, surface, entityType)) {
                continue;
            }

            return surface;
        }

        return null;
    }

    private static int spawnGroup(
            ServerLevel level,
            BlockPos basePosition,
            ResolvedSpawnEntry resolvedEntry,
            AioaConfig.DaySurfaceSpawns settings,
            RandomSource random
    ) {
        int groupSize = Mth.nextInt(random, resolvedEntry.entry().minGroupSize(), resolvedEntry.entry().maxGroupSize());
        int spawned = 0;

        for (int index = 0; index < groupSize; index++) {
            for (int retry = 0; retry < 4; retry++) {
                int x = basePosition.getX() + random.nextInt(9) - 4;
                int z = basePosition.getZ() + random.nextInt(9) - 4;
                BlockPos spawnPosition = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));

                if (trySpawn(level, spawnPosition, resolvedEntry.entityType(), settings, random)) {
                    spawned++;
                    break;
                }
            }
        }

        return spawned;
    }

    private static boolean trySpawn(
            ServerLevel level,
            BlockPos spawnPosition,
            EntityType<?> entityType,
            AioaConfig.DaySurfaceSpawns settings,
            RandomSource random
    ) {
        if (!isPotentialSpawnPosition(level, spawnPosition, entityType)) {
            return false;
        }

        Entity entity = entityType.create(level);
        if (!(entity instanceof Mob mob)) {
            return false;
        }

        mob.moveTo(
                spawnPosition.getX() + 0.5D,
                spawnPosition.getY(),
                spawnPosition.getZ() + 0.5D,
                random.nextFloat() * 360.0F,
                0.0F
        );

        if (!mob.checkSpawnObstruction(level)) {
            return false;
        }

        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPosition), MobSpawnType.EVENT, null, null);
        if (!AioaZombieBehaviour.applyVariantMode(mob, settings)) {
            return false;
        }
        if (settings.preventSunlightBurn) {
            mob.addTag(AioaConstants.DAY_SPAWN_TAG);
        }

        level.addFreshEntityWithPassengers(mob);
        return true;
    }

    private static boolean isPotentialSpawnPosition(ServerLevel level, BlockPos spawnPosition, EntityType<?> entityType) {
        SpawnPlacements.Type placementType = SpawnPlacements.getPlacementType(entityType);
        if (!NaturalSpawner.isSpawnPositionOk(placementType, level, spawnPosition, entityType)) {
            return false;
        }

        BlockState state = level.getBlockState(spawnPosition);
        return NaturalSpawner.isValidEmptySpawnBlock(level, spawnPosition, state, state.getFluidState(), entityType);
    }

    private static boolean isAllowedBiome(ServerLevel level, BlockPos pos, AioaConfig.DaySurfaceSpawns settings) {
        if (settings.allowedBiomeIds.isEmpty()) {
            return true;
        }

        ResourceLocation biomeId = level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getKey(level.getBiome(pos).value());

        if (biomeId == null) {
            return false;
        }

        for (String rawBiomeId : settings.allowedBiomeIds) {
            ResourceLocation configuredBiomeId = ResourceLocation.tryParse(rawBiomeId == null ? "" : rawBiomeId.trim());
            if (configuredBiomeId == null) {
                AioaConfigManager.warnOnce(
                        "invalid-biome:" + rawBiomeId,
                        "AIOA ignored invalid biome id '" + rawBiomeId + "' in allowedBiomeIds."
                );
                continue;
            }

            if (configuredBiomeId.equals(biomeId)) {
                return true;
            }
        }

        return false;
    }

    private static int countNearbyManagedMobs(ServerLevel level, BlockPos center, int range) {
        AABB searchBox = new AABB(center).inflate(range);
        return level.getEntitiesOfClass(Mob.class, searchBox, mob -> mob.getTags().contains(AioaConstants.DAY_SPAWN_TAG)).size();
    }

    private static int randomOffset(RandomSource random, int minDistance, int maxDistance) {
        int distance = Mth.nextInt(random, minDistance, maxDistance);
        return random.nextBoolean() ? distance : -distance;
    }

    private record ResolvedSpawnEntry(AioaSpawnEntry entry, EntityType<?> entityType) {
    }
}
