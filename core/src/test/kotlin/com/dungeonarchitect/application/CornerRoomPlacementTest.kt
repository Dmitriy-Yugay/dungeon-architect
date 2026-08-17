package com.dungeonarchitect.application

import com.dungeonarchitect.content.RoomBlueprintParser
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomDoor
import com.dungeonarchitect.domain.RoomOrientation
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CornerRoomPlacementTest {
    @Test
    fun `corner rotations extend every cardinal frontier without overlap`() {
        val cornerBlueprint = RoomBlueprintParser.parse(cornerRoomJson())
        val cases = listOf(
            RotationCase(
                frontierFacing = CardinalDirection.EAST,
                orientation = RoomOrientation.UNROTATED,
                remainingFacing = CardinalDirection.NORTH,
            ),
            RotationCase(
                frontierFacing = CardinalDirection.SOUTH,
                orientation = RoomOrientation.CLOCKWISE_90,
                remainingFacing = CardinalDirection.EAST,
            ),
            RotationCase(
                frontierFacing = CardinalDirection.WEST,
                orientation = RoomOrientation.CLOCKWISE_180,
                remainingFacing = CardinalDirection.SOUTH,
            ),
            RotationCase(
                frontierFacing = CardinalDirection.NORTH,
                orientation = RoomOrientation.CLOCKWISE_270,
                remainingFacing = CardinalDirection.WEST,
            ),
        )

        cases.forEach { case ->
            val existingRoom = frontierRoom(case.frontierFacing)
            val grid = gridWith(existingRoom)
            val hoveredPosition = case.frontierFacing.move(existingRoom.origin)

            val preview = requireNotNull(
                grid.snappedPlacementPreview(
                    blueprint = cornerBlueprint,
                    hoveredPosition = hoveredPosition,
                    orientation = case.orientation,
                ),
            )

            assertTrue(preview.isValid, case.orientation.name)
            assertSame(cornerBlueprint, preview.room.blueprint)
            assertEquals(case.orientation, preview.room.orientation)
            assertFalse(preview.room.overlaps(existingRoom), case.orientation.name)
            assertTrue(grid.place(preview.room), case.orientation.name)
            assertEquals(1, grid.roomDoorConnections.size, case.orientation.name)

            val remainingDoor = grid.openRoomDoors.single()
            assertEquals(preview.room, remainingDoor.room, case.orientation.name)
            assertEquals(
                case.remainingFacing,
                remainingDoor.door.facing,
                case.orientation.name,
            )
            assertFalse(
                grid.placedRooms[0].overlaps(grid.placedRooms[1]),
                case.orientation.name,
            )
        }
    }

    private fun gridWith(room: PlacedRoom) = DungeonGrid(
        width = 12,
        height = 12,
        entrance = position(0, 0),
        objective = position(11, 11),
        placedRooms = listOf(room),
    )

    private fun frontierRoom(facing: CardinalDirection) = PlacedRoom(
        blueprint = RoomBlueprint(
            id = "frontier-${facing.name.lowercase()}",
            displayName = "Frontier ${facing.name}",
            heartAnchor = GridPosition(column = 0, row = 0),
            footprint = setOf(position(0, 0)),
            doors = listOf(RoomDoor(position(0, 0), facing)),
        ),
        origin = position(5, 5),
    )

    private fun cornerRoomJson(): String {
        val config = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("assets/content/corner-room.json") }
            .firstOrNull { Files.isRegularFile(it) }
            ?: error("Could not locate authored corner-room content.")
        return Files.readString(config)
    }

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)

    private data class RotationCase(
        val frontierFacing: CardinalDirection,
        val orientation: RoomOrientation,
        val remainingFacing: CardinalDirection,
    )
}
