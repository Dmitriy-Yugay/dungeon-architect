# Dungeon Architect

A planned dungeon-builder tower defense game built with Kotlin and libGDX.
The player designs a dungeon, places defenses, and survives waves of invading
heroes.

## Status

The flexible-construction prototype now runs an authored three-wave loop. The
desktop application supports an opening build, intermission intelligence and
room drafts, persistent dungeon state, Gold-priced defenses, deterministic
combat, causal wave reports and rewards, victory, defeat, wave retry, and a
distinct new-run reset. Visuals remain placeholder shapes.

## Prototype controls

- Choose one of the three room cards in the bottom panel. Each card previews its
  footprint, doors, sockets, and heart anchor.
- Click the entrance or a highlighted open door to choose an attachment target.
  An exact room ghost remains visible at the active target.
- Rotate the ghost with **Q**/**E** or the **< Q**/**E >** controls. Green
  previews can be placed; red previews show why that orientation is invalid.
- Click the room ghost to place it.
- Click **CANCEL** to undo the most recently placed room during the build phase.
- Build two well-spaced, on-route Spike Traps for the opening wave. Each later
  wave survives the existing route, so spend its predecessor's one-Gold reward
  to arm the drafted extension and move the Heart behind it.
- Click **PLACE HEART**, then a placed room, to select or relocate the dungeon
  heart.
- Connect the entrance to the heart to enable **START WAVE**.
- The panel always shows current heart health and **Gold**, the selected trap's
  cost, and the upcoming wave reward. If a hovered trap cannot be afforded, the
  panel states how much more Gold is needed.
- After each non-final wave, click **CLAIM +N GOLD**, then **CONTINUE**. Review
  the next-wave count, visible role, traits, and defense implication, then click
  **REVIEW ROOM OFFER**, choose one of the three authored room cards, and place
  that room. Room construction locks after this single placement, while trap
  and heart preparation remain available.
- After a defeat report, click **END RUN**.
- Click **RETRY WAVE** after victory or defeat to preserve the current layout,
  heart, traps, and prepared resources, or **NEW RUN** to restore the initial
  dungeon and authored starting values.

### Accepted demo route

The focused desktop playtest uses a Long Gallery from the entrance, a Corner
Room at its open door, and a Prototype Room rotated left with **Q** at the
corner's north door. Place the heart in the final room and arm the on-route
sockets in the Long Gallery and Corner Room. Two separated spike traps defeat
all four recruits; one trap remains insufficient. Restart preserves this exact
layout, its traps, and the selected heart.

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
- [Competitive gameplay plan](docs/competitive-gameplay-plan.md)

## Technology

- Kotlin
- libGDX
- Gradle
