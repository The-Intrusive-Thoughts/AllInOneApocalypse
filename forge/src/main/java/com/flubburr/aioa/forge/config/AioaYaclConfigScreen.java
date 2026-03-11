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
                .title(Component.literal("AIOA Configuration"))
                .category(buildHostileCategory(defaults, editable))
                .category(buildDaySpawnCategory(defaults, editable))
                .save(() -> AioaConfigManager.save(editable))
                .build()
                .generateScreen(parent);
    }

    private static ConfigCategory buildHostileCategory(AioaConfig defaults, AioaConfig editable) {
        return ConfigCategory.createBuilder()
                .name(Component.literal("Hostile Spawn Control"))
                .option(booleanOption(
                        "Enable hostile spawn nullification",
                        "Cancel hostile natural-style spawns unless they are explicitly allow-listed.",
                        defaults.hostileSpawnControl.enabled,
                        () -> editable.hostileSpawnControl.enabled,
                        value -> editable.hostileSpawnControl.enabled = value
                ))
                .option(booleanOption(
                        "Overworld only",
                        "Leave the Nether, End, and any other dimensions alone.",
                        defaults.hostileSpawnControl.overworldOnly,
                        () -> editable.hostileSpawnControl.overworldOnly,
                        value -> editable.hostileSpawnControl.overworldOnly = value
                ))
                .option(booleanOption(
                        "Ignore structure spawns",
                        "Do not suppress hostiles spawned by structure rules when this is enabled.",
                        defaults.hostileSpawnControl.ignoreStructureSpawns,
                        () -> editable.hostileSpawnControl.ignoreStructureSpawns,
                        value -> editable.hostileSpawnControl.ignoreStructureSpawns = value
                ))
                .option(booleanOption(
                        "Ignore spawner spawns",
                        "Leaves mob spawners and similar block-driven spawn sources untouched.",
                        defaults.hostileSpawnControl.ignoreSpawnerSpawns,
                        () -> editable.hostileSpawnControl.ignoreSpawnerSpawns,
                        value -> editable.hostileSpawnControl.ignoreSpawnerSpawns = value
                ))
                .option(booleanOption(
                        "Ignore special spawns",
                        "Skips patrols, events, reinforcements, spawn eggs, commands, and similar special cases.",
                        defaults.hostileSpawnControl.ignoreSpecialSpawns,
                        () -> editable.hostileSpawnControl.ignoreSpecialSpawns,
                        value -> editable.hostileSpawnControl.ignoreSpecialSpawns = value
                ))
                .group(stringListGroup(
                        "Allowed hostile entity IDs",
                        "Each entry is a registry id such as minecraft:zombie or modid:infected.",
                        defaults.hostileSpawnControl.whitelistEntityIds,
                        () -> editable.hostileSpawnControl.whitelistEntityIds,
                        value -> editable.hostileSpawnControl.whitelistEntityIds = value,
                        "minecraft:zombie"
                ))
                .build();
    }

    private static ConfigCategory buildDaySpawnCategory(AioaConfig defaults, AioaConfig editable) {
        return ConfigCategory.createBuilder()
                .name(Component.literal("Day Surface Spawns"))
                .option(booleanOption(
                        "Enable day surface spawns",
                        "Allows configured apocalypse mobs to spawn on the surface during the day.",
                        defaults.daySurfaceSpawns.enabled,
                        () -> editable.daySurfaceSpawns.enabled,
                        value -> editable.daySurfaceSpawns.enabled = value
                ))
                .option(booleanOption(
                        "Overworld only",
                        "Restricts the day spawn system to the Overworld by default.",
                        defaults.daySurfaceSpawns.overworldOnly,
                        () -> editable.daySurfaceSpawns.overworldOnly,
                        value -> editable.daySurfaceSpawns.overworldOnly = value
                ))
                .option(booleanOption(
                        "Require daytime",
                        "Only runs the apocalypse surface spawner while the world is in daytime.",
                        defaults.daySurfaceSpawns.requireDaytime,
                        () -> editable.daySurfaceSpawns.requireDaytime,
                        value -> editable.daySurfaceSpawns.requireDaytime = value
                ))
                .option(booleanOption(
                        "Require clear sky",
                        "Makes AIOA only place day-spawn mobs in spots with direct sky access.",
                        defaults.daySurfaceSpawns.requireClearSky,
                        () -> editable.daySurfaceSpawns.requireClearSky,
                        value -> editable.daySurfaceSpawns.requireClearSky = value
                ))
                .option(booleanOption(
                        "Prevent sunlight burn",
                        "Adds sun-burn protection to mobs spawned by the AIOA day system.",
                        defaults.daySurfaceSpawns.preventSunlightBurn,
                        () -> editable.daySurfaceSpawns.preventSunlightBurn,
                        value -> editable.daySurfaceSpawns.preventSunlightBurn = value
                ))
                .option(integerOption(
                        "Spawn interval (ticks)",
                        "How often the day-surface spawner runs. 200 ticks is 10 seconds.",
                        defaults.daySurfaceSpawns.spawnIntervalTicks,
                        () -> editable.daySurfaceSpawns.spawnIntervalTicks,
                        value -> editable.daySurfaceSpawns.spawnIntervalTicks = value,
                        20,
                        24000
                ))
                .option(integerOption(
                        "Spawn attempts per player",
                        "How many weighted spawn attempts AIOA makes around each valid player per cycle.",
                        defaults.daySurfaceSpawns.spawnAttemptsPerPlayer,
                        () -> editable.daySurfaceSpawns.spawnAttemptsPerPlayer,
                        value -> editable.daySurfaceSpawns.spawnAttemptsPerPlayer = value,
                        1,
                        16
                ))
                .option(integerOption(
                        "Minimum spawn distance",
                        "Prevents AIOA from dropping mobs too close to players.",
                        defaults.daySurfaceSpawns.minSpawnDistance,
                        () -> editable.daySurfaceSpawns.minSpawnDistance,
                        value -> editable.daySurfaceSpawns.minSpawnDistance = value,
                        8,
                        128
                ))
                .option(integerOption(
                        "Maximum spawn distance",
                        "Sets the outer ring radius for day-spawn attempts around players.",
                        defaults.daySurfaceSpawns.maxSpawnDistance,
                        () -> editable.daySurfaceSpawns.maxSpawnDistance,
                        value -> editable.daySurfaceSpawns.maxSpawnDistance = value,
                        16,
                        256
                ))
                .option(integerOption(
                        "Maximum nearby managed mobs",
                        "Caps the number of AIOA-managed day spawns near each player.",
                        defaults.daySurfaceSpawns.maxNearbyManagedMobs,
                        () -> editable.daySurfaceSpawns.maxNearbyManagedMobs,
                        value -> editable.daySurfaceSpawns.maxNearbyManagedMobs = value,
                        1,
                        256
                ))
                .group(stringListGroup(
                        "Allowed biome IDs",
                        "Leave this empty to allow all biomes. Use exact registry names such as minecraft:plains.",
                        defaults.daySurfaceSpawns.allowedBiomeIds,
                        () -> editable.daySurfaceSpawns.allowedBiomeIds,
                        value -> editable.daySurfaceSpawns.allowedBiomeIds = value,
                        "minecraft:plains"
                ))
                .group(stringListGroup(
                        "Day spawn pool entries",
                        "Format: entity_id;enabled=true;weight=10;chance=1.0;min=1;max=3",
                        defaults.daySurfaceSpawns.spawnPoolEntries,
                        () -> editable.daySurfaceSpawns.spawnPoolEntries,
                        value -> editable.daySurfaceSpawns.spawnPoolEntries = value,
                        "minecraft:zombie;enabled=true;weight=10;chance=1.0;min=1;max=3"
                ))
                .build();
    }

    private static Option<Boolean> booleanOption(
            String name,
            String description,
            boolean defaultValue,
            Supplier<Boolean> getter,
            Consumer<Boolean> setter
    ) {
        return Option.<Boolean>createBuilder()
                .name(Component.literal(name))
                .description(OptionDescription.of(Component.literal(description)))
                .binding(defaultValue, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<Integer> integerOption(
            String name,
            String description,
            int defaultValue,
            Supplier<Integer> getter,
            Consumer<Integer> setter,
            int min,
            int max
    ) {
        return Option.<Integer>createBuilder()
                .name(Component.literal(name))
                .description(OptionDescription.of(Component.literal(description)))
                .binding(defaultValue, getter, setter)
                .controller(option -> IntegerFieldControllerBuilder.create(option).range(min, max))
                .build();
    }

    private static ListOption<String> stringListGroup(
            String name,
            String description,
            List<String> defaultValue,
            Supplier<List<String>> getter,
            Consumer<List<String>> setter,
            String initialValue
    ) {
        return ListOption.<String>createBuilder()
                .name(Component.literal(name))
                .description(OptionDescription.of(Component.literal(description)))
                .binding(new ArrayList<>(defaultValue), () -> new ArrayList<>(getter.get()), value -> setter.accept(new ArrayList<>(value)))
                .controller(StringControllerBuilder::create)
                .initial(initialValue)
                .collapsed(false)
                .build();
    }
}
