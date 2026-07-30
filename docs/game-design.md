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
- Rooms connect through compatible doors.
- Every valid layout must leave a route from the entrance to the objective.
- Layout creates tactical value through distance, choke points, and defense
  coverage.
- Construction and defense placement consume a shared, limited resource.

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

## First playable content

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

## Open questions

- How does adding a room extend or alter the required entrance-to-objective
  route?
- When is room replacement unlocked, and how is its cost calculated?
- How many room choices should appear before each wave?
- Can some heroes pursue room-specific goals instead of the main objective?
