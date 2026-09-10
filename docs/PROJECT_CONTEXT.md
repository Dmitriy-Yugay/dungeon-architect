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

- a grid-based dungeon with an entrance and a protected dungeon heart;
- placeable paths, rooms, and a small set of defenses;
- heroes that navigate the dungeon and attack the objective;
- waves, resources, win/loss states, and a basic build/combat cycle;
- desktop support with placeholder visuals and audio.

## Out of scope for the first version

Multiplayer, procedural campaigns, advanced progression, mod support, and
additional platforms are deferred until the core loop is proven.

## Current state

The repository contains a runnable Kotlin and libGDX desktop project split into
`core` and `lwjgl3` modules. The current prototype supports cancellable and
rotatable room placement, straight and turned authored rooms, open-frontier
topology, a player-selected dungeon heart, route-aware trap placement, an
authored hero wave, deterministic combat and evaluation, visible outcomes, and
layout-preserving restart. Visuals remain placeholders, and audio has not been
implemented.

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

Recent technical foundations now implemented are:

- validated, data-driven room and defense content;
- property-based tests for placement, routing, and simulation invariants;
- structured simulation events and post-wave metrics;
- a headless evaluator that uses the same rules as the playable game;
- reversible room placement during the build phase;
- room rotation and authored turns so the dungeon can grow in every cardinal
  direction;
- a player-selected dungeon heart that replaces the fixed objective endpoint.

## Foundation milestone — complete

The first end-to-end desktop loop is complete. The player can:

- build a valid route from the entrance to the dungeon heart;
- start a wave containing one hero type;
- use one socketed defense type that can damage the hero;
- win, lose, and restart with clear visual feedback.

## Flexible construction milestone — complete

The player can now build a non-linear-direction dungeon without introducing
special room effects. The completed milestone allows the player to:

- choose among three simple authored room blueprints, including a corner;
- rotate, preview, place, and cancel rooms only during the build phase;
- extend any compatible open frontier door and preserve explicit connections;
- place or relocate the dungeon heart in a chosen room;
- place the basic trap into a compatible empty socket outside the heart cell;
- complete and evaluate the existing wave along the entrance-to-heart route;
- restart while preserving the rooms, traps, and selected heart.

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

### Room choice — complete

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

### Flexible construction and selectable dungeon heart — complete

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
58. [x] Add a required local heart anchor to room-blueprint content and validate
    that it occupies a footprint cell; update the JSON parser, authored rooms,
    rotation transformation, and parser/domain tests.
59. [x] Add immutable placed-heart state and build-phase operations to place or
    relocate it to any placed room, reject invalid targets, and unplace it when
    its room is canceled; test occupancy and phase rules without rendering.
60. [x] Replace the fixed objective port in routing and wave-start validation
    with the placed heart, while keeping the entrance fixed; test missing-heart,
    disconnected-heart, connected-heart, relocated-heart, and branched-layout
    routes.
61. [x] Add a heart-placement control, valid and invalid room hover feedback,
    and a distinct heart marker; ensure heart placement takes precedence over
    room and trap placement clicks, with presentation and application tests.
62. [x] Update objective damage, restart, authored scenario fixtures, headless
    evaluation, reports, and determinism tests to use the selected heart without
    making simulation or domain code depend on rendering.
63. [x] Add an end-to-end application test for building a turned or branched
    dungeon, canceling and replacing a mistaken room, selecting the heart room,
    placing a trap, and completing the wave; then update `game-design.md` and
    `architecture.md` with the verified behavior and remaining constraints.

### Demo-readiness and combat readability — next

The next milestone should make the existing loop immediately understandable
before expanding it with resources or multiple waves. The current bottom panel
packs status text and every build control into the same vertical area: control
bounds do not intersect one another, but the heart-health text overlaps the
control row. Room placement also requires the pointer to already be inside an
otherwise invisible snapped candidate before a ghost appears. Rotation can move
that candidate away from the pointer and make the preview disappear. These are
interaction problems, not only placeholder-art limitations.

The current spike trap deals 5 damage to a 10-health recruit and activates twice
while the recruit crosses its cell. Consequently, one on-route trap kills every
hero in the authored wave, and the evaluated room choices differ only in elapsed
time. The balance pass should make the number and position of defenses matter,
while retaining deterministic, data-driven values.

