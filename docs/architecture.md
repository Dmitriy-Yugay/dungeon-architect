# Architecture

## Goals

- Keep game rules testable without rendering.
- Separate simulation, presentation, and platform startup.
- Prefer simple composition over a large framework or premature ECS.
- Make content data-driven where practical.

## Planned project shape

Use the standard Gradle-based libGDX split:

- `core`: shared game logic, screens, rendering, input, and assets;
- `lwjgl3`: desktop launcher and desktop-specific configuration.

Additional platform modules should be added only when needed.

## Core boundaries

- **Domain:** dungeon grid, entities, stats, resources, waves, and rules.
- **Simulation:** pathfinding, targeting, combat, movement, and wave updates.
- **Presentation:** libGDX rendering, animation, audio, camera, and UI.
- **Application:** screens, game-state transitions, input mapping, and saves.
- **Content:** external definitions for defenses, heroes, and waves.

Domain objects should not depend on rendering classes. Systems update the game
state on a fixed simulation step; rendering may interpolate between steps.

## State flow

Player input becomes an application command. The simulation validates and
applies the command to domain state. Presentation reads that state and displays
the result. This keeps placement rules and combat deterministic enough for unit
tests.

## Early technical decisions

- Use a tile grid and a straightforward pathfinding algorithm.
- Load disposable game content from JSON or another simple text format.
- Manage asset lifetimes centrally through libGDX `AssetManager`.
- Save versioned data rather than serialized runtime objects.
- Add unit tests for placement validity, routing, combat, and wave completion.

Final package names and detailed APIs should be chosen during the first
prototype, when their responsibilities are concrete.
