package com.dungeonarchitect.application

import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.UpcomingHeroWave
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WaveStartControllerTest {
    @Test
    fun `start is disabled while the dungeon has no complete route`() {
        val controller = WaveStartController(
            grid = gridWithoutRoute(),
            upcomingWave = upcomingWave(),
        )

        assertFalse(controller.isStartEnabled)
        assertFalse(controller.start())
        assertNull(controller.startedWave)
    }

    @Test
    fun `ready route starts the wave exactly once and snapshots its combat setup`() {
        val grid = gridWithRoute()
        val room = grid.placedRooms.single()
        assertTrue(
            grid.placeTrap(
                room = room,
                localSocketPosition = GridPosition(column = 0, row = 0),
                definition = TrapDefinition(
                    id = "spike_trap",
                    displayName = "Spike Trap",
                    damage = 5,
                    cooldownSeconds = 0.25f,
                    compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
                ),
            ),
        )
        val wave = upcomingWave()
        val controller = WaveStartController(grid, wave)

        assertTrue(controller.isStartEnabled)
        assertTrue(controller.start())
        assertFalse(controller.isStartEnabled)
        assertEquals(wave, controller.startedWave?.wave)
        assertEquals(grid.entranceToObjectiveRoute, controller.startedWave?.route)
        assertEquals(grid.placedTraps, controller.startedWave?.traps)

        assertFalse(controller.start())
        assertEquals(wave, controller.startedWave?.wave)
        assertEquals(grid.entranceToObjectiveRoute, controller.startedWave?.route)

        controller.restart()

        assertTrue(controller.isStartEnabled)
        assertNull(controller.startedWave)
    }

    private fun gridWithoutRoute() = DungeonGrid(
        width = 3,
        height = 1,
        entrance = GridPosition(column = 0, row = 0),
        objective = GridPosition(column = 2, row = 0),
    )

    private fun gridWithRoute() = DungeonGrid(
        width = 3,
        height = 1,
        entrance = GridPosition(column = 0, row = 0),
        objective = GridPosition(column = 2, row = 0),
        placedRooms = listOf(
            PlacedRoom(
                blueprint = RoomBlueprint(
                    id = "socket-room",
                    displayName = "Socket Room",
                    footprint = setOf(GridPosition(column = 0, row = 0)),
                    doorPositions = setOf(GridPosition(column = 0, row = 0)),
                    sockets = mapOf(
                        GridPosition(column = 0, row = 0) to
                            RoomSocketType.FLOOR,
                    ),
                ),
                origin = GridPosition(column = 1, row = 0),
            ),
        ),
    )

    private fun upcomingWave() = UpcomingHeroWave(
        heroType = "militia_recruit",
        heroDisplayName = "Militia Recruit",
        count = 4,
        heroHealth = 10,
        objectiveDamage = 10,
        movementSpeedTilesPerSecond = 2f,
        traitDescription = "A straightforward melee fighter.",
    )
}
