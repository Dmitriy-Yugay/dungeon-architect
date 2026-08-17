package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class RoomOrientationTest {
    @Test
    fun `all orientations transform asymmetric room geometry consistently`() {
        val blueprint = asymmetricBlueprint()
        val expected = mapOf(
            RoomOrientation.UNROTATED to ExpectedGeometry(
                footprint = setOf(
                    position(0, 0),
                    position(1, 0),
                    position(2, 0),
                    position(0, 1),
                ),
                doors = setOf(
                    RoomDoor(position(0, 1), CardinalDirection.NORTH),
                    RoomDoor(position(2, 0), CardinalDirection.EAST),
                ),
                sockets = mapOf(
                    position(1, 0) to RoomSocketType.FLOOR,
                    position(0, 1) to RoomSocketType.WALL,
                ),
            ),
            RoomOrientation.CLOCKWISE_90 to ExpectedGeometry(
                footprint = setOf(
                    position(0, 2),
                    position(0, 1),
                    position(0, 0),
                    position(1, 2),
                ),
                doors = setOf(
                    RoomDoor(position(1, 2), CardinalDirection.EAST),
                    RoomDoor(position(0, 0), CardinalDirection.SOUTH),
                ),
                sockets = mapOf(
                    position(0, 1) to RoomSocketType.FLOOR,
                    position(1, 2) to RoomSocketType.WALL,
                ),
            ),
            RoomOrientation.CLOCKWISE_180 to ExpectedGeometry(
                footprint = setOf(
                    position(2, 1),
                    position(1, 1),
                    position(0, 1),
                    position(2, 0),
                ),
                doors = setOf(
                    RoomDoor(position(2, 0), CardinalDirection.SOUTH),
                    RoomDoor(position(0, 1), CardinalDirection.WEST),
                ),
                sockets = mapOf(
                    position(1, 1) to RoomSocketType.FLOOR,
                    position(2, 0) to RoomSocketType.WALL,
                ),
            ),
            RoomOrientation.CLOCKWISE_270 to ExpectedGeometry(
                footprint = setOf(
                    position(1, 0),
                    position(1, 1),
                    position(1, 2),
                    position(0, 0),
                ),
                doors = setOf(
                    RoomDoor(position(0, 0), CardinalDirection.WEST),
                    RoomDoor(position(1, 2), CardinalDirection.NORTH),
                ),
                sockets = mapOf(
                    position(1, 1) to RoomSocketType.FLOOR,
                    position(0, 0) to RoomSocketType.WALL,
                ),
            ),
        )

        expected.forEach { (orientation, expectedGeometry) ->
            val geometry = blueprint.geometry(orientation)

            assertEquals(
                expectedGeometry.footprint,
                geometry.footprint,
                orientation.name,
            )
            assertEquals(expectedGeometry.doors, geometry.doors, orientation.name)
            assertEquals(
                expectedGeometry.doors.mapTo(
                    mutableSetOf(),
                    RoomDoor::position,
                ),
                geometry.doorPositions,
                orientation.name,
            )
            assertEquals(expectedGeometry.sockets, geometry.sockets, orientation.name)
            assertEquals(
                0,
                geometry.footprint.minOf(GridPosition::column),
                orientation.name,
            )
            assertEquals(0, geometry.footprint.minOf(GridPosition::row), orientation.name)
        }
    }

    @Test
    fun `four clockwise quarter turns return orientation and geometry to identity`() {
        val blueprint = asymmetricBlueprint()
        val original = blueprint.geometry()

        val fullTurn = (1..4).fold(RoomOrientation.UNROTATED) { orientation, _ ->
            orientation.rotateClockwise()
        }
        val transformed = blueprint.geometry(fullTurn)

        assertEquals(RoomOrientation.UNROTATED, fullTurn)
        assertEquals(original.footprint, transformed.footprint)
        assertEquals(original.doors, transformed.doors)
        assertEquals(original.sockets, transformed.sockets)
    }

    private fun asymmetricBlueprint() = RoomBlueprint(
        id = "asymmetric-gallery",
        displayName = "Asymmetric Gallery",
        footprint = setOf(
            position(0, 0),
            position(1, 0),
            position(2, 0),
            position(0, 1),
        ),
        doors = listOf(
            RoomDoor(position(0, 1), CardinalDirection.NORTH),
            RoomDoor(position(2, 0), CardinalDirection.EAST),
        ),
        sockets = mapOf(
            position(1, 0) to RoomSocketType.FLOOR,
            position(0, 1) to RoomSocketType.WALL,
        ),
    )

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)

    private data class ExpectedGeometry(
        val footprint: Set<GridPosition>,
        val doors: Set<RoomDoor>,
        val sockets: Map<GridPosition, RoomSocketType>,
    )
}
