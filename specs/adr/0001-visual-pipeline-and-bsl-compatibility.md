# ADR 0001: Visual pipeline and BSL shader compatibility

- Status: Proposed
- Phase: Elaborate
- Date: 2026-10-09

## Context

The user identifies visual quality as a primary differentiator. The mod needs original models for a launch site, Saturn-V-inspired rocket, lunar lander, spacesuit, rover, aliens, and later an ISS-like station. The user also requires BSL Shaders support, but confirmed the mod must remain playable without shaders. BSL is a separate shader pack; Iris is the relevant shader-loader compatibility path.

## Decision proposal

1. Keep vanilla rendering as the baseline and ensure every feature works with shaders disabled.
2. Target optional use of Iris + player-installed BSL. Do not bundle or redistribute the shader pack.
3. Prefer Minecraft/Fabric rendering paths that cooperate with shader loaders. Avoid a custom GLSL/shader pipeline unless a concrete visual requirement cannot be met otherwise.
4. Author original, Minecraft-scale models and textures from the eight user screenshots. Treat reference screenshots as inspiration, not redistributable assets.
5. Validate key visuals both with shaders disabled and with Iris + BSL. Verify actual Minecraft 26.3 support before making a compatibility claim or release.

## Alternatives considered

- Require Iris + BSL: rejected because the user wants the mod playable without shaders.
- Bundle BSL: rejected because it is a separately distributed shader pack and is not a mod dependency.
- Build a bespoke shader stack: deferred because it adds incompatibility risk and is not needed to define the visual goal.

## Consequences

- Two rendering modes are part of acceptance testing: vanilla and Iris + BSL.
- Shader compatibility is an empirical release gate, not something guaranteed by design alone.
- Earth, Moon, structure silhouettes, custom entities, and particles need checks in both modes.
- The final modeling/export pipeline remains to be selected during implementation planning.

## Evidence

- Iris official site: https://irisshaders.dev/ — describes Iris as an open-source shader mod for Minecraft Java Edition and says it supports OptiFine shader packs.
- User visual references are in the project root; the Earth image is `Screenshot 2026-10-09 at 19.44.58.png`.
