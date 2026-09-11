# Game Design

## Player fantasy

The player is a dungeon architect who turns limited space and resources into a
deadly, efficient stronghold.

## Core idea

Rooms are the central strategic unit. The intended loop asks the player to
study advance intelligence, choose from a small room selection, connect rooms
to the persistent dungeon, and equip available defense sockets. The current
prototype permits several placements during its single build phase; drafting
limits are not implemented yet.

The dungeon records earlier decisions: a room chosen for the current wave may
create a useful synergy or an awkward layout later.

## Run loop

1. Receive information about the next hero party.
2. Choose one of several room blueprints.
3. Attach the room to the dungeon.
4. Place or relocate the dungeon heart in a chosen room.
5. Equip compatible traps or guardians.
6. Start the wave and observe the result.
7. Gain resources or upgrades and prepare for the next wave.

Rooms persist during a run. Replacing an arbitrary established room is
unavailable early and may become possible later at a significant cost.

During the build phase, Cancel undoes the most recently placed room so an
accidental placement can be corrected before it becomes part of the persistent
run. Its attached traps are removed, and its selected heart is unplaced if the
heart was in that room. Repeated cancellation may be used to reach an earlier
mistake. This is an undo rule, not free replacement of an arbitrary established
room.

## Room design

A room may eventually define:

- its footprint, doors, and effect on the route;
- the number and types of defense sockets;
- allowed traps, guardians, or magical effects;
- a special rule or interaction with heroes;
- adjacency synergies and a meaningful disadvantage.

Early rooms remain simple. The prototype rooms differ mainly by shape, door
placement, socket layout, and authored heart anchor. Explicit quarter-turn
rotation and the simple corner room allow routes to turn and grow in every
cardinal direction. Strong passive effects, complex restrictions, room
upgrades, and adjacency bonuses come later.

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
- The entrance remains a fixed external port. The dungeon heart is not an
  external endpoint: the player selects any placed room, and the heart occupies
  that blueprint's authored local anchor after orientation and placement are
  applied.
- A dedicated build-phase heart control enters heart-placement mode. Hovering
  any cell in a placed room previews that room's transformed anchor; a valid
  click places or relocates the heart and leaves the mode. Empty cells and
  anchors occupied by traps are rejected without falling through to room or
  trap placement.
- Heart and trap occupancy is exclusive. A trap cannot occupy the selected
  heart anchor, and a room whose anchor already contains a trap cannot receive
  the heart.
- A layout is ready for a wave only when it has a route from the entrance to
  the selected dungeon heart.
- In a branched layout, heroes use the entrance-to-heart branch. Unused side
  branches are future build capacity and have no automatic combat value in the
  early prototype.
- Rooms may be rotated by explicit clockwise or counter-clockwise quarter turns
  before placement. Footprints, doors, sockets, and heart anchors transform
  together. The corner room supplies the adjacent door facings needed to turn
  a route; open doors on any branch remain eligible frontier attachment points.
- Individual room placements may leave that route incomplete while the player
  is still constructing the layout.
- Layout creates tactical value through distance, choke points, and defense
  coverage.
- Resources and per-wave construction limits are not implemented yet.

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
spatial choice. Quarter-turn rotation and the authored corner room now extend
this rule without changing it: every accepted room still connects
through exactly one open frontier door, while its other doors remain available
for straight extensions, turns, or later branches.

## Wave intelligence

The preview should reveal enough information to support a deliberate room
choice without exposing the entire wave script. Useful information may include
hero types, approximate counts, resistances, special abilities, and goals.

Early waves should contain one straightforward hero type and clearly explain
its important traits. Later waves may mix roles, introduce uncertainty, and
reward rooms that solve more than one problem. Room selection should not become
a simple one-to-one counter puzzle.

The authored run now briefs each intermission with the next wave's hero count,
visible role, relevant trait, and a practical defense implication. The baseline
Militia Recruit is described as a frontline attacker with no special defenses,
so the advice is to cover multiple on-route sockets. Exact health, heart damage,
movement speed, spawn timing, and route script stay out of the briefing: the
player chooses a room from readable strategic guidance rather than a hidden
numeric counter. Additional hero roles remain reserved for the later
counterplay package.

## Combat

Heroes enter in waves and follow the valid route to the selected dungeon heart.
Different hero roles may vary in speed, health, damage, or resistance. Defenses
should have clear strengths, counters, ranges, and costs.

The player wins a wave when the heart survives after every hero is resolved;
heroes that reach it deal their authored heart damage. The run ends in defeat
when heart health reaches zero.

## Prototype stages

### Foundation — complete

The foundation proved room connection, routing, one simple hero wave, one
socketed trap, deterministic combat, outcomes, and restart.

### Room choice — complete

