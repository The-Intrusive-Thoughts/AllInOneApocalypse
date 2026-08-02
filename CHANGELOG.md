# Changelog

## 0.2.0

- Fixed the Forge 1.20.1 `javafml:53` startup failure by locking the artifact to Forge 47, Java 17 bytecode, and a bounded `[47,48)` loader range so a jar from a newer Minecraft line cannot masquerade as a 1.20.1 build.
- Disabled the vanilla background post-processing path on every AIOA screen, preventing stacked blur, framebuffer conflicts, and the background scale/shrink effect seen with other UI mods.
- Added a compact, ImGui-inspired creative Spawn Studio, available from the config home or with `F7`, for spawning any registered mob at the cursor with No AI, face-player, and persistence controls.
- Added validated Fabric and Forge client/server networking for Spawn Studio requests. Servers require creative mode and independently validate entity ids, range, build height, mob type, and collision.
- Added per-player spawn instances: every automatic group has an owner tag and its own nearby cap, preventing one player from consuming another player's apocalypse budget.
- Added coordinated horde AI. Configured apocalypse mobs can share live targets with nearby refined-AI mobs and immediately join the pursuit.
- Added Balanced Survival, Cinematic/Manual, and High-Intensity Horde quick presets while preserving full manual editing after a preset is applied.
- Kept the UI compact with fixed maximum logical panel sizes, cleaner one-action rows, existing hover/press/slider micro-animations, and non-pausing screens for live content creation.
- Preserved compatibility with vanilla and modded entity registries, Fabric API, Forge, MidnightLib, Mod Menu, YACL, Cloth Config, Catalogue, Configured, and SuperMartijn642's Config Lib without making optional UI integrations mandatory.

## 0.1.2c

- Fixed the new in-game config UI so the allowed biomes and allowed hostiles selectors reliably close with `Done`, `Cancel`, and `Esc` instead of trapping the player.
- Added looping AIOA menu music that now plays consistently while the custom config UI is open.
- Improved the custom config screen transitions so they fade more cleanly without the old black flash or background scaling/shrinking effect.
- Improved client/server safety around the custom config flow so dedicated and multiplayer sessions keep using the safer restricted behavior introduced for the 1.20.1 line.
- Server startup crash context: [#1](https://github.com/The-Intrusive-Thoughts/AllInOneApocalypse/issues/1)

## 0.1.2b

- Fixed a Fabric dedicated server startup crash by avoiding client-only MidnightLib config initialization during the `main` entrypoint on servers.
- Issue: [#1](https://github.com/The-Intrusive-Thoughts/AllInOneApocalypse/issues/1)

## 0.1.2

- Added a custom in-game config UI for tuning hostile spawn control, day surface spawns, zombie AI, entity selectors, biome selectors, and spawn pool entries.
- Added custom-styled sliders and improved spacing in the config UI for cleaner layout and readability.
- Improved hostile and biome selector screens with better search bar placement, cleaner preview panels, and less overlapping content.
- Improved hostile mob previews so rendered mob models stay in a dedicated preview area instead of covering text.
- Enhanced zombie wall-climbing pursuit so configured zombies climb more reliably while chasing targets, and stop doing so when the option is disabled.
- Restricted the `F6` config hotkey to creative mode so it no longer opens the config in survival.
- Updated the README so screenshots and linked images render correctly on Modrinth by using absolute image URLs.
- Fixed Forge 1.20.1 startup compatibility by replacing newer `ResourceLocation` parsing calls with a runtime-safe implementation.
