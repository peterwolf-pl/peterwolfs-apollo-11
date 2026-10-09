# ADR 0002: Lander navigation difficulty

- Status: Proposed
- Phase: Elaborate
- Date: 2026-10-09

## Context

The user wants an Apollo Guidance Computer-inspired navigation computer inside the lunar lander, using Apollo 11 Lunar Module source `Luminary099` as historical reference. The selected difficulty is to be controlled by an in-game command. The user clarified three levels: EASY (automatic landing), MID (hints), and PROFESSIONAL (the highest difficulty).

## Decision proposal

Use three explicit modes, selected through a command such as `/apollo difficulty <easy|mid|professional>`:

- **EASY:** landing autopilot completes descent for the player.
- **MID:** player participates in landing with contextual hints and assistance.
- **PROFESSIONAL:** manual, minimally assisted navigation inspired by AGC/DSKY procedures; require more precise decisions and piloting than MID.

Implement a gameplay-native simulation rather than executing the historical AGC code in Minecraft. Consult `Luminary099` for authentic concepts, terminology, and procedure flavor. Confirm exact command permissions, persistence, input UI, and failure consequences during implementation planning.

## Alternatives considered

- One binary easy/hard toggle: superseded by the user's three-level direction.
- Run original `Luminary099` source as a runtime engine: not selected; feasibility and gameplay value are unproven, and the user's ask is for an Apollo-inspired in-game computer rather than a literal AGC emulator.

## Consequences

- Difficulty must be server-authoritative if multiplayer is supported.
- Each level requires distinct testable landing behavior, not only different labels.
- PROFESSIONAL should be challenging but fair; exact AGC-inspired inputs and failure/recovery rules are open.

## Evidence

- Apollo 11 AGC repository: https://github.com/chrislgarry/Apollo-11 — contains `Luminary099`, the Apollo 11 Lunar Module AGC source, and identifies it as public domain.
