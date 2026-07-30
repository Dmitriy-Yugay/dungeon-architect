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
`core` and `lwjgl3` modules. The current prototype renders a selectable dungeon
grid with an entrance and objective. Enemy movement, tower placement, combat,
assets, and audio have not been implemented.

## Technology

Language:
Kotlin

Engine:
libGDX

Platform:
Steam

## Current milestone

Create the smallest end-to-end playable desktop loop. The milestone is complete
when the player can:

- build a valid route from the entrance to the objective;
- start a wave containing one hero type;
- place one defense type that can damage the hero;
- win, lose, and restart with clear visual feedback.

All gameplay rules must remain testable without starting libGDX.

## Next steps for Codex

Before changing code, read `AGENTS.md` and these documents, inspect the Git
status, preserve unrelated changes, and run `./gradlew test`.

Treat every numbered item below as a separate task:

1. [x] Add an immutable room blueprint with a footprint and door positions; test
   its validation.
2. [x] Add a placed-room value with an origin; test conversion from local room
   cells to grid positions.
3. [x] Check whether a room fits inside the grid; test boundary cases.
4. [x] Reject overlap with an existing room; test touching and overlapping rooms.
5. [x] Validate a connection between compatible adjacent doors.
6. [x] Place one hard-coded room in the domain model.
7. [x] Render placed room cells with placeholder colors.
8. [ ] Show a valid or invalid placement preview under the pointer.
9. [ ] Commit a valid room placement on click.
10. [ ] Produce walkable grid positions from placed rooms.
11. [ ] Find a four-directional path through those positions; test success and
    failure.
12. [ ] Require a route between the entrance and objective.
13. [ ] Add data describing one simple upcoming hero wave.
14. [ ] Display the wave description and provide a start control.
15. [ ] Move one plain Kotlin hero along a route using a fixed simulation step;
    test movement and arrival.
16. [ ] Render the hero.
17. [ ] Add one room socket and one compatible trap definition.
18. [ ] Place the trap in the socket.
19. [ ] Apply deterministic trap damage; test targeting, cooldown, and death.
20. [ ] Add objective damage, victory, defeat, and restart.

For every task:

- implement only the first incomplete item unless asked for a wider scope;
- avoid adding abstractions needed only by later items;
- keep gameplay rules independent of libGDX rendering;
- add tests for new non-visual behavior;
- run `./gradlew test` and `./gradlew build`;
- update the documents when a design or architecture decision changes.

Do not add special room effects, advanced hero roles, an ECS, saves, additional
platforms, or a large asset pipeline during this milestone.
