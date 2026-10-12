# Impact Analysis: deterministic lunar crater field and cockpit porthole

## Target
- `src/main/java/pl/peterwolf/apollo11/world/MoonCraterFeature.java`: replace three hand-authored craters patched at liftoff with a deterministic, world-seed-based crater field applied during Moon chunk generation.
- `src/main/java/pl/peterwolf/apollo11/client/FlightWindowHud.java`: replace the full-screen dark flight panel with an Apollo-inspired cockpit interior; Earth must appear only inside a circular porthole.
- Supporting targets: Moon dimension/biome worldgen resources, Fabric feature registration, flight HUD textures/atlas, and client GameTests.

## Dependents
- `src/main/java/pl/peterwolf/apollo11/launch/LaunchSequence.java` no longer edits terrain at landing; custom biome worldgen creates craters only as new Moon chunks generate.
- `src/main/java/pl/peterwolf/apollo11/client/Apollo11Client.java` registers `FlightWindowHud`.
- `src/main/java/pl/peterwolf/apollo11/network/FlightStartPayload.java` triggers the client HUD at translunar flight start.
- `src/gametest/java/pl/peterwolf/apollo11/test/ItemRegistrationGameTest.java` asserts fixed crater coordinates and HUD state; update it for generated craters, safe landing zone, and cockpit/porthole assets.
- `src/main/resources/data/peterwolfs_apollo11/dimension/moon.json` currently uses `minecraft:flat` with biome features disabled.

## Affected stories
- Lunar world generation and landing-surface quality (scope e05 / Moon terrain slice).
- Translunar flight presentation (scope e03 / cockpit view).
- No release-plan/epic capsules exist yet; phase 2 ELABORATE is still open.

## Test coverage
- `ItemRegistrationGameTest` verifies seed-stable crater centers/radii, small/medium/large size classes, generated crater floors and raised rims, a clear landing area, one-sixth gravity, HUD state, atlas sprite loading, and the cockpit texture's transparent circular aperture.
- `./gradlew runClientGameTest` passes after crater and cockpit implementation; `./gradlew build` remains required before commit.
- Visual appearance is validated through deterministic asset/geometry checks only; no manual UI test was performed, following the project test rule.

## Risk: Medium
The high-risk design is implemented and GameTest-verified: the feature is seed-derived, writes only the generating chunk, checks neighboring grid cells for boundary continuity, and reserves a 32-block radius at spawn. Existing chunks are intentionally not retrofitted. HUD rendering is client-only and uses a transparent circular cockpit aperture with a uniform cover scale; shader compatibility is not required.

## Implemented design
`src/main/java/pl/peterwolf/apollo11/world/MoonCraterFeature.java` derives one candidate per 32×32 cell from the world seed and cell coordinates. The distribution is 68% radius 2–5, 26% radius 6–11, and 6% radius 12–18. The feature considers neighboring cells but mutates only the current chunk; a 32-block landing radius remains clear. No chunk-load listener, large-area scan, or persistent crater bookkeeping is used. The flight HUD draws the Earth sprite and stars before the instrument-panel sprite; the cockpit texture's circular aperture is transparent, and a uniform cover scale keeps it round at non-native aspect ratios.
