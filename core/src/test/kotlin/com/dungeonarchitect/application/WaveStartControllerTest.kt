package com.dungeonarchitect.application

import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomDoor
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
    fun `start is disabled while the dungeon has no placed heart`() {
        val controller = WaveStartController(
            grid = gridWithoutRoute(),
            upcomingWave = upcomingWave(),
        )

        assertFalse(controller.isStartEnabled)
        assertFalse(controller.start())
        assertNull(controller.startedWave)
    }

    @Test
    fun `start is disabled when the placed heart is disconnected from entrance`() {
        val entranceRoom = PlacedRoom(
            blueprint = RoomBlueprint(
                id = "entrance-room",
                displayName = "Entrance Room",
                heartAnchor = GridPosition(0, 0),
                footprint = setOf(GridPosition(0, 0)),
                doors = listOf(
                    RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                ),
            ),
            origin = GridPosition(1, 0),
        )
        val heartRoom = PlacedRoom(
            blueprint = RoomBlueprint(
                id = "heart-room",
                displayName = "Heart Room",
                heartAnchor = GridPosition(0, 0),
                footprint = setOf(GridPosition(0, 0)),
                doors = listOf(
                    RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                ),
            ),
            origin = GridPosition(3, 0),
        )
        val grid = DungeonGrid(
            width = 5,
            height = 1,
            entrance = GridPosition(0, 0),
            placedRooms = listOf(entranceRoom, heartRoom),
        )
        assertTrue(grid.placeOrRelocateHeart(heartRoom))
        val controller = WaveStartController(grid, upcomingWave())

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
        assertEquals(grid.entranceToHeartRoute, controller.startedWave?.route)
        assertEquals(grid.placedTraps, controller.startedWave?.traps)

        assertFalse(controller.start())
        assertEquals(wave, controller.startedWave?.wave)
        assertEquals(grid.entranceToHeartRoute, controller.startedWave?.route)

        controller.restart()

        assertTrue(controller.isStartEnabled)
        assertNull(controller.startedWave)
    }

    private fun gridWithoutRoute() = DungeonGrid(
        width = 3,
        height = 1,
        entrance = GridPosition(column = 0, row = 0),
    )

    private fun gridWithRoute(): DungeonGrid {
        val trapSocket = GridPosition(column = 0, row = 0)
        val room = PlacedRoom(
            blueprint = RoomBlueprint(
                id = "socket-room",
                displayName = "Socket Room",
                heartAnchor = GridPosition(column = 1, row = 0),
                footprint = setOf(
                    GridPosition(column = 0, row = 0),
                    GridPosition(column = 1, row = 0),
                ),
                doors = listOf(
                    RoomDoor(trapSocket, CardinalDirection.WEST),
                    RoomDoor(
                        GridPosition(column = 1, row = 0),
                        CardinalDirection.EAST,
                    ),
                ),
                sockets = mapOf(trapSocket to RoomSocketType.FLOOR),
            ),
            origin = GridPosition(column = 1, row = 0),
        )
        return DungeonGrid(
            width = 3,
            height = 1,
            entrance = GridPosition(column = 0, row = 0),
            placedRooms = listOf(room),
        ).also { grid ->
            assertTrue(grid.placeOrRelocateHeart(room))
        }
    }

    private fun upcomingWave() = UpcomingHeroWave(
        heroType = "militia_recruit",
        heroDisplayName = "Militia Recruit",
        count = 4,
        heroHealth = 10,
        heartDamage = 10,
        movementSpeedTilesPerSecond = 2f,
        traitDescription = "A straightforward melee fighter.",
    )
}
