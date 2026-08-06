# Dungeon Architect

A planned dungeon-builder tower defense game built with Kotlin and libGDX.
The player designs a dungeon, places defenses, and survives waves of invading
heroes.

## Status

The first end-to-end prototype is complete. The desktop application supports
connected room placement, route validation, an authored hero wave, deterministic
trap combat, objective damage, victory, defeat, and restart. Visuals remain
placeholder shapes.

## Prototype controls

- Move the pointer over the grid to preview the prototype room.
- Click a green preview to place the room; red previews are invalid.
- Connect rooms from the entrance to the objective to enable the wave.
- Click **START WAVE** to run the authored wave.
- Click **RESTART** after victory or defeat.

The current trap is installed automatically in the starter room.

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
