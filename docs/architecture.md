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
  information, heart health and placement state, run controls, and post-wave
  explanation values derived from evaluation reports.
- **Application:** the prototype screen plus controllers for starting,
  advancing, resolving, and restarting the wave.
- **Content:** JSON definitions and parsers for room blueprints, the prototype
  run, hero wave, and trap.

Domain objects should not depend on rendering classes. Systems update the game
state on a fixed simulation step; rendering may interpolate between steps.

## State flow

Player input becomes an application command. The simulation validates and
applies the command to domain state. Presentation reads that state and displays
the result. This keeps placement rules and combat deterministic enough for unit
tests.

Application commands are explicit controller or click-dispatch operations, and
simulation systems emit small immutable events. Events are observations of
completed gameplay facts, not a replacement for domain state and not a global
event-bus requirement. The headless evaluator composes the same controller and
simulation systems used by the playable application and summarizes their events
into evaluation reports.

This creates the current non-visual flow:

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
  the connected pair, and rejects accidental multi-door joins. Every other door
  on the newly placed room remains open, so later rooms may extend any visible
  branch. The entrance is the only fixed external directional port.
- Apply a selected quarter-turn orientation to a room's footprint, door cells
  and facings, sockets, and heart anchor before snapping or validation. The
  authored corner room combines adjacent door facings with these rotations to
  support turns and construction in every cardinal direction.
- Store the dungeon heart as immutable placement state referencing a placed
  room. Each blueprint owns a required local heart anchor; the selected heart's
  grid position is derived from that anchor, the room orientation, and its
  origin. The heart is therefore an arbitrary chosen room endpoint rather than
  an external fixed port.
- Build pathfinding neighbors from cells within the same room plus explicit
  room-door and entrance connections. Adjacent cells in different rooms are
  not traversable unless their directional doors connect. Wave-start validation
  and simulation route from the fixed entrance to the currently selected heart;
  unused branches are excluded from that route.
- Cancel removes only the newest placed room during DEFENSE_PREPARATION. It also
  removes every trap attached to that room and clears the heart if that room
  held it. Because routes, connections, and open doors are derived from
  remaining state, no separate topology cache needs repair after cancellation.
- Keep trap and heart occupancy mutually exclusive. Trap placement rejects the
  selected heart cell, and heart placement rejects a room whose transformed
  anchor contains a trap. Presentation exposes a DEFENSE_PREPARATION-only heart
  mode whose grid clicks take precedence over trap and room placement, but
  validation and mutation remain in non-rendering domain and application code.
- Load disposable game content from JSON or another simple text format.
- Parse authored content from supplied text; the application layer owns file
  loading so content validation does not depend on libGDX global state.
- Author the expanded run as an ordered list of stable wave entries. Run JSON
  owns starting Gold, per-wave Gold rewards, and the explicit completion rule;
  each entry references a wave-content path that is checked against the
  application's available wave catalog during parsing. Content validation also
  requires the maximum authored Gold envelope (starting Gold plus every reward)
  to fit in a 32-bit integer.
- Represent run cadence with explicit INTELLIGENCE, ROOM_DRAFT,
  DEFENSE_PREPARATION, COMBAT, WAVE_REPORT, RUN_VICTORY, and RUN_DEFEAT phases.
  The domain phase type owns the legal transition table: preparation advances
  in that order, every combat result passes through WAVE_REPORT, and the report
  either opens the next wave's intelligence or resolves the run. Terminal
  phases have no successors. The first authored wave begins directly in
  DEFENSE_PREPARATION to preserve the accepted demo entry point. A non-final
  successful report selects the next authored wave and enters INTELLIGENCE;
  acknowledging that briefing enters ROOM_DRAFT. Committing one compatible
  offered room advances to DEFENSE_PREPARATION. Single-wave definitions still
  advance a resolved report directly to the terminal result. Restart is a
  lifecycle reset rather than a transition within a completed run.
- Use Gold as the run's single defense resource. Trap JSON owns each defense's
  `costGold`, while run JSON owns starting Gold and each wave's `rewardGold`.
  The run controller permits purchases only during DEFENSE_PREPARATION and
  debits Gold only after the grid accepts the trap, so insufficient funds,
  occupied sockets, and repeated clicks leave both Gold and topology unchanged.
  Canceling the newest opening-build room refunds the authored costs of traps
  removed with that room, keeping the existing correction workflow affordable.
  A victorious WAVE_REPORT exposes one explicit reward claim; it may succeed
  once, and the report cannot advance until it has been claimed. Defeats never
  grant rewards, and the per-wave claim marker resets when the next authored
  wave is selected. The legacy single-wave completion path claims its reward
  before advancing directly to the terminal result.
- Author no room offer before the opening wave, then author exactly three
  distinct room blueprint IDs on every later wave entry as the offer in its
  preceding intermission. Parsing checks those IDs against the available room
  catalog. During ROOM_DRAFT, the run controller accepts only a room whose ID is
  in the active offer and whose placement passes the existing grid topology
  rules. A successful placement commits the choice, closes the draft, and
  rejects cancellation or additional room placement during defense preparation.
  The initial no-offer preparation retains unrestricted construction for the
  accepted demo. A deterministic 16 x 9 fixture verifies both authored offers:
  it begins with two connected Long Galleries and chooses a Prototype Room in
  the first intermission, leaving every offered blueprint legally attachable in
  each intended state.
