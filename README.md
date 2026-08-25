# Dungeon Architect

A planned dungeon-builder tower defense game built with Kotlin and libGDX.
The player designs a dungeon, places defenses, and survives waves of invading
heroes.

## Status

The flexible-construction prototype is complete. The desktop application
supports room choice, snapped and rotated room placement, build-phase undo, a
player-placed dungeon heart, route-aware trap placement, an authored hero wave,
deterministic combat and evaluation, victory, defeat, and layout-preserving
restart. Visuals remain placeholder shapes.

## Prototype controls

- Choose one of the three room blueprints in the bottom panel.
- Use **CCW** or **CW**, then move the pointer over a snapped grid location to
  preview the selected orientation.
- Click a green room preview to place it; red previews are invalid.
- Click **CANCEL** to undo the most recently placed room during the build phase.
- Click an empty compatible socket to place the spike trap.
- Click **PLACE HEART**, then a placed room, to select or relocate the dungeon
  heart.
- Connect the entrance to the heart to enable **START WAVE**.
- Click **RESTART** after victory or defeat; the built layout is preserved.

## Requirements

- JDK 21

## Run

```shell
./gradlew lwjgl3:run
```

On macOS, use the Gradle run task so the LWJGL process starts on the first
thread.

## Build

```shell
./gradlew build
```

## Modules

- `core`: shared domain, simulation, application, presentation, and tests.
- `lwjgl3`: desktop launcher and LWJGL3-specific window configuration.
- `assets`: JSON definitions for the prototype run, wave, and trap.

## Documentation

- [Project context](docs/PROJECT_CONTEXT.md)
- [Game design](docs/game-design.md)
- [Architecture](docs/architecture.md)

## Technology

- Kotlin
- libGDX
- Gradle
