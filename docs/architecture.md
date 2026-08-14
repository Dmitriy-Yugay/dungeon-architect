# Architecture

## Goals

- Keep game rules testable without rendering.
- Separate simulation, presentation, and platform startup.
- Prefer simple composition over a large framework or premature ECS.
- Make content data-driven where practical.
- Make important simulation outcomes observable and explainable.
- Adopt useful frameworks and tools when their benefit is clearer than the
  maintenance cost of a custom alternative.

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
- **Evaluation:** immutable, non-visual summaries of resolved simulation
  outcomes and metrics.
- **Presentation:** placeholder grid, placement preview, hero marker, wave
  information, objective health, run controls, and post-wave explanation values
  derived from evaluation reports.
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

As the prototype grows, application commands should become recordable and
simulation systems should emit small immutable events. Events are observations
of completed gameplay facts, not a replacement for domain state and not a
global event-bus requirement. The headless evaluator composes the same
controller and simulation systems used by the playable application and
summarizes their events into evaluation reports.

This creates the intended future flow:

`authored scenario + commands + seed -> deterministic simulation -> events -> evaluation report -> presentation or balance analysis`

## Early technical decisions

- Use a tile grid and a straightforward pathfinding algorithm.
- Represent room footprints as normalized, four-directionally connected sets
  of local grid positions. Each door identifies an exposed footprint cell and
  a cardinal facing. Room socket maps identify at most one typed defense socket
  per local cell, and authored trap definitions declare compatible socket types.
- A placed room combines a blueprint with a grid origin and translates its
  local footprint without enforcing grid bounds; dungeon placement rules own
  bounds validation.
- Grow the persistent dungeon through an open-door frontier. Placement snaps a
  compatible candidate door to exactly one unused placed-room door, consumes
  the connected pair, and rejects accidental multi-door joins. Entrance and
  objective cells are directional ports rather than room floor.
- Build pathfinding neighbors from cells within the same room plus explicit
  room-door and endpoint connections. Adjacent cells in different rooms are not
  traversable unless their directional doors connect.
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
- Represent significant simulation facts as small immutable event values using
  stable scalar identifiers and state snapshots. Record hero arrival rather
  than every fixed-step movement update so evaluation remains meaningful and
  compact. Event types do not contain rendering, analytics, or mutable runtime
  objects.
- Emit simulation events through a caller-supplied function. The prototype run
  controller records their deterministic order and exposes defensive snapshots;
  restart clears that transient history. This keeps gameplay systems independent
  of rendering, analytics infrastructure, and a global event bus.
- Evaluate authored layouts headlessly by driving the prototype run controller
  at its fixed simulation step. Derive report totals from the resulting event
  sequence and count exact completed steps for elapsed simulation time rather
  than duplicating combat or wave-resolution rules.

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

## Adopted development dependencies

- **Kotest property testing:** adopted as a test-only dependency for generated
  gameplay invariants. Its generators and shrinking cover many valid room-chain
  dimensions while preserving useful minimal counterexamples, giving placement
  and routing rules broader coverage than a growing table of hand-written
  examples. The JUnit 5 runner keeps these focused property specs compatible
  with the project's existing Gradle test task and does not enter production
  gameplay code.

## Technology candidates and adoption triggers

These technologies are recorded so they can be reconsidered when the project
has the corresponding problem. Listing one is not a commitment to adopt it.

### Near-term candidates

- **Structured simulation events:** implement as small Kotlin types first. Add a
  logging or telemetry framework only when local reports and tests no longer
  satisfy the diagnostic need.
- **JSON Schema or stronger serialization validation:** reconsider when the
  authored content surface becomes large enough that editor validation,
  migrations, or cross-file tooling would materially improve iteration.

### Later gameplay and generation candidates

- **Finite-state machines:** suitable for a few explicit hero modes.
- **Utility AI:** suitable when heroes choose among competing goals and the
  scores should remain visible to designers and players.
- **Behavior trees:** suitable for reusable, hierarchical, designer-authored
  behaviors. Avoid adopting a framework before such behaviors exist.
- **Constraint solving:** begin with Kotlin search or backtracking. Reconsider a
  JVM solver such as Choco Solver when generation has numerous interacting
  constraints and custom search becomes difficult to maintain.
- **Seeded procedural generation:** useful only with reproducible seeds,
  solvability validation, and evaluation metrics.

### Development and analysis candidates

- **Python balance-analysis sidecar:** potentially useful once the headless
  evaluator can export stable results. It may provide statistics,
  visualizations, experiment orchestration, or AI-assisted investigation
  without introducing Python into the game runtime.
- **OpenTelemetry or an observability platform:** reconsider if local event logs
  are insufficient for playtest builds or production diagnostics. Do not make
  the deterministic simulation depend directly on a vendor SDK.
- **LLM tool-calling or an agent framework such as LangGraph:** potentially
  useful for an offline content or balance assistant that invokes validators
  and the headless evaluator. All proposed changes must remain reviewable and
  pass deterministic checks.
- **RAG, embeddings, and a vector store:** reconsider only when design documents
  and content are numerous enough that ordinary repository search is no longer
  effective. They are unnecessary for the current project size.
- **Natural-language content authoring:** may draft structured rooms, waves, or
  flavor text, but generated output must pass parsers, cross-reference checks,
  simulation evaluation, and human review.

### Explicitly deferred

- Runtime LLM-controlled heroes, multi-agent NPC frameworks, and online model
  calls in combat are deferred because they conflict with deterministic,
  readable outcomes and create availability, latency, cost, and testing risks.
- An ECS remains deferred until the number and interaction of gameplay entities
  demonstrate a concrete need.
- A large telemetry, asset, or procedural-generation platform remains deferred
  until the corresponding workflow exists and can justify it.

When adopting a significant tool, record the problem, considered alternatives,
chosen scope, and removal or migration cost in this document or a focused
architecture decision record.
