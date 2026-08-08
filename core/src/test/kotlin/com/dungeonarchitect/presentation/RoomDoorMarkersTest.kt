package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomBlueprint
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomDoorMarkersTest {
    @Test
    fun `door marker positions translate from room local to grid coordinates`() {
        val room = placedRoom(
            origin = position(4, 6),
            doorPositions = setOf(position(0, 0), position(1, 0)),
        )

        assertEquals(
            setOf(position(4, 6), position(5, 6)),
            roomDoorGridPositions(listOf(room)),
        )
    }

    @Test
    fun `door marker positions include every door from multiple rooms`() {
        val rooms = listOf(
            placedRoom(
                origin = position(1, 1),
                doorPositions = setOf(position(0, 0), position(1, 0)),
            ),
            placedRoom(
                origin = position(5, 3),
                doorPositions = setOf(position(0, 0), position(1, 0)),
            ),
        )

        assertEquals(
            setOf(
                position(1, 1),
                position(2, 1),
                position(5, 3),
                position(6, 3),
            ),
            roomDoorGridPositions(rooms),
        )
    }

    private fun placedRoom(
        origin: GridPosition,
        doorPositions: Set<GridPosition>,
    ) = PlacedRoom(
        blueprint = RoomBlueprint(
            id = "test-room",
            displayName = "Test Room",
            footprint = setOf(position(0, 0), position(1, 0)),
            doorPositions = doorPositions,
        ),
        origin = origin,
    )

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)
}
