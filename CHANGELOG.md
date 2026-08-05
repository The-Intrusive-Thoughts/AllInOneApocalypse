# Changelog

## 0.2.1 (Unreleased)

- Replaced the Graph Studio action strip with File, Edit, View, Graph, and Help menus covering new/save/save-as/import/export/quit, undo/redo, layout controls, validation, linking, grouping, preview, guides, and documentation.
- Added direct curve rerouting: pick up an existing connection anywhere along its Bezier, drop it on another typed input, or release on empty canvas to restore the original link safely.
- Changed Node Parameters from a draggable persistent window into a cursor-local double-click popover that closes with its `x` or an outside click, while preserving edits and contextual entity/player/script controls.
- Added node-aware right-click menus with compact action icons, custom node labels, parameter editing, duplication, linking, deletion, fitting, and callable function groups. Group-call nodes open an isolated breadcrumb workspace on double-click, automatically contain newly added nodes, execute with bounded recursion/step budgets, and run inside the deterministic viewport preview.
- Rebuilt Creator Script as a bounded code-like language with comments, variables, `if`/`else` blocks, mob/target/phase/tick built-ins, interpolation, validation, and safe action calls for messages, movement, combat, state, healing, targeting, and tags.
- Added hover tooltips for graph nodes and strengthened first-run onboarding with Back/Next/Skip controls, an animated guide card, modal input capture, and a final handoff into the integrated documentation.
- Fixed the Forge 1.20.1 `NoSuchFieldError: SOUND_EVENT` startup crash by moving custom sound registration into each loader and using a version-safe common registry lookup.
- Fixed daytime apocalypse mobs failing to spawn by broadening surface searches and adding a conservative ground-placement fallback when loader-specific vanilla placement checks reject otherwise valid EVENT spawns.
- Fixed the first-run tutorial and Quick Presets documentation hint returning after dismissal by persisting Skip and close actions immediately.
- Rebuilt behavior links with persistent link IDs, explicit source outputs and destination inputs, strict port validation, selectable curved links, `Delete` disconnection, and drag-to-reroute support while migrating existing `.aioagraph` files.
- Fixed branch execution so `next` links no longer run incorrectly when a node returns `false`, `missing`, `waiting`, or another named output.
- Added atomic workspace synchronization so closing graph tabs removes them on the server and all valid graph changes are applied together instead of as incomplete per-graph upserts.
- Added debounced local autosaving and validated server autosync. Invalid work-in-progress graphs remain recoverable as local drafts without replacing the last valid server runtime.
- Fixed world mob binding to persist the exact UUID and detected mob type, and made graph targeting controls scope-aware instead of hiding a stale raw selector behind a button.
- Made variables, event state, delays, intervals, and spawn cooldowns persist per graph and per mob, with stale runtime state cleanup.
- Added explicit floating-window z-order and occlusion-aware controls so lower Inspector, Parameters, and Palette widgets no longer bleed through the active window.
- Added scrollable layered graph tabs that keep the active document visible when more graphs are open than fit in the workspace.
- Rebuilt the viewport as a deterministic 3D graph sandbox with separate Play, Pause, Stop, 1x, 2x, and 3x controls; it follows real graph edges and output ports while previewing timers, movement, targeting, combat, health, phases, variables, rotation, and glow without changing the world.
- Added Delay Seconds, Repeat Count, Heal Target, Clear Target Effects, and Set Target Glowing creator nodes with matching runtime execution, validation, defaults, branching, inspector fields, and documentation.
- Categorized the Node Palette by node family and made editor sizing compensate for Minecraft GUI scales above 2 so high-scale setups retain a compact, usable workspace.
- Added 25 boss and creator nodes covering cooldowns, four-lane sequences, target/player/weather conditions, attacker targeting, orbit/dash/target teleport, direct and area damage, fire, launch, armor, follow range, knockback resistance, status effects, lightning, explosions, action bars, persistent phases, phase routing, and despawning.
- Expanded node configuration with immediate validation feedback, safer field limits, contextual effect/equipment/phase/sound controls, and preserved parameter edits during editor window rebuilds.
- Updated the portable graph format to version 2 while retaining version 1 import compatibility.

## 0.2.0

