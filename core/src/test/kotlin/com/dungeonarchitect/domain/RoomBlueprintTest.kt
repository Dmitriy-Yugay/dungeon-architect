package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RoomBlueprintTest {
    @Test
    fun `blueprint stores its footprint and door positions`() {
        val footprint = setOf(
            position(0, 0),
            position(1, 0),
            position(0, 1),
            position(1, 1),
        )
        val doors = setOf(position(0, 0), position(1, 1))

        val blueprint = RoomBlueprint(footprint, doors)

        assertEquals(footprint, blueprint.footprint)
        assertEquals(doors, blueprint.doorPositions)
    }

    @Test
    fun `blueprint snapshots mutable input collections`() {
        val footprint = mutableSetOf(position(0, 0), position(1, 0))
        val doors = mutableSetOf(position(0, 0))
        val sockets = mutableMapOf(position(1, 0) to RoomSocketType.FLOOR)
        val blueprint = RoomBlueprint(footprint, doors, sockets)

        footprint += position(2, 0)
        doors += position(1, 0)
        sockets.clear()

        assertEquals(setOf(position(0, 0), position(1, 0)), blueprint.footprint)
        assertEquals(setOf(position(0, 0)), blueprint.doorPositions)
        assertEquals(
            mapOf(position(1, 0) to RoomSocketType.FLOOR),
            blueprint.sockets,
        )
    }

    @Test
    fun `blueprint allows no room sockets`() {
        val blueprint = RoomBlueprint(
            footprint = setOf(position(0, 0)),
            doorPositions = setOf(position(0, 0)),
        )

        assertEquals(emptyMap(), blueprint.sockets)
    }

    @Test
    fun `blueprint rejects a room socket outside its footprint`() {
        assertFailsWith<IllegalArgumentException> {
            RoomBlueprint(
                footprint = setOf(position(0, 0)),
                doorPositions = setOf(position(0, 0)),
                sockets = mapOf(position(1, 0) to RoomSocketType.FLOOR),
            )
        }
    }

    @Test
    fun `blueprint rejects an empty footprint`() {
        assertFailsWith<IllegalArgumentException> {
            RoomBlueprint(
                footprint = emptySet(),
                doorPositions = setOf(position(0, 0)),
            )
        }
    }

    @Test
    fun `blueprint rejects negative or unnormalized footprint positions`() {
        assertFailsWith<IllegalArgumentException> {
            RoomBlueprint(
                footprint = setOf(position(-1, 0), position(0, 0)),
                doorPositions = setOf(position(0, 0)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            RoomBlueprint(
                footprint = setOf(position(1, 1), position(2, 1)),
                doorPositions = setOf(position(1, 1)),
            )
        }
    }

    @Test
    fun `blueprint rejects a disconnected footprint`() {
        assertFailsWith<IllegalArgumentException> {
            RoomBlueprint(
                footprint = setOf(position(0, 0), position(1, 1)),
                doorPositions = setOf(position(0, 0)),
            )
        }
    }

    @Test
    fun `blueprint requires at least one door`() {
        assertFailsWith<IllegalArgumentException> {
            RoomBlueprint(
                footprint = setOf(position(0, 0)),
                doorPositions = emptySet(),
            )
        }
    }

    @Test
    fun `blueprint rejects a door outside its footprint`() {
        assertFailsWith<IllegalArgumentException> {
            RoomBlueprint(
                footprint = setOf(position(0, 0), position(1, 0)),
                doorPositions = setOf(position(2, 0)),
            )
        }
    }

    @Test
    fun `blueprint rejects a door that is not on an exposed edge`() {
        val footprint = buildSet {
            for (column in 0..2) {
                for (row in 0..2) {
                    add(position(column, row))
                }
            }
        }

        assertFailsWith<IllegalArgumentException> {
            RoomBlueprint(
                footprint = footprint,
                doorPositions = setOf(position(1, 1)),
            )
        }
    }

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)
}
