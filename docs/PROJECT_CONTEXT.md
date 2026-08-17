# Project Context

## Vision

Dungeon Architect is a dungeon-builder tower defense game. Instead of invading
a dungeon, the player creates one: shaping routes and placing defenses to stop
increasingly dangerous parties of heroes.

## Product pillars

- **Meaningful construction:** layout decisions change hero movement and
  defensive opportunities.
- **Readable defense:** the player can understand why a defense succeeded or
  failed.
- **Strategic variety:** different layouts and defense combinations support
  multiple solutions.
- **Fast iteration:** rebuilding after a wave should be quick and rewarding.

## Initial scope

The first playable version should include:

- a grid-based dungeon with an entrance and a protected objective;
- placeable paths, rooms, and a small set of defenses;
- heroes that navigate the dungeon and attack the objective;
- waves, resources, win/loss states, and a basic build/combat cycle;
- desktop support with placeholder visuals and audio.

## Out of scope for the first version

Multiplayer, procedural campaigns, advanced progression, mod support, and
additional platforms are deferred until the core loop is proven.

## Current state

The repository contains a runnable Kotlin and libGDX desktop project split into
`core` and `lwjgl3` modules. The current prototype supports room placement,
routing, an authored hero wave, a socketed trap, deterministic combat, objective
damage, visible outcomes, and restart. Visuals remain placeholders, and audio
has not been implemented.

## Technology

Language:
Kotlin

Engine:
libGDX

Platform:
Steam

## Strategic technical direction

The game should remain deterministic, inspectable, and testable without
rendering. Its most useful agent-inspired architecture is not an LLM inside the
gameplay loop. It is a closed, verifiable workflow:

1. represent game state and authored content in structured forms;
2. expose constrained player and simulation actions;
3. execute those actions through deterministic gameplay systems;
4. record observable events and outcomes;
5. evaluate layouts, waves, and balance automatically;
6. keep design and content changes subject to human review.

This direction supports the product pillars directly. It makes defense results
easier to explain, enables rapid balance experiments, and leaves room for
development-time AI tools without making core gameplay dependent on an online
model.

Near-term technical priorities are:

- validated, data-driven room and defense content;
- property-based tests for placement, routing, and simulation invariants;
- structured simulation events and post-wave metrics;
- a headless evaluator that uses the same rules as the playable game;
- reversible room placement during the build phase;
- room rotation and authored turns so the dungeon can grow in every cardinal
  direction;
- a player-selected dungeon heart that replaces the fixed objective endpoint.

## Completed milestone

The first end-to-end desktop loop is complete. The player can:

- build a valid route from the entrance to the objective;
- start a wave containing one hero type;
- use one socketed defense type that can damage the hero;
- win, lose, and restart with clear visual feedback.

## Current milestone

Prove meaningful room choice without introducing special room effects. The
milestone is complete when the player can:

- choose between two simple, authored room blueprints;
- preview and place the selected room only during the build phase;
- see doors, sockets, and placed traps;
- place the basic trap into a compatible empty socket;
- complete the existing wave with either room layout.

## Implementation plan

Before changing code, read `AGENTS.md` and these documents, inspect the Git
status, preserve unrelated changes, and run `./gradlew test`.

Treat every numbered item as a separate task.

### Foundation — complete

1. [x] Add an immutable room blueprint with a footprint and door positions; test
   its validation.
2. [x] Add a placed-room value with an origin; test conversion from local room
   cells to grid positions.
3. [x] Check whether a room fits inside the grid; test boundary cases.
4. [x] Reject overlap with an existing room; test touching and overlapping rooms.
5. [x] Validate a connection between compatible adjacent doors.
6. [x] Place one hard-coded room in the domain model.
7. [x] Render placed room cells with placeholder colors.
8. [x] Show a valid or invalid placement preview under the pointer.
9. [x] Commit a valid room placement on click.
10. [x] Produce walkable grid positions from placed rooms.
11. [x] Find a four-directional path through those positions; test success and
    failure.
