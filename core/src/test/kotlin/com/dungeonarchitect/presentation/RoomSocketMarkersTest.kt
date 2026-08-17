package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomOrientation
import com.dungeonarchitect.domain.RoomSocketType
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomSocketMarkersTest {
    @Test
    fun `rotated socket marker uses oriented local position`() {
        val room = placedRoom(
            origin = position(4, 6),
            sockets = mapOf(position(1, 0) to RoomSocketType.WALL),
            orientation = RoomOrientation.CLOCKWISE_90,
        )

        assertEquals(
            setOf(
                RoomSocketGridMarker(
                    position = position(4, 6),
                    type = RoomSocketType.WALL,
                ),
            ),
            roomSocketGridMarkers(listOf(room)),
        )
    }

    @Test
    fun `socket marker translates its position and preserves its type`() {
        val room = placedRoom(
            origin = position(4, 6),
            sockets = mapOf(position(1, 0) to RoomSocketType.WALL),
        )

        assertEquals(
            setOf(
                RoomSocketGridMarker(
                    position = position(5, 6),
                    type = RoomSocketType.WALL,
                ),
            ),
            roomSocketGridMarkers(listOf(room)),
        )
    }

    @Test
    fun `socket markers include typed sockets from multiple rooms`() {
        val rooms = listOf(
            placedRoom(
                origin = position(1, 1),
                sockets = mapOf(
                    position(0, 0) to RoomSocketType.FLOOR,
                    position(1, 0) to RoomSocketType.WALL,
                ),
            ),
            placedRoom(
                origin = position(5, 3),
                sockets = mapOf(position(1, 0) to RoomSocketType.FLOOR),
            ),
        )

        assertEquals(
            setOf(
                RoomSocketGridMarker(position(1, 1), RoomSocketType.FLOOR),
                RoomSocketGridMarker(position(2, 1), RoomSocketType.WALL),
                RoomSocketGridMarker(position(6, 3), RoomSocketType.FLOOR),
            ),
            roomSocketGridMarkers(rooms),
        )
    }

    private fun placedRoom(
        origin: GridPosition,
        sockets: Map<GridPosition, RoomSocketType>,
        orientation: RoomOrientation = RoomOrientation.UNROTATED,
    ) = PlacedRoom(
        blueprint = RoomBlueprint(
            id = "test-room",
            displayName = "Test Room",
            footprint = setOf(position(0, 0), position(1, 0)),
            doorPositions = setOf(position(0, 0)),
            sockets = sockets,
        ),
        origin = origin,
        orientation = orientation,
    )

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)
}
