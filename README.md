![AIOA Logo](https://raw.githubusercontent.com/The-Intrusive-Thoughts/AllInOneApocalypse/1.20.1/common/logo/AIOA-logo2.png)

All In One Apocalypse (AIOA) is a lightweight mod that changes mob spawning to create a zombie apocalypse style experience.

By default, the mod disables the spawning of most hostile mobs so that zombies become the main threat in the world. Mobs that spawn in structures or other dimensions are not affected. This option can be changed in the config.

The mod also allows zombies to spawn on the surface during the day and prevents them from burning in sunlight. This can also be enabled or disabled in the config.

![AIOA Config Screen](https://raw.githubusercontent.com/The-Intrusive-Thoughts/AllInOneApocalypse/1.20.1/common/logo/AIOA-config.png)

AIOA includes a simple spawn configuration that allows you to add modded mobs to the daytime spawn list.

You can also adjust how frequently those mobs spawn, making it easy to balance mobs from other mods.

![AIOA Features](https://raw.githubusercontent.com/The-Intrusive-Thoughts/AllInOneApocalypse/1.20.1/common/logo/AIOA-features.png)

- Disables most hostile mob spawning by default
- Allows zombies to spawn during the day
- Prevents zombies from burning in sunlight
- Configurable spawn list for modded mobs
- Adjustable spawn rarity
- Creative Spawn Studio (`F7`) with cursor placement, No AI, face-player, and persistence controls
- Per-player instanced horde spawning for predictable multiplayer budgets
- Coordinated target-sharing AI for configurable vanilla and modded mobs
- Visual Behavior Graph Studio for scripting individual mobs, entity types, tags, managed mobs, or global mob behavior
- 35+ graph nodes for sensing, conditions, movement, combat, state changes, mob interaction, spawning, and effects
- Right-click world selection, multi-graph projects, live mob viewport, draggable paged help, validation, and secure multiplayer syncing
- Configurable graph cadence, execution budgets, and mob-spawn safety caps
- Balanced, Cinematic, and Horde quick presets
- Compact ImGui-inspired UI with blur-safe animated-grid backgrounds and micro-animated controls



## Compatible Config Mods

[![YetAnotherConfigLib](https://raw.githubusercontent.com/The-Intrusive-Thoughts/AllInOneApocalypse/1.20.1/common/logo/YACL-TEXTURE.png)](https://modrinth.com/mod/yacl)
[![MidnightLib](https://raw.githubusercontent.com/The-Intrusive-Thoughts/AllInOneApocalypse/1.20.1/common/logo/ML-TEXTURE.png)](https://modrinth.com/mod/midnightlib)
[![Mod Menu](https://raw.githubusercontent.com/The-Intrusive-Thoughts/AllInOneApocalypse/1.20.1/common/logo/MM-TEXTURE.png)](https://modrinth.com/mod/modmenu)
[![Catalogue](https://raw.githubusercontent.com/The-Intrusive-Thoughts/AllInOneApocalypse/1.20.1/common/logo/C-TEXTURE.png)](https://www.curseforge.com/minecraft/mc-mods/catalogue)
[![Cloth Config Lib](https://raw.githubusercontent.com/The-Intrusive-Thoughts/AllInOneApocalypse/1.20.1/common/logo/CCL-TEXTURE.png)](https://modrinth.com/mod/cloth-config)
[![Configured](https://raw.githubusercontent.com/The-Intrusive-Thoughts/AllInOneApocalypse/1.20.1/common/logo/CONF-TEXTURE.png)](https://modrinth.com/mod/project/HlpEBg3R)

## Release Workflow

- `.\gradlew.bat verifyAll`
  Builds both Fabric and Forge to catch loader-specific breakages before release.

- `.\gradlew.bat publishModrinth`
  Publishes the current checked-in version to Modrinth without changing the Modrinth project page body.

- `.\gradlew.bat syncModrinthBody`
  Updates the Modrinth project page body separately when you actually want the public description changed.

- `.\gradlew.bat releaseModrinth -PreleaseVersion=0.1.2d -PreleaseChangelog="First note||Second note"`
  Updates `gradle.properties`, prepends a matching `CHANGELOG.md` entry, builds both loaders, and publishes both artifacts to Modrinth.

This still does not remove the need for separate Minecraft branches when Mojang changes APIs, but it does keep each branch much easier to maintain and publish consistently.