12. [x] Require a route between the entrance and objective.
13. [x] Add data describing one simple upcoming hero wave.
14. [x] Display the wave description and provide a start control.
15. [x] Move one plain Kotlin hero along a route using a fixed simulation step;
    test movement and arrival.
16. [x] Render the hero.
17. [x] Add one room socket and one compatible trap definition.
18. [x] Place the trap in the socket.
19. [x] Apply deterministic trap damage; test targeting, cooldown, and death.
20. [x] Add objective damage, victory, defeat, and restart.

### Room choice — next

21. [x] Add a stable ID and display name to a room blueprint; test blank-value
    validation.
22. [x] Parse one room blueprint from supplied JSON text; test required fields,
    positions, doors, and sockets.
23. [x] Add authored JSON for the existing room and load it instead of
    constructing it in `PrototypeScreen`.
24. [x] Add a second authored room with different geometry or socket placement,
    but no special effect.
25. [x] Add build state containing the available room blueprints and the
    currently selected blueprint.
26. [x] Select an available blueprint in that build state; test valid and
    unknown selections.
27. [x] Make placement previews use the selected blueprint.
28. [x] Make committed room placements use the selected blueprint.
29. [x] Add a presentation-only view model for the available room choices and
    selected state.
30. [x] Render two room-choice controls with placeholder shapes and text.
31. [x] Handle a room-choice click without also placing a room.
32. [x] Reject room placement after the wave starts; test every non-building
    run phase.
33. [x] Render door markers on placed rooms.
34. [x] Render socket markers on placed rooms.
35. [x] Render the currently placed trap.
36. [x] Add the selected trap definition to the build state.
37. [x] Resolve whether a hovered grid cell is a compatible empty socket; test
    valid, occupied, incompatible, and non-socket cells.
38. [x] Render valid and invalid trap-placement previews.
39. [x] Place the selected trap through a click on a compatible socket.
40. [x] Remove automatic trap placement from prototype startup.
41. [x] Verify with an application test that the started wave snapshots the
    player-placed trap.

After item 41, stop feature expansion and complete the persistent-dungeon
topology decision in item 42 before implementing multiple waves, rewards, or a
one-room-per-wave limit. Evaluation and observability work may follow that
decision without adding new gameplay content.

### Persistent topology and evaluation foundation — after room choice

42. [x] Compare two or three persistent-dungeon topology rules against explicit
    criteria, run the smallest useful playable or test-fixture experiments, and
    record the selected rule in `game-design.md`.
43. [x] Add Kotest and one focused property-based test suite for room placement
    and route invariants; record why the dependency is useful.
44. [x] Define small, immutable simulation events for the significant actions
    already present: hero spawn, movement or arrival, trap activation, damage,
    death, objective damage, and wave resolution.
45. [x] Emit those events from non-visual gameplay code without making domain or
    simulation classes depend on rendering or analytics infrastructure.
46. [x] Add a structured wave evaluation report containing outcome, objective
    health, hero kills and arrivals, elapsed simulation time, trap activations,
    and trap damage.
47. [x] Add a headless scenario evaluator that accepts an authored layout and
    wave, executes the same gameplay rules as the application, and returns the
    wave evaluation report.
48. [x] Add determinism regression tests proving that identical scenario inputs
    produce identical reports and event sequences.
49. [x] Compare the two authored room choices with the evaluator and document
    whether their geometry and socket placement create meaningfully different
    results; adjust authored values only through a separate reviewed task.
50. [x] Add a presentation-only post-wave explanation model derived from the
    report, showing why the defense won or lost.

### Flexible construction and selectable dungeon heart — next

For this milestone, **Cancel** means undoing the most recently placed room while
the run is still in the build phase. Repeated cancellation can walk back to an
earlier mistake without introducing arbitrary mid-layout demolition rules. The
operation also removes traps attached to that room. Once heart placement is
available, canceling its room also returns the heart to the unplaced state.

Room placement will support explicit quarter-turn rotation. Rotation alone is
not enough to bend the route because both current blueprints have two opposite
doors, so the milestone also adds a simple authored corner room. Together with
the existing open-door frontier, this permits construction in every cardinal
direction while keeping every connection visible and deterministic.

