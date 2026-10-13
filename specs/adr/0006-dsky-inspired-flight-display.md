# ADR 0006: DSKY-inspired flight display

- Status: Accepted
- Phase: Elaborate
- Date: 2026-10-13

## Context

The cockpit image should read as an Apollo 11-era spacecraft, not a generic sci-fi panel. Apollo's Guidance Computer was operated through a DSKY (display and keyboard assembly), with program, verb, and noun fields, a numeric/control keypad, and status indicators. The mod currently models flight through server-authoritative gameplay rules and does not run original AGC software.

## Decision

Add a DSKY-inspired display and keypad to the cockpit panel. During flight, show a simplified program/status readout, elapsed flight time, Earth range, and progress. Keep the existing transparent circular porthole; draw the shrinking Earth and stars behind it. Treat the screen as a game visualization, not an AGC-compatible command interface or a literal recreation of the LM/CM cockpit.

## Consequences

- The cockpit has recognizable PROG / VERB / NOUN fields and keypad cues consistent with the AGC operator interface.
- Flight state remains controlled by the mod's existing difficulty and landing mechanics, not by AGC verb/noun commands.
- More authentic CM/LM layouts or interactive DSKY controls require separate research and scope decisions.
- The HUD uses the GUI atlas and vanilla rendering pipeline, preserving shader compatibility and the transparent round porthole.

## Alternatives considered

- Keep the generic analog-only panel: rejected because it omits the recognizable AGC interface.
- Recreate the full Apollo Guidance Computer and historical Luminary code: deferred as unnecessary complexity for the current gameplay slice.
