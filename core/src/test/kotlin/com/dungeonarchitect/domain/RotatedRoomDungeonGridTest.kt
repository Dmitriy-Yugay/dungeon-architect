package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RotatedRoomDungeonGridTest {
    @Test
    fun `trap lookup and placement use oriented socket coordinates`() {
        val blueprint = RoomBlueprint(
            id = "socket-gallery",
            displayName = "Socket Gallery",
            footprint = setOf(position(0, 0), position(1, 0)),
            doors = listOf(
                RoomDoor(position(0, 0), CardinalDirection.WEST),
                RoomDoor(position(1, 0), CardinalDirection.EAST),
            ),
            sockets = mapOf(position(1, 0) to RoomSocketType.FLOOR),
        )
        val room = PlacedRoom(
            blueprint = blueprint,
            origin = position(2, 1),
            orientation = RoomOrientation.CLOCKWISE_90,
        )
        val grid = DungeonGrid(
            width = 5,
            height = 5,
            entrance = position(0, 0),
            objective = position(4, 4),
            placedRooms = listOf(room),
        )
        val trap = floorTrap()

        val hover = assertIs<TrapSocketHoverResult.Valid>(
            grid.trapSocketHoverResult(position(2, 1), trap),
        )
        assertSame(room, hover.room)
        assertEquals(position(0, 0), hover.localSocketPosition)
        assertFalse(grid.placeTrap(room, position(1, 0), trap))
        assertTrue(grid.placeTrap(room, hover.localSocketPosition, trap))
        assertEquals(position(2, 1), grid.placedTraps.single().gridPosition)
    }

    @Test
    fun `snapping uses rotated doors and commits the oriented connection`() {
        val existingRoom = northFacingRoom(origin = position(2, 1))
        val blueprint = horizontalBlueprint(width = 2)
        val grid = DungeonGrid(
            width = 5,
            height = 5,
            entrance = position(0, 0),
            objective = position(4, 4),
            placedRooms = listOf(existingRoom),
        )

        val preview = requireNotNull(
            grid.snappedPlacementPreview(
                blueprint = blueprint,
                hoveredPosition = position(2, 3),
                orientation = RoomOrientation.CLOCKWISE_90,
            ),
        )

        assertTrue(preview.isValid)
        assertSame(blueprint, preview.room.blueprint)
        assertEquals(RoomOrientation.CLOCKWISE_90, preview.room.orientation)
        assertEquals(position(2, 2), preview.room.origin)
        assertEquals(
            setOf(position(2, 2), position(2, 3)),
            preview.room.gridPositions,
        )
        assertTrue(grid.place(preview.room))
        assertEquals(1, grid.roomDoorConnections.size)
    }

    @Test
    fun `placement validation uses rotated bounds and footprint overlap`() {
        val existingRoom = northFacingRoom(origin = position(2, 1))
        val blueprint = horizontalBlueprint(width = 2)
        val grid = DungeonGrid(
            width = 5,
            height = 4,
            entrance = position(0, 0),
            objective = position(4, 0),
            placedRooms = listOf(existingRoom),
        )

        assertTrue(
            grid.canPlace(
                PlacedRoom(
                    blueprint = blueprint,
                    origin = position(2, 2),
                    orientation = RoomOrientation.CLOCKWISE_90,
                ),
            ),
        )
        assertFalse(
            grid.canPlace(
                PlacedRoom(
                    blueprint = blueprint,
                    origin = position(2, 3),
                    orientation = RoomOrientation.CLOCKWISE_90,
                ),
            ),
        )
        assertFalse(
            grid.canPlace(
                PlacedRoom(
                    blueprint = blueprint,
                    origin = position(2, 1),
                    orientation = RoomOrientation.CLOCKWISE_90,
                ),
            ),
        )
    }

    @Test
    fun `route traverses a rotated room through its oriented doors`() {
        val room = PlacedRoom(
            blueprint = horizontalBlueprint(width = 3),
            origin = position(2, 1),
            orientation = RoomOrientation.CLOCKWISE_90,
        )
        val grid = DungeonGrid(
            width = 5,
            height = 5,
            entrance = position(2, 0),
            objective = position(2, 4),
            entranceFacing = CardinalDirection.NORTH,
            objectiveFacing = CardinalDirection.SOUTH,
            placedRooms = listOf(room),
        )

        assertEquals(
            listOf(
                position(2, 0),
                position(2, 1),
                position(2, 2),
                position(2, 3),
                position(2, 4),
            ),
            grid.entranceToObjectiveRoute,
        )
    }

    private fun northFacingRoom(origin: GridPosition) = PlacedRoom(
        blueprint = RoomBlueprint(
            id = "north-facing-room",
            displayName = "North Facing Room",
            footprint = setOf(position(0, 0)),
            doors = listOf(
                RoomDoor(position(0, 0), CardinalDirection.NORTH),
            ),
        ),
        origin = origin,
    )

    private fun horizontalBlueprint(width: Int): RoomBlueprint {
        val footprint = (0 until width)
            .mapTo(mutableSetOf()) { column -> position(column, 0) }
        return RoomBlueprint(
            id = "horizontal-$width",
            displayName = "Horizontal $width",
            footprint = footprint,
            doors = listOf(
                RoomDoor(position(0, 0), CardinalDirection.WEST),
                RoomDoor(position(width - 1, 0), CardinalDirection.EAST),
            ),
        )
    }

    private fun floorTrap() = TrapDefinition(
        id = "floor-trap",
        displayName = "Floor Trap",
        damage = 5,
        cooldownSeconds = 0.25f,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
    )

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)
}
