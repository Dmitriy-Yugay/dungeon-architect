package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HeartPlacementPreviewTest {
    @Test
    fun `hovering any oriented room cell previews its transformed heart anchor`() {
        val room = PlacedRoom(
            blueprint = RoomBlueprint(
                id = "oriented-heart-room",
                displayName = "Oriented Heart Room",
                heartAnchor = position(0, 1),
                footprint = setOf(
                    position(0, 0),
                    position(1, 0),
                    position(0, 1),
                ),
                doors = listOf(
                    RoomDoor(position(1, 0), CardinalDirection.EAST),
                ),
            ),
            origin = position(4, 2),
            orientation = RoomOrientation.CLOCKWISE_90,
        )
        val grid = gridWith(room)

        room.gridPositions.forEach { hoveredPosition ->
            val preview = grid.heartPlacementPreview(hoveredPosition)

            assertSame(room, preview.room)
            assertEquals(position(5, 3), preview.position)
            assertTrue(preview.isValid)
        }
        assertNull(grid.placedHeart)
    }

    @Test
    fun `empty cell and trap occupied anchor produce invalid previews without mutation`() {
        val anchor = position(1, 0)
        val room = PlacedRoom(
            blueprint = RoomBlueprint(
                id = "trapped-anchor-room",
                displayName = "Trapped Anchor Room",
                heartAnchor = anchor,
                footprint = setOf(position(0, 0), anchor),
                doors = listOf(
                    RoomDoor(position(0, 0), CardinalDirection.WEST),
                ),
                sockets = mapOf(anchor to RoomSocketType.FLOOR),
            ),
            origin = position(3, 2),
        )
        val grid = gridWith(room)

        val emptyPreview = grid.heartPlacementPreview(position(8, 6))
        assertNull(emptyPreview.room)
        assertEquals(position(8, 6), emptyPreview.position)
        assertFalse(emptyPreview.isValid)

        assertTrue(grid.placeTrap(room, anchor, floorTrap()))
        val occupiedPreview = grid.heartPlacementPreview(room.origin)
        assertSame(room, occupiedPreview.room)
        assertEquals(position(4, 2), occupiedPreview.position)
        assertFalse(occupiedPreview.isValid)
        assertNull(grid.placedHeart)
        assertEquals(1, grid.placedTraps.size)
    }

    private fun gridWith(room: PlacedRoom) = DungeonGrid(
        width = 10,
        height = 8,
        entrance = position(0, 0),
        placedRooms = listOf(room),
    )

    private fun floorTrap() = TrapDefinition(
        id = "floor-trap",
        displayName = "Floor Trap",
        damage = 1,
        cooldownSeconds = 1f,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
    )

    private fun position(column: Int, row: Int) = GridPosition(column, row)
}
