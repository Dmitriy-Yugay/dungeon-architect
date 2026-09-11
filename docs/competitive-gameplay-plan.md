# Competitive Gameplay Plan

Research date: 2026-08-28

## Product direction

Dungeon Architect should become a compact, deterministic dungeon-defense game
about drafting one consequential room at a time. The player studies an incoming
hero party, changes the persistent route, equips a small number of defenses,
and then watches an explainable simulation test the plan.

The intended market position is between the living-dungeon fantasy of
*Dungeon Keeper* and *War for the Overworld*, the trap puzzles of *Dungeon
Warfare*, and the constrained spatial drafting of *Emberward* and *Isle of
Arrows*. It should not attempt to match the colony simulation, real-time
strategy, or content volume of those games.

The near-term product promise is:

> Draft a dungeon. Read the heroes. Build a devious counter. Understand exactly
> why it worked.

## Current foundation

The prototype already has the right technical foundation for this direction:

- persistent, rotatable rooms connected through an open-door frontier;
- a player-placed dungeon heart and explicit entrance-to-heart routing;
- data-authored rooms, waves, and traps;
- deterministic combat, structured events, and a headless evaluator;
- route-aware placement previews and a post-wave explanation;
- strong non-visual and property-based test coverage.

The current playable slice has only one wave, one hero type, one trap, and
geometry-only rooms. Two deliberate spike-trap placements prove basic coverage,
but room selection does not yet create a distinct strategy. The first incomplete
repository task is the focused demo playtest in item 72 of `PROJECT_CONTEXT.md`.

## Competitive findings

Review figures are storefront snapshots used as evidence of player interest,
not sales estimates or a strict ranking.

