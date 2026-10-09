# Moon travel — first playable vertical slice

## Goal

Complete the launch sequence by moving its initiating player from the overworld into a persistent, server-authoritative Apollo Moon dimension and landing them safely on its surface.

## In scope

- Define a `peterwolfs_apollo11:moon` dimension and dimension type with a simple, safe gray test surface.
- At the existing liftoff event, resolve the Moon `ServerLevel`, teleport only the initiating player, and report successful arrival.
- Keep the existing launch countdown, cancellation, rocket consumption, and multiplayer isolation.
- Extend the client GameTest to verify the dimension loads and a completed countdown moves the player to the Moon dimension.

## Out of scope for this slice

- Earth-through-window transit visuals, bespoke lunar terrain/craters, reduced gravity, AGC landing modes, suit/rover, aliens, and lunar landmarks. These remain subsequent slices; do not claim the Moon environment is production-complete.

## Verification

- `./gradlew runClientGameTest`: starting a launch consumes one rocket, runs the countdown, and moves only the initiating player into the Moon dimension at a safe surface position.
- `./gradlew build`: package the mod without invoking the unrelated server GameTest task.
