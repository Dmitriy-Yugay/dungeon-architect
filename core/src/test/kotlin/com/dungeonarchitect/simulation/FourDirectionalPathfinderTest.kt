package com.dungeonarchitect.simulation

import com.dungeonarchitect.domain.GridPosition
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FourDirectionalPathfinderTest {
    @Test
    fun `finds a shortest path containing both endpoints`() {
        val walkablePositions = buildSet {
            for (column in 0..2) {
                for (row in 0..2) {
                    add(position(column, row))
                }
            }
        }

        val path = FourDirectionalPathfinder.findPath(
            walkablePositions = walkablePositions,
            start = position(0, 0),
            end = position(2, 2),
        )

        assertEquals(
            listOf(
                position(0, 0),
                position(1, 0),
                position(2, 0),
                position(2, 1),
                position(2, 2),
            ),
            path,
        )
        assertTrue(
            path.orEmpty().zipWithNext().all { (first, second) ->
                abs(first.column - second.column) + abs(first.row - second.row) == 1
            },
        )
    }

    @Test
    fun `returns no path when walkable positions are disconnected`() {
        val path = FourDirectionalPathfinder.findPath(
            walkablePositions = setOf(
                position(0, 0),
                position(1, 0),
                position(3, 0),
                position(4, 0),
            ),
            start = position(0, 0),
            end = position(4, 0),
        )

        assertNull(path)
    }

    @Test
    fun `returns no path when either endpoint is not walkable`() {
        val walkablePositions = setOf(position(0, 0), position(1, 0))

        assertNull(
            FourDirectionalPathfinder.findPath(
                walkablePositions = walkablePositions,
                start = position(-1, 0),
                end = position(1, 0),
            ),
        )
        assertNull(
            FourDirectionalPathfinder.findPath(
                walkablePositions = walkablePositions,
                start = position(0, 0),
                end = position(2, 0),
            ),
        )
    }

    @Test
    fun `coordinate extremes do not wrap into adjacent positions`() {
        assertNull(
            FourDirectionalPathfinder.findPath(
                walkablePositions = setOf(
                    position(Int.MAX_VALUE, 0),
                    position(Int.MIN_VALUE, 0),
                ),
                start = position(Int.MAX_VALUE, 0),
                end = position(Int.MIN_VALUE, 0),
            ),
        )
        assertNull(
            FourDirectionalPathfinder.findPath(
                walkablePositions = setOf(
                    position(0, Int.MAX_VALUE),
                    position(0, Int.MIN_VALUE),
                ),
                start = position(0, Int.MAX_VALUE),
                end = position(0, Int.MIN_VALUE),
            ),
        )
    }

    @Test
    fun `returns the endpoint when start and end are the same walkable position`() {
        val endpoint = position(3, 4)

        assertEquals(
            listOf(endpoint),
            FourDirectionalPathfinder.findPath(
                walkablePositions = setOf(endpoint),
                start = endpoint,
                end = endpoint,
            ),
        )
    }

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)
}
