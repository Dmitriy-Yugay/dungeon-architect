package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class PlacedDungeonHeartTest {
    @Test
    fun `heart derives its local and grid positions from oriented room anchor`() {
        val blueprint = RoomBlueprint(
            id = "asymmetric-heart-room",
            displayName = "Asymmetric Heart Room",
            heartAnchor = position(0, 1),
            footprint = setOf(
                position(0, 0),
                position(1, 0),
                position(0, 1),
            ),
            doors = listOf(
                RoomDoor(position(0, 0), CardinalDirection.SOUTH),
            ),
        )
        val room = PlacedRoom(
            blueprint = blueprint,
            origin = position(4, 3),
            orientation = RoomOrientation.CLOCKWISE_90,
        )

        val heart = PlacedDungeonHeart(room)

        assertSame(room, heart.room)
        assertEquals(position(1, 1), heart.localAnchorPosition)
        assertEquals(position(5, 4), heart.gridPosition)
    }

    private fun position(column: Int, row: Int) = GridPosition(column, row)
}
