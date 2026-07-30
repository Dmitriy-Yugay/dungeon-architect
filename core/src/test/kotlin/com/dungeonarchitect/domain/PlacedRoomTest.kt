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

    private fun dungeonGrid() = DungeonGrid(
        width = 4,
        height = 3,
        entrance = position(0, 1),
        objective = position(3, 1),
    )

    private fun placedRoom(origin: GridPosition) = PlacedRoom(
        blueprint = RoomBlueprint(
            footprint = setOf(
                position(0, 0),
                position(1, 0),
                position(0, 1),
                position(1, 1),
            ),
            doorPositions = setOf(position(0, 0)),
        ),
        origin = origin,
    )

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)
}
