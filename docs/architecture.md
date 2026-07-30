# Architecture

## Goals

- Keep game rules testable without rendering.
- Separate simulation, presentation, and platform startup.
- Prefer simple composition over a large framework or premature ECS.
- Make content data-driven where practical.

## Current project shape

The repository uses the standard Gradle-based libGDX split:

- `core`: shared game logic, screens, rendering, input, and tests;
- `lwjgl3`: desktop launcher and desktop-specific configuration.

Additional platform modules should be added only when needed.

## Core boundaries

- **Domain:** currently contains the grid and tile model; it will also own
  entities, stats, resources, waves, and rules.
- **Simulation:** will contain pathfinding, targeting, combat, movement, and
  wave updates.
- **Presentation:** currently contains grid rendering; it will also contain
  animation, audio, camera, and UI.
- **Application:** currently contains the prototype screen and pointer input;
  it will coordinate commands and game-state transitions.
- **Content:** will contain external definitions for defenses, heroes, and
  waves.

Domain objects should not depend on rendering classes. Systems update the game
state on a fixed simulation step; rendering may interpolate between steps.

## State flow

Player input becomes an application command. The simulation validates and
applies the command to domain state. Presentation reads that state and displays
the result. This keeps placement rules and combat deterministic enough for unit
tests.

## Early technical decisions

- Use a tile grid and a straightforward pathfinding algorithm.
- Represent room footprints as normalized, four-directionally connected sets
  of local grid positions. Door positions identify exposed cells within that
  footprint.
- Load disposable game content from JSON or another simple text format.
- Manage asset lifetimes centrally through libGDX `AssetManager`.
- Save versioned data rather than serialized runtime objects.
- Add unit tests for placement validity, routing, combat, and wave completion.

Final package names and detailed APIs should be chosen during the first
prototype, when their responsibilities are concrete.
