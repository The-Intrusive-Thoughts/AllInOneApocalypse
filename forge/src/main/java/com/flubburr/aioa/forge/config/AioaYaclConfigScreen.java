package com.flubburr.aioa.forge.config;

import com.flubburr.aioa.config.AioaConfig;
import com.flubburr.aioa.config.AioaConfigManager;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AioaYaclConfigScreen {

    private AioaYaclConfigScreen() {
    }

    public static Screen create(Screen parent) {
        AioaConfig defaults = AioaConfig.createDefault();
        AioaConfig editable = AioaConfigManager.getConfigCopy();

        return YetAnotherConfigLib.createBuilder()
                .title(tr("aioa.config.screen.title"))
                .category(buildHostileCategory(defaults, editable))
                .category(buildDaySpawnCategory(defaults, editable))
                .save(() -> AioaConfigManager.save(editable))
                .build()
                .generateScreen(parent);
    }

    private static ConfigCategory buildHostileCategory(AioaConfig defaults, AioaConfig editable) {
        return ConfigCategory.createBuilder()
                .name(tr("aioa.config.category.hostile_spawn_control"))
                .option(booleanOption(
                        "aioa.config.option.hostile.enabled",
                        "aioa.config.option.hostile.enabled.desc",
                        defaults.hostileSpawnControl.enabled,
                        () -> editable.hostileSpawnControl.enabled,
                        value -> editable.hostileSpawnControl.enabled = value
                ))
                .option(booleanOption(
                        "aioa.config.option.hostile.overworld_only",
                        "aioa.config.option.hostile.overworld_only.desc",
                        defaults.hostileSpawnControl.overworldOnly,
                        () -> editable.hostileSpawnControl.overworldOnly,
                        value -> editable.hostileSpawnControl.overworldOnly = value
                ))
                .option(booleanOption(
                        "aioa.config.option.hostile.ignore_structure_spawns",
                        "aioa.config.option.hostile.ignore_structure_spawns.desc",
                        defaults.hostileSpawnControl.ignoreStructureSpawns,
                        () -> editable.hostileSpawnControl.ignoreStructureSpawns,
                        value -> editable.hostileSpawnControl.ignoreStructureSpawns = value
                ))
                .option(booleanOption(
                        "aioa.config.option.hostile.ignore_spawner_spawns",
                        "aioa.config.option.hostile.ignore_spawner_spawns.desc",
                        defaults.hostileSpawnControl.ignoreSpawnerSpawns,
                        () -> editable.hostileSpawnControl.ignoreSpawnerSpawns,
                        value -> editable.hostileSpawnControl.ignoreSpawnerSpawns = value
                ))
                .option(booleanOption(
                        "aioa.config.option.hostile.ignore_special_spawns",
                        "aioa.config.option.hostile.ignore_special_spawns.desc",
                        defaults.hostileSpawnControl.ignoreSpecialSpawns,
                        () -> editable.hostileSpawnControl.ignoreSpecialSpawns,
                        value -> editable.hostileSpawnControl.ignoreSpecialSpawns = value
                ))
                .group(stringListGroup(
                        "aioa.config.option.hostile.whitelist_entity_ids",
                        "aioa.config.option.hostile.whitelist_entity_ids.desc",
                        defaults.hostileSpawnControl.whitelistEntityIds,
                        () -> editable.hostileSpawnControl.whitelistEntityIds,
                        value -> editable.hostileSpawnControl.whitelistEntityIds = value,
                        "minecraft:zombie"
                ))
                .build();
    }

    private static ConfigCategory buildDaySpawnCategory(AioaConfig defaults, AioaConfig editable) {
        return ConfigCategory.createBuilder()
                .name(tr("aioa.config.category.day_surface_spawns"))
                .option(booleanOption(
                        "aioa.config.option.day.enabled",
                        "aioa.config.option.day.enabled.desc",
                        defaults.daySurfaceSpawns.enabled,
                        () -> editable.daySurfaceSpawns.enabled,
                        value -> editable.daySurfaceSpawns.enabled = value
                ))
                .option(booleanOption(
                        "aioa.config.option.day.overworld_only",
                        "aioa.config.option.day.overworld_only.desc",
                        defaults.daySurfaceSpawns.overworldOnly,
                        () -> editable.daySurfaceSpawns.overworldOnly,
                        value -> editable.daySurfaceSpawns.overworldOnly = value
                ))
                .option(booleanOption(
                        "aioa.config.option.day.require_daytime",
                        "aioa.config.option.day.require_daytime.desc",
                        defaults.daySurfaceSpawns.requireDaytime,
                        () -> editable.daySurfaceSpawns.requireDaytime,
                        value -> editable.daySurfaceSpawns.requireDaytime = value
                ))
                .option(booleanOption(
                        "aioa.config.option.day.require_clear_sky",
                        "aioa.config.option.day.require_clear_sky.desc",
                        defaults.daySurfaceSpawns.requireClearSky,
                        () -> editable.daySurfaceSpawns.requireClearSky,
                        value -> editable.daySurfaceSpawns.requireClearSky = value
                ))
                .option(booleanOption(
                        "aioa.config.option.day.prevent_sunlight_burn",
                        "aioa.config.option.day.prevent_sunlight_burn.desc",
                        defaults.daySurfaceSpawns.preventSunlightBurn,
                        () -> editable.daySurfaceSpawns.preventSunlightBurn,
                        value -> editable.daySurfaceSpawns.preventSunlightBurn = value
                ))
                .option(booleanOption(
                        "aioa.config.option.day.remove_baby_variants",
                        "aioa.config.option.day.remove_baby_variants.desc",
                        defaults.daySurfaceSpawns.removeBabyVariants,
                        () -> editable.daySurfaceSpawns.removeBabyVariants,
                        value -> editable.daySurfaceSpawns.removeBabyVariants = value
                ))
                .option(booleanOption(
                        "aioa.config.option.day.export_mob_catalog",
                        "aioa.config.option.day.export_mob_catalog.desc",
                        defaults.daySurfaceSpawns.exportMobCatalog,
                        () -> editable.daySurfaceSpawns.exportMobCatalog,
                        value -> editable.daySurfaceSpawns.exportMobCatalog = value
                ))
                .option(integerOption(
                        "aioa.config.option.day.spawn_interval_ticks",
                        "aioa.config.option.day.spawn_interval_ticks.desc",
                        defaults.daySurfaceSpawns.spawnIntervalTicks,
                        () -> editable.daySurfaceSpawns.spawnIntervalTicks,
                        value -> editable.daySurfaceSpawns.spawnIntervalTicks = value,
                        20,
                        24000
                ))
                .option(integerOption(
                        "aioa.config.option.day.spawn_attempts_per_player",
                        "aioa.config.option.day.spawn_attempts_per_player.desc",
                        defaults.daySurfaceSpawns.spawnAttemptsPerPlayer,
                        () -> editable.daySurfaceSpawns.spawnAttemptsPerPlayer,
                        value -> editable.daySurfaceSpawns.spawnAttemptsPerPlayer = value,
                        1,
                        16
                ))
                .option(integerOption(
                        "aioa.config.option.day.min_spawn_distance",
                        "aioa.config.option.day.min_spawn_distance.desc",
                        defaults.daySurfaceSpawns.minSpawnDistance,
                        () -> editable.daySurfaceSpawns.minSpawnDistance,
                        value -> editable.daySurfaceSpawns.minSpawnDistance = value,
                        8,
                        128
                ))
                .option(integerOption(
                        "aioa.config.option.day.max_spawn_distance",
                        "aioa.config.option.day.max_spawn_distance.desc",
                        defaults.daySurfaceSpawns.maxSpawnDistance,
                        () -> editable.daySurfaceSpawns.maxSpawnDistance,
                        value -> editable.daySurfaceSpawns.maxSpawnDistance = value,
                        16,
                        256
                ))
                .option(integerOption(
                        "aioa.config.option.day.max_nearby_managed_mobs",
                        "aioa.config.option.day.max_nearby_managed_mobs.desc",
                        defaults.daySurfaceSpawns.maxNearbyManagedMobs,
                        () -> editable.daySurfaceSpawns.maxNearbyManagedMobs,
                        value -> editable.daySurfaceSpawns.maxNearbyManagedMobs = value,
                        1,
                        256
                ))
                .group(stringListGroup(
                        "aioa.config.option.day.allowed_biome_ids",
                        "aioa.config.option.day.allowed_biome_ids.desc",
                        defaults.daySurfaceSpawns.allowedBiomeIds,
                        () -> editable.daySurfaceSpawns.allowedBiomeIds,
                        value -> editable.daySurfaceSpawns.allowedBiomeIds = value,
                        "minecraft:plains"
                ))
                .group(stringListGroup(
                        "aioa.config.option.day.spawn_pool_entries",
                        "aioa.config.option.day.spawn_pool_entries.desc",
                        defaults.daySurfaceSpawns.spawnPoolEntries,
                        () -> editable.daySurfaceSpawns.spawnPoolEntries,
                        value -> editable.daySurfaceSpawns.spawnPoolEntries = value,
                        "minecraft:zombie;enabled=true;weight=10;chance=1.0;min=1;max=3"
                ))
                .build();
    }

    private static Option<Boolean> booleanOption(
            String nameKey,
            String descriptionKey,
            boolean defaultValue,
            Supplier<Boolean> getter,
            Consumer<Boolean> setter
    ) {
        return Option.<Boolean>createBuilder()
                .name(tr(nameKey))
                .description(OptionDescription.of(tr(descriptionKey)))
                .binding(defaultValue, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<Integer> integerOption(
            String nameKey,
            String descriptionKey,
            int defaultValue,
            Supplier<Integer> getter,
            Consumer<Integer> setter,
            int min,
            int max
    ) {
        return Option.<Integer>createBuilder()
                .name(tr(nameKey))
                .description(OptionDescription.of(tr(descriptionKey)))
                .binding(defaultValue, getter, setter)
                .controller(option -> IntegerFieldControllerBuilder.create(option).range(min, max))
                .build();
    }

    private static ListOption<String> stringListGroup(
            String nameKey,
            String descriptionKey,
            List<String> defaultValue,
            Supplier<List<String>> getter,
            Consumer<List<String>> setter,
            String initialValue
    ) {
        return ListOption.<String>createBuilder()
                .name(tr(nameKey))
                .description(OptionDescription.of(tr(descriptionKey)))
                .binding(new ArrayList<>(defaultValue), () -> new ArrayList<>(getter.get()), value -> setter.accept(new ArrayList<>(value)))
                .controller(StringControllerBuilder::create)
                .initial(initialValue)
                .collapsed(false)
                .build();
    }

    private static Component tr(String key) {
        return Component.translatable(key);
    }
}
