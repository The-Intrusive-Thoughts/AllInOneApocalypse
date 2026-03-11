# AIOA

`AIOA` is a clean Minecraft 1.20.1 multi-loader mod scaffold focused entirely on spawning control.

It is structured so the real gameplay logic lives in `common`, while the loader modules only handle bootstrap, config UI integration, and platform-specific glue.

## Module Layout

- `common`
  - Shared AIOA constants, config model, config file manager, spawn entry parser, entity filtering, hostile spawn suppression, day surface spawning, and shared mixins.
- `fabric`
  - Fabric entrypoints, Fabric platform service, MidnightLib config bridge, and optional Mod Menu integration.
- `forge`
  - Forge entrypoint, Forge platform service, YACL config screen bridge, and fallback config screen.

## Shared Gameplay Logic

The common module is the source of truth for AIOA gameplay behavior:

- Hostile spawn nullification is driven by `common/src/main/java/com/flubburr/aioa/spawn/HostileSpawnFilter.java`.
- Daytime surface apocalypse spawning is driven by `common/src/main/java/com/flubburr/aioa/spawn/ApocalypseSpawnManager.java`.
- Registry lookup, mob validation, and broad modded-entity compatibility live in `common/src/main/java/com/flubburr/aioa/compat/AioaEntityHelper.java`.
- The shared JSON config model and loader-neutral persistence live in `common/src/main/java/com/flubburr/aioa/config`.

The config file is written to:

- Fabric: `<minecraft config dir>/aioa.json`
- Forge: `<minecraft config dir>/aioa.json`

That keeps the structure logically identical across loaders.

## Default Behavior

Out of the box, AIOA is configured to:

- Suppress most hostile mob spawning.
- Leave non-hostile mobs like villagers alone.
- Ignore structure-based spawns by default.
- Ignore non-overworld hostile suppression by default through the `overworldOnly` setting.
- Spawn zombies and husks on the surface during the day through the day-spawn pool.
- Prevent AIOA-managed day spawns from burning in sunlight.

## Spawn Pool Format

Day spawn entries are registry-driven strings so they work for vanilla and modded mobs without hardcoded class lists:

```text
entity_id;enabled=true;weight=10;chance=1.0;min=1;max=3
```

Examples:

```text
minecraft:zombie;enabled=true;weight=12;chance=1.0;min=1;max=3
minecraft:husk;enabled=true;weight=4;chance=0.45;min=1;max=2
modid:custom_zombie;enabled=false;weight=1;chance=0.1;min=1;max=1
```

Supported keys:

- `enabled`
- `weight`
- `chance`
- `min`
- `max`

## Manual Dependency Jars

You said these UI jars will be supplied manually, so the Gradle files include obvious placeholders instead of hardcoded downloaded coordinates.

Place the jars here:

- Fabric UI jars: `fabric/libs/`
- Forge UI jars: `forge/libs/`

Then update the filenames in:

- `fabric/build.gradle`
  - `midnightlib-fabric-...jar`
  - `modmenu-...jar`
  - `cloth-config-...-fabric.jar` (optional)
  - `supermartijn642sconfiglib-...-fabric-...jar` (optional)
- `forge/build.gradle`
  - `yet_another_config_lib_v3-...-forge.jar`
  - `catalogue-...jar` (optional)
  - `configured-...jar` (optional)
  - `cloth-config-...-forge.jar` (optional)
  - `supermartijn642configlib-...-forge-...jar` (optional)

Important for Forge userdev:

- `aioa.enableYaclRuntime` defaults to `false` in `gradle.properties`.
- This prevents incompatible YACL jars from crashing `:forge:runClient` in development.
- Once you have a Forge-dev-compatible YACL jar, enable it with:
  - `-Paioa.enableYaclRuntime=true`

Optional config-mod runtime flags:

- Fabric optional runtime compatibility mods (`Cloth Config`, `SuperMartijn642 Config Lib`):
  - `-Paioa.fabric.enableOptionalConfigModsRuntime=true`
- Fabric Mod Menu runtime loading:
  - `-Paioa.fabric.enableModMenuRuntime=true`
- Forge optional runtime compatibility mods (`Catalogue`, `Configured`, `Cloth Config`, `SuperMartijn642 Config Lib`):
  - `-Paioa.forge.enableOptionalConfigModsRuntime=true`

## Loader-Specific Config UI

### Fabric

Fabric uses:

- MidnightLib for the config screen bridge.
- Mod Menu (optional) to open that config screen from the mods list.
- Optional compatibility metadata for `Cloth Config` and `SuperMartijn642 Config Lib` (no hard dependency).

Relevant files:

- `fabric/src/main/java/com/flubburr/aioa/fabric/config/AioaMidnightConfig.java`
- `fabric/src/main/java/com/flubburr/aioa/fabric/config/AioaModMenuIntegration.java`

### Forge

Forge uses:

- YACL for the config screen when runtime-enabled (`aioa.enableYaclRuntime=true`).
- A built-in Forge fallback config screen when YACL is missing/incompatible, so the config button still opens and remains editable.
- Optional compatibility metadata for `Catalogue`, `Configured`, `Cloth Config`, and `SuperMartijn642 Config Lib` (no hard dependency).

Relevant files:

- `forge/src/main/java/com/flubburr/aioa/forge/config/AioaForgeClient.java`
- `forge/src/main/java/com/flubburr/aioa/forge/config/AioaYaclConfigScreen.java`
- `forge/src/main/java/com/flubburr/aioa/forge/config/AioaForgeFallbackConfigScreen.java`

## Build Notes

- Java 17 is required.
- In this workspace, Gradle verification only succeeded once it was launched with JDK 21 instead of the machine default JDK 25. If you hit Groovy/precompiled-plugin errors, switch Gradle to Java 17 or 21.
- The shared logic can be worked on independently in `common`.
- Fabric compile requires MidnightLib and Mod Menu jars in `fabric/libs` (Mod Menu remains runtime-optional).
- Forge compile requires a YACL jar in `forge/libs`.
- Forge runtime (`runClient`) only loads YACL when `aioa.enableYaclRuntime=true`.
- Forge uses `common/src/main/resources/logo.png` (`mods.toml` `logoFile`).
- Fabric also uses `common/src/main/resources/logo.png` (`fabric.mod.json` `icon`); the logo file is square so Mod Menu accepts it.
