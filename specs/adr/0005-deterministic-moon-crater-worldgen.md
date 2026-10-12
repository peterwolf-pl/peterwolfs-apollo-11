# ADR 0005: Generate lunar craters deterministically per chunk

- Status: Proposed
- Date: 2026-10-10

## Context

The Moon needs many craters of different sizes. The previous approach carved three fixed craters around the player after each landing. That does not scale with exploration, repeats work, and cannot produce a coherent world beyond the landing area. Crater results must be the same on single-player and dedicated servers, independent of chunk-generation order, and must not damage the launch landing zone.

## Decision

Use a registered, Moon-only world-generation feature. Derive one candidate crater from each 32×32 world cell using the world seed and cell coordinates. Use a heavily small-biased distribution: 68% radius 2–5, 26% radius 6–11, and 6% radius 12–18. Shape each crater as a shallow bowl with a raised rim; inspect neighboring cells when generating a chunk so craters do not stop at chunk borders. Keep a 32-block clear radius around the landing pad. Generate only while a new chunk is being built and mutate only that chunk.

For the flight view, use a static cockpit panel with a transparent circular aperture. Render stars and the shrinking Earth sprite behind it, then draw the panel and instruments over them. Keep rendering on Minecraft's GUI sprite atlas and vanilla GUI pipeline; do not add a shader dependency.

## Consequences

- Craters are deterministic across multiplayer servers and can be explored without loading or scanning a larger area.
- New terrain contains a varied crater field, while already-generated chunks are not retroactively changed.
- The reserved landing area remains flat and safe.
- A static panel texture plus circular alpha aperture keeps the flight cockpit independent of shader packs and preserves the Earth-only-through-the-window effect.
- The GameTest checks generation, seed repeatability, crater size classes, the safe zone, and the aperture alpha. Visual art direction can still be refined later.

## Alternatives rejected

- Carving fixed craters at liftoff: affects only one small area and repeats work per landing.
- Random craters on chunk-load events: risks order-dependent results, duplicated edits, and server/client desynchronization.
- Scanning a broad area from a tick or landing callback: does unnecessary work and forces chunk loads.
- A shader or full-screen Earth image: adds compatibility risk and violates the circular-window requirement.
