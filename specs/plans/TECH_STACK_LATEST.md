# Initial technical direction — Peterwolf's-Apollo-11

**Status:** Initial build baseline verified; remaining gameplay architecture stays open.

- **Game/API:** Minecraft 26.3, Fabric Loader, Fabric API, Mojang mappings.
- **Language/runtime:** Java 25; Gradle Wrapper 9.5.1 + Fabric Loom 1.17.21. Installed Homebrew OpenJDK 25.0.4.1 and Gradle 9.8.1 for wrapper generation. Verified Loader 0.19.5, Fabric API 0.162.0+26.3. Minecraft 26.3's client jar already contains Mojang-named classes but its version manifest omits Mojang mapping downloads; build uses Fabric's empty `intermediary:0.0.0:v2` mapping artifact. Fabric API is declared as regular `implementation` to avoid Loom failing to remap its source jar against the empty mapping tree. `./gradlew clean build` and `runClient` both succeeded.
- **Mod identity:** Display name `Peterwolf's-Apollo-11`; Java/resource namespace must use a valid lowercase identifier such as `peterwolfs_apollo11` (confirm or choose during elaboration).
- **World/content architecture:** likely custom Moon dimension, registered blocks/items/recipes, custom entities for rover/aliens, and server-authoritative travel/low-gravity mechanics. Exact approach is undecided until API research and design.
- **Models/visuals:** assess Blockbench-compatible assets versus hand-authored Java models; use screenshot references for original designs, not as textures. Visual fidelity is a primary differentiator, not polish deferred to the end.
- **Shader compatibility:** target BSL Shaders through Iris (shader pack supplied/installed by the player), while retaining a correct vanilla-rendering path. Avoid unnecessary custom shader pipelines and validate custom blocks/entities/particles under both modes. Iris's current 26.3 + BSL compatibility must be checked at implementation time; it is not assumed from general Iris compatibility claims.
- **Validation:** Gradle compile/build, automated tests where practical, and in-game checks for world generation, recipes, travel, gravity, entity behavior, and client/server behavior.
- **Repository status:** Git branch `feat/apollo-foundation` created from local `main` (no commits made); remote is `https://github.com/peterwolf-pl/peterwolfs-apollo-11.git`. User screenshots and initial specs remain local/untracked. Fabric scaffold now builds and launches; asset tasks are in progress.
