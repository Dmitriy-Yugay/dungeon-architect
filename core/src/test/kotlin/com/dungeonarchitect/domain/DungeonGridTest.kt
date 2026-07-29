package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DungeonGridTest {
    @Test
    fun `grid exposes its entrance and objective`() {
        val grid = dungeonGrid()

        assertEquals(TileType.ENTRANCE, grid.tileAt(grid.entrance))
        assertEquals(TileType.OBJECTIVE, grid.tileAt(grid.objective))
        assertEquals(TileType.EMPTY, grid.tileAt(column = 1, row = 1))
    }

    @Test
    fun `grid contains only positions inside its bounds`() {
        val grid = dungeonGrid()

        assertTrue(grid.contains(column = 0, row = 0))
        assertTrue(grid.contains(column = grid.width - 1, row = grid.height - 1))
        assertFalse(grid.contains(column = -1, row = 0))
        assertFalse(grid.contains(column = grid.width, row = 0))
        assertFalse(grid.contains(column = 0, row = grid.height))
    }

    @Test
    fun `grid rejects invalid dimensions`() {
        assertFailsWith<IllegalArgumentException> {
            dungeonGrid(width = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            dungeonGrid(height = 0)
        }
    }

    @Test
    fun `grid rejects invalid special tile positions`() {
        assertFailsWith<IllegalArgumentException> {
            dungeonGrid(entrance = GridPosition(column = -1, row = 0))
        }
        assertFailsWith<IllegalArgumentException> {
            dungeonGrid(objective = GridPosition(column = 4, row = 2))
        }
        assertFailsWith<IllegalArgumentException> {
            dungeonGrid(
                entrance = GridPosition(column = 0, row = 1),
                objective = GridPosition(column = 0, row = 1),
            )
        }
    }

    @Test
    fun `grid rejects tile access outside its bounds`() {
        val grid = dungeonGrid()

        assertFailsWith<IllegalArgumentException> {
            grid.tileAt(column = grid.width, row = 0)
        }
    }

    private fun dungeonGrid(
        width: Int = 4,
        height: Int = 3,
        entrance: GridPosition = GridPosition(column = 0, row = 1),
        objective: GridPosition = GridPosition(column = width - 1, row = 1),
    ) = DungeonGrid(
        width = width,
        height = height,
        entrance = entrance,
        objective = objective,
    )
}