- Map the run cadence onto one phase-aware primary panel control. It reviews
  intelligence, leaves ROOM_DRAFT completion to the required grid placement,
  starts prepared waves, claims a victorious report's reward before offering
  CONTINUE, resolves defeat reports, and restarts only terminal runs. The panel
  always shows heart health, current Gold, the selected defense's authored
  cost, and the active wave reward; its disabled labels explain missing routes,
  required room placement, combat waiting, or unavailable report state.
  Intermission room cards are shown in authored offer order only during
  ROOM_DRAFT. Room previews, attachment markers, rotation input, and placement
  use the controller's explicit room-placement capability, so they remain
  available for the accepted opening build but are hidden or disabled after an
  intermission commits its single room. Trap and heart interactions remain during
  that later DEFENSE_PREPARATION, and unaffordable trap hover guidance states
  the missing Gold amount.
- Keep decision-facing wave intelligence in authored wave content. Each wave
  requires a stable visible `heroRole`, concise `traitDescription`, and
  practical `defenseImplication` in addition to its visible hero name and
  count. During INTELLIGENCE, the panel's first line shows count, name, and
  role; its second line shows the trait and defense implication before the room
  draft opens. Combat-only health, heart-damage, movement-speed, spawn timing,
  and route details are not interpolated into this briefing. These fields
  explain the current baseline recruit only; additional roles and mechanics
  remain part of the later counterplay package.
- Run prototype hero movement at a fixed 60 Hz simulation step. Hero movement
  speed remains authored wave content, while presentation-facing grid position
  interpolates the remainder between simulation steps.
- Resolve trap targeting and cooldown on that same fixed step. A trap targets
  the hero occupying its socket's grid cell; hero health, trap damage, and trap
  cooldown remain authored content.
- Coordinate the prototype run in plain Kotlin. Resolve the run definition's
  ordered content references before simulation, and expose the active wave and
  its stable authored entry from the controller. Heroes traverse each wave one
  at a time up to its configured count and share trap cooldown state within that
  wave. Arrivals apply authored heart damage; resolving the final wave with
  heart health remaining is run victory, while zero health is run defeat.
- Keep the dungeon grid, placed rooms and traps, selected heart, remaining heart
  health, and run resource total as persistent run state. Advancing from a
  successful non-final wave report resets the started-wave snapshot, hero,
  runtime trap system and cooldowns, event history, evaluation report, resolved
  hero count, and elapsed-wave counters before presenting the next wave. The
  next combat snapshots the same persistent dungeon into fresh wave-local
  runtime state.
- Restart resets transient wave progress, heart health, hero state, event
  history, and trap cooldowns while preserving the player's room layout, traps,
  and exact selected heart. A restarted wave snapshots a fresh route and trap
  runtime state from that persistent dungeon.
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
  than duplicating combat or wave-resolution rules. Headless evaluation requires
  a selected, connected heart and uses that heart's route; relocating the heart
  in the same layout can therefore change the evaluated route and timing.
- Represent a complete-run evaluation as an explicit player strategy: one
  ordered action plan per authored wave, including the required room draft,
  optional heart relocation, and defense purchases. The evaluator submits those
  actions through the same controller API as the playable screen and returns
  both immutable per-wave reports and a summed run report. Invalid cadence or
  purchases fail at the action that violates the authored rules, while defeat
  returns the reports resolved so far instead of simulating later waves.
- Capture two deliberately different reset boundaries. Starting combat records
  only pre-wave health and Gold. A retry restores those values, retains the
  current wave index, committed offer choice, rooms, selected heart, and traps,
  and rebuilds transient simulation state. Starting a new run instead restores
  the controller's initial dungeon snapshot, first wave, maximum health, and
  authored starting Gold. Both operations discard heroes, cooldowns, events,
  reports, and reward-claim state.

The full application flow is covered without starting libGDX rendering. The
test drives the same click dispatcher and control bounds as the prototype to
select authored rooms, rotate, place, cancel, select the heart, place a trap,
start, simulate, resolve, and restart. Its checked-in scenario turns through a
corner into a rotated gallery, proves canceled-room trap cleanup, and verifies
that the selected-heart route crosses the surviving trap before deterministic
victory.

A second application-level test executes the complete authored cadence without
rendering: opening construction and purchases, three combats, both report and
reward handshakes, both intelligence briefings, two offered-room commitments,
Heart relocation into each extension, and final run victory. It loads the same
three distinct wave files as the desktop screen, so content-path wiring and the
16/24/32-health progression are checked together with the interaction flow.

## Near-term constraints

- Keep early room differences limited to geometry, doors, sockets, and heart
  anchors.
- Load room blueprints from authored content before adding special room rules.
- Keep selection and build-phase rules in the application or domain layers,
  not in renderers.
- Do not allow room rotation, cancellation, heart placement, room placement, or
  trap placement after a wave starts.
- Continue testing placement, routing, combat, and run outcomes without
  starting libGDX.

Only the entrance-to-heart branch has current combat meaning. Off-route rooms
and traps persist as future construction capacity but do not attract heroes,
split the wave, grant bonuses, or contribute to evaluation. The prototype also
lacks an authored three-door junction, so branch invariants are covered in the
domain while the checked-in room catalog currently builds straight and turned
chains. The controller now advances through ordered authored waves, retains the
starting resource total, and enforces authored one-room intermission drafts.
Reward grants, defense spending, and the playable intermission interface remain
absent.

Central asset management, saves, additional platforms, and richer content are
deferred until their workflows justify the added infrastructure.

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

- **Simulation-event export:** the runtime already uses small Kotlin event
  values. Add persistent logging or telemetry only when in-memory reports and
  tests no longer satisfy the diagnostic need.
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
