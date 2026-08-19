package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomDoor
import com.dungeonarchitect.domain.RoomOrientation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DungeonHeartMarkersTest {
    @Test
    fun `marker uses the placed heart translated oriented anchor`() {
        val room = PlacedRoom(
            blueprint = RoomBlueprint(
                id = "marker-room",
                displayName = "Marker Room",
                heartAnchor = GridPosition(0, 1),
                footprint = setOf(
                    GridPosition(0, 0),
                    GridPosition(1, 0),
                    GridPosition(0, 1),
                ),
                doors = listOf(
                    RoomDoor(GridPosition(1, 0), CardinalDirection.EAST),
                ),
            ),
            origin = GridPosition(4, 2),
            orientation = RoomOrientation.CLOCKWISE_90,
        )
        val grid = DungeonGrid(
            width = 10,
            height = 8,
            entrance = GridPosition(0, 0),
            placedRooms = listOf(room),
        )
        assertNull(placedDungeonHeartGridMarker(grid))

        assertTrue(grid.placeOrRelocateHeart(room))

        assertEquals(
            PlacedDungeonHeartGridMarker(GridPosition(5, 3)),
            placedDungeonHeartGridMarker(grid),
        )
    }
}
