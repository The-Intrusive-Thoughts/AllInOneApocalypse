package com.flubburr.aioa.config;

import java.util.ArrayList;
import java.util.List;

public final class AioaConfig {

    public static final int CURRENT_SCHEMA_VERSION = 3;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public HostileSpawnControl hostileSpawnControl = new HostileSpawnControl();
    public DaySurfaceSpawns daySurfaceSpawns = new DaySurfaceSpawns();

    public static AioaConfig createDefault() {
        return new AioaConfig().sanitize();
    }

    public AioaConfig copy() {
        AioaConfig copy = new AioaConfig();
        copy.schemaVersion = this.schemaVersion;
        copy.hostileSpawnControl = this.hostileSpawnControl.copy();
        copy.daySurfaceSpawns = this.daySurfaceSpawns.copy();
        return copy;
    }

    public AioaConfig sanitize() {
        this.schemaVersion = CURRENT_SCHEMA_VERSION;
        if (this.hostileSpawnControl == null) {
            this.hostileSpawnControl = new HostileSpawnControl();
        }
        if (this.daySurfaceSpawns == null) {
            this.daySurfaceSpawns = new DaySurfaceSpawns();
        }

        this.hostileSpawnControl.sanitize();
        this.daySurfaceSpawns.sanitize();
        return this;
    }

    public AioaConfig applyPreset(Preset preset) {
        HostileSpawnControl hostile = this.hostileSpawnControl;
        DaySurfaceSpawns day = this.daySurfaceSpawns;
        switch (preset) {
            case BALANCED -> {
                hostile.enabled = true;
                day.enabled = true;
                day.spawnIntervalTicks = 200;
                day.spawnAttemptsPerPlayer = 2;
                day.maxNearbyManagedMobs = 20;
                day.refinedZombieAi = true;
                day.coordinatedHordeAi = true;
            }
            case CINEMATIC -> {
                hostile.enabled = false;
                day.enabled = false;
                day.preventSunlightBurn = true;
                day.refinedZombieAi = false;
                day.coordinatedHordeAi = false;
            }
            case HORDE -> {
                hostile.enabled = true;
                day.enabled = true;
                day.spawnIntervalTicks = 80;
                day.spawnAttemptsPerPlayer = 5;
                day.maxNearbyManagedMobs = 60;
                day.refinedZombieAi = true;
                day.coordinatedHordeAi = true;
            }
        }
        return this.sanitize();
    }

    public enum Preset {
        BALANCED,
        CINEMATIC,
        HORDE
    }

    public static final class HostileSpawnControl {
        public boolean enabled = true;
        public boolean overworldOnly = true;
        public boolean ignoreStructureSpawns = true;
        public boolean ignoreSpawnerSpawns = true;
        public boolean ignoreSpecialSpawns = true;
        public List<String> whitelistEntityIds = new ArrayList<>();

        private HostileSpawnControl copy() {
            HostileSpawnControl copy = new HostileSpawnControl();
            copy.enabled = this.enabled;
            copy.overworldOnly = this.overworldOnly;
            copy.ignoreStructureSpawns = this.ignoreStructureSpawns;
            copy.ignoreSpawnerSpawns = this.ignoreSpawnerSpawns;
            copy.ignoreSpecialSpawns = this.ignoreSpecialSpawns;
            copy.whitelistEntityIds = new ArrayList<>(this.whitelistEntityIds);
            return copy;
        }

        private void sanitize() {
            if (this.whitelistEntityIds == null) {
                this.whitelistEntityIds = new ArrayList<>();
            }
        }
    }

    public static final class DaySurfaceSpawns {
        private static final List<String> DEFAULT_APOCALYPSE_MOB_IDS = List.of(
                "minecraft:zombie",
                "minecraft:husk",
                "minecraft:drowned",
                "minecraft:zombie_villager",
                "minecraft:zombified_piglin",
                "minecraft:giant"
        );

        public boolean enabled = true;
        public boolean overworldOnly = true;
        public boolean requireDaytime = true;
        public boolean requireClearSky = true;
        public boolean preventSunlightBurn = true;
        @Deprecated
        public boolean removeBabyVariants = false;
        public ZombieVariantMode zombieVariantMode = ZombieVariantMode.REGULAR_AND_BABY;
        public ZombieTargetMode zombieTargetMode = ZombieTargetMode.VANILLA;
        public boolean zombiesCanClimbWalls = true;
        public boolean refinedZombieAi = true;
        public boolean refinedPathfindingOpensDoors = true;
        public boolean coordinatedHordeAi = true;
        public boolean exportMobCatalog = true;
        public int spawnIntervalTicks = 200;
        public int spawnAttemptsPerPlayer = 2;
        public int minSpawnDistance = 24;
        public int maxSpawnDistance = 56;
        public int maxNearbyManagedMobs = 20;
        public List<String> allowedBiomeIds = new ArrayList<>();
        public List<String> refinedAiEntityIds = new ArrayList<>(DEFAULT_APOCALYPSE_MOB_IDS);
        public List<String> wallClimbingEntityIds = new ArrayList<>(DEFAULT_APOCALYPSE_MOB_IDS);
        public List<String> playerOnlyTargetEntityIds = new ArrayList<>();
        public List<String> animalTargetEntityIds = new ArrayList<>();
        public List<String> otherMobTargetEntityIds = new ArrayList<>();
        public List<String> everythingTargetEntityIds = new ArrayList<>();
        public List<String> spawnPoolEntries = new ArrayList<>(List.of(
                "minecraft:zombie;enabled=true;weight=12;chance=1.0;min=1;max=3",
                "minecraft:husk;enabled=true;weight=4;chance=0.45;min=1;max=2",
                "minecraft:drowned;enabled=false;weight=2;chance=0.2;min=1;max=2"
        ));

