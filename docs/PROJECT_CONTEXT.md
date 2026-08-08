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
- deterministic seeds and command records before meaningful randomness is
  introduced;
- batch balance experiments after the game contains enough strategic choices
  to produce useful comparisons.

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
30. [ ] Render two room-choice controls with placeholder shapes and text.
31. [ ] Handle a room-choice click without also placing a room.
32. [ ] Reject room placement after the wave starts; test every non-building
    run phase.
33. [ ] Render door markers on placed rooms.
34. [ ] Render socket markers on placed rooms.
35. [ ] Render the currently placed trap.
36. [ ] Add the selected trap definition to the build state.
37. [ ] Resolve whether a hovered grid cell is a compatible empty socket; test
    valid, occupied, incompatible, and non-socket cells.
38. [ ] Render valid and invalid trap-placement previews.
39. [ ] Place the selected trap through a click on a compatible socket.
40. [ ] Remove automatic trap placement from prototype startup.
41. [ ] Verify with an application test that the started wave snapshots the
    player-placed trap.

After item 41, stop feature expansion and complete the persistent-dungeon
topology decision in item 42 before implementing multiple waves, rewards, or a
one-room-per-wave limit. Evaluation and observability work may follow that
decision without adding new gameplay content.

### Persistent topology and evaluation foundation — after room choice

42. [ ] Compare two or three persistent-dungeon topology rules against explicit
    criteria, run the smallest useful playable or test-fixture experiments, and
    record the selected rule in `game-design.md`.
43. [ ] Add Kotest and one focused property-based test suite for room placement
    and route invariants; record why the dependency is useful.
44. [ ] Define small, immutable simulation events for the significant actions
    already present: hero spawn, movement or arrival, trap activation, damage,
    death, objective damage, and wave resolution.
45. [ ] Emit those events from non-visual gameplay code without making domain or
    simulation classes depend on rendering or analytics infrastructure.
46. [ ] Add a structured wave evaluation report containing outcome, objective
    health, hero kills and arrivals, elapsed simulation time, trap activations,
    and trap damage.
47. [ ] Add a headless scenario evaluator that accepts an authored layout and
    wave, executes the same gameplay rules as the application, and returns the
    wave evaluation report.
48. [ ] Add determinism regression tests proving that identical scenario inputs
    produce identical reports and event sequences.
49. [ ] Compare the two authored room choices with the evaluator and document
    whether their geometry and socket placement create meaningfully different
    results; adjust authored values only through a separate reviewed task.
50. [ ] Add a presentation-only post-wave explanation model derived from the
    report, showing why the defense won or lost.

### Replayable runs and balance tooling — after multiple waves are designed

51. [ ] Introduce an injected, seeded randomness source before adding the first
    random gameplay rule; preserve deterministic behavior for a known seed.
52. [ ] Represent player decisions as recordable application commands and add a
    replay test that reconstructs the same final state from commands and seed.
53. [ ] Add a batch balance runner that evaluates authored combinations of room
    layouts, trap placements, and waves without rendering.
54. [ ] Report dominant choices, unwinnable scenarios, near-identical choices,
    and large difficulty discontinuities using explicit, documented metrics.
55. [ ] Export balance results in a stable machine-readable format for offline
    analysis and optional visualization.
56. [ ] Decide whether a separate Python analysis tool provides enough benefit
    for statistical analysis, charts, or AI-assisted balance investigation;
    document the decision before adding it.

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