64. [x] Add a presentation-only hero health-bar model and render a compact,
    high-contrast HP bar above the active hero. Derive its maximum from authored
    wave content, clamp the displayed fraction, hide it with the dead hero, and
    test full, damaged, zero, and invalid-boundary cases without starting libGDX.
65. [x] Replace the bottom panel's independent coordinate calculations with one
    layout model containing separate status and control regions. Use a clear
    two-row hierarchy for wave information, room selection, placement actions,
    and run actions; verify that text safe areas and controls do not overlap at
    the 1024-unit logical viewport and the default 1280 x 720 window.
66. [x] Make room attachment points discoverable without cursor hunting. Render
    eligible open doors, select the entrance or an open door as the active
    attachment target, and keep one exact room ghost visible there. The ghost
    must include footprint, connecting door, other doors, sockets, and heart
    anchor, with valid/invalid state and a short reason where useful.
67. [x] Rework rotation around that stable attachment target so rotating never
    makes the ghost disappear merely because the pointer moved outside the new
    footprint. Replace `CCW`, `CW`, and `ROT n` jargon with arrow affordances,
    plain orientation feedback, and discoverable keyboard shortcuts; test click
    precedence and every quarter turn.
68. [x] Improve room-choice controls from text-only buttons to compact cards or
    thumbnails that show footprint, doors, sockets, selected state, and current
    orientation. Add concise build-mode guidance and active-mode feedback for
    room, trap, and heart placement, then test layout and state mapping.
69. [x] Add an evaluator-backed balance matrix for zero, one, and two well-spaced
    on-route spike traps across the authored room geometries. Tune damage and/or
    cooldown only in JSON so zero traps loses, one trap no longer perfect-clears
    the wave, and deliberate multi-trap coverage can win; lock the chosen reports
    in regression tests and record the design rationale in `game-design.md`.
70. [x] Add readable live combat feedback for trap activation and hero damage,
    such as a brief trap pulse and HP-bar change, derived from simulation state
    or events without moving gameplay rules into rendering.
71. [x] Render the existing post-wave explanation model in the playable screen
    so kills, arrivals, trap activations, damage, heart health, and elapsed time
    explain the result before restart.
72. [x] Run a focused keyboard-and-mouse playtest of first-room placement, a
    turned multi-room route, heart and trap placement, wave resolution, and
    restart. Fix discovered demo-blocking usability defects, update the control
    guide, and capture the accepted layout and balance expectations in tests.

The accepted 1280 x 720 desktop playtest builds a Long Gallery from the
entrance, attaches a Corner Room, rotates a Prototype Room counter-clockwise at
the north-facing door, and places the heart in that final room. Spike traps in
the Gallery and Corner produce a victory with all four recruits defeated,
10 heart health, 12 activations, and 40 recorded damage. The post-wave report
shows those causes and restart preserves the rooms, traps, orientation, and
heart. No demo-blocking usability defect was found. The exact 16 x 9 layout and
outcome are covered by the flexible-dungeon end-to-end test and the README
control guide.

After item 72, proceed with the multi-wave room-draft loop. The competitive
review in `competitive-gameplay-plan.md` selected this direction because it
exercises the game's distinctive room construction, advance intelligence,
persistent consequences, deterministic evaluation, and readable outcomes
before broadening the content catalog.

### Authored multi-wave room-draft loop — after demo acceptance

The first expanded run should remain authored and reproducible. Each
intermission gives the player one consequential room choice, while the dungeon,
traps, selected heart, and heart damage persist across waves. The initial offer
schedule should be authored rather than randomized so the cadence and economy
can be judged without offer variance.

73. [x] Add a validated, data-authored multi-wave run definition containing an
    ordered wave list, starting Gold, Gold reward values, and run-completion
    conditions; keep balancing values out of Kotlin and test invalid references
    and boundary values.
74. [x] Extend the run phase model with explicit intelligence, room-draft,
    defense-preparation, combat, wave-report, run-victory, and run-defeat states;
    define and test the legal transition table without rendering.
75. [x] Make dungeon topology, placed traps, the selected heart, run Gold,
    and remaining heart health persist between waves while wave-local hero,
    cooldown, event, and report state resets deterministically; test every
    persistence boundary.
