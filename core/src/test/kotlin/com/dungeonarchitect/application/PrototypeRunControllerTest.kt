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
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import com.dungeonarchitect.simulation.HeroArrived
import com.dungeonarchitect.simulation.HeroDamaged
import com.dungeonarchitect.simulation.HeroDied
import com.dungeonarchitect.simulation.HeroSpawned
import com.dungeonarchitect.simulation.ObjectiveDamaged
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

            repeat(heroCount) { resolvedBeforeStep ->
                controller.advance(elapsedSeconds = fixedSteps(2))

                assertEquals(resolvedBeforeStep + 1, controller.resolvedHeroCount)
                assertEquals(
                    if (resolvedBeforeStep + 1 == heroCount) {
                        PrototypeRunPhase.VICTORY
                    } else {
                        PrototypeRunPhase.RUNNING
                    },
                    controller.phase,
                )
            }

            assertEquals(10, controller.objectiveHealth)
            assertTrue(controller.isControlEnabled)
        }
    }

    @Test
    fun `hero arrival damages the objective and zero health causes defeat`() {
        listOf(1, 4).forEach { heroCount ->
            val controller = controller(
                grid = gridWithRoute(),
                heroCount = heroCount,
            )
            assertTrue(controller.start())

            controller.advance(elapsedSeconds = fixedSteps(2))

            assertEquals(PrototypeRunPhase.DEFEAT, controller.phase)
            assertEquals(0, controller.objectiveHealth)
            assertEquals(1, controller.resolvedHeroCount)
        }
    }

    @Test
    fun `surviving objective receives damage from successive heroes`() {
        val controller = controller(
            grid = gridWithRoute(),
            heroCount = 2,
            objectiveHealth = 15,
            objectiveDamage = 10,
        )
        assertTrue(controller.start())

        controller.advance(elapsedSeconds = fixedSteps(2))

        assertEquals(PrototypeRunPhase.RUNNING, controller.phase)
        assertEquals(5, controller.objectiveHealth)
        assertEquals(1, controller.resolvedHeroCount)

        controller.advance(elapsedSeconds = fixedSteps(2))

        assertEquals(PrototypeRunPhase.DEFEAT, controller.phase)
        assertEquals(0, controller.objectiveHealth)
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
        assertEquals(singleChunk.objectiveHealth, fourChunks.objectiveHealth)
        assertEquals(singleChunk.resolvedHeroCount, fourChunks.resolvedHeroCount)
        assertEquals(singleChunk.heroState, fourChunks.heroState)
        assertEquals(singleChunk.events, fourChunks.events)
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
                WaveResolved(WaveOutcome.VICTORY, objectiveHealth = 10),
            ),
            controller.events,
        )
    }

    @Test
    fun `arrivals emit actual objective damage before defeat`() {
        val controller = controller(
            grid = gridWithRoute(),
            heroCount = 2,
            objectiveHealth = 15,
            objectiveDamage = 10,
        )
        assertTrue(controller.start())

        controller.advance(elapsedSeconds = fixedSteps(4))

        assertEquals(
            listOf(
                HeroSpawned(1, "militia_recruit", heroPosition(0, 0), 5),
                HeroArrived(heroNumber = 1, position = heroPosition(2, 0)),
                ObjectiveDamaged(1, damage = 10, remainingHealth = 5),
                HeroSpawned(2, "militia_recruit", heroPosition(0, 0), 5),
                HeroArrived(heroNumber = 2, position = heroPosition(2, 0)),
                ObjectiveDamaged(2, damage = 5, remainingHealth = 0),
                WaveResolved(WaveOutcome.DEFEAT, objectiveHealth = 0),
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
        assertEquals(PrototypeRunPhase.VICTORY, controller.phase)
        val completedRunEvents = controller.events

        assertTrue(controller.restart())

        assertEquals(PrototypeRunPhase.BUILDING, controller.phase)
        assertEquals(10, controller.objectiveHealth)
        assertEquals(0, controller.resolvedHeroCount)
        assertNull(controller.heroState)
        assertTrue(controller.isStartEnabled)
        assertEquals(rooms, grid.placedRooms)
        assertEquals(traps, grid.placedTraps)
        assertEquals(emptyList(), controller.events)
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
        assertEquals(PrototypeRunPhase.VICTORY, controller.phase)
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
            objectiveHealth = 30,
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
        assertEquals(PrototypeRunPhase.VICTORY, victoryController.phase)

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
        assertEquals(PrototypeRunPhase.DEFEAT, defeatController.phase)

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
        assertEquals(PrototypeRunPhase.DEFEAT, controller.phase)

        assertTrue(controller.restart())

        assertEquals(PrototypeRunPhase.BUILDING, controller.phase)
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
            objectiveHealth = 30,
        )
        assertTrue(runningController.start())

        val victoryGrid = gridWithLethalTrap()
        val victoryController = controller(grid = victoryGrid, heroCount = 1)
        assertTrue(victoryController.start())
        victoryController.advance(elapsedSeconds = fixedSteps(2))
        assertEquals(PrototypeRunPhase.VICTORY, victoryController.phase)

        val defeatGrid = gridWithRoute()
        val defeatController = controller(grid = defeatGrid, heroCount = 1)
        assertTrue(defeatController.start())
        defeatController.advance(elapsedSeconds = fixedSteps(2))
        assertEquals(PrototypeRunPhase.DEFEAT, defeatController.phase)

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
        objectiveHealth: Int = 10,
        objectiveDamage: Int = 10,
    ) = PrototypeRunController(
        grid = grid,
        upcomingWave = UpcomingHeroWave(
            heroType = "militia_recruit",
            heroDisplayName = "Militia Recruit",
            count = heroCount,
            heroHealth = 5,
            objectiveDamage = objectiveDamage,
            movementSpeedTilesPerSecond = 60f,
            traitDescription = "A straightforward melee fighter.",
        ),
        runDefinition = PrototypeRunDefinition(
            objectiveHealth = objectiveHealth,
        ),
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

    private fun gridWithSeparateHeartAndLethalTrap(): DungeonGrid {
        val trapSocket = GridPosition(column = 1, row = 0)
        val room = PlacedRoom(
            blueprint = RoomBlueprint(
                id = "heart-and-trap-room",
                displayName = "Heart and Trap Room",
                heartAnchor = GridPosition(column = 0, row = 0),
                footprint = setOf(
                    GridPosition(column = 0, row = 0),
                    trapSocket,
                ),
                doors = listOf(
                    RoomDoor(
                        GridPosition(column = 0, row = 0),
                        CardinalDirection.WEST,
                    ),
                    RoomDoor(trapSocket, CardinalDirection.EAST),
                ),
                sockets = mapOf(trapSocket to RoomSocketType.FLOOR),
            ),
            origin = GridPosition(column = 1, row = 0),
        )
        val grid = DungeonGrid(
            width = 4,
            height = 1,
            entrance = GridPosition(column = 0, row = 0),
            objective = GridPosition(column = 3, row = 0),
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
        return grid
    }

    private fun gridWithRoute(
        hasSocket: Boolean = false,
    ) = DungeonGrid(
        width = 3,
        height = 1,
        entrance = GridPosition(column = 0, row = 0),
        objective = GridPosition(column = 2, row = 0),
        placedRooms = listOf(
            PlacedRoom(
                blueprint = RoomBlueprint(
                    id = "test-room",
                    displayName = "Test Room",
                    heartAnchor = GridPosition(column = 0, row = 0),
                    footprint = setOf(GridPosition(column = 0, row = 0)),
                    doors = listOf(
                        RoomDoor(
                            position = GridPosition(column = 0, row = 0),
                            facing = CardinalDirection.WEST,
                        ),
                        RoomDoor(
                            position = GridPosition(column = 0, row = 0),
                            facing = CardinalDirection.EAST,
                        ),
                    ),
                    sockets = if (hasSocket) {
                        mapOf(
                            GridPosition(column = 0, row = 0) to
                                RoomSocketType.FLOOR,
                        )
                    } else {
                        emptyMap()
                    },
                ),
                origin = GridPosition(column = 1, row = 0),
            ),
        ),
    )

    private fun fixedSteps(count: Int): Float =
        (FixedStepHeroSimulation.FIXED_STEP_SECONDS * count).toFloat()

    private fun heroPosition(column: Int, row: Int) =
        com.dungeonarchitect.domain.HeroGridPosition(
            column = column.toFloat(),
            row = row.toFloat(),
        )
}
