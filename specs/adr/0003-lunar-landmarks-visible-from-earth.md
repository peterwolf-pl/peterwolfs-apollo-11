# ADR 0003: Lunar landmarks visible from Earth

- Status: Exploration needed
- Phase: Elaborate
- Date: 2026-10-09

## Context

The user wants very large player-built lunar structures to appear as a visible outline on the Moon's disc when viewed from Earth. Normal Minecraft entity/block render distance cannot render a structure across a separate dimension or astronomical distance. The visual must also remain legible at the Moon's small apparent size and work with vanilla rendering and Iris + BSL.

## Candidate direction

Treat Minecraft's small square Moon sprite as a coarse pixel canvas. Project the Earth-facing lunar surface into a 2D grid aligned to that sprite; construction changes the projected cells at its mapped lunar coordinates. Preserve occlusion/visibility rules so the Earth view shows the near-side surface, not structures on the hidden far side. Update and persist the projection when relevant lunar blocks change. Do not keep lunar chunks loaded or render every distant block individually.

For development and regression testing, add a hidden, permission-restricted command that creates an ears-and-muzzle silhouette (Mickey-like as a visual test fixture) on the near-side Moon and checks the resulting sprite. The ears must visibly extend beyond the Moon's circular outline into the surrounding sky; placing both ears entirely inside the lunar disc does not pass. Keep this test-only content out of normal gameplay and release assets.

## Open design questions

- What pixel-grid resolution and minimum visible footprint make structures legible while keeping updates inexpensive?
- How should lunar latitude/longitude and near-side visibility map to sprite cells and phases?
- How frequently should the projection update, and what is the multiplayer visibility rule?
- Is an Earth-view Moon visible only during a launch/window scene, or also from the Earth surface at night?

## Risks and validation

- This is a high-risk, signature visual feature; a small technical prototype may be needed before committing to a data model.
- Verify legibility at the Moon's apparent size, rendering cost, save/reload persistence, multiplayer synchronization, and BSL compatibility.
- Keep a non-shader rendering path; do not make the visual depend on Iris.