76. [x] Add an authored three-room offer for each intermission, allow exactly one
    choice and one committed room placement, reject incompatible or additional
    placements, and prove that every authored offer has a legal attachment in
    its intended state.
77. [x] Add one plainly named run resource, data-authored defense costs and wave
    rewards, and domain operations for affordable purchase and reward grants;
    test exact-cost, insufficient-funds, duplicate-spend, and phase rules.
78. [ ] Present the full intermission cadence in the playable screen:
    intelligence -> room choice and placement -> defense preparation -> wave ->
    causal report -> reward, with visible resource totals, costs, and disabled
    reasons.
79. [ ] Expand wave intelligence to show hero count, role, relevant traits, and
    the practical defense implication before the room decision; keep hidden
    information from becoming a required counter.
80. [ ] Extend the headless evaluator to execute a complete authored run and
    return per-wave plus aggregate reports; add deterministic regression tests
    and compare at least two legal complete-run strategies.
81. [ ] Separate retrying the current authored test from starting a new run, with
    explicit and tested rules for which layout, heart, trap, health, resource,
    and offer state is retained or reset.
82. [ ] Add an end-to-end non-rendering application test for the complete run
    cadence, then playtest the multi-wave loop and record whether room choices
    change later defense plans. Update the design documents with the accepted
    run length, resource envelope, and persistence rules.

Do not add random offers, rerolls, meta-progression, saves, or procedural runs
during this milestone. Its gate is an authored short run with at least two
understandable winning strategies and reproducible evaluator results.

### First strategic counterplay package — after the run loop

Add hero, defense, and room variety as one evaluated package. The purpose is to
create several readable plans from a small catalog, not to accumulate content.
The initial target roles are a baseline recruit, a fast scout, a tough
vanguard, a repeating spike trap, a slowing trap, and a heavy long-cooldown
trap. Exact names and values remain authored content decisions.

83. [ ] Extend authored hero content with stable role and visible defense-trait
    data, then add Scout and Vanguard definitions whose speed, durability, and
    counterplay remain explainable; test parsing, validation, and wave preview
    mapping.
84. [ ] Add the smallest deterministic status-effect model required for one
    visible slowing defense, including duration, refresh behavior, movement
    impact, simulation events, and fixed-step tests; do not create a general
    effect framework beyond the accepted behavior.
85. [ ] Add an authored slow defense compatible with an explicit socket type;
    preview its affected tile and status rule, render its activation, and report
    the additional exposure time it creates for other defenses.
86. [ ] Add an authored heavy defense with high impact and a long visible
    cooldown; make its targeting, compatible socket, damage, cooldown gaps, and
    strengths against the Vanguard readable and evaluator-verifiable.
87. [ ] Author a compact Gallery, Guard Post, and one Workshop or Alchemy room
    experiment with distinct geometry, sockets, future attachment value, and a
    visible disadvantage; validate that every blueprint remains placeable and
    route-safe in all supported orientations.
88. [ ] Compare candidate behavioral effects for the Workshop or Alchemy room
    using focused evaluator fixtures, select one effect that changes a defense
    relationship rather than adding a generic percentage bonus, and record the
    rejected alternatives and rationale before implementation.
89. [ ] Implement only the selected room effect behind the room/content layer
    that needs it, with pre-commit previews, simulation events, focused tests,
    and no general room-effect framework for hypothetical later rules.
90. [ ] Build an evaluator matrix across the three hero roles, three defense
    roles, and strategic room geometries; tune values only in content so every
    option has a useful context and no option dominates the authored run.
91. [ ] Expand live feedback and the causal post-wave explanation for status,
    effective damage, cooldown gaps, room exposure, and the first decisive
    breach or winning combination.
92. [ ] Run focused comprehension and strategy playtests for the counterplay
    package, capture accepted counter relationships in tests and documents, and
    defer healers, summoners, flying heroes, and alternative objectives unless
    the playtest establishes a concrete need.

### Persistent room identity and meaningful branches — after counterplay

Only add topology or room-rule machinery together with a player-visible
purpose. A junction is not useful content while off-route branches have no
opportunity cost or benefit.

93. [ ] Compare the smallest branch-value rules against explicit criteria and
    evaluator fixtures, beginning with an off-route Treasury that trades the
    current room choice and build space for later run resources; record the
    decision before implementation.