        private DaySurfaceSpawns copy() {
            DaySurfaceSpawns copy = new DaySurfaceSpawns();
            copy.enabled = this.enabled;
            copy.overworldOnly = this.overworldOnly;
            copy.requireDaytime = this.requireDaytime;
            copy.requireClearSky = this.requireClearSky;
            copy.preventSunlightBurn = this.preventSunlightBurn;
            copy.removeBabyVariants = this.removeBabyVariants;
            copy.zombieVariantMode = this.zombieVariantMode;
            copy.zombieTargetMode = this.zombieTargetMode;
            copy.zombiesCanClimbWalls = this.zombiesCanClimbWalls;
            copy.refinedZombieAi = this.refinedZombieAi;
            copy.refinedPathfindingOpensDoors = this.refinedPathfindingOpensDoors;
            copy.coordinatedHordeAi = this.coordinatedHordeAi;
            copy.exportMobCatalog = this.exportMobCatalog;
            copy.spawnIntervalTicks = this.spawnIntervalTicks;
            copy.spawnAttemptsPerPlayer = this.spawnAttemptsPerPlayer;
            copy.minSpawnDistance = this.minSpawnDistance;
            copy.maxSpawnDistance = this.maxSpawnDistance;
            copy.maxNearbyManagedMobs = this.maxNearbyManagedMobs;
            copy.allowedBiomeIds = new ArrayList<>(this.allowedBiomeIds);
            copy.refinedAiEntityIds = new ArrayList<>(this.refinedAiEntityIds);
            copy.wallClimbingEntityIds = new ArrayList<>(this.wallClimbingEntityIds);
            copy.playerOnlyTargetEntityIds = new ArrayList<>(this.playerOnlyTargetEntityIds);
            copy.animalTargetEntityIds = new ArrayList<>(this.animalTargetEntityIds);
            copy.otherMobTargetEntityIds = new ArrayList<>(this.otherMobTargetEntityIds);
            copy.everythingTargetEntityIds = new ArrayList<>(this.everythingTargetEntityIds);
            copy.spawnPoolEntries = new ArrayList<>(this.spawnPoolEntries);
            return copy;
        }

        private void sanitize() {
            this.spawnIntervalTicks = Math.max(20, this.spawnIntervalTicks);
            this.spawnAttemptsPerPlayer = Math.max(1, Math.min(16, this.spawnAttemptsPerPlayer));
            this.minSpawnDistance = Math.max(8, this.minSpawnDistance);
            this.maxSpawnDistance = Math.max(this.minSpawnDistance + 8, this.maxSpawnDistance);
            this.maxNearbyManagedMobs = Math.max(1, this.maxNearbyManagedMobs);
            if (this.zombieVariantMode == null) {
                this.zombieVariantMode = this.removeBabyVariants ? ZombieVariantMode.REGULAR_ONLY : ZombieVariantMode.REGULAR_AND_BABY;
            }
            if (this.zombieTargetMode == null) {
                this.zombieTargetMode = ZombieTargetMode.VANILLA;
            }

            if (this.allowedBiomeIds == null) {
                this.allowedBiomeIds = new ArrayList<>();
            }
            if (this.refinedAiEntityIds == null || this.refinedAiEntityIds.isEmpty()) {
                this.refinedAiEntityIds = new ArrayList<>(DEFAULT_APOCALYPSE_MOB_IDS);
            }
            if (this.wallClimbingEntityIds == null || this.wallClimbingEntityIds.isEmpty()) {
                this.wallClimbingEntityIds = new ArrayList<>(DEFAULT_APOCALYPSE_MOB_IDS);
            }
            if (this.playerOnlyTargetEntityIds == null) {
                this.playerOnlyTargetEntityIds = new ArrayList<>();
            }
            if (this.animalTargetEntityIds == null) {
                this.animalTargetEntityIds = new ArrayList<>();
            }
            if (this.otherMobTargetEntityIds == null) {
                this.otherMobTargetEntityIds = new ArrayList<>();
            }
            if (this.everythingTargetEntityIds == null) {
                this.everythingTargetEntityIds = new ArrayList<>();
            }
            if (this.spawnPoolEntries == null || this.spawnPoolEntries.isEmpty()) {
                this.spawnPoolEntries = new ArrayList<>(List.of(
                        "minecraft:zombie;enabled=true;weight=12;chance=1.0;min=1;max=3",
                        "minecraft:husk;enabled=true;weight=4;chance=0.45;min=1;max=2"
                ));
            }
        }
    }

    public enum ZombieVariantMode {
        REGULAR_ONLY,
        REGULAR_AND_BABY,
        BABY_ONLY
    }

    public enum ZombieTargetMode {
        VANILLA,
        PLAYERS_ONLY,
        ANIMALS_ONLY,
        OTHER_MOBS_ONLY,
        EVERYTHING
    }
}
