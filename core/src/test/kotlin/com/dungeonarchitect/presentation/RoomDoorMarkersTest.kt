package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomOrientation
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomDoorMarkersTest {
    @Test
    fun `rotated door markers use oriented positions and facings`() {
        val room = placedRoom(
            origin = position(2, 1),
            orientation = RoomOrientation.CLOCKWISE_90,
        )
        val grid = gridWith(room)

        assertEquals(
            setOf(
                RoomDoorGridMarker(
                    position = position(2, 2),
                    facing = CardinalDirection.NORTH,
                    isOpen = true,
                ),
                RoomDoorGridMarker(
                    position = position(2, 1),
                    facing = CardinalDirection.SOUTH,
                    isOpen = true,
                ),
            ),
            roomDoorGridMarkers(grid),
        )
    }

    @Test
    fun `door markers preserve translated position facing and open state`() {
        val room = placedRoom(origin = position(2, 1))
        val grid = gridWith(room)

        assertEquals(
            setOf(
                RoomDoorGridMarker(
                    position = position(2, 1),
                    facing = CardinalDirection.WEST,
                    isOpen = true,
                ),
                RoomDoorGridMarker(
                    position = position(3, 1),
                    facing = CardinalDirection.EAST,
                    isOpen = true,
                ),
            ),
            roomDoorGridMarkers(grid),
        )
    }

    @Test
    fun `connected doors are no longer marked open`() {
        val first = placedRoom(origin = position(1, 1))
        val second = placedRoom(origin = position(3, 1))
        val grid = gridWith(first, second)

        val markers = roomDoorGridMarkers(grid)

        assertEquals(
            false,
            markers.single {
                it.position == position(2, 1) &&
                    it.facing == CardinalDirection.EAST
            }.isOpen,
        )
        assertEquals(
            false,
            markers.single {
                it.position == position(3, 1) &&
                    it.facing == CardinalDirection.WEST
            }.isOpen,
        )
    }

    private fun gridWith(vararg rooms: PlacedRoom) = DungeonGrid(
        width = 8,
        height = 4,
        entrance = position(0, 2),
        placedRooms = rooms.toList(),
    )

    private fun placedRoom(
        origin: GridPosition,
        orientation: RoomOrientation = RoomOrientation.UNROTATED,
    ) = PlacedRoom(
        blueprint = RoomBlueprint(
            id = "test-room",
            displayName = "Test Room",
            heartAnchor = GridPosition(column = 0, row = 0),
            footprint = setOf(position(0, 0), position(1, 0)),
            doorPositions = setOf(position(0, 0), position(1, 0)),
            doorFacings = mapOf(
                position(0, 0) to CardinalDirection.WEST,
                position(1, 0) to CardinalDirection.EAST,
            ),
        ),
        origin = origin,
        orientation = orientation,
    )

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)
}
