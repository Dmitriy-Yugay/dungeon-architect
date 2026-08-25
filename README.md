# Dungeon Architect

A planned dungeon-builder tower defense game built with Kotlin and libGDX.
The player designs a dungeon, places defenses, and survives waves of invading
heroes.

## Status

The flexible-construction prototype is complete. The desktop application
supports room choice, persistent attachment previews, snapped and rotated room
placement, build-phase undo, a player-placed dungeon heart, route-aware trap
placement, an authored hero wave, deterministic combat and evaluation, victory,
defeat, and layout-preserving restart. Hero health, combat events, and the final
wave result are visible in the playable screen. Visuals remain placeholder
shapes.

## Prototype controls

- Choose one of the three room cards in the bottom panel. Each card previews its
  footprint, doors, sockets, and heart anchor.
- Click the entrance or a highlighted open door to choose an attachment target.
  An exact room ghost remains visible at the active target.
- Rotate the ghost with **Q**/**E** or the **< Q**/**E >** controls. Green
  previews can be placed; red previews show why that orientation is invalid.
- Click the room ghost to place it.
- Click **CANCEL** to undo the most recently placed room during the build phase.
- Click two well-spaced, on-route sockets to place spike traps. The authored
  wave is balanced so one trap is insufficient and deliberate coverage wins.
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