The dungeon heart will replace the fixed objective endpoint. Each room
blueprint will declare a valid local heart anchor, and the player may place or
relocate the heart in any placed room during the build phase. A wave may start
only when the entrance has a valid door-connected route to the heart. The heart
therefore lets the player choose which branch contains the active hero route
without adding destination-choice AI. Unused side branches remain future build
capacity rather than providing an immediate combat benefit in this milestone.

51. [x] Add a build-phase operation that cancels the most recently placed room,
    removes traps belonging to it, and leaves the grid, open-door frontier, and
    route derived state consistent; test empty, single-room, multi-room, trapped,
    and non-building cases.
52. [x] Start the prototype with an empty player-built layout and add an enabled
    or disabled Cancel control that consumes its click without selecting or
    placing anything else; cover the control view, click handling, and repeated
    cancellation with presentation and application tests.
53. [x] Add an immutable quarter-turn room orientation and transform footprint
    cells, door positions and facings, and socket positions consistently; test
    all four orientations and full-turn identity.
54. [x] Make placed rooms retain their orientation, and make snapping,
    placement validation, door connections, trap sockets, rendering markers,
    and route traversal use the transformed room geometry; add focused domain
    regression tests.
55. [x] Add clockwise and counter-clockwise rotation controls to build state and
    the build UI, update the live preview immediately, and prevent either
    control from also committing a placement; test input behavior in every run
    phase.
56. [x] Add one simple authored corner-room blueprint with adjacent doors and no
    special effect, expose it as a room choice, and verify that its rotations
    can extend each compatible open frontier door without overlap.
57. [x] Add property-based topology tests that generate legal paths containing
    horizontal and vertical segments, turns, and branches, proving that every
    recorded connection is cardinally adjacent and that entrance routes remain
    valid after placement and cancellation.
58. [ ] Add a required local heart anchor to room-blueprint content and validate
    that it occupies a footprint cell; update the JSON parser, authored rooms,
    rotation transformation, and parser/domain tests.
59. [ ] Add immutable placed-heart state and build-phase operations to place or
    relocate it to any placed room, reject invalid targets, and unplace it when
    its room is canceled; test occupancy and phase rules without rendering.
60. [ ] Replace the fixed objective port in routing and wave-start validation
    with the placed heart, while keeping the entrance fixed; test missing-heart,
    disconnected-heart, connected-heart, relocated-heart, and branched-layout
    routes.
61. [ ] Add a heart-placement control, valid and invalid room hover feedback,
    and a distinct heart marker; ensure heart placement takes precedence over
    room and trap placement clicks, with presentation and application tests.
62. [ ] Update objective damage, restart, authored scenario fixtures, headless
    evaluation, reports, and determinism tests to use the selected heart without
    making simulation or domain code depend on rendering.
63. [ ] Add an end-to-end application test for building a turned or branched
    dungeon, canceling and replacing a mistaken room, selecting the heart room,
    placing a trap, and completing the wave; then update `game-design.md` and
    `architecture.md` with the verified behavior and remaining constraints.

For every task:

- implement only the first incomplete item unless asked for a wider scope;
- avoid adding abstractions needed only by later items;
- keep gameplay rules independent of libGDX rendering;
- add tests for new non-visual behavior;
- use property-based tests when invariants are more important than individual
  examples;
- justify new frameworks or tools by the concrete problem they solve;
- run `./gradlew test` and `./gradlew build`;
- update the documents when a design or architecture decision changes.

Do not add special room effects, advanced hero roles, an ECS, saves, additional
platforms, or a large asset pipeline during this milestone.

When subagents are used, the primary Codex agent owns Git and the plan. Start
one implementation worker for one unchecked item, review its diff, run
verification, and mark the item complete only after it passes. Workers must not
commit or push. The primary agent creates one focused commit, pushes it to the
current feature branch, and only then starts the next item. Stop if the push
fails or the working tree contains unrelated changes.
