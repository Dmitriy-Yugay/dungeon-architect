package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomAttachmentTargetTest {
    @Test
    fun `empty dungeon exposes its entrance as the initial attachment`() {
        val grid = grid()

        assertEquals(
            listOf(
                RoomAttachmentTarget(
                    position = GridPosition(0, 2),
                    facing = CardinalDirection.EAST,
                    type = RoomAttachmentTargetType.ENTRANCE,
                ),
            ),
            grid.roomAttachmentTargets,
        )
    }

    @Test
    fun `placing a room replaces entrance target with its open doors`() {
        val grid = grid()
        val entranceTarget = grid.roomAttachmentTargets.single()
        val preview = assertNotNull(
            grid.targetedPlacementPreview(room(), entranceTarget),
        )

        assertTrue(preview.isValid)
        assertTrue(grid.place(preview.room))
        assertEquals(
            listOf(
                RoomAttachmentTarget(
                    position = GridPosition(2, 2),
                    facing = CardinalDirection.EAST,
                    type = RoomAttachmentTargetType.OPEN_DOOR,
                ),
            ),
            grid.roomAttachmentTargets,
        )
    }

    @Test
    fun `rotation keeps an exact invalid ghost at the selected target`() {
        val grid = grid()
        val target = grid.roomAttachmentTargets.single()

        val original = assertNotNull(
            grid.targetedPlacementPreview(room(), target, RoomOrientation.UNROTATED),
        )
        val rotated = assertNotNull(
            grid.targetedPlacementPreview(room(), target, RoomOrientation.CLOCKWISE_90),
        )

        assertTrue(original.isValid)
        assertFalse(rotated.isValid)
        assertEquals(RoomPlacementInvalidReason.DOOR_FACES_AWAY, rotated.invalidReason)
        assertEquals(target, rotated.attachmentTarget)
        assertNotNull(rotated.connectingDoor)
        assertTrue(target.roomPosition in rotated.room.gridPositions)
    }

    @Test
    fun `stale attachment no longer creates a ghost`() {
        val grid = grid()
        val target = grid.roomAttachmentTargets.single()
        val preview = assertNotNull(grid.targetedPlacementPreview(room(), target))
        assertTrue(grid.place(preview.room))

        assertNull(grid.targetedPlacementPreview(room(), target))
    }

    private fun grid() = DungeonGrid(
        width = 8,
        height = 5,
        entrance = GridPosition(0, 2),
        entranceFacing = CardinalDirection.EAST,
    )

    private fun room() = RoomBlueprint(
        id = "corridor",
        displayName = "Corridor",
        footprint = setOf(GridPosition(0, 0), GridPosition(1, 0)),
        heartAnchor = GridPosition(1, 0),
        doors = listOf(
            RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
            RoomDoor(GridPosition(1, 0), CardinalDirection.EAST),
        ),
        sockets = mapOf(GridPosition(1, 0) to RoomSocketType.FLOOR),
    )
}
