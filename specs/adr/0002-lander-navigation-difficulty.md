# ADR 0002: Lander navigation difficulty

- Status: Accepted
- Phase: Elaborate
- Date: 2026-10-09

## Context

The user wants an Apollo Guidance Computer-inspired navigation computer inside the lunar lander, using Apollo 11 Lunar Module source `Luminary099` as historical reference. The selected difficulty is to be controlled by an in-game command. The user clarified three levels: EASY (automatic landing), MID (hints), and PROFESSIONAL (the highest difficulty).

## Decision

Use three explicit modes, selected through `/apollo difficulty <easy|mid|professional>`. The selection is per-player and lasts for the current server session.

- **EASY:** autopilot completes a safe descent automatically.
- **MID:** the guidance computer offers an optional `/apollo landing burn` input and automatically corrects the descent if the player does not use it.
- **PROFESSIONAL:** require `/apollo landing burn` during translunar flight; if the player misses the burn, safely abort to the launch position without death.

Implement a gameplay-native simulation rather than executing historical AGC code in Minecraft. Consult `Luminary099` for authentic concepts, terminology, and procedure flavor. Additional AGC/DSKY controls may expand PROFESSIONAL later.

## Alternatives considered

- One binary easy/hard toggle: superseded by the user's three-level direction.
- Run original `Luminary099` source as a runtime engine: not selected; feasibility and gameplay value are unproven, and the user's ask is for an Apollo-inspired in-game computer rather than a literal AGC emulator.

## Consequences

- Landing outcome is server-authoritative and the selected difficulty is captured when launch begins.
- Each level has a distinct testable outcome; one manual retrograde burn is the first PROFESSIONAL input.
- A missed PROFESSIONAL burn returns the player to the recorded launch position; no death or forced fall is used.

## Evidence

- Apollo 11 AGC repository: https://github.com/chrislgarry/Apollo-11 — contains `Luminary099`, the Apollo 11 Lunar Module AGC source, and identifies it as public domain.