This stage introduced two simple authored room blueprints and room selection.
The flexible-construction stage subsequently added the third, corner blueprint.
The rooms differ only in geometry, doors, socket layout, and heart anchor. The
player also places the existing basic trap into a compatible socket.

This stage may allow several rooms to be placed during the initial build phase
so the current fixed map can form a complete route. It is a temporary prototype
rule, not the final run economy.

#### Authored trap balance evaluation

All three room geometries are evaluated headlessly using the checked-in room,
wave, trap, and heart JSON. Each fixture builds a connected route from repeated
copies of one blueprint and selects the final room's heart anchor. Prototype
Rooms and Long Galleries use three-room straight routes. The Corner Room uses
four alternating turns so two of its sockets lie on the chosen route without
overlap. The two armed sockets are separated by at least two route tiles.

| Room geometry | Traps | Outcome | Heart health | Kills | Arrivals | Activations | Trap damage | Elapsed simulation time |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Prototype Room | 0 | Defeat | 0 | 0 | 1 | 0 | 0 | 270 steps (4.5000 s) |
| Prototype Room | 1 | Defeat | 0 | 0 | 1 | 2 | 8 | 270 steps (4.5000 s) |
| Prototype Room | 2 | Victory | 10 | 4 | 0 | 12 | 40 | 544 steps (9.0667 s) |
| Long Gallery | 0 | Defeat | 0 | 0 | 1 | 0 | 0 | 390 steps (6.5000 s) |
| Long Gallery | 1 | Defeat | 0 | 0 | 1 | 2 | 8 | 390 steps (6.5000 s) |
| Long Gallery | 2 | Victory | 10 | 4 | 0 | 12 | 40 | 784 steps (13.0667 s) |
| Corner Room | 0 | Defeat | 0 | 0 | 1 | 0 | 0 | 330 steps (5.5000 s) |
| Corner Room | 1 | Defeat | 0 | 0 | 1 | 2 | 8 | 330 steps (5.5000 s) |
| Corner Room | 2 | Victory | 10 | 4 | 0 | 12 | 40 | 904 steps (15.0667 s) |

Spike damage is authored as 4, down from 5; cooldown remains 0.25 seconds. A
recruit has 10 health and receives two activations while crossing one socket,
so a lone trap deals 8 damage and cannot perfect-clear the wave. A second,
well-spaced on-route trap supplies the third activation needed for a kill. Each
hero therefore dies after 10 recorded damage, and two deliberate placements
protect the untouched heart against all four recruits. With zero or one trap,
the first surviving recruit deals the heart's full 10 health in damage and ends
the run.

This is intentionally a placement-count threshold, not a claim that the room
choices are strategically distinct yet. Geometry currently changes route and
observation time; later content should make socket pattern, timing, or room
rules affect which defensive plan is best. Human playtesting still needs to
confirm that acquiring and placing two traps feels clear and attainable.

### Flexible construction and dungeon heart — complete

The application flow now supports selecting authored rooms, rotating them,
building a turned persistent dungeon, undoing the newest mistaken room,
choosing the heart room, placing compatible traps, and resolving the authored
wave. The open-frontier rule also supports branches whenever a blueprint leaves
multiple doors open. An automated application-level scenario verifies a
horizontal gallery, corner, and rotated gallery route; cancellation and
attached-trap cleanup; a trap lying on the selected-heart route; deterministic
victory; and layout, trap, and heart persistence after restart.

Branches that do not lead to the selected heart remain construction options
only. They do not split heroes, attract targets, grant bonuses, or make their
off-route traps useful in combat. The current authored catalog also has no
three-door junction, so it demonstrates straight and turned chains but cannot
create a branch without another blueprint. Multiple waves, rewards,
one-room-per-wave limits, and late-game replacement costs remain later work.

## First playable content target

- One fixed entrance, one player-placed dungeon heart, and a fixed build area
- Three simple room blueprints without special effects, including one corner
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
game state. The current post-wave summary reports hero arrivals, trap
activations and damage, heart health, and elapsed simulation time. Cooldown gaps
and route length remain useful future explanation details.

Automated evaluation may search or simulate player choices for design and
balance analysis, but it does not define the intended experience by itself.
Human playtesting remains the authority on whether a choice is understandable,
interesting, and consistent with the player fantasy.

The complete-run evaluator currently preserves two reproducible winning plans:
a straight-gallery route that banks both intermission rewards, and a turned
corner route that spends each reward on the newly drafted extension and moves
the heart forward. Both resolve all three authored waves without damage, while
their different route lengths produce distinct completion times. These are
regression fixtures, not a claim that the current balance already makes every
purchase necessary; that judgment belongs to the milestone playtest.

## Open questions

- At what late-game point is room replacement unlocked, and how is its
  significant cost calculated?
- How many room choices should appear before each wave?
- Can some heroes pursue room-specific goals instead of the dungeon heart?

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