| Reference | Evidence and relevant strength | Lesson for Dungeon Architect |
| --- | --- | --- |
| [Dungeon Warfare 2](https://store.steampowered.com/app/698540/Dungeon_Warfare_2/) | 89% positive from about 1,120 Steam reviews; 33 traps, enemy traits, hazards, and physics-based combinations | A few traps should combine through timing, status, displacement, and terrain. Add depth through interactions before quantity. |
| [Dungeon Warfare 3](https://store.steampowered.com/app/3419220/Dungeon_Warfare_3/) | Released in 2025; shapes terrain tile by tile and combines traps with terrain. A later official update added a mode that offers a new dungeon chamber every three waves. Its roughly 78% positive reception is also a caution that more systems do not automatically improve the formula. | The room draft should be part of the wave cadence. Keep construction readable and avoid piling progression systems onto an unproven loop. |
| [War for the Overworld](https://store.steampowered.com/app/230190/War_for_the_Overworld/) | About 86% positive from more than 4,000 English Steam reviews; durable appeal from the fantasy of building rooms, traps, and a living evil dungeon | Give rooms visible purposes and personality, but do not import autonomous workforce simulation, multiplayer, or an overworld RTS. |
| [Dungeons 4](https://store.steampowered.com/app/1643310/Dungeons_4/) | About 85% positive from roughly 1,200 English Steam reviews; accessible dungeon-management fantasy and strong presentation | Make the dungeon feel inhabited and mischievous over time. Keep the actual play loop focused on fast build, defense, and revision. |
| [Legend of Keepers](https://store.steampowered.com/app/978520/Legend_of_Keepers_Career_of_a_Dungeon_Manager/) | A direct dungeon-defender roguelite built around preparing a limited defense against known parties | Turn advance intelligence into constrained, visible choices. Keep Dungeon Architect's free spatial construction as its differentiator. |
| [Emberward](https://store.steampowered.com/app/2459550/Emberward/) | 97% positive from about 2,400 English Steam reviews; limited block cards make route construction a primary combat resource | Every room offer should be a spatial instrument with a recognizable role. Room-family upgrades can later modify how that instrument works. |
| [Isle of Arrows](https://store.steampowered.com/app/1946970/Isle_of_Arrows/) | 80% positive from more than 900 Steam reviews; randomly drawn construction is moderated by guaranteed tile categories and a paid skip | Use seeded, composition-aware room offers with guarantees. Never allow unlucky offers to make a run structurally impossible. |
| [Thronefall](https://store.steampowered.com/app/2239150/Thronefall/) | About 95% positive from roughly 11,000 English Steam reviews; strips build-and-defend strategy down to a fast day/night cadence | Protect a short decision loop and small interaction surface. Show costs, ranges, route effects, and threat implications before commitment. |
| [Gnomes](https://store.steampowered.com/app/3133060/Gnomes/) | 95% positive from more than 1,700 Steam reviews; accessible turn-based defense gains replayability from synergistic upgrades, guilds, and expanding maps | Prefer run-defining sidegrades and clear synergies over permanent numerical power. Add replayability only after the base wave loop is satisfying. |
| [Monster Train](https://store.steampowered.com/app/1102190/Monster_Train/) | 96% positive from more than 12,000 English Steam reviews; previews threats and creates meaningful choices across compact defensive layers | Preview the immediate wave in detail and later milestone threats at a higher level. A future architect kit can establish a run's strategic identity. |
| [Loop Hero](https://store.steampowered.com/app/1282730/Loop_Hero/) | 91% positive from more than 16,000 English Steam reviews; placing terrain changes an observable automated expedition | Preserve the pleasure of modifying the world and watching the consequences. Do not hide important interactions or make the player author enemy spawns. |
| [Orcs Must Die! 3](https://store.steampowered.com/app/1522820/Orcs_Must_Die_3/) | About 85% positive from roughly 4,900 English Steam reviews; distinctive traps, enemy roles, combinations, and strong impact feedback | Build a small counter system with excellent telegraphs and satisfying trap feedback. Do not add an action-hero control layer. |

## Chosen design principles

1. **Rooms are the build.** A room changes route geometry, defensive capacity,
   future attachment options, or a clearly stated rule. Small passive percentage
   bonuses are not enough to justify a room choice.
2. **Intelligence creates agency.** The next party is described before the room
   and defense decision. Counters are useful but no offer should be a mandatory
   one-to-one answer.
3. **Randomness is constrained and reproducible.** Offers come from a seeded,
   composition-aware source with compatibility and role guarantees.
4. **Defense interactions create depth.** A small trap and hero catalog should
   produce several plans through timing, status, socket type, and room geometry.
5. **Every outcome is explainable.** The route, targeting, cooldowns, damage,
   status effects, and decisive breach are visible before or after the wave.
6. **Progression unlocks options, not automatic wins.** Early replayability
   comes from sidegrades, kits, and mutators rather than permanent damage inflation.
7. **The architect remains indirect.** No controllable combat hero, opaque
   autonomous workforce, or runtime language-model behavior is needed.

## Roadmap

Each phase ends with a decision gate. Do not start the next phase merely because
the systems exist; the playable result must demonstrate the stated behavior.
All balancing values and content composition belong in configuration files.

### Phase 0 — Accept the current demo

**Goal:** finish the existing demo-readiness milestone before changing the
gameplay scope.

Work:

- Complete the keyboard-and-mouse playtest in item 72.
- Test first-room discovery, turned construction, heart placement, two-trap
  coverage, wave resolution, explanation, and restart.
- Record confusion, misclicks, time-to-first-valid-route, and whether the player
  can predict why one trap loses and two win.
- Fix only demo-blocking usability defects and preserve the current balance
  baseline in evaluator-backed tests.

Gate:

- A new player can complete the single-wave loop without verbal instruction.
- The player can correctly explain the route and the decisive win/loss cause.
- No critical placement or control defect remains.

### Phase 1 — Prove the repeatable run loop

**Goal:** turn the prototype into a short run in which one dungeon grows across
several waves and every intermission contains one meaningful construction choice.

Work, in implementation order:

1. Define a data-authored run as an ordered set of waves with a persistent
   dungeon, persistent heart health, run victory, and run defeat.
2. Add the cadence: intelligence briefing -> room offer -> one room placement ->
   defense preparation -> wave -> report -> reward.
3. Offer three compatible room blueprints and permit one committed room choice
   per intermission. Compatibility filtering must not silently change the
   offered strategic roles.
4. Add one plainly named run resource used for defense purchases. Show current
   amount, costs, and the exact reward before a choice is committed.
5. Keep the first offer schedule authored and deterministic. Do not add rerolls,
   rarity, or procedural offer generation yet.
6. Expand wave intelligence to show count, roles, major traits, and the route or
   defense consequence those traits imply.
7. Separate "retry the current test" from "start a new run" so persistence rules
   are unambiguous.
8. Extend the headless evaluator from one wave to a complete run report.

Initial tuning target:

- A complete run should fit a short play session and contain enough waves for
  early room choices to constrain later ones.
- At least two different legal build plans should be able to win the authored
  run; neither should rely on hidden information.
- Saving, meta-progression, and procedural campaigns remain out of scope.

Gate:

- Players describe the loop as "build, test, learn, improve" without prompting.
- The room choice changes the next defense plan rather than acting as busywork.
- The evaluator can replay the same authored run with identical events and
  result, and can compare at least two winning strategies.

Accepted on 2026-09-11 with a three-wave authored run. Its two starting Gold and
two one-Gold intermission rewards buy the exact two-, three-, and four-trap
coverage required by escalating authored hero health. Straight and turned
complete-run strategies both win deterministically. A desktop Long Gallery
playtest confirmed that the second-wave room choice changes the next plan by
requiring an armed extension and Heart relocation; the full cadence is also
covered through the application click dispatcher without rendering.

### Phase 2 — Add the first counterplay package

**Goal:** create strategic variety with a deliberately small content set.

Add content as one balanced package rather than isolated features:

#### Hero roles

- **Recruit:** baseline health, speed, and no special defense.
- **Scout:** faster and lighter; punishes sparse coverage and overly long trap
  cooldowns.
- **Vanguard:** slower and tougher, with an explicitly displayed defense rule;
  rewards burst or repeated setup rather than hidden resistance math.

Defer healers, summoners, flying units, and alternative objectives until
multiple simultaneous heroes and targeting priorities have a proven need.

#### Defense roles

- **Spike trap:** reliable repeated damage and the baseline for comparison.
- **Snare or tar trap:** low damage but visible slowing that extends exposure to
  another defense.
- **Crusher or heavy trap:** high impact with a long, obvious cooldown; useful
  against tough targets and vulnerable to fast groups.

At least one pair must create a readable combination, such as slow -> repeated
spikes or trigger -> heavy strike. Status duration, cooldown, targeting, and
compatible sockets must be previewed and recorded in the wave report.

#### Room roles

- **Gallery:** long exposure and several defense opportunities, paid for with a
  large footprint and limited turning.
- **Guard post:** a compact chokepoint with a new defensive socket role, paid for
  with less route length or future expansion.
- **Workshop or alchemy chamber:** changes how a compatible defense behaves in
  one explicit way, paid for with weaker geometry or fewer sockets.

The exact third-room effect should be selected through evaluator experiments.
Test one behavioral effect at a time; avoid a generic damage percentage.

Gate:

- Every hero and defense has a one-sentence role that a player can verify during
  the wave.
- Each room is best in some wave contexts and meaningfully awkward in others.
- The authored run has multiple winning combinations and no universally dominant
  room or defense.
- The post-wave explanation identifies contributions from status, timing, and
  room exposure without requiring raw event-log inspection.

### Phase 3 — Make room identity and topology matter

**Goal:** turn the persistent dungeon itself into the source of longer-term
strategy.

Work:

1. Introduce a small room-rule model only after the Phase 2 experiment identifies
   concrete needs. Keep each rule data-authored and owned by the gameplay layer
   that uses it.
2. Add a three-door junction room only together with a reason to build a branch.
   The first reason should be simple and visible, such as spending a room choice
   on an off-route treasury that improves later purchasing power.
3. Add one secondary room objective experiment. A greedy hero targeting treasure
   is a later candidate, but it should not ship until destination choice is
   previewable and deterministic.
4. Test adjacency with one local, visual rule before creating a general synergy
   framework. An example is a workshop affecting only defenses in directly
   connected rooms.
5. Decide when heart relocation is allowed and what it costs once changing the
   active branch has real combat value.

Gate:

- A room selected early creates a visible opportunity or constraint several
  waves later.
- A branch has an understandable opportunity cost; it is never free value or a
  trap for a player who cannot predict hero routing.
- Moving the heart or adding a junction cannot make routing behavior ambiguous.

### Phase 4 — Add constrained replayability

**Goal:** create varied runs without weakening fairness or explainability.

Work:

1. Replace the authored offer schedule with a seeded, composition-aware room
   deck. Guarantee route-shaping, defense-capacity, and expansion roles within
   configured offer windows.
2. Add a paid skip or reroll only after testing its resource cost against the
   value of accepting an awkward room.
3. Add a pre-run **architect kit**: a small visible selection of room families,
   defenses, or one sidegrade that defines the run's intended style.
4. Add a modest set of run-only relics or room-family modifications. Each must
   change a rule or relationship, not merely raise all damage.
5. Use fixed seeds and the existing evaluator for challenge runs. Daily rotation,
   leaderboards, and online services are separate later decisions.
6. Let account progression unlock new sidegrade options, kits, or challenges;
   do not require grinding permanent power to beat the base game.

Gate:

- Repeating a seed reproduces offers, waves, and results.
- Offer guarantees prevent structural dead ends without making every choice
  interchangeable.
- At least three architect kits support visibly different but viable plans.

### Phase 5 — Production expansion

Only after the preceding loop is retained in playtests:

- establish a coherent visual identity and audio feedback pass;
- add save/resume for complete runs;
- build a larger authored wave and room catalog;
- evaluate bosses, biome rules, challenge mutators, and secondary hero goals;
- use playtest telemetry or exported evaluator reports if local observation is
  no longer enough.

This phase is not authorization for procedural campaigns, workshop support,
multiplayer, or a large asset pipeline. Each requires its own product case.

## Readability requirements across all phases

Before commitment, show:

- the exact active route and how the candidate room changes it;
- doors, footprint, sockets, heart anchor, and future open-frontier doors;
- defense range or affected tiles, target rule, cooldown, and status behavior;
- upcoming hero roles, relevant traits, and objective;
- resource cost and remaining budget.

After a wave, show:

- route length and time spent in each relevant room;
- activations, effective damage, overkill, and cooldown gaps by defense;
- status applications and the extra exposure they created;
- deaths, arrivals, and heart damage;
- the first decisive breach or the combination that secured the defense.

The explanation should favor causal summaries over more raw statistics. The
underlying events remain available to tests and evaluators.

## Evaluation and playtest strategy

For every new gameplay package:

1. Define the player-facing hypothesis before implementation.
2. Add or extend structured content rather than embedding balance values in code.
3. Test non-visual rules and invariants, using property-based tests where many
   legal layouts express the requirement better than examples.
4. Build an evaluator matrix across relevant rooms, hero parties, and defense
   combinations.
5. Reject degenerate strategies and impossible offers, but do not balance solely
   against the evaluator.
6. Run focused human playtests for comprehension, satisfaction, and perceived
   agency.
7. Record the accepted behavior and balance envelope before expanding content.

Useful milestone metrics include:

- time to understand and complete a build phase;
- percentage of testers who can predict the active route and likely failure;
- distinct winning plans found without hints;
- frequency of offers with no legal or strategically relevant placement;
- concentration of room and defense picks;
- agreement between the post-wave explanation and the player's own account.

## Explicitly deferred ideas

The competitor scan shows that these features can be valuable, but they would
blur the current product or multiply scope before the central loop is proven:

- autonomous minion needs, jobs, morale, and colony simulation;
- a controllable combat hero or action-game layer;
- an overworld RTS or offensive campaign;
- physics-heavy interactions that compromise determinism or explanation;
- large skill trees, equipment inventories, and permanent stat inflation;
- healers, summoners, flying units, and opaque target-selection AI;
- unrestricted procedural room generation or procedural campaigns;
- arbitrary room demolition, loops, or accidental multi-door joins;
- multiplayer, map editors, mod support, live-service events, and leaderboards;
- runtime LLM-controlled heroes or online dependencies in combat.

## Recommended next decision

Complete Phase 0, then implement Phase 1 before choosing between more content
and more progression. The first new gameplay milestone should be the authored,
deterministic multi-wave room-draft loop. It exercises the project's unique
room construction, wave intelligence, persistence, evaluation, and explanation
pillars while adding the least speculative machinery.
