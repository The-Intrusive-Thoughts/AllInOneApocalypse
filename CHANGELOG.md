# Changelog

## 0.2.0

- Replaced the old fixed zombie-AI editor with Behavior Graph Studio: a Scratch/Blueprint-style node editor for one mob, entity types, entity tags, managed apocalypse mobs, or every mob.
- Added more than 35 event, sensing, condition, targeting, movement, combat, interaction, state, world, effect, and organization nodes, including walk a fixed block distance, rotate exact degrees, attack, flee, strafe, jump, teleport, spawn another mob, play sound, and share targets.
- Added multi-graph editing, draggable nodes and editor window, typed output ports, quick linking, undo/redo, validation, canvas panning, animated grid/scanline feedback, selected-node pulses, and a contextual live mob viewport.
- Added cursor-centered `Ctrl + wheel` zoom, `Shift + wheel` horizontal panning, `Alt + drag` and middle-drag canvas movement, graph fitting, and creator shortcuts for undo, redo, duplicate, new, delete, help, save/apply, and fit-to-view.
- Added an integrated, tabbed Documentation & Creator Guide with first-open guidance, live mob showcase cards, complete paged node reference, editor controls, Spawn Studio instructions, networking safety, and compatibility guidance without exposing implementation code.
- Added Minecraft-language-system labels and bundled English, French, Spanish, and German navigation translations; other languages safely fall back to English.
- Added a proper world picker: the editor closes, the player right-clicks a mob, and the editor reopens bound to that exact entity. `F7` cancels or the picker safely times out.
- Added a movable, resizable, closable, paged help overlay covering graph basics, scopes, world selection, links, permissions, and server safety.
- Added Behavior Engine configuration for execution cadence, graph and node-step budgets, world-node permission, and spawn-node caps so creators and servers can tune complex scenes safely.
- Added validated Fabric and Forge graph networking with creative/operator permission checks, bounded payloads, graph/node/link limits, and server-authoritative persistence.
- Removed the legacy refined-AI tick pipeline in favor of the graph runtime while keeping existing settings readable for migration and compatibility.
- Fixed the Forge 1.20.1 `javafml:53` startup failure by locking the artifact to Forge 47, Java 17 bytecode, and a bounded `[47,48)` loader range so a jar from a newer Minecraft line cannot masquerade as a 1.20.1 build.
- Disabled the vanilla background post-processing path on every AIOA screen, preventing stacked blur, framebuffer conflicts, and the background scale/shrink effect seen with other UI mods.
- Added a compact, ImGui-inspired creative Spawn Studio, available from the config home or with `F7`, for spawning any registered mob at the cursor with No AI, face-player, and persistence controls.
- Added validated Fabric and Forge client/server networking for Spawn Studio requests. Servers require creative mode and independently validate entity ids, range, build height, mob type, and collision.
- Added per-player spawn instances: every automatic group has an owner tag and its own nearby cap, preventing one player from consuming another player's apocalypse budget.
- Added coordinated horde AI. Configured apocalypse mobs can share live targets with nearby refined-AI mobs and immediately join the pursuit.
- Added Balanced Survival, Cinematic/Manual, and High-Intensity Horde quick presets while preserving full manual editing after a preset is applied.
- Kept the UI compact with fixed maximum logical panel sizes, near-instant opening/closing, richer hover/press/slider/link micro-animations, and non-pausing screens for live content creation.
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
