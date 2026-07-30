package com.dungeonarchitect.application

import com.dungeonarchitect.domain.GridPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrototypeScreenTest {
    @Test
    fun `prototype grid starts with one authored room`() {
        val room = PrototypeScreen.prototypeGrid().placedRooms.single()

        assertEquals(GridPosition(column = 6, row = 3), room.origin)
        assertEquals(
            setOf(
                GridPosition(column = 6, row = 3),
                GridPosition(column = 7, row = 3),
                GridPosition(column = 8, row = 3),
                GridPosition(column = 6, row = 4),
                GridPosition(column = 7, row = 4),
                GridPosition(column = 8, row = 4),
                GridPosition(column = 6, row = 5),
                GridPosition(column = 7, row = 5),
                GridPosition(column = 8, row = 5),
            ),
            room.gridPositions,
        )
    }

    @Test
    fun `placement preview uses the authored blueprint at the hovered origin`() {
        val grid = PrototypeScreen.prototypeGrid()

        val preview = PrototypeScreen.placementPreview(
            grid = grid,
            hoveredPosition = GridPosition(column = 3, row = 3),
        )!!

        assertEquals(GridPosition(column = 3, row = 3), preview.room.origin)
        assertEquals(grid.placedRooms.single().blueprint, preview.room.blueprint)
        assertTrue(preview.isValid)
    }

    @Test
    fun `placement preview reports invalid candidate without changing placed rooms`() {
        val grid = PrototypeScreen.prototypeGrid()

        val preview = PrototypeScreen.placementPreview(
            grid = grid,
            hoveredPosition = GridPosition(column = 6, row = 3),
        )!!

        assertFalse(preview.isValid)
        assertEquals(1, grid.placedRooms.size)
    }

    @Test
    fun `placement preview is absent when the pointer is outside the grid`() {
        assertNull(
            PrototypeScreen.placementPreview(
                grid = PrototypeScreen.prototypeGrid(),
                hoveredPosition = null,
            ),
        )
    }
}
