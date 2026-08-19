package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DungeonHeartRoutingTest {
    @Test
    fun `branched route follows the placed and relocated heart without consuming frontier`() {
        val junction = room(
            id = "junction",
            origin = position(1, 1),
            facings = setOf(
                CardinalDirection.WEST,
                CardinalDirection.EAST,
                CardinalDirection.NORTH,
            ),
        )
        val eastRoom = room(
            id = "east",
            origin = position(2, 1),
            facings = setOf(CardinalDirection.WEST, CardinalDirection.EAST),
        )
        val northBranch = room(
            id = "north",
            origin = position(1, 2),
            facings = setOf(CardinalDirection.SOUTH, CardinalDirection.NORTH),
        )
        val grid = DungeonGrid(
            width = 5,
            height = 4,
            entrance = position(0, 1),
            placedRooms = listOf(junction, eastRoom, northBranch),
        )
        val expectedFrontier = setOf(
            eastRoom.door(CardinalDirection.EAST),
            northBranch.door(CardinalDirection.NORTH),
        )

        assertNull(grid.entranceToHeartRoute)
        assertEquals(expectedFrontier, grid.openRoomDoors)

        assertTrue(grid.placeOrRelocateHeart(eastRoom))
        assertEquals(
            listOf(position(0, 1), position(1, 1), position(2, 1)),
            grid.entranceToHeartRoute,
        )
        assertEquals(
            grid.placedHeart?.gridPosition,
            grid.entranceToHeartRoute?.last(),
        )
        assertEquals(expectedFrontier, grid.openRoomDoors)

        assertTrue(grid.placeOrRelocateHeart(northBranch))
        assertEquals(
            listOf(position(0, 1), position(1, 1), position(1, 2)),
            grid.entranceToHeartRoute,
        )
        assertEquals(
            grid.placedHeart?.gridPosition,
            grid.entranceToHeartRoute?.last(),
        )
        assertEquals(expectedFrontier, grid.openRoomDoors)
    }

    @Test
    fun `heart in a disconnected room has no entrance route`() {
        val entranceRoom = room(
            id = "entrance-room",
            origin = position(1, 1),
            facings = setOf(CardinalDirection.WEST, CardinalDirection.EAST),
        )
        val disconnectedRoom = room(
            id = "disconnected-room",
            origin = position(4, 3),
            facings = setOf(CardinalDirection.WEST),
        )
        val grid = DungeonGrid(
            width = 6,
            height = 5,
            entrance = position(0, 1),
            placedRooms = listOf(entranceRoom, disconnectedRoom),
        )

        assertTrue(grid.placeOrRelocateHeart(disconnectedRoom))

        assertEquals(disconnectedRoom, grid.placedHeart?.room)
        assertNull(grid.entranceToHeartRoute)
    }

    @Test
    fun `rotated heart anchor is the route endpoint and does not consume its room door`() {
        val blueprint = RoomBlueprint(
            id = "rotated-heart-gallery",
            displayName = "Rotated Heart Gallery",
            heartAnchor = position(0, 0),
            footprint = setOf(
                position(0, 0),
                position(1, 0),
                position(2, 0),
            ),
            doors = listOf(
                RoomDoor(position(0, 0), CardinalDirection.WEST),
                RoomDoor(position(2, 0), CardinalDirection.EAST),
            ),
        )
        val room = PlacedRoom(
            blueprint = blueprint,
            origin = position(2, 1),
            orientation = RoomOrientation.CLOCKWISE_90,
        )
        val grid = DungeonGrid(
            width = 5,
            height = 5,
            entrance = position(2, 0),
            entranceFacing = CardinalDirection.NORTH,
            placedRooms = listOf(room),
        )

        assertTrue(grid.placeOrRelocateHeart(room))

        assertEquals(position(2, 3), grid.placedHeart?.gridPosition)
        assertEquals(
            listOf(
                position(2, 0),
                position(2, 1),
                position(2, 2),
                position(2, 3),
            ),
            grid.entranceToHeartRoute,
        )
        assertEquals(
            setOf(room.door(CardinalDirection.NORTH)),
            grid.openRoomDoors,
        )
    }

    private fun room(
        id: String,
        origin: GridPosition,
        facings: Set<CardinalDirection>,
    ) = PlacedRoom(
        blueprint = RoomBlueprint(
            id = id,
            displayName = id,
            heartAnchor = position(0, 0),
            footprint = setOf(position(0, 0)),
            doors = facings.map { facing -> RoomDoor(position(0, 0), facing) },
        ),
        origin = origin,
    )

    private fun PlacedRoom.door(facing: CardinalDirection): PlacedRoomDoor =
        doors.single { it.door.facing == facing }

    private fun position(column: Int, row: Int) = GridPosition(column, row)
}
