package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RoomBlueprintTest {
    @Test
    fun `blueprint stores its identity footprint and door positions`() {
        val footprint = setOf(
            position(0, 0),
            position(1, 0),
            position(0, 1),
            position(1, 1),
        )
        val doors = setOf(position(0, 0), position(1, 1))

        val blueprint = blueprint(
            id = "guard-hall",
            displayName = "Guard Hall",
            footprint = footprint,
            doorPositions = doors,
        )

        assertEquals("guard-hall", blueprint.id)
        assertEquals("Guard Hall", blueprint.displayName)
        assertEquals(footprint, blueprint.footprint)
        assertEquals(doors, blueprint.doorPositions)
    }

    @Test
    fun `blueprint snapshots mutable input collections`() {
        val footprint = mutableSetOf(position(0, 0), position(1, 0))
        val doors = mutableSetOf(position(0, 0))
        val sockets = mutableMapOf(position(1, 0) to RoomSocketType.FLOOR)
        val blueprint = blueprint(
            footprint = footprint,
            doorPositions = doors,
            sockets = sockets,
        )

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
        val blueprint = blueprint(
            footprint = setOf(position(0, 0)),
            doorPositions = setOf(position(0, 0)),
        )

        assertEquals(emptyMap(), blueprint.sockets)
    }

    @Test
    fun `blueprint rejects a blank ID`() {
        assertFailsWith<IllegalArgumentException> {
            blueprint(id = " \t")
        }
    }

    @Test
    fun `blueprint rejects a blank display name`() {
        assertFailsWith<IllegalArgumentException> {
            blueprint(displayName = "\n")
        }
    }

    @Test
    fun `blueprint rejects a room socket outside its footprint`() {
        assertFailsWith<IllegalArgumentException> {
            blueprint(
                footprint = setOf(position(0, 0)),
                doorPositions = setOf(position(0, 0)),
                sockets = mapOf(position(1, 0) to RoomSocketType.FLOOR),
            )
        }
    }

    @Test
    fun `blueprint rejects an empty footprint`() {
        assertFailsWith<IllegalArgumentException> {
            blueprint(
                footprint = emptySet(),
                doorPositions = setOf(position(0, 0)),
            )
        }
    }

    @Test
    fun `blueprint rejects negative or unnormalized footprint positions`() {
        assertFailsWith<IllegalArgumentException> {
            blueprint(
                footprint = setOf(position(-1, 0), position(0, 0)),
                doorPositions = setOf(position(0, 0)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            blueprint(
                footprint = setOf(position(1, 1), position(2, 1)),
                doorPositions = setOf(position(1, 1)),
            )
        }
    }

    @Test
    fun `blueprint rejects a disconnected footprint`() {
        assertFailsWith<IllegalArgumentException> {
            blueprint(
                footprint = setOf(position(0, 0), position(1, 1)),
                doorPositions = setOf(position(0, 0)),
            )
        }
    }

    @Test
    fun `blueprint requires at least one door`() {
        assertFailsWith<IllegalArgumentException> {
            blueprint(
                footprint = setOf(position(0, 0)),
                doorPositions = emptySet(),
            )
        }
    }

    @Test
    fun `blueprint rejects a door outside its footprint`() {
        assertFailsWith<IllegalArgumentException> {
            blueprint(
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
            blueprint(
                footprint = footprint,
                doorPositions = setOf(position(1, 1)),
            )
        }
    }

    @Test
    fun `blueprint allows differently facing doors on the same cell`() {
        val blueprint = RoomBlueprint(
            id = "junction",
            displayName = "Junction",
            footprint = setOf(position(0, 0)),
            doors = listOf(
                RoomDoor(position(0, 0), CardinalDirection.WEST),
                RoomDoor(position(0, 0), CardinalDirection.EAST),
            ),
        )

        assertEquals(2, blueprint.doors.size)
        assertEquals(setOf(position(0, 0)), blueprint.doorPositions)
    }

    @Test
    fun `blueprint rejects repeated and inward-facing directional doors`() {
        val westDoor = RoomDoor(position(0, 0), CardinalDirection.WEST)
        assertFailsWith<IllegalArgumentException> {
            RoomBlueprint(
                id = "repeated-door",
                displayName = "Repeated Door",
                footprint = setOf(position(0, 0)),
                doors = listOf(westDoor, westDoor),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            RoomBlueprint(
                id = "inward-door",
                displayName = "Inward Door",
                footprint = setOf(position(0, 0), position(1, 0)),
                doors = listOf(
                    RoomDoor(position(0, 0), CardinalDirection.EAST),
                ),
            )
        }
    }

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)

    private fun blueprint(
        id: String = "test-room",
        displayName: String = "Test Room",
        footprint: Set<GridPosition> = setOf(position(0, 0)),
        doorPositions: Set<GridPosition> = setOf(position(0, 0)),
        sockets: Map<GridPosition, RoomSocketType> = emptyMap(),
    ) = RoomBlueprint(
        id = id,
        displayName = displayName,
        footprint = footprint,
        doorPositions = doorPositions,
        sockets = sockets,
    )
}
