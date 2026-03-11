package com.flubburr.aioa.fabric.config;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import eu.midnightdust.lib.config.EntryInfo;
import eu.midnightdust.lib.config.MidnightConfig;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class AioaMidnightConfig extends MidnightConfig {

    private static final String HOSTILE_CATEGORY = "hostile_spawn_control";
    private static final String DAY_CATEGORY = "day_surface_spawns";
    private static final String ENTRY_KEY_PREFIX = AioaConstants.MOD_ID + ":";

    private static boolean initialized;
    private static final Field ENTRY_VALUE_FIELD;
    private static final Field ENTRY_TEMP_VALUE_FIELD;
    private static final Field ENTRY_DEFAULT_VALUE_FIELD;

    static {
        ENTRY_VALUE_FIELD = resolveEntryInfoField("value");
        ENTRY_TEMP_VALUE_FIELD = resolveEntryInfoField("tempValue");
        ENTRY_DEFAULT_VALUE_FIELD = resolveEntryInfoField("defaultValue");
    }

    @Comment(category = HOSTILE_CATEGORY)
    public static String hostile_spawn_settings_comment = "";

    @Entry(category = HOSTILE_CATEGORY)
    public static boolean enable_hostile_spawn_nullification = true;

    @Entry(category = HOSTILE_CATEGORY)
    public static boolean hostile_nullification_overworld_only = true;

    @Entry(category = HOSTILE_CATEGORY)
    public static boolean ignore_structure_spawns = true;

    @Entry(category = HOSTILE_CATEGORY)
    public static boolean ignore_spawner_spawns = true;

    @Entry(category = HOSTILE_CATEGORY)
    public static boolean ignore_special_spawns = true;

    @Entry(category = HOSTILE_CATEGORY)
    public static List<String> whitelist_entity_ids = new ArrayList<>();

    @Comment(category = DAY_CATEGORY)
    public static String day_spawn_settings_comment = "";

    @Entry(category = DAY_CATEGORY)
    public static boolean enable_day_surface_spawns = true;

    @Entry(category = DAY_CATEGORY)
    public static boolean day_spawns_overworld_only = true;

    @Entry(category = DAY_CATEGORY)
    public static boolean require_daytime = true;

    @Entry(category = DAY_CATEGORY)
    public static boolean require_clear_sky = true;

    @Entry(category = DAY_CATEGORY)
    public static boolean prevent_sunlight_burn = true;

    @Entry(category = DAY_CATEGORY, min = 20, max = 24000)
    public static int spawn_interval_ticks = 200;

    @Entry(category = DAY_CATEGORY, min = 1, max = 16)
    public static int spawn_attempts_per_player = 2;

    @Entry(category = DAY_CATEGORY, min = 8, max = 128)
    public static int min_spawn_distance = 24;

    @Entry(category = DAY_CATEGORY, min = 16, max = 256)
    public static int max_spawn_distance = 56;

    @Entry(category = DAY_CATEGORY, min = 1, max = 256)
    public static int max_nearby_managed_mobs = 20;

    @Entry(category = DAY_CATEGORY)
    public static List<String> allowed_biome_ids = new ArrayList<>();

    @Entry(category = DAY_CATEGORY)
    public static List<String> spawn_pool_entries = new ArrayList<>();

    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        MidnightConfig.init(AioaConstants.MOD_ID, AioaMidnightConfig.class);
        pullFromCommon();
    }

    public static void pullFromCommon() {
        AioaConfig config = AioaConfigManager.getConfigCopy();
        enable_hostile_spawn_nullification = config.hostileSpawnControl.enabled;
        hostile_nullification_overworld_only = config.hostileSpawnControl.overworldOnly;
        ignore_structure_spawns = config.hostileSpawnControl.ignoreStructureSpawns;
        ignore_spawner_spawns = config.hostileSpawnControl.ignoreSpawnerSpawns;
        ignore_special_spawns = config.hostileSpawnControl.ignoreSpecialSpawns;
        whitelist_entity_ids = safeList(config.hostileSpawnControl.whitelistEntityIds);

        enable_day_surface_spawns = config.daySurfaceSpawns.enabled;
        day_spawns_overworld_only = config.daySurfaceSpawns.overworldOnly;
        require_daytime = config.daySurfaceSpawns.requireDaytime;
        require_clear_sky = config.daySurfaceSpawns.requireClearSky;
        prevent_sunlight_burn = config.daySurfaceSpawns.preventSunlightBurn;
        spawn_interval_ticks = config.daySurfaceSpawns.spawnIntervalTicks;
        spawn_attempts_per_player = config.daySurfaceSpawns.spawnAttemptsPerPlayer;
        min_spawn_distance = config.daySurfaceSpawns.minSpawnDistance;
        max_spawn_distance = config.daySurfaceSpawns.maxSpawnDistance;
        max_nearby_managed_mobs = config.daySurfaceSpawns.maxNearbyManagedMobs;
        allowed_biome_ids = safeList(config.daySurfaceSpawns.allowedBiomeIds);
        spawn_pool_entries = safeList(config.daySurfaceSpawns.spawnPoolEntries);

        syncMidnightEntryState();
    }

    @Override
    public void loadValuesFromJson() {
        pullFromCommon();
    }

    @Override
    public void writeChanges() {
        AioaConfig config = AioaConfigManager.getConfigCopy();
        config.hostileSpawnControl.enabled = enable_hostile_spawn_nullification;
        config.hostileSpawnControl.overworldOnly = hostile_nullification_overworld_only;
        config.hostileSpawnControl.ignoreStructureSpawns = ignore_structure_spawns;
        config.hostileSpawnControl.ignoreSpawnerSpawns = ignore_spawner_spawns;
        config.hostileSpawnControl.ignoreSpecialSpawns = ignore_special_spawns;
        config.hostileSpawnControl.whitelistEntityIds = safeList(whitelist_entity_ids);

        config.daySurfaceSpawns.enabled = enable_day_surface_spawns;
        config.daySurfaceSpawns.overworldOnly = day_spawns_overworld_only;
        config.daySurfaceSpawns.requireDaytime = require_daytime;
        config.daySurfaceSpawns.requireClearSky = require_clear_sky;
        config.daySurfaceSpawns.preventSunlightBurn = prevent_sunlight_burn;
        config.daySurfaceSpawns.spawnIntervalTicks = spawn_interval_ticks;
        config.daySurfaceSpawns.spawnAttemptsPerPlayer = spawn_attempts_per_player;
        config.daySurfaceSpawns.minSpawnDistance = min_spawn_distance;
        config.daySurfaceSpawns.maxSpawnDistance = max_spawn_distance;
        config.daySurfaceSpawns.maxNearbyManagedMobs = max_nearby_managed_mobs;
        config.daySurfaceSpawns.allowedBiomeIds = safeList(allowed_biome_ids);
        config.daySurfaceSpawns.spawnPoolEntries = safeList(spawn_pool_entries);

        AioaConfigManager.save(config);
        pullFromCommon();
    }

    private static List<String> safeList(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private static Field resolveEntryInfoField(String fieldName) {
        try {
            Field field = EntryInfo.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Exception exception) {
            AioaConstants.LOG.warn("AIOA could not access MidnightLib internals for '{}' state sync.", fieldName, exception);
            return null;
        }
    }

    private static void syncMidnightEntryState() {
        if (ENTRY_VALUE_FIELD == null || ENTRY_TEMP_VALUE_FIELD == null || ENTRY_DEFAULT_VALUE_FIELD == null) {
            return;
        }

        entries.forEach((key, entryInfo) -> {
            if (!key.startsWith(ENTRY_KEY_PREFIX) || entryInfo.field == null || entryInfo.entry == null) {
                return;
            }

            try {
                Object fieldValue = entryInfo.field.get(null);
                if (fieldValue == null) {
                    fieldValue = ENTRY_DEFAULT_VALUE_FIELD.get(entryInfo);
                }
                if (fieldValue == null) {
                    return;
                }

                ENTRY_VALUE_FIELD.set(entryInfo, fieldValue);
                ENTRY_TEMP_VALUE_FIELD.set(entryInfo, entryInfo.toTemporaryValue());
                entryInfo.updateConditions();
            } catch (Exception exception) {
                AioaConfigManager.warnOnce(
                        "midnight-sync-" + key,
                        "AIOA could not sync MidnightLib state for '" + key + "'. Config UI may be partially degraded."
                );
            }
        });
    }
}
