package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlacedRoomTest {
    @Test
    fun `placed room translates its local footprint to grid positions`() {
        val blueprint = RoomBlueprint(
            footprint = setOf(
                position(0, 0),
                position(1, 0),
                position(0, 1),
            ),
            doorPositions = setOf(position(1, 0)),
        )

        val room = PlacedRoom(
            blueprint = blueprint,
            origin = position(4, 6),
        )

        assertEquals(
            setOf(
                position(4, 6),
                position(5, 6),
                position(4, 7),
            ),
            room.gridPositions,
        )
        assertEquals(position(5, 6), room.toGridPosition(position(1, 0)))
    }

    @Test
    fun `placed room rejects a local position outside its footprint`() {
        val blueprint = RoomBlueprint(
            footprint = setOf(position(0, 0)),
            doorPositions = setOf(position(0, 0)),
        )
        val room = PlacedRoom(
            blueprint = blueprint,
            origin = position(2, 3),
        )

        assertFailsWith<IllegalArgumentException> {
            room.toGridPosition(position(1, 0))
        }
    }

    @Test
    fun `placed room fits exactly against each grid boundary`() {
        val grid = dungeonGrid()

        assertTrue(placedRoom(origin = position(0, 0)).fitsInside(grid))
        assertTrue(placedRoom(origin = position(2, 1)).fitsInside(grid))
    }

    @Test
    fun `placed room does not fit when it crosses a grid boundary`() {
        val grid = dungeonGrid()

        assertFalse(placedRoom(origin = position(-1, 0)).fitsInside(grid))
        assertFalse(placedRoom(origin = position(0, -1)).fitsInside(grid))
        assertFalse(placedRoom(origin = position(3, 0)).fitsInside(grid))
        assertFalse(placedRoom(origin = position(0, 2)).fitsInside(grid))
    }

    @Test
    fun `placed room does not overlap a room touching its edge`() {
        val room = placedRoom(origin = position(0, 0))
        val touchingRoom = placedRoom(origin = position(2, 0))

        assertFalse(room.overlaps(touchingRoom))
        assertFalse(touchingRoom.overlaps(room))
    }

    @Test
    fun `placed room overlaps a room sharing a grid position`() {
        val room = placedRoom(origin = position(0, 0))
        val overlappingRoom = placedRoom(origin = position(1, 1))

        assertTrue(room.overlaps(overlappingRoom))
        assertTrue(overlappingRoom.overlaps(room))
    }

    @Test
    fun `placed rooms connect through cardinally adjacent doors`() {
        val room = placedRoom(
            origin = position(0, 0),
            doorPosition = position(1, 0),
        )
        val connectedRoom = placedRoom(
            origin = position(2, 0),
            doorPosition = position(0, 0),
        )

        assertTrue(room.connectsTo(connectedRoom))
        assertTrue(connectedRoom.connectsTo(room))
    }

    @Test
    fun `placed rooms do not connect when only non-door cells are adjacent`() {
        val room = placedRoom(
            origin = position(0, 0),
            doorPosition = position(0, 0),
        )
        val touchingRoom = placedRoom(
            origin = position(2, 0),
            doorPosition = position(1, 0),
        )

        assertFalse(room.connectsTo(touchingRoom))
        assertFalse(touchingRoom.connectsTo(room))
    }

    @Test
    fun `placed rooms do not connect through diagonal doors`() {
        val room = singleCellRoom(origin = position(0, 0))
        val diagonalRoom = singleCellRoom(origin = position(1, 1))

        assertFalse(room.connectsTo(diagonalRoom))
        assertFalse(diagonalRoom.connectsTo(room))
    }

    @Test
    fun `overlapping placed rooms are not connected`() {
        val room = placedRoom(origin = position(0, 0))
        val overlappingRoom = placedRoom(origin = position(1, 0))

        assertFalse(room.connectsTo(overlappingRoom))
        assertFalse(overlappingRoom.connectsTo(room))
    }

    private fun dungeonGrid() = DungeonGrid(
        width = 4,
        height = 3,
        entrance = position(0, 1),
        objective = position(3, 1),
    )

    private fun placedRoom(
        origin: GridPosition,
        doorPosition: GridPosition = position(0, 0),
    ) = PlacedRoom(
        blueprint = RoomBlueprint(
            footprint = setOf(
                position(0, 0),
                position(1, 0),
                position(0, 1),
                position(1, 1),
            ),
            doorPositions = setOf(doorPosition),
        ),
        origin = origin,
    )

    private fun singleCellRoom(origin: GridPosition) = PlacedRoom(
        blueprint = RoomBlueprint(
            footprint = setOf(position(0, 0)),
            doorPositions = setOf(position(0, 0)),
        ),
        origin = origin,
    )

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)
}
