# Peterwolf's Apollo 11

A Fabric mod for Minecraft 26.3, focused on an Apollo-inspired mission from Earth to the Moon.

## Development setup

- Java 25
- Gradle Wrapper 9.5.1
- Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Fabric Loom 1.17.21

On macOS with Homebrew's `openjdk@25`, set `JAVA_HOME` before running Gradle:

```sh
export JAVA_HOME="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
./gradlew build
./gradlew runClient
```

Minecraft 26.3's client jar already contains Mojang-named classes and its version manifest has no Mojang mapping downloads. The build therefore uses Fabric's empty `intermediary:0.0.0` mapping artifact and keeps Fabric API on the regular runtime/compile classpath rather than asking Loom to remap its sources. The dev client has been smoke-tested with Fabric API 0.162.0+26.3.

## Tests

Run the client GameTests with:

```sh
./gradlew runClientGameTest
```

The test covers item and recipe resources, the placeable gantry, `/apollo launch`, landing difficulty and retrograde burn, the five-second flight HUD, Earth and cockpit GUI sprites, Moon transfer, seed-stable varied-size craters, a clear landing zone, and reduced gravity.

## Current milestone

The first crafting slice adds a rocket frame, engine, and Apollo Guidance Computer, then recipes for the Saturn V rocket and a placeable launch gantry. Use the Apollo rocket on the gantry to consume it and start a five-second countdown with smoke, flame, and launch sound. Alternatively, hold the rocket and run `/apollo launch` within four blocks of a gantry. The initiating player sees a five-second cockpit interior with instruments and a circular porthole; the shrinking Earth sprite and stars appear only through its transparent round opening. A DSKY-inspired PROG/VERB/NOUN display and keypad visualize flight status, but do not emulate the AGC. The Moon's crater field is deterministic per world seed, has varied crater sizes, and leaves the landing zone clear. New Moon chunks generate their craters independently; existing chunks are not retroactively changed. Lunar gravity is one-sixth of normal. `/apollo difficulty [easy|mid|professional]` selects a per-player mode for the current server session. EASY lands automatically; MID offers `/apollo landing burn` as an optional input with automatic correction; PROFESSIONAL requires `/apollo landing burn` or safely aborts to the launch position. The astronaut suit, rover, and alien encounters remain future slices. The ISS remains deferred; shader packs are optional and are not bundled.