- Replaced the old fixed zombie-AI editor with Behavior Graph Studio: a Scratch/Blueprint-style node editor for one mob, entity types, entity tags, managed apocalypse mobs, or every mob.
- Added more than 35 event, sensing, condition, targeting, movement, combat, interaction, state, world, effect, and organization nodes, including walk a fixed block distance, rotate exact degrees, attack, flee, strafe, jump, teleport, spawn another mob, play sound, and share targets.
- Added multi-graph editing, draggable nodes and editor window, typed output ports, quick linking, undo/redo, validation, canvas panning, animated grid/scanline feedback, selected-node pulses, and a contextual live mob viewport.
- Added cursor-centered `Ctrl + wheel` zoom, `Shift + wheel` horizontal panning, `Alt + drag` and middle-drag canvas movement, graph fitting, and creator shortcuts for undo, redo, duplicate, new, delete, help, save/apply, and fit-to-view.
- Upgraded the graph workspace with normal wheel zoom, blank-canvas left-drag panning, palette wheel scrolling and scrollbar feedback, direct output-port drag linking, smooth curved links, duplicate/self-link protection, double-click parameter editing, Enter-to-apply, and a right-click action menu.
- Added a required, protected Base Mob root that auto-detects the graph target and applies real health, attack-damage, and movement-speed attributes; older graphs are migrated with the new root automatically.
- Added executable maximum-health, attack-damage, movement-speed, and dynamic equipment nodes with typed defaults and registry/slot validation.
- Strengthened validation with Base Mob/root checks, graph reachability, supported-parameter checks, numeric ranges, registry checks, equipment-slot checks, and export blocking when a graph cannot execute as authored.
- Added draggable/resizable live viewport panels, clickable graph tabs, and portable `.aioagraph` import/export with a configurable safe graph-library directory under the Minecraft config folder.
- Rebuilt Graph Studio as a stretchable workspace with independently draggable and collapsible Node Palette, Inspector, Parameters, 3D Viewport, and Quick Guide windows; double-clicking a node now opens its dedicated parameter window.
- Replaced blocky graph links with thin, rotated 48-segment Bezier rendering and added browser-style hover close controls for graph document tabs.
- Upgraded the viewport into a full live 3D workspace with perspective floor grid, mouse orbit, real entity rendering, and side-by-side player targeting previews when space permits.
- Changed the viewport into a self-running simulation preview: models no longer track the cursor, automatically rotate, advance walk animation, and preview the mob/player scene on a perspective floor.
- Added Every Seconds, Delay Ticks, Say in Chat, Particle Pattern, safe Script, Body Rotation, and Head Rotation nodes, including server-side execution, validation, defaults, and a dedicated scripting documentation tab.
- Replaced raw mob registry-id entry in Spawn Studio and graph targeting with translated, searchable mob pickers and live previews.
- Made node parameters a double-click-only floating inspector, removed graph navigation controls from the inspector, displayed the active graph name prominently, and expanded UI/graph scaling from 25% to 500% with graph zoom from 10% to 800%.
- Matched Graph Studio's default composition to the wide creator layout, with the simulated viewport separated on the left, a large central graph canvas, and palette/inspector/parameter/guide windows arranged around the edges.
- Added per-graph editor-layout persistence for every window position, size, collapsed state, help visibility, canvas pan, and zoom; reopening or switching graph tabs no longer resets the creator's workspace.
- Added visible and interactive resize grips to every movable editor window and expanded viewport/graph zoom limits for large recording and multi-monitor layouts.
- Rebuilt Node Parameters with previous/next field navigation and contextual tools for entity selection, scripts, booleans, particle patterns, variable math, comparisons, and recommended defaults.
- Added Heal Self, Set Velocity, Add Tag, Remove Tag, Has Tag, Set Variable, Math Variable, and Compare Variable nodes with runtime execution, validation, defaults, branching, and documentation.
- Added layered overlapping graph tabs with title/accent bars, moved selected-node information into the inspector header/description area, added a dedicated color-guided multi-command Script editor, and added an online-player picker for the new Find Player Name node.
- Added persistent first-run onboarding with guided steps and a skip control, plus a closable helper card and arrow that continues to point to Documentation until dismissed.
- Added Interface & Menu Audio settings for compact editor scale, menu mob/ambience volume, and UI sound volume; settings persist in config schema 6 and no longer inherit Minecraft GUI scale behavior.
- Reorganized the configuration home around Quick Presets, Behavior Graph Studio, Spawn Studio, Docs, and grouped advanced settings, and standardized editing flows on one clear `Done` action.
- Added an integrated, tabbed Documentation & Creator Guide with first-open guidance, live mob showcase cards, complete paged node reference, editor controls, Spawn Studio instructions, networking safety, and compatibility guidance without exposing implementation code.
- Added Minecraft-language-system labels and bundled English, French, Spanish, and German navigation translations; other languages safely fall back to English.
- Added a proper world picker: the editor closes, the player right-clicks a mob, and the editor reopens bound to that exact entity. Cancelling or timing out now always restores Graph Studio instead of trapping the player.
- Added a movable, resizable, closable, paged help overlay covering graph basics, scopes, world selection, links, permissions, and server safety.
- Added Behavior Engine configuration for execution cadence, graph and node-step budgets, world-node permission, and spawn-node caps so creators and servers can tune complex scenes safely.
- Added validated Fabric and Forge graph networking with creative/operator permission checks, bounded payloads, graph/node/link limits, and server-authoritative persistence.
- Removed the legacy refined-AI tick pipeline in favor of the graph runtime while keeping existing settings readable for migration and compatibility.
- Fixed the Forge 1.20.1 `javafml:53` startup failure by locking the artifact to Forge 47, Java 17 bytecode, and a bounded `[47,48)` loader range so a jar from a newer Minecraft line cannot masquerade as a 1.20.1 build.
- Disabled the vanilla background post-processing path on every AIOA screen, preventing stacked blur, framebuffer conflicts, and the background scale/shrink effect seen with other UI mods.
- Added a compact, ImGui-inspired creative Spawn Studio from the config home for spawning any registered mob at the cursor with No AI, face-player, and persistence controls. `F7` now opens Behavior Graph Studio directly.
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
