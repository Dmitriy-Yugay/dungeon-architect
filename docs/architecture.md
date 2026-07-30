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

- **Domain:** grids, rooms, doors, sockets, traps, authored wave data, hero
  state, and run phases.
- **Simulation:** four-directional pathfinding, fixed-step hero movement, trap
  targeting, cooldown, and damage.
- **Presentation:** placeholder grid, placement preview, hero marker, wave
  information, objective health, and run controls.
- **Application:** the prototype screen plus controllers for starting,
  advancing, resolving, and restarting the wave.
- **Content:** JSON definitions and parsers for the prototype run, hero wave,
  and trap.

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
  footprint. Room socket maps identify at most one typed defense socket per
  local cell, and authored trap definitions declare compatible socket types.
- A placed room combines a blueprint with a grid origin and translates its
  local footprint without enforcing grid bounds; dungeon placement rules own
  bounds validation.
- Load disposable game content from JSON or another simple text format.
- Parse authored content from supplied text; the application layer owns file
  loading so content validation does not depend on libGDX global state.
- Run prototype hero movement at a fixed 60 Hz simulation step. Hero movement
  speed remains authored wave content, while presentation-facing grid position
  interpolates the remainder between simulation steps.
- Resolve trap targeting and cooldown on that same fixed step. A trap targets
  the hero occupying its socket's grid cell; hero health, trap damage, and trap
  cooldown remain authored content.
- Coordinate the prototype run in plain Kotlin. Heroes of the single authored
  type traverse the route one at a time up to the wave's configured count and
  share trap cooldown state. Arrivals apply authored objective damage; resolving
  the wave with objective health remaining is victory, while zero health is
  defeat.
- Restart resets transient wave progress, objective health, hero state, and
  trap cooldowns while preserving the player's room and trap layout.

## Near-term constraints

- Keep early room differences limited to geometry, doors, and sockets.
- Load room blueprints from authored content before adding special room rules.
- Keep selection and build-phase rules in the application or domain layers,
  not in renderers.
- Do not allow room or trap placement after a wave starts.
- Continue testing placement, routing, combat, and run outcomes without
  starting libGDX.

Central asset management, saves, additional platforms, and richer content are
deferred until the room-choice loop is proven.
