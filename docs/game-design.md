# Game Design

## Player fantasy

The player is a dungeon architect who turns limited space and resources into a
deadly, efficient stronghold.

## Core idea

Rooms are the central strategic unit. Before each wave, the player studies
advance intelligence, chooses one room from a small selection, connects it to
the persistent dungeon, and equips its available defense sockets.

The dungeon records earlier decisions: a room chosen for the current wave may
create a useful synergy or an awkward layout later.

## Run loop

1. Receive information about the next hero party.
2. Choose one of several room blueprints.
3. Attach the room to the dungeon.
4. Equip compatible traps or guardians.
5. Start the wave and observe the result.
6. Gain resources or upgrades and prepare for the next wave.

Rooms persist during a run. Replacing an old room is unavailable early and may
become possible later at a significant cost.

## Room design

A room may eventually define:

- its footprint, doors, and effect on the route;
- the number and types of defense sockets;
- allowed traps, guardians, or magical effects;
- a special rule or interaction with heroes;
- adjacency synergies and a meaningful disadvantage.

Early rooms must remain simple. For the first prototype, rooms should differ
mainly by shape, door placement, and socket layout. Strong passive effects,
complex restrictions, room upgrades, rotation, and adjacency bonuses come
later.

Possible late-game room directions include a guardian-focused barracks, a
trap-heavy gallery, an alchemy room that changes damage effects, and a treasury
that influences greedy heroes. These are ideas to validate, not committed
content.

## Dungeon rules

- The dungeon uses a grid for predictable placement and navigation.
- A room door belongs to an exposed edge of a footprint cell and has a cardinal
  facing. Two room doors connect only when they occupy adjacent cells and face
  directly toward one another.
- New rooms snap one compatible door to exactly one unused door on the existing
  dungeon. The connected pair is then consumed; any other doors on the new room
  become possible attachment points for later choices.
- Merely touching another room does not connect floor space. Heroes cross room
  boundaries only through recorded door connections.
- The entrance faces east and the objective faces west in the prototype. A room
  must meet each endpoint through a correspondingly facing door; rooms cannot
  cover endpoint cells.
- A layout is ready for a wave only when it has a route from the entrance to
  the objective.
- Individual room placements may leave that route incomplete while the player
  is still constructing the layout.
- Layout creates tactical value through distance, choke points, and defense
  coverage.
- Construction and defense placement consume a shared, limited resource.

### Persistent topology decision

Three rules were compared for growing the dungeon:

1. **Open-door frontier:** attach each new room to one unused directional door,
   consume the connected pair, and leave the new room's remaining doors open.
2. **Flexible attachment:** permit a placement to connect every compatible door
   it touches, allowing loops and accidental multi-room joins.
3. **Single active tail:** permit attachment only to the most recently placed
   room, producing a strict chain.

The comparison used four criteria: whether a legal placement is obvious before
clicking, whether earlier room choices constrain later choices, whether the
rule can create branches and varied layouts, and how easily routing can be
tested and explained. Small non-rendering fixtures covered facing compatibility,
consumed doors, wall-to-wall contact, multi-door joins, endpoint connections,
and snapped preview placement.

The selected rule is **open-door frontier**. It makes every future attachment
point visible and persistent, supports branching without permitting a single
placement to create a surprising loop, and gives routing an explicit graph of
door connections. Flexible attachment offered more topology but made accidental
joins hard to preview; the single active tail was clear but discarded too much
spatial choice. Rotation remains deferred, so authored room orientation is part
of the choice for now.

## Wave intelligence

The preview should reveal enough information to support a deliberate room
choice without exposing the entire wave script. Useful information may include
hero types, approximate counts, resistances, special abilities, and goals.

Early waves should contain one straightforward hero type and clearly explain
its important traits. Later waves may mix roles, introduce uncertainty, and
reward rooms that solve more than one problem. Room selection should not become
a simple one-to-one counter puzzle.

## Combat

Heroes enter in waves and follow a valid route to the objective. Different hero
roles may vary in speed, health, damage, or resistance. Defenses should have
clear strengths, counters, ranges, and costs.

The player wins a wave by defeating all heroes. The run ends when the objective
loses all health.

## Prototype stages

### Foundation — complete

The foundation proved room connection, routing, one simple hero wave, one
socketed trap, deterministic combat, outcomes, and restart.

### Room choice — complete

The prototype has two simple authored room blueprints and lets the player choose
which one to place. The rooms differ only in geometry, doors, and socket layout.
The player also places the existing basic trap into a compatible socket.

This stage may allow several rooms to be placed during the initial build phase
so the current fixed map can form a complete route. It is a temporary prototype
rule, not the final run economy.

### Persistent drafting — topology selected

Each new room extends the open-door frontier described above. Multiple waves,
rewards, one-room-per-wave limits, and late-game replacement costs remain later
work.

## First playable content target

- One entrance, one objective, and a fixed build area
- Two simple room blueprints without special effects
- One basic trap
- One basic hero type
- One authored wave with a clear preview
- Placement, routing, combat, win, loss, and restart

## Design constraints

Important information—routes, ranges, targets, damage, and resource costs—must
be visible before the player commits to a decision. Randomness should add
variation without hiding the reason for an outcome.

Rooms should change geometry, available actions, or hero behavior rather than
only providing small numerical bonuses.

Important combat decisions and outcomes should be explainable from recorded
game state. A post-wave summary should eventually identify facts such as hero
arrivals, trap activations, damage, cooldown gaps, and route length instead of
presenting only victory or defeat.

Automated evaluation may search or simulate player choices for design and
balance analysis, but it does not define the intended experience by itself.
Human playtesting remains the authority on whether a choice is understandable,
interesting, and consistent with the player fantasy.

## Open questions

- At what late-game point is room replacement unlocked, and how is its
  significant cost calculated?
- How many room choices should appear before each wave?
- Can some heroes pursue room-specific goals instead of the main objective?

## Future gameplay decision points

- Start advanced hero behavior with the simplest suitable model. Finite-state
  machines fit small mode changes; utility scoring may fit transparent choices
  between competing goals; behavior trees may fit larger designer-authored
  hierarchies. Do not add one until a concrete hero behavior needs it.
- Consider constraint-based generation only when authored layouts or ordinary
  Kotlin search can no longer provide enough valid variation. Generated
  dungeons must remain solvable, readable, and reproducible.
- LLM-generated narrative, intelligence reports, or flavor text may be explored
  as optional authored-content assistance. Core combat and progression must not
  require an online model response.
- Runtime LLM-controlled heroes are not currently aligned with deterministic,
  readable defense. Reconsider only if a future design specifically requires
  open-ended language or behavior that simpler gameplay AI cannot provide.