94. [ ] If accepted, add one three-door junction and one Treasury blueprint,
    validate their rotation and attachment behavior, and keep Treasury values
    data-authored.
95. [ ] Implement the accepted branch-value rule with visible projected rewards,
    exact trigger timing, deterministic events, and tests proving that an
    off-route room is neither free value nor an invisible routing trap.
96. [ ] Test one local adjacency rule, such as a Workshop affecting defenses in
    directly connected rooms, through a narrow experiment; implement it only if
    players can preview and explain it more easily than an equivalent room-local
    rule.
97. [ ] Compare one secondary-objective hero concept, such as a greedy hero
    visiting a Treasury, against leaving branches economy-only; do not implement
    destination choice until every possible target and route can be previewed
    deterministically.
98. [ ] Decide and implement when heart relocation is allowed and what it costs
    once changing the active branch has combat value; test affordability,
    phase, route, trap-occupancy, and report consequences.
99. [ ] Add an end-to-end branched-run scenario and playtest whether early rooms
    create understandable opportunities or constraints several waves later;
    document the accepted topology rules and rejected complexity.

### Constrained replayability — after authored topology is proven

Replayability must preserve structural fairness, deterministic reproduction,
and visible causality. Permanent progression should unlock sidegrades rather
than provide the numerical power required to finish the base run.

100. [ ] Replace the authored offer schedule with a seeded,
     composition-aware room deck whose full input state is serializable and
     reproducible; retain the authored schedule as a deterministic fixture.
101. [ ] Add configured offer-window guarantees for route-shaping,
     defense-capacity, and expansion roles, with property-based tests proving
     that generated offers remain legal and cannot create structural dead ends.
102. [ ] Add a resource-priced skip or reroll only after evaluator and playtest
     experiments establish an opportunity cost; expose the resulting offer and
     seed transition in events and reports.
103. [ ] Add a pre-run Architect Kit containing a small visible set of room
     families, defenses, and at most one rule-changing sidegrade; author and
     test at least three kits with distinct but viable plans.
104. [ ] Add a modest set of run-only relics or room-family modifications that
     change relationships instead of raising all damage; validate conflicts and
     make every active rule inspectable during play.
105. [ ] Add fixed-seed challenge runs using the existing evaluator and complete
     run reports; keep daily rotation, online services, and leaderboards out of
     scope until fixed challenges are fun without them.
106. [ ] Define account progression, if retained, as unlocking sidegrade
     options, kits, or challenges; compare it with a fully unlocked game and
     reject any progression required to overcome base-run balance.
107. [ ] Run repeated-seed, offer-fairness, kit-diversity, and comprehension
     playtests; record the accepted guarantees and progression limits before
     expanding the catalog.

### Production expansion — after the strategic loop is retained

108. [ ] Establish and playtest a coherent visual and audio direction that makes
     rooms, routes, hero roles, defense timing, and impact readable before
     increasing cosmetic variety.
109. [ ] Add versioned save and resume for an in-progress run only after run
     state is stable; test round-trip determinism, invalid content references,
     and safe failure for incompatible versions.
110. [ ] Expand the authored room, hero, defense, and wave catalog from observed
     strategy gaps, adding one evaluated package at a time rather than a
     quantity target.
111. [ ] Evaluate bosses, biome rules, challenge mutators, and secondary hero
     goals as separate design tasks with their own previews, evaluator fixtures,
     and removal criteria.
112. [ ] Export evaluator or playtest diagnostics only when current in-memory
     reports no longer answer a concrete balance question; keep gameplay
     independent of telemetry vendors and online availability.
113. [ ] Run a complete product playtest and reassess saves, accessibility,
     onboarding, content breadth, performance, and release scope before
     considering procedural campaigns, multiplayer, editors, or mod support.

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

Do not add an ECS, additional platforms, autonomous minion management, a
controllable combat hero, an overworld RTS, runtime LLM behavior, multiplayer,
procedural campaigns, editor/mod support, or a large asset pipeline unless a
later explicitly approved milestone establishes the need.

When subagents are used, the primary Codex agent owns Git and the plan. Start
one implementation worker for one unchecked item, review its diff, run
verification, and mark the item complete only after it passes. Workers must not
commit or push. The primary agent creates one focused commit, pushes it to the
current feature branch, and only then starts the next item. Stop if the push
fails or the working tree contains unrelated changes.
