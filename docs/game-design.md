# Game Design

## Player fantasy

The player is a dungeon architect who turns limited space and resources into a
deadly, efficient stronghold.

## Core loop

1. Build or revise the dungeon layout.
2. Spend resources on defenses.
3. Start a hero wave.
4. Observe combat and protect the dungeon objective.
5. Earn resources, identify weaknesses, and rebuild.

## Dungeon rules

- The dungeon uses a grid for predictable placement and navigation.
- Every valid layout must leave a route from the entrance to the objective.
- Layout creates tactical value through distance, choke points, and defense
  coverage.
- Construction and defense placement consume a shared, limited resource.

## Combat

Heroes enter in waves and follow a valid route to the objective. Different hero
roles may vary in speed, health, damage, or resistance. Defenses should have
clear strengths, counters, ranges, and costs.

The player wins a wave by defeating all heroes. The run ends when the objective
loses all health.

## First playable content

- One map and one objective
- Straight paths, corners, and basic rooms
- Three distinct defenses
- Three hero roles
- Several authored waves
- Resource rewards and simple difficulty scaling

## Design constraints

Important information—routes, ranges, targets, damage, and resource costs—must
be visible before the player commits to a decision. Randomness should add
variation without hiding the reason for an outcome.
