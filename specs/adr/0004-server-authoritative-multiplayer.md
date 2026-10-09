# ADR 0004: Server-authoritative multiplayer

- **Status:** Accepted
- **Date:** 2026-10-10
- **Decision owners:** User and project team

## Context

The mod's launch sequence changes player inventory, runs a countdown, loads a custom Moon dimension, and transfers players between dimensions. Those actions must behave consistently in single-player and dedicated-server multiplayer.

## Decision

- Support both single-player and dedicated-server multiplayer.
- Keep launch countdown, rocket consumption, and dimension transfer server-authoritative.
- A launch transfers only the player who initiates it. Other players are not moved automatically.

## Consequences

- Client interactions request launch; server code validates and owns countdown and transfer state.
- Multiplayer tests must verify that non-initiating players remain in their original dimension.
- A coordinated group launch is not part of the first implementation; revisit only if the gameplay design requires it.
