package com.dungeonarchitect.application

import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomDoor
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.RunCompletionCondition
import com.dungeonarchitect.domain.RunWaveDefinition
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.evaluation.WaveEvaluationReport
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import com.dungeonarchitect.simulation.HeroArrived
import com.dungeonarchitect.simulation.HeroDamaged
import com.dungeonarchitect.simulation.HeroDied
import com.dungeonarchitect.simulation.HeroSpawned
import com.dungeonarchitect.simulation.HeartDamaged
import com.dungeonarchitect.simulation.TrapActivated
import com.dungeonarchitect.simulation.WaveOutcome
import com.dungeonarchitect.simulation.WaveResolved
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PrototypeRunControllerTest {
    @Test
    fun `one-hero and four-hero waves win only after every hero dies`() {
        listOf(1, 4).forEach { heroCount ->
            val controller = controller(
                grid = gridWithLethalTrap(),
                heroCount = heroCount,
            )
            assertTrue(controller.start())
            assertNull(controller.evaluationReport)

            repeat(heroCount) { resolvedBeforeStep ->
                controller.advance(elapsedSeconds = fixedSteps(2))

                assertEquals(resolvedBeforeStep + 1, controller.resolvedHeroCount)
                assertEquals(
                    if (resolvedBeforeStep + 1 == heroCount) {
                        PrototypeRunPhase.RUN_VICTORY
                    } else {
                        PrototypeRunPhase.COMBAT
                    },
                    controller.phase,
                )
            }

            assertEquals(10, controller.heartHealth)
            assertTrue(controller.isControlEnabled)
            assertEquals(
                WaveEvaluationReport(
                    outcome = WaveOutcome.VICTORY,
                    heartHealth = 10,
                    heroKills = heroCount,
                    heroArrivals = 0,
                    elapsedSimulationSeconds =
                        heroCount * 2 *
                            FixedStepHeroSimulation.FIXED_STEP_SECONDS,
                    trapActivations = heroCount,
                    trapDamage = heroCount * 5,
                ),
                controller.evaluationReport,
            )
        }
    }

    @Test
    fun `hero arrival damages the heart and zero health causes defeat`() {
        listOf(1, 4).forEach { heroCount ->
            val controller = controller(
                grid = gridWithRoute(),
                heroCount = heroCount,
            )
            assertTrue(controller.start())

            controller.advance(elapsedSeconds = fixedSteps(2))

            assertEquals(PrototypeRunPhase.RUN_DEFEAT, controller.phase)
            assertEquals(0, controller.heartHealth)
            assertEquals(1, controller.resolvedHeroCount)
            assertEquals(
                WaveEvaluationReport(
                    outcome = WaveOutcome.DEFEAT,
                    heartHealth = 0,
                    heroKills = 0,
                    heroArrivals = 1,
                    elapsedSimulationSeconds =
                        2 * FixedStepHeroSimulation.FIXED_STEP_SECONDS,
                    trapActivations = 0,
                    trapDamage = 0,
                ),
                controller.evaluationReport,
            )
        }
    }

    @Test
    fun `surviving heart receives damage from successive heroes`() {
        val controller = controller(
            grid = gridWithRoute(),
            heroCount = 2,
            heartHealth = 15,
            heartDamage = 10,
        )
        assertTrue(controller.start())

        controller.advance(elapsedSeconds = fixedSteps(2))

        assertEquals(PrototypeRunPhase.COMBAT, controller.phase)
        assertEquals(5, controller.heartHealth)
        assertEquals(1, controller.resolvedHeroCount)

        controller.advance(elapsedSeconds = fixedSteps(2))

        assertEquals(PrototypeRunPhase.RUN_DEFEAT, controller.phase)
        assertEquals(0, controller.heartHealth)
        assertEquals(2, controller.resolvedHeroCount)
    }

    @Test
    fun `four-hero result is deterministic across elapsed-time chunks`() {
        val singleChunk = controller(
            grid = gridWithLethalTrap(),
            heroCount = 4,
        )
        val fourChunks = controller(
            grid = gridWithLethalTrap(),
            heroCount = 4,
        )
        assertTrue(singleChunk.start())
        assertTrue(fourChunks.start())

        singleChunk.advance(elapsedSeconds = fixedSteps(8))
        repeat(4) {
            fourChunks.advance(elapsedSeconds = fixedSteps(2))
        }

        assertEquals(singleChunk.phase, fourChunks.phase)
        assertEquals(singleChunk.heartHealth, fourChunks.heartHealth)
        assertEquals(singleChunk.resolvedHeroCount, fourChunks.resolvedHeroCount)
        assertEquals(singleChunk.heroState, fourChunks.heroState)
        assertEquals(singleChunk.events, fourChunks.events)
        assertEquals(singleChunk.evaluationReport, fourChunks.evaluationReport)
    }

    @Test
    fun `evaluation elapsed time is deterministic across irregular frame chunks`() {
        val fixedChunkController = controller(
            grid = gridWithLethalTrap(),
            heroCount = 2,
        )
        val irregularChunkController = controller(
            grid = gridWithLethalTrap(),
            heroCount = 2,
        )
        assertTrue(fixedChunkController.start())
        assertTrue(irregularChunkController.start())

        fixedChunkController.advance(elapsedSeconds = fixedSteps(4))
        repeat(100) {
            if (irregularChunkController.phase == PrototypeRunPhase.COMBAT) {
                irregularChunkController.advance(elapsedSeconds = 0.005f)
            }
        }

        assertEquals(PrototypeRunPhase.RUN_VICTORY, irregularChunkController.phase)
        assertEquals(
            fixedChunkController.evaluationReport,
            irregularChunkController.evaluationReport,
        )
        assertEquals(
            4 * FixedStepHeroSimulation.FIXED_STEP_SECONDS,
            irregularChunkController.evaluationReport?.elapsedSimulationSeconds,
        )
    }

    @Test
    fun `lethal traps emit ordered events with one-based hero numbers`() {
        val controller = controller(
            grid = gridWithLethalTrap(),
            heroCount = 2,
        )
        assertTrue(controller.start())

        controller.advance(elapsedSeconds = fixedSteps(4))

        assertEquals(
            listOf(
                HeroSpawned(
                    heroNumber = 1,
                    heroType = "militia_recruit",
                    position = heroPosition(0, 0),
                    health = 5,
                ),
                TrapActivated(1, "spike_trap", GridPosition(1, 0)),
                HeroDamaged(1, "spike_trap", damage = 5, remainingHealth = 0),
                HeroDied(heroNumber = 1, position = heroPosition(1, 0)),
                HeroSpawned(
                    heroNumber = 2,
                    heroType = "militia_recruit",
                    position = heroPosition(0, 0),
                    health = 5,
                ),
                TrapActivated(2, "spike_trap", GridPosition(1, 0)),
                HeroDamaged(2, "spike_trap", damage = 5, remainingHealth = 0),
                HeroDied(heroNumber = 2, position = heroPosition(1, 0)),
                WaveResolved(WaveOutcome.VICTORY, heartHealth = 10),
            ),
            controller.events,
        )
    }

    @Test
    fun `arrivals emit actual heart damage before defeat`() {
        val controller = controller(
            grid = gridWithRoute(),
            heroCount = 2,
            heartHealth = 15,
            heartDamage = 10,
        )
        assertTrue(controller.start())

        controller.advance(elapsedSeconds = fixedSteps(4))

        assertEquals(
            listOf(
                HeroSpawned(1, "militia_recruit", heroPosition(0, 0), 5),
                HeroArrived(heroNumber = 1, position = heroPosition(2, 0)),
                HeartDamaged(1, damage = 10, remainingHealth = 5),
                HeroSpawned(2, "militia_recruit", heroPosition(0, 0), 5),
                HeroArrived(heroNumber = 2, position = heroPosition(2, 0)),
                HeartDamaged(2, damage = 5, remainingHealth = 0),
                WaveResolved(WaveOutcome.DEFEAT, heartHealth = 0),
            ),
            controller.events,
        )
    }

    @Test
    fun `restart preserves layout and resets run state and trap cooldowns`() {
        val grid = gridWithLethalTrap()
        val rooms = grid.placedRooms
        val traps = grid.placedTraps
        val controller = controller(
            grid = grid,
            heroCount = 1,
        )
        assertTrue(controller.start())
        controller.advance(elapsedSeconds = fixedSteps(2))
        assertEquals(PrototypeRunPhase.RUN_VICTORY, controller.phase)
        val completedRunEvents = controller.events

        assertTrue(controller.restart())

        assertEquals(PrototypeRunPhase.DEFENSE_PREPARATION, controller.phase)
        assertEquals(10, controller.heartHealth)
        assertEquals(0, controller.resolvedHeroCount)
        assertNull(controller.heroState)
        assertTrue(controller.isStartEnabled)
        assertEquals(rooms, grid.placedRooms)
        assertEquals(traps, grid.placedTraps)
        assertEquals(emptyList(), controller.events)
        assertNull(controller.evaluationReport)
        assertTrue(completedRunEvents.isNotEmpty())

        assertTrue(controller.start())
        assertEquals(
            listOf(
                HeroSpawned(1, "militia_recruit", heroPosition(0, 0), 5),
            ),
            controller.events,
        )
        assertTrue(completedRunEvents.size > controller.events.size)
        controller.advance(elapsedSeconds = fixedSteps(2))
        assertEquals(PrototypeRunPhase.RUN_VICTORY, controller.phase)
    }

    @Test
    fun `heart placement and relocation are enabled only while building`() {
        val buildingGrid = gridWithRoute()
        val buildingController = controller(buildingGrid, heroCount = 1)
        assertTrue(
            buildingController.placeOrRelocateHeart(
                buildingGrid.placedRooms.single(),
            ),
        )

        val runningGrid = gridWithRoute()
        val runningController = controller(
            grid = runningGrid,
            heroCount = 2,
            heartHealth = 30,
        )
        assertTrue(
            runningController.placeOrRelocateHeart(
                runningGrid.placedRooms.single(),
            ),
        )
        val runningHeart = requireNotNull(runningGrid.placedHeart)
        assertTrue(runningController.start())

        val victoryGrid = gridWithSeparateHeartAndLethalTrap()
        val victoryController = controller(victoryGrid, heroCount = 1)
        assertTrue(
            victoryController.placeOrRelocateHeart(
                victoryGrid.placedRooms.single(),
            ),
        )
        val victoryHeart = requireNotNull(victoryGrid.placedHeart)
        assertTrue(victoryController.start())
        victoryController.advance(elapsedSeconds = fixedSteps(4))
        assertEquals(PrototypeRunPhase.RUN_VICTORY, victoryController.phase)

        val defeatGrid = gridWithRoute()
        val defeatController = controller(defeatGrid, heroCount = 1)
        assertTrue(
            defeatController.placeOrRelocateHeart(
                defeatGrid.placedRooms.single(),
            ),
        )
        val defeatHeart = requireNotNull(defeatGrid.placedHeart)
        assertTrue(defeatController.start())
        defeatController.advance(elapsedSeconds = fixedSteps(2))
        assertEquals(PrototypeRunPhase.RUN_DEFEAT, defeatController.phase)

        listOf(
            Triple(runningController, runningGrid, runningHeart),
            Triple(victoryController, victoryGrid, victoryHeart),
            Triple(defeatController, defeatGrid, defeatHeart),
        ).forEach { (controller, grid, originalHeart) ->
            assertFalse(
                controller.placeOrRelocateHeart(grid.placedRooms.single()),
                controller.phase.name,
            )
            assertSame(originalHeart, grid.placedHeart, controller.phase.name)
        }
    }

    @Test
    fun `restart preserves the placed heart with the persistent layout`() {
        val grid = gridWithRoute()
        val controller = controller(grid, heroCount = 1)
        assertTrue(controller.placeOrRelocateHeart(grid.placedRooms.single()))
        val placedHeart = requireNotNull(grid.placedHeart)
        assertTrue(controller.start())
        controller.advance(elapsedSeconds = fixedSteps(2))
        assertEquals(PrototypeRunPhase.RUN_DEFEAT, controller.phase)

        assertTrue(controller.restart())

        assertEquals(PrototypeRunPhase.DEFENSE_PREPARATION, controller.phase)
        assertSame(placedHeart, grid.placedHeart)
        assertSame(grid.placedRooms.single(), grid.placedHeart?.room)
    }

    @Test
    fun `cancel delegates room removal while building`() {
        val grid = gridWithRoute()
        val controller = controller(grid = grid, heroCount = 1)

        assertTrue(controller.isCancelEnabled)
        assertTrue(controller.cancelLastPlacedRoom())

        assertEquals(emptyList(), grid.placedRooms)
        assertFalse(controller.isStartEnabled)
        assertFalse(controller.isCancelEnabled)
    }

    @Test
    fun `cancel leaves the layout unchanged outside the building phase`() {
        val runningGrid = gridWithRoute()
        val runningController = controller(
            grid = runningGrid,
            heroCount = 2,
            heartHealth = 30,
        )
        assertTrue(runningController.start())

        val victoryGrid = gridWithLethalTrap()
        val victoryController = controller(grid = victoryGrid, heroCount = 1)
        assertTrue(victoryController.start())
        victoryController.advance(elapsedSeconds = fixedSteps(2))
        assertEquals(PrototypeRunPhase.RUN_VICTORY, victoryController.phase)

        val defeatGrid = gridWithRoute()
        val defeatController = controller(grid = defeatGrid, heroCount = 1)
        assertTrue(defeatController.start())
        defeatController.advance(elapsedSeconds = fixedSteps(2))
        assertEquals(PrototypeRunPhase.RUN_DEFEAT, defeatController.phase)

        listOf(
            runningController to runningGrid,
            victoryController to victoryGrid,
            defeatController to defeatGrid,
        ).forEach { (controller, grid) ->
            val roomsBeforeCancel = grid.placedRooms
            val trapsBeforeCancel = grid.placedTraps

            assertFalse(controller.isCancelEnabled, controller.phase.name)
            assertFalse(controller.cancelLastPlacedRoom(), controller.phase.name)
            assertEquals(roomsBeforeCancel, grid.placedRooms, controller.phase.name)
            assertEquals(trapsBeforeCancel, grid.placedTraps, controller.phase.name)
        }
    }

    @Test
    fun `defense purchase succeeds at exact cost and spends Gold once`() {
        val grid = gridWithRoute(hasSocket = true)
        val room = grid.placedRooms.single()
        val controller = controllerWithGold(grid, startingGold = 2)
        val trap = defense(costGold = 2)

        assertTrue(
            controller.purchaseDefense(room, GridPosition(0, 0), trap),
        )
        assertEquals(0, controller.gold)
        assertEquals(1, grid.placedTraps.size)

        assertFalse(
            controller.purchaseDefense(room, GridPosition(0, 0), trap),
        )
        assertEquals(0, controller.gold)
        assertEquals(1, grid.placedTraps.size)

        assertTrue(controller.cancelLastPlacedRoom())
        assertEquals(2, controller.gold)
        assertEquals(emptyList(), grid.placedTraps)
    }

    @Test
    fun `unaffordable and wrong-phase defense purchases are atomic`() {
        val unaffordableGrid = gridWithRoute(hasSocket = true)
        val unaffordable = controllerWithGold(
            grid = unaffordableGrid,
            startingGold = 1,
        )
        assertFalse(
            unaffordable.purchaseDefense(
                unaffordableGrid.placedRooms.single(),
                GridPosition(0, 0),
                defense(costGold = 2),
            ),
        )
        assertEquals(1, unaffordable.gold)
        assertEquals(emptyList(), unaffordableGrid.placedTraps)

        val combatGrid = gridWithRoute(hasSocket = true)
        val combat = controllerWithGold(combatGrid, startingGold = 2)
        assertTrue(combat.start())
        assertFalse(
            combat.purchaseDefense(
                combatGrid.placedRooms.single(),
                GridPosition(0, 0),
                defense(costGold = 2),
            ),
        )
        assertEquals(2, combat.gold)
        assertEquals(emptyList(), combatGrid.placedTraps)
    }

    @Test
    fun `victory reward is claimable once only from its wave report`() {
        val grid = gridWithPersistentTrap()
        val controller = PrototypeRunController(
            grid = grid,
            waveContentByPath = mapOf(
                "content/opening.json" to wave(),
                "content/finale.json" to wave(),
            ),
            runDefinition = multiWaveRunDefinition(startingGold = 4),
        )

        assertFalse(controller.claimWaveReward())
        assertTrue(controller.start())
        assertFalse(controller.claimWaveReward())
        controller.advance(fixedSteps(2))
        assertEquals(PrototypeRunPhase.WAVE_REPORT, controller.phase)
        assertFalse(controller.acknowledgeWaveReport())
        assertEquals(4, controller.gold)

        assertTrue(controller.claimWaveReward())
        assertEquals(5, controller.gold)
        assertTrue(controller.isWaveRewardClaimed)
        assertFalse(controller.claimWaveReward())
        assertEquals(5, controller.gold)

        assertTrue(controller.acknowledgeWaveReport())
        assertEquals(PrototypeRunPhase.INTELLIGENCE, controller.phase)
        assertFalse(controller.isWaveRewardClaimed)
        assertFalse(controller.claimWaveReward())
        assertEquals(5, controller.gold)
    }

    @Test
    fun `defeat report cannot grant a wave reward`() {
        val grid = gridWithRoute()
        val controller = PrototypeRunController(
            grid = grid,
            waveContentByPath = mapOf(
                "content/opening.json" to wave(heartDamage = 3),
                "content/finale.json" to wave(),
            ),
            runDefinition = multiWaveRunDefinition(
                heartHealth = 2,
                startingGold = 4,
            ),
        )
        assertTrue(controller.start())
        controller.advance(fixedSteps(2))

        assertEquals(PrototypeRunPhase.WAVE_REPORT, controller.phase)
        assertEquals(WaveOutcome.DEFEAT, controller.evaluationReport?.outcome)
        assertFalse(controller.claimWaveReward())
        assertEquals(4, controller.gold)
        assertTrue(controller.acknowledgeWaveReport())
        assertEquals(PrototypeRunPhase.RUN_DEFEAT, controller.phase)
        assertFalse(controller.claimWaveReward())
    }

    @Test
    fun `next authored wave preserves run state and resets every wave-local boundary`() {
        val grid = gridWithPersistentTrap()
        val rooms = grid.placedRooms
        val traps = grid.placedTraps
        val heart = requireNotNull(grid.placedHeart)
        val opening = wave(
            heroType = "opening_recruit",
            heartDamage = 3,
        )
        val finale = wave(
            heroType = "final_recruit",
            heartDamage = 3,
        )
        val controller = PrototypeRunController(
            grid = grid,
            waveContentByPath = mapOf(
                "content/opening.json" to opening,
                "content/finale.json" to finale,
            ),
            runDefinition = multiWaveRunDefinition(startingGold = 4),
        )

        assertEquals("opening", controller.currentWaveDefinition.id)
        assertSame(opening, controller.upcomingWave)
        assertEquals(4, controller.gold)
        assertTrue(controller.start())
        controller.advance(elapsedSeconds = fixedSteps(2))

        assertEquals(PrototypeRunPhase.WAVE_REPORT, controller.phase)
        assertEquals(7, controller.heartHealth)
        assertEquals(1, controller.resolvedHeroCount)
        assertTrue(controller.events.isNotEmpty())
        assertTrue(controller.events.any { it is TrapActivated })
        assertTrue(controller.evaluationReport != null)
        assertTrue(controller.heroState?.hasArrived == true)

        assertTrue(controller.claimWaveReward())
        assertTrue(controller.acknowledgeWaveReport())

        assertEquals(PrototypeRunPhase.INTELLIGENCE, controller.phase)
        assertEquals(1, controller.currentWaveIndex)
        assertEquals("finale", controller.currentWaveDefinition.id)
        assertSame(finale, controller.upcomingWave)
        assertEquals(rooms, grid.placedRooms)
        assertEquals(traps, grid.placedTraps)
        assertSame(heart, grid.placedHeart)
        assertEquals(7, controller.heartHealth)
        assertEquals(5, controller.gold)
        assertEquals(0, controller.resolvedHeroCount)
        assertNull(controller.startedWave)
        assertNull(controller.heroState)
        assertEquals(emptyList(), controller.events)
        assertNull(controller.evaluationReport)
        assertFalse(controller.isStartEnabled)

        assertTrue(controller.acknowledgeIntelligence())
        assertEquals(PrototypeRunPhase.ROOM_DRAFT, controller.phase)
        assertTrue(
            controller.placeRoom(
                PlacedRoom(
                    blueprint = draftRoom(),
                    origin = GridPosition(column = 3, row = 0),
                ),
            ),
        )
        assertEquals(PrototypeRunPhase.DEFENSE_PREPARATION, controller.phase)
        assertTrue(controller.start())
        assertSame(finale, controller.startedWave?.wave)
        controller.advance(elapsedSeconds = fixedSteps(2))

        assertEquals(PrototypeRunPhase.WAVE_REPORT, controller.phase)
        assertEquals(4, controller.heartHealth)
        assertTrue(controller.events.any { it is TrapActivated })
        assertEquals(
            2 * FixedStepHeroSimulation.FIXED_STEP_SECONDS,
            controller.evaluationReport?.elapsedSimulationSeconds,
        )
        assertTrue(controller.claimWaveReward())
        assertTrue(controller.acknowledgeWaveReport())
        assertEquals(PrototypeRunPhase.RUN_VICTORY, controller.phase)
    }

    @Test
    fun `defeated authored wave reports before ending run without advancing`() {
        val controller = PrototypeRunController(
            grid = gridWithRoute(),
            waveContentByPath = mapOf(
                "content/opening.json" to wave(heartDamage = 3),
                "content/finale.json" to wave(heartDamage = 1),
            ),
            runDefinition = multiWaveRunDefinition(heartHealth = 2),
        )
        assertTrue(controller.start())

        controller.advance(elapsedSeconds = fixedSteps(2))

        assertEquals(PrototypeRunPhase.WAVE_REPORT, controller.phase)
        assertEquals(WaveOutcome.DEFEAT, controller.evaluationReport?.outcome)
        assertEquals(0, controller.currentWaveIndex)
        assertTrue(controller.acknowledgeWaveReport())
        assertEquals(PrototypeRunPhase.RUN_DEFEAT, controller.phase)
        assertEquals(0, controller.currentWaveIndex)
        assertFalse(controller.acknowledgeWaveReport())
    }

    @Test
    fun `three-wave run accepts one committed room in each intermission`() {
        val grid = gridWithPersistentTrap()
        val wave = wave()
        val controller = PrototypeRunController(
            grid = grid,
            waveContentByPath = mapOf(
                "content/opening.json" to wave,
                "content/middle.json" to wave,
                "content/finale.json" to wave,
            ),
            runDefinition = threeWaveRunDefinition(),
        )

        assertTrue(controller.start())
        controller.advance(fixedSteps(2))
        assertTrue(controller.claimWaveReward())
        assertTrue(controller.acknowledgeWaveReport())
        assertTrue(controller.acknowledgeIntelligence())
        assertEquals(roomOffer(), controller.offeredRoomBlueprintIds)
        assertFalse(
            controller.placeRoom(
                PlacedRoom(
                    blueprint = draftRoom(id = "not-offered"),
                    origin = GridPosition(3, 0),
                ),
            ),
        )
        assertFalse(
            controller.placeRoom(
                PlacedRoom(
                    blueprint = draftRoom(),
                    origin = GridPosition(1, 0),
                ),
            ),
        )
        assertEquals(1, grid.placedRooms.size)
        assertEquals(PrototypeRunPhase.ROOM_DRAFT, controller.phase)
        val middleRoom = PlacedRoom(
            blueprint = draftRoom(),
            origin = GridPosition(3, 0),
        )
        assertTrue(controller.placeRoom(middleRoom))
        assertSame(middleRoom, controller.committedRoom)
        assertFalse(controller.isRoomPlacementEnabled)
        assertFalse(
            controller.placeRoom(
                PlacedRoom(
                    blueprint = draftRoom(id = "alternate-room"),
                    origin = GridPosition(4, 0),
                ),
            ),
        )
        assertFalse(controller.cancelLastPlacedRoom())
        assertFalse(controller.isCancelEnabled)
        assertEquals(listOf(middleRoom), grid.placedRooms.takeLast(1))

        assertTrue(controller.start())
        controller.advance(fixedSteps(2))
        assertTrue(controller.claimWaveReward())
        assertTrue(controller.acknowledgeWaveReport())
        assertNull(controller.committedRoom)
        assertTrue(controller.acknowledgeIntelligence())
        val finalRoom = PlacedRoom(
            blueprint = draftRoom(id = "alternate-room"),
            origin = GridPosition(4, 0),
        )
        assertTrue(controller.placeRoom(finalRoom))
        assertSame(finalRoom, controller.committedRoom)
        assertEquals(listOf(middleRoom, finalRoom), grid.placedRooms.takeLast(2))
    }

    @Test
    fun `authored run rejects missing wave content before simulation`() {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            PrototypeRunController(
                grid = gridWithRoute(),
                waveContentByPath = mapOf(
                    "content/opening.json" to wave(),
                ),
                runDefinition = multiWaveRunDefinition(),
            )
        }
    }

    @Test
    fun `single-wave compatibility constructor rejects distinct authored content paths`() {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            PrototypeRunController(
                grid = gridWithRoute(),
                upcomingWave = wave(),
                runDefinition = multiWaveRunDefinition(),
            )
        }
    }

    @Test
    fun `run rejects invalid elapsed time and restart before an outcome`() {
        val controller = controller(grid = gridWithRoute(), heroCount = 1)

        assertFalse(controller.restart())
        assertTrue(controller.start())
        assertFalse(controller.restart())
        listOf(-0.1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { elapsed ->
            kotlin.test.assertFailsWith<IllegalArgumentException> {
                controller.advance(elapsed)
            }
        }
    }

    private fun controller(
        grid: DungeonGrid,
        heroCount: Int,
        heartHealth: Int = 10,
        heartDamage: Int = 10,
    ) = PrototypeRunController(
        grid = grid,
        upcomingWave = UpcomingHeroWave(
            heroType = "militia_recruit",
            heroDisplayName = "Militia Recruit",
            count = heroCount,
            heroHealth = 5,
            heartDamage = heartDamage,
            movementSpeedTilesPerSecond = 60f,
            traitDescription = "A straightforward melee fighter.",
        ),
        runDefinition = PrototypeRunDefinition.singleWave(
            heartHealth = heartHealth,
            contentPath = "test-wave.json",
        ),
    )

    private fun controllerWithGold(
        grid: DungeonGrid,
        startingGold: Int,
    ) = PrototypeRunController(
        grid = grid,
        upcomingWave = wave(),
        runDefinition = PrototypeRunDefinition.singleWave(
            heartHealth = 10,
            contentPath = "test-wave.json",
            startingGold = startingGold,
        ),
    )

    private fun defense(costGold: Int) = TrapDefinition(
        id = "purchased_spike",
        displayName = "Purchased Spike",
        damage = 5,
        cooldownSeconds = 0.25f,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
        costGold = costGold,
    )

    private fun gridWithLethalTrap(): DungeonGrid {
        val grid = gridWithRoute(hasSocket = true)
        assertTrue(
            grid.placeTrap(
                room = grid.placedRooms.single(),
                localSocketPosition = GridPosition(column = 0, row = 0),
                definition = TrapDefinition(
                    id = "spike_trap",
                    displayName = "Spike Trap",
                    damage = 5,
                    cooldownSeconds = 0.001f,
                    compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
                ),
            ),
        )
        return grid
    }

    private fun gridWithPersistentTrap(): DungeonGrid {
        val grid = gridWithRoute(hasSocket = true, width = 5)
        assertTrue(
            grid.placeTrap(
                room = grid.placedRooms.single(),
                localSocketPosition = GridPosition(column = 0, row = 0),
                definition = TrapDefinition(
                    id = "persistent_spike",
                    displayName = "Persistent Spike",
                    damage = 1,
                    cooldownSeconds = 10f,
                    compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
                ),
            ),
        )
        return grid
    }

    private fun multiWaveRunDefinition(
        heartHealth: Int = 10,
        startingGold: Int = 0,
    ) = PrototypeRunDefinition(
        heartHealth = heartHealth,
        startingGold = startingGold,
        waves = listOf(
            RunWaveDefinition(
                id = "opening",
                contentPath = "content/opening.json",
                rewardGold = 1,
            ),
            RunWaveDefinition(
                id = "finale",
                contentPath = "content/finale.json",
                rewardGold = 0,
                roomOfferBlueprintIds = roomOffer(),
            ),
        ),
        completionCondition = RunCompletionCondition.CLEAR_ALL_WAVES,
    )

    private fun threeWaveRunDefinition() = PrototypeRunDefinition(
        heartHealth = 10,
        startingGold = 0,
        waves = listOf(
            RunWaveDefinition(
                id = "opening",
                contentPath = "content/opening.json",
                rewardGold = 0,
            ),
            RunWaveDefinition(
                id = "middle",
                contentPath = "content/middle.json",
                rewardGold = 0,
                roomOfferBlueprintIds = roomOffer(),
            ),
            RunWaveDefinition(
                id = "finale",
                contentPath = "content/finale.json",
                rewardGold = 0,
                roomOfferBlueprintIds = roomOffer(),
            ),
        ),
        completionCondition = RunCompletionCondition.CLEAR_ALL_WAVES,
    )

    private fun wave(
        heroType: String = "militia_recruit",
        heartDamage: Int = 1,
    ) = UpcomingHeroWave(
        heroType = heroType,
        heroDisplayName = "Militia Recruit",
        count = 1,
        heroHealth = 5,
        heartDamage = heartDamage,
        movementSpeedTilesPerSecond = 60f,
        traitDescription = "A straightforward melee fighter.",
    )

    private fun draftRoom(id: String = "draft-room") = RoomBlueprint(
        id = id,
        displayName = "Draft Room",
        footprint = setOf(GridPosition(0, 0)),
        heartAnchor = GridPosition(0, 0),
        doors = listOf(
            RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
            RoomDoor(GridPosition(0, 0), CardinalDirection.EAST),
        ),
    )

    private fun roomOffer() = listOf(
        "draft-room",
        "alternate-room",
        "corner-room",
    )

    private fun gridWithSeparateHeartAndLethalTrap(): DungeonGrid {
        val trapSocket = GridPosition(column = 0, row = 0)
        val heartAnchor = GridPosition(column = 1, row = 0)
        val room = PlacedRoom(
            blueprint = RoomBlueprint(
                id = "heart-and-trap-room",
                displayName = "Heart and Trap Room",
                heartAnchor = heartAnchor,
                footprint = setOf(
                    trapSocket,
                    heartAnchor,
                ),
                doors = listOf(
                    RoomDoor(
                        trapSocket,
                        CardinalDirection.WEST,
                    ),
                    RoomDoor(heartAnchor, CardinalDirection.EAST),
                ),
                sockets = mapOf(trapSocket to RoomSocketType.FLOOR),
            ),
            origin = GridPosition(column = 1, row = 0),
        )
        val grid = DungeonGrid(
            width = 4,
            height = 1,
            entrance = GridPosition(column = 0, row = 0),
            placedRooms = listOf(room),
        )
        assertTrue(
            grid.placeTrap(
                room = room,
                localSocketPosition = trapSocket,
                definition = TrapDefinition(
                    id = "spike_trap",
                    displayName = "Spike Trap",
                    damage = 5,
                    cooldownSeconds = 0.001f,
                    compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
                ),
            ),
        )
        assertTrue(grid.placeOrRelocateHeart(room))
        return grid
    }

    private fun gridWithRoute(
        hasSocket: Boolean = false,
        width: Int = 3,
    ): DungeonGrid {
        val trapSocket = GridPosition(column = 0, row = 0)
        val heartAnchor = GridPosition(column = 1, row = 0)
        val room = PlacedRoom(
            blueprint = RoomBlueprint(
                id = "test-room",
                displayName = "Test Room",
                heartAnchor = heartAnchor,
                footprint = setOf(trapSocket, heartAnchor),
                doors = listOf(
                    RoomDoor(trapSocket, CardinalDirection.WEST),
                    RoomDoor(heartAnchor, CardinalDirection.EAST),
                ),
                sockets = if (hasSocket) {
                    mapOf(trapSocket to RoomSocketType.FLOOR)
                } else {
                    emptyMap()
                },
            ),
            origin = GridPosition(column = 1, row = 0),
        )
        return DungeonGrid(
            width = width,
            height = 1,
            entrance = GridPosition(column = 0, row = 0),
            placedRooms = listOf(room),
        ).also { grid ->
            assertTrue(grid.placeOrRelocateHeart(room))
        }
    }

    private fun fixedSteps(count: Int): Float =
        (FixedStepHeroSimulation.FIXED_STEP_SECONDS * count).toFloat()

    private fun heroPosition(column: Int, row: Int) =
        com.dungeonarchitect.domain.HeroGridPosition(
            column = column.toFloat(),
            row = row.toFloat(),
        )
}
