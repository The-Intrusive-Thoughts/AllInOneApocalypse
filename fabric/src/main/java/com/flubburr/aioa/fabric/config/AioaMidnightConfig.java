package com.flubburr.aioa.fabric.config;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import eu.midnightdust.lib.config.MidnightConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AioaMidnightConfig extends MidnightConfig {

    private static final String HOSTILE_CATEGORY = "hostile_spawn_control";
    private static final String DAY_CATEGORY = "day_surface_spawns";

    private static boolean initialized;

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

    @Entry(category = DAY_CATEGORY)
    public static boolean remove_baby_variants = false;

    @Entry(category = DAY_CATEGORY)
    public static boolean export_mob_catalog = true;

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
        remove_baby_variants = config.daySurfaceSpawns.removeBabyVariants;
        export_mob_catalog = config.daySurfaceSpawns.exportMobCatalog;
        spawn_interval_ticks = config.daySurfaceSpawns.spawnIntervalTicks;
        spawn_attempts_per_player = config.daySurfaceSpawns.spawnAttemptsPerPlayer;
        min_spawn_distance = config.daySurfaceSpawns.minSpawnDistance;
        max_spawn_distance = config.daySurfaceSpawns.maxSpawnDistance;
        max_nearby_managed_mobs = config.daySurfaceSpawns.maxNearbyManagedMobs;
        allowed_biome_ids = safeList(config.daySurfaceSpawns.allowedBiomeIds);
        spawn_pool_entries = safeList(config.daySurfaceSpawns.spawnPoolEntries);

        syncMidnightEntryState();
    }

    public void loadValuesFromJson() {
        pullFromCommon();
    }

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
        config.daySurfaceSpawns.removeBabyVariants = remove_baby_variants;
        config.daySurfaceSpawns.exportMobCatalog = export_mob_catalog;
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

    private static Field resolveField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Exception exception) {
                AioaConstants.LOG.warn("AIOA could not access MidnightLib field '{}' on {}.", fieldName, type.getName(), exception);
                return null;
            }
        }
        return null;
    }

    private static Method resolveMethod(Class<?> type, String methodName) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            } catch (Exception exception) {
                AioaConstants.LOG.warn("AIOA could not access MidnightLib method '{}' on {}.", methodName, type.getName(), exception);
                return null;
            }
        }
        return null;
    }

    private static Field resolveEntriesField() {
        try {
            Field field = MidnightConfig.class.getDeclaredField("entries");
            field.setAccessible(true);
            return field;
        } catch (Exception exception) {
            AioaConstants.LOG.warn("AIOA could not access MidnightLib 'entries' field for state sync.", exception);
            return null;
        }
    }

    private static void syncMidnightEntryState() {
        Field entriesField = resolveEntriesField();
        if (entriesField == null) {
            return;
        }

        Object container;
        try {
            container = entriesField.get(null);
        } catch (Exception exception) {
            AioaConstants.LOG.warn("AIOA could not read MidnightLib entries for state sync.", exception);
            return;
        }

        if (container instanceof Map<?, ?> entryMap) {
            for (Map.Entry<?, ?> entry : entryMap.entrySet()) {
                syncOneEntry(entry.getValue(), String.valueOf(entry.getKey()));
            }
            return;
        }

        if (container instanceof Iterable<?> iterableEntries) {
            for (Object entryInfo : iterableEntries) {
                syncOneEntry(entryInfo, null);
            }
        }
    }

    private static void syncOneEntry(Object entryInfo, String key) {
        if (entryInfo == null) {
            return;
        }

        Class<?> entryType = entryInfo.getClass();
        Field reflectedFieldHolder = resolveField(entryType, "field");
        if (reflectedFieldHolder == null) {
            return;
        }

        Field configField;
        try {
            configField = (Field) reflectedFieldHolder.get(entryInfo);
        } catch (Exception exception) {
            return;
        }

        if (configField == null || configField.getDeclaringClass() != AioaMidnightConfig.class) {
            return;
        }

        Field valueField = resolveField(entryType, "value");
        Field tempValueField = resolveField(entryType, "tempValue");
        Field defaultValueField = resolveField(entryType, "defaultValue");
        Method toTemporaryValueMethod = resolveMethod(entryType, "toTemporaryValue");
        Method updateConditionsMethod = resolveMethod(entryType, "updateConditions");
        if (valueField == null) {
            return;
        }

        try {
            Object fieldValue = configField.get(null);
            if (fieldValue == null && defaultValueField != null) {
                fieldValue = defaultValueField.get(entryInfo);
            }
            if (fieldValue == null) {
                return;
            }

            valueField.set(entryInfo, fieldValue);

            if (tempValueField != null && toTemporaryValueMethod != null) {
                tempValueField.set(entryInfo, toTemporaryValueMethod.invoke(entryInfo));
            }

            if (updateConditionsMethod != null) {
                updateConditionsMethod.invoke(entryInfo);
            }
        } catch (Exception exception) {
            String keySuffix = key == null ? configField.getName() : key;
            AioaConfigManager.warnOnce(
                    "midnight-sync-" + keySuffix,
                    "AIOA could not sync MidnightLib state for '" + keySuffix + "'. Config UI may be partially degraded."
            );
        }
    }
}
