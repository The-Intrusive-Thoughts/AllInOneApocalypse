# Changelog

## 0.2.0

- Added a compact creative Spawn Studio (`F7`) with cursor placement, No AI, face-player, and persistence controls.
- Added server-validated Fabric and Forge networking for Spawn Studio actions.
- Added per-player automatic spawn instances and independent nearby mob budgets.
- Added coordinated horde AI target sharing for configurable vanilla and modded mobs.
- Added Balanced, Cinematic, and Horde quick presets and a cleaner compact config flow.
- Hardened Java bytecode targeting and loader/game metadata to prevent cross-version jar startup failures.
- Kept optional config-menu integrations non-mandatory to reduce mod conflicts.

## 0.1.2

- Added a custom in-game config UI for tuning hostile spawn control, day surface spawns, zombie AI, entity selectors, biome selectors, and spawn pool entries.
- Added custom-styled sliders and improved spacing in the config UI for cleaner layout and readability.
- Improved hostile and biome selector screens with better search bar placement, cleaner preview panels, and less overlapping content.
- Improved hostile mob previews so rendered mob models stay in a dedicated preview area instead of covering text.
- Enhanced zombie wall-climbing pursuit so configured zombies climb more reliably while chasing targets, and stop doing so when the option is disabled.
- Restricted the `F6` config hotkey to creative mode so it no longer opens the config in survival.
- Updated the README so screenshots and linked images render correctly on Modrinth by using absolute image URLs.
- Fixed runtime-safe resource id parsing so the shared config and spawn code works cleanly on the target Minecraft runtime.
