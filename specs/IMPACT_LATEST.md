# Impact Analysis: Difficulty-Specific Lunar Landing

## Target
Make EASY, MID, and PROFESSIONAL produce distinct, server-authoritative lunar landing behavior.

## Dependents
- `src/main/java/pl/peterwolf/apollo11/landing/LandingDifficulty.java`: mode values/parser.
- `src/main/java/pl/peterwolf/apollo11/landing/LandingDifficultyStore.java`: per-player, session-scoped mode.
- `src/main/java/pl/peterwolf/apollo11/command/ApolloCommands.java`: launch and difficulty commands; candidate landing-control commands.
- `src/main/java/pl/peterwolf/apollo11/launch/LaunchSequence.java`: countdown, translunar flight, and final Moon transfer.
- `src/main/java/pl/peterwolf/apollo11/network/FlightStartPayload.java` and `src/main/java/pl/peterwolf/apollo11/client/FlightWindowHud.java`: server-to-client flight UI.
- `src/main/java/pl/peterwolf/apollo11/block/LaunchGantryBlock.java`: alternate physical launch entry point.
- `src/gametest/java/pl/peterwolf/apollo11/test/ItemRegistrationGameTest.java`: end-to-end launch and existing mode-selection coverage.

## Affected Requirements
- `specs/product/SCOPE_LATEST.yaml`: landing modes must be observably distinct; EASY auto-lands, MID offers hints, PROFESSIONAL is manual/highest difficulty.
- `specs/adr/0002-lander-navigation-difficulty.md`: control inputs and failure/recovery consequences are explicitly unresolved.

No `specs/release-plan.yaml` or epic capsules exist yet, so there is no story ID to map.

## Test Coverage
- `ItemRegistrationGameTest` verifies the EASY, MID, and PROFESSIONAL outcome rules, PROFESSIONAL `/apollo landing burn`, both launch entry points, HUD, Moon transfer, crater terrain, and reduced gravity.
- The test does not yet drive full EASY/MID flights or the PROFESSIONAL missed-burn abort through the world; those remain integration-test gaps.
- Required gate: `./gradlew runClientGameTest` (GREEN, 28s).

## Result and Residual Risk: Medium
The user approved the behavior proposal. Landing difficulty is captured when launch starts and resolved on the server. EASY auto-lands, MID may use an optional manual burn but receives autopilot correction, and PROFESSIONAL requires the burn or returns the player to the recorded launch position. Residual risk is limited to end-to-end coverage of each profile and the abort path.
