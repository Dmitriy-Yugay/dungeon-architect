package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DungeonHeartPlacementTest {
    @Test
    fun `heart placement relocation and same-room selection are transactional`() {
        val firstRoom = heartRoom("first", origin = position(2, 2))
        val secondRoom = heartRoom("second", origin = position(6, 2))
        val foreignRoom = heartRoom("foreign", origin = position(8, 2))
        val grid = gridWith(firstRoom, secondRoom)

        assertNull(grid.placedHeart)
        assertTrue(grid.placeOrRelocateHeart(firstRoom))
        val firstHeart = requireNotNull(grid.placedHeart)
        assertSame(firstRoom, firstHeart.room)

        assertTrue(grid.placeOrRelocateHeart(firstRoom))
        assertSame(firstHeart, grid.placedHeart)

        assertFalse(grid.placeOrRelocateHeart(foreignRoom))
        assertSame(firstHeart, grid.placedHeart)
        val unplacedCopy = firstRoom.copy()
        assertEquals(firstRoom, unplacedCopy)
        assertFalse(grid.placeOrRelocateHeart(unplacedCopy))
        assertSame(firstHeart, grid.placedHeart)

        assertTrue(grid.placeOrRelocateHeart(secondRoom))
        val relocatedHeart = requireNotNull(grid.placedHeart)
        assertSame(secondRoom, relocatedHeart.room)
        assertEquals(secondRoom.toGridPosition(HEART_ANCHOR), relocatedHeart.gridPosition)
    }

    @Test
    fun `heart and trap reject co-occupancy in either placement order`() {
        val heartFirstRoom = heartRoom(
            id = "heart-first",
            origin = position(2, 2),
            anchorHasSocket = true,
        )
        val heartFirstGrid = gridWith(heartFirstRoom)
        val trap = floorTrap()

        assertTrue(heartFirstGrid.placeOrRelocateHeart(heartFirstRoom))
        val placedHeart = requireNotNull(heartFirstGrid.placedHeart)
        assertFalse(heartFirstGrid.placeTrap(heartFirstRoom, HEART_ANCHOR, trap))
        assertEquals(emptyList(), heartFirstGrid.placedTraps)
        val hover = assertIs<TrapSocketHoverResult.HeartOccupied>(
            heartFirstGrid.trapSocketHoverResult(placedHeart.gridPosition, trap),
        )
        assertSame(placedHeart, hover.placedHeart)

        val trapFirstRoom = heartRoom(
            id = "trap-first",
            origin = position(6, 2),
            anchorHasSocket = true,
        )
        val trapFirstGrid = gridWith(trapFirstRoom)
        assertTrue(trapFirstGrid.placeTrap(trapFirstRoom, HEART_ANCHOR, trap))

        assertFalse(trapFirstGrid.placeOrRelocateHeart(trapFirstRoom))
        assertNull(trapFirstGrid.placedHeart)
        assertEquals(1, trapFirstGrid.placedTraps.size)
    }

    @Test
    fun `rejected occupied relocation preserves the prior heart`() {
        val currentRoom = heartRoom("current", origin = position(2, 2))
        val occupiedRoom = heartRoom(
            id = "occupied",
            origin = position(6, 2),
            anchorHasSocket = true,
        )
        val grid = gridWith(currentRoom, occupiedRoom)
        assertTrue(grid.placeOrRelocateHeart(currentRoom))
        val originalHeart = requireNotNull(grid.placedHeart)
        assertTrue(grid.placeTrap(occupiedRoom, HEART_ANCHOR, floorTrap()))

        assertFalse(grid.placeOrRelocateHeart(occupiedRoom))

        assertSame(originalHeart, grid.placedHeart)
        assertSame(currentRoom, grid.placedHeart?.room)
    }

    @Test
    fun `cancel preserves a heart in another room and unplaces a heart with its room`() {
        val heartRoom = heartRoom("heart", origin = position(2, 2))
        val newestRoom = heartRoom("newest", origin = position(6, 2))
        val preservingGrid = gridWith(heartRoom, newestRoom)
        assertTrue(preservingGrid.placeOrRelocateHeart(heartRoom))
        val placedHeart = requireNotNull(preservingGrid.placedHeart)

        assertTrue(preservingGrid.cancelLastPlacedRoom())

        assertSame(placedHeart, preservingGrid.placedHeart)
        assertSame(heartRoom, preservingGrid.placedHeart?.room)

        val olderRoom = heartRoom("older", origin = position(2, 2))
        val canceledHeartRoom = heartRoom("canceled-heart", origin = position(6, 2))
        val clearingGrid = gridWith(olderRoom, canceledHeartRoom)
        assertTrue(clearingGrid.placeOrRelocateHeart(canceledHeartRoom))

        assertTrue(clearingGrid.cancelLastPlacedRoom())

        assertNull(clearingGrid.placedHeart)
        assertEquals(listOf(olderRoom), clearingGrid.placedRooms)
    }

    private fun gridWith(vararg rooms: PlacedRoom) = DungeonGrid(
        width = 12,
        height = 8,
        entrance = position(0, 0),
        placedRooms = rooms.toList(),
    )

    private fun heartRoom(
        id: String,
        origin: GridPosition,
        anchorHasSocket: Boolean = false,
    ) = PlacedRoom(
        blueprint = RoomBlueprint(
            id = id,
            displayName = "Heart Room $id",
            heartAnchor = HEART_ANCHOR,
            footprint = setOf(position(0, 0), HEART_ANCHOR),
            doors = listOf(
                RoomDoor(position(0, 0), CardinalDirection.WEST),
            ),
            sockets = if (anchorHasSocket) {
                mapOf(HEART_ANCHOR to RoomSocketType.FLOOR)
            } else {
                emptyMap()
            },
        ),
        origin = origin,
    )

    private fun floorTrap() = TrapDefinition(
        id = "spike-trap",
        displayName = "Spike Trap",
        damage = 1,
        cooldownSeconds = 1f,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
    )

    private fun position(column: Int, row: Int) = GridPosition(column, row)

    private companion object {
        val HEART_ANCHOR = GridPosition(column = 1, row = 0)
    }
}
