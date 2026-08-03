![All In One Apocalypse logo](https://raw.githubusercontent.com/The-Intrusive-Thoughts/AllInOneApocalypse/1.20.1/common/logo/AIOA-logo2.png)

# All In One Apocalypse

**All In One Apocalypse (AIOA)** turns Minecraft's ordinary hostile spawning into a configurable zombie-apocalypse experience. Keep the lightweight default setup, build cinematic encounters, or create advanced mob behavior with the visual editor—without needing to write a mod.

Available for **Fabric** and **Forge**. Choose a file marked for your exact Minecraft version and loader from the Versions tab.

## The apocalypse, your way

By default, AIOA suppresses most ordinary hostile spawns so zombies become the main threat. Structure, spawner, special, and non-Overworld spawning can remain untouched, and every major rule can be adjusted in the in-game configuration.

Configured apocalypse mobs can spawn on the surface during the day and can be protected from sunlight burning. The spawn pool accepts vanilla and modded mobs, with controls for rarity, chance, group size, distance, biome, and per-player limits.

![AIOA configuration screen](https://raw.githubusercontent.com/The-Intrusive-Thoughts/AllInOneApocalypse/1.20.1/common/logo/AIOA-config.png)

## Highlights

- Configurable hostile-spawn suppression
- Daytime surface spawning for vanilla or modded mobs
- Optional sunlight protection and zombie-variant controls
- Per-player instanced spawning with multiplayer-safe budgets
- Refined pursuit, door use, wall climbing, and coordinated target sharing
- Per-mob targeting rules for players, animals, other mobs, or everything
- Balanced, Cinematic, and Horde quick presets
- Server-authoritative networking, validation, cooldowns, and spawn safety caps
- Compact, scalable editor UI with draggable windows, animated grids, contextual help, and configurable sounds

![AIOA feature overview](https://raw.githubusercontent.com/The-Intrusive-Thoughts/AllInOneApocalypse/1.20.1/common/logo/AIOA-features.png)

## Behavior Graph Studio

Press **F7** to open the visual Behavior Graph Studio. Build mob logic by dragging nodes onto a graph and connecting their ports, similar to a visual scripting or node-compositing tool.

Graphs can target:

- One selected mob
- A mob type
- A mob tag
- AIOA-managed mobs
- All eligible mobs globally

The editor includes sensing, timing, conditions, variables, movement, rotation, navigation, combat, equipment, attributes, effects, model-part controls, mob spawning, chat actions, and safe script nodes. Graphs support multiple tabs, undo and redo, validation, zooming, panning, reusable files, an inspector, node parameters, and an interactive 3D preview viewport.

To select a world mob, leave the editor's selection mode and **right-click the mob**. This avoids accidental selection through the UI.

## Creative Spawn Studio

Press **F6** or open Spawn Studio from AIOA's configuration screen. Creative players can spawn a chosen mob with controls such as:

- Placement at the aimed block or cursor position
- Face the player
- Disable AI
- Persistent mob
- Custom name and basic spawn options

Use Spawn Studio with the Behavior Graph Studio to create, select, and test actors for videos, maps, machinima, and gameplay scenarios.

## Built-in guidance

The first-open tutorial points to the integrated Docs tab and can be skipped or closed. The movable help and documentation windows explain editor controls, graph targeting, node categories, scripting, validation, and common workflows directly in game.

## Controls

| Action | Default control |
| --- | --- |
| Open Creative Spawn Studio | `F6` |
| Open Behavior Graph Studio | `F7` |
| Pan graph | Hold left mouse button on empty space |
| Zoom graph | Mouse wheel |
| Edit a node | Double-click the node |
| Connect nodes | Drag between compatible ports |
| Delete selection | `Delete` |
| Undo | `Ctrl` + `Z` |
| Redo | `Ctrl` + `Y` |
| Context actions | Right-click the graph |

Controls can be changed from Minecraft's keybind settings.

## Optional configuration integrations

AIOA includes its own configuration and editor screens. These supported mods provide additional entry points or loader-specific configuration integration; they are not all required.

- [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl)
- [MidnightLib](https://modrinth.com/mod/midnightlib)
- [Mod Menu](https://modrinth.com/mod/modmenu)
- [Cloth Config API](https://modrinth.com/mod/cloth-config)
- [Catalogue](https://www.curseforge.com/minecraft/mc-mods/catalogue)
- [Configured](https://modrinth.com/mod/configured)

## Installation

1. Install the Fabric or Forge loader required by the file.
2. Install any dependency marked **Required** on that version's Modrinth page.
3. Place the AIOA `.jar` in your `mods` folder.
4. Make sure the AIOA file, loader, dependencies, and Minecraft version all match.

Do not use a Forge build on Fabric or a Fabric build on Forge. A file compiled for a newer Minecraft/Java version may show a `javafml` or language-provider error on an older installation.

## Links

- [Download AIOA versions](https://modrinth.com/mod/aioa/versions)
- [Source code and issue tracker](https://github.com/The-Intrusive-Thoughts/AllInOneApocalypse)
- [Changelog](https://github.com/The-Intrusive-Thoughts/AllInOneApocalypse/blob/1.20.1/CHANGELOG.md)

AIOA is designed for survival packs, adventure maps, multiplayer events, testing, and content creation. Start with a preset, then tune only the systems you need.
