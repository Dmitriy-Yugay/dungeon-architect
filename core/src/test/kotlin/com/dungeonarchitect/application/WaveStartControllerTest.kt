package com.dungeonarchitect.application

import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomBlueprint
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
    fun `ready route starts the wave exactly once and records its route`() {
        val grid = gridWithRoute()
        val wave = upcomingWave()
        val controller = WaveStartController(grid, wave)

        assertTrue(controller.isStartEnabled)
        assertTrue(controller.start())
        assertFalse(controller.isStartEnabled)
        assertEquals(wave, controller.startedWave?.wave)
        assertEquals(grid.entranceToObjectiveRoute, controller.startedWave?.route)

        assertFalse(controller.start())
        assertEquals(wave, controller.startedWave?.wave)
        assertEquals(grid.entranceToObjectiveRoute, controller.startedWave?.route)
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
                    footprint = setOf(GridPosition(column = 0, row = 0)),
                    doorPositions = setOf(GridPosition(column = 0, row = 0)),
                ),
                origin = GridPosition(column = 1, row = 0),
            ),
        ),
    )

    private fun upcomingWave() = UpcomingHeroWave(
        heroType = "militia_recruit",
        heroDisplayName = "Militia Recruit",
        count = 4,
        movementSpeedTilesPerSecond = 2f,
        traitDescription = "A straightforward melee fighter.",
    )
}
