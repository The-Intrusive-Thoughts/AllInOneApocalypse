# Changelog

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
