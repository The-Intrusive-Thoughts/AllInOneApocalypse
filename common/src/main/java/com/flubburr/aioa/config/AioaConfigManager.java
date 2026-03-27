package com.flubburr.aioa.config;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.compat.AioaEntityHelper;
import com.flubburr.aioa.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class AioaConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final Set<String> WARNED_KEYS = ConcurrentHashMap.newKeySet();

    private static volatile boolean bootstrapped;
    private static volatile AioaConfig currentConfig = AioaConfig.createDefault();
    private static volatile Path configPath;
    private static volatile long lastModified = Long.MIN_VALUE;
    private static volatile long nextFilesystemCheckAt = 0L;
    private static volatile Set<Identifier> burnSafeDaySpawnEntityIds = Set.of();
    private static volatile boolean mobCatalogWritten;

    private AioaConfigManager() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        synchronized (AioaConfigManager.class) {
            if (bootstrapped) {
                return;
            }

            configPath = Services.PLATFORM.getConfigDirectory().resolve(AioaConstants.MOD_ID + ".json");
            loadOrCreateConfig();
            bootstrapped = true;
        }
    }

    public static void refreshIfChanged() {
        bootstrap();

        long now = System.currentTimeMillis();
        if (now < nextFilesystemCheckAt) {
            return;
        }

        nextFilesystemCheckAt = now + 2_000L;
        long currentLastModified = getLastModified(configPath);
        if (currentLastModified > lastModified) {
            loadOrCreateConfig();
        }
    }

    public static AioaConfig getConfig() {
        bootstrap();
        return currentConfig;
    }

    public static AioaConfig getConfigCopy() {
        return getConfig().copy();
    }

    public static void save(AioaConfig updatedConfig) {
        bootstrap();

        AioaConfig sanitized = updatedConfig.copy().sanitize();
        writeConfig(sanitized);
        currentConfig = sanitized;
        refreshDerivedCaches(sanitized);
        lastModified = getLastModified(configPath);
    }

    public static boolean isBurnSafeDaySpawnEntity(EntityType<?> entityType) {
        bootstrap();
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return id != null && burnSafeDaySpawnEntityIds.contains(id);
    }

    public static void ensureMobCatalogWritten(ServerLevel level) {
        bootstrap();
        AioaConfig config = getConfig();
        if (!config.daySurfaceSpawns.exportMobCatalog || mobCatalogWritten) {
            return;
        }

        synchronized (AioaConfigManager.class) {
            if (!config.daySurfaceSpawns.exportMobCatalog || mobCatalogWritten) {
                return;
            }

            Path catalogPath = configPath.getParent().resolve(AioaConstants.MOD_ID + "-mob-catalog.txt");
            List<String> lines = new ArrayList<>();
            lines.add("# AIOA Mob Catalog");
            lines.add("# Generated at: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            lines.add("# This list includes all registered living mobs AIOA can safely configure.");
            lines.add("# Copy the template part into daySurfaceSpawns.spawnPoolEntries in aioa.json.");
            lines.add("# Format: entity_id | category=<mob_category> | hostile=<true/false> | template=<spawn_entry_template>");
            lines.add("");

            AioaEntityHelper.enumerateConfigurableMobIds(level).forEach(entityId -> {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(entityId);
                boolean hostile = AioaEntityHelper.isHostileMob(type, level);
                String template = entityId + ";enabled=true;rarity=common;chance=1.0;min=1;max=3";
                lines.add(entityId + " | category=" + type.getCategory().getName() + " | hostile=" + hostile + " | template=" + template);
            });

            try {
                Files.createDirectories(catalogPath.getParent());
                Files.write(catalogPath, lines, StandardCharsets.UTF_8);
                mobCatalogWritten = true;
                AioaConstants.LOG.info("AIOA wrote mob catalog to {}", catalogPath);
            } catch (IOException exception) {
                warnOnce("mob-catalog-write-failed", "AIOA could not write its generated mob catalog file.");
                AioaConstants.LOG.debug("Failed to write AIOA mob catalog", exception);
            }
        }
    }

    public static void warnOnce(String key, String message) {
        if (WARNED_KEYS.add(key)) {
            AioaConstants.LOG.warn(message);
        }
    }

    private static void loadOrCreateConfig() {
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException exception) {
            throw new RuntimeException("Failed to create AIOA config directory", exception);
        }

        if (!Files.exists(configPath)) {
            AioaConfig defaults = AioaConfig.createDefault();
            writeConfig(defaults);
            currentConfig = defaults;
            refreshDerivedCaches(defaults);
            lastModified = getLastModified(configPath);
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            AioaConfig loaded = GSON.fromJson(reader, AioaConfig.class);
            if (loaded == null) {
                loaded = AioaConfig.createDefault();
            }

            currentConfig = loaded.sanitize();
            refreshDerivedCaches(currentConfig);
            lastModified = getLastModified(configPath);
        } catch (Exception exception) {
            AioaConstants.LOG.warn("AIOA could not read its config file. Rebuilding a clean default copy.", exception);
            AioaConfig defaults = AioaConfig.createDefault();
            writeConfig(defaults);
            currentConfig = defaults;
            refreshDerivedCaches(defaults);
            lastModified = getLastModified(configPath);
        }
    }

    private static void refreshDerivedCaches(AioaConfig config) {
        burnSafeDaySpawnEntityIds = config.daySurfaceSpawns.spawnPoolEntries.stream()
                .map(rawEntry -> AioaSpawnEntry.parse(rawEntry, warning -> warnOnce("spawn-entry-cache:" + rawEntry, warning)))
                .flatMap(Optional::stream)
                .filter(AioaSpawnEntry::enabled)
                .map(AioaSpawnEntry::entityId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static void writeConfig(AioaConfig config) {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to write AIOA config", exception);
        }
    }

    private static long getLastModified(Path path) {
        try {
            return Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : Long.MIN_VALUE;
        } catch (IOException exception) {
            return Long.MIN_VALUE;
        }
    }
}
