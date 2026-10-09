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

The test covers item and recipe resources, placeable gantry registration, the five-second launch countdown, and transfer to the Moon dimension's safe landing surface.

## Current milestone

The first crafting slice adds a rocket frame, engine, and Apollo Guidance Computer, then recipes for the Saturn V rocket and a placeable launch gantry. Use the Apollo rocket on the gantry to consume it and start a five-second countdown with smoke, flame, and launch sound. Liftoff transfers the initiating player to a simple gray Moon test dimension. The Earth-through-window transition, cratered terrain, reduced gravity, navigation computer, suit, rover, and alien encounters remain future slices. The ISS remains deferred; shader packs are optional and are not bundled.
