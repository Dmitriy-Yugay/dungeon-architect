package com.dungeonarchitect.application

import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

        assertTrue(controller.restart())

        assertEquals(PrototypeRunPhase.BUILDING, controller.phase)
        assertEquals(10, controller.objectiveHealth)
        assertEquals(0, controller.resolvedHeroCount)
        assertNull(controller.heroState)
        assertTrue(controller.isStartEnabled)
        assertEquals(rooms, grid.placedRooms)
        assertEquals(traps, grid.placedTraps)

        assertTrue(controller.start())
        controller.advance(elapsedSeconds = fixedSteps(2))
        assertEquals(PrototypeRunPhase.VICTORY, controller.phase)
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
                    footprint = setOf(GridPosition(column = 0, row = 0)),
                    doorPositions = setOf(GridPosition(column = 0, row = 0)),
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
}
