package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TrapSocketHoverResultTest {
    @Test
    fun `compatible empty socket resolves canonical room and translated local position`() {
        val room = socketRoom(origin = position(4, 2))
        val grid = gridWith(room)

        val result = assertIs<TrapSocketHoverResult.Valid>(
            grid.trapSocketHoverResult(
                hoveredPosition = position(5, 3),
                definition = floorTrap(),
            ),
        )

        assertSame(room, result.room)
        assertEquals(FLOOR_SOCKET, result.localSocketPosition)
    }

    @Test
    fun `occupied compatible socket reports its placed trap`() {
        val room = socketRoom(origin = position(4, 2))
        val grid = gridWith(room)
        assertTrue(grid.placeTrap(room, FLOOR_SOCKET, floorTrap()))
        val placedTrap = grid.placedTraps.single()

        val result = assertIs<TrapSocketHoverResult.Occupied>(
            grid.trapSocketHoverResult(
                hoveredPosition = position(5, 3),
                definition = floorTrap(),
            ),
        )

        assertSame(placedTrap, result.placedTrap)
    }

    @Test
    fun `incompatible socket reports its authored type`() {
        val room = socketRoom(origin = position(4, 2))
        val grid = gridWith(room)

        val result = assertIs<TrapSocketHoverResult.Incompatible>(
            grid.trapSocketHoverResult(
                hoveredPosition = position(4, 3),
                definition = floorTrap(),
            ),
        )

        assertEquals(RoomSocketType.WALL, result.socketType)
    }

    @Test
    fun `cell without an authored socket reports non-socket`() {
        val room = socketRoom(origin = position(4, 2))
        val grid = gridWith(room)

        assertSame(
            TrapSocketHoverResult.NonSocket,
            grid.trapSocketHoverResult(
                hoveredPosition = position(4, 2),
                definition = floorTrap(),
            ),
        )
    }

    private fun gridWith(room: PlacedRoom) = DungeonGrid(
        width = 10,
        height = 8,
        entrance = position(0, 0),
        objective = position(9, 7),
        placedRooms = listOf(room),
    )

    private fun socketRoom(origin: GridPosition) = PlacedRoom(
        blueprint = RoomBlueprint(
            id = "socket-room",
            displayName = "Socket Room",
            footprint = setOf(
                position(0, 0),
                position(1, 0),
                WALL_SOCKET,
                FLOOR_SOCKET,
            ),
            doorPositions = setOf(position(0, 0)),
            sockets = mapOf(
                WALL_SOCKET to RoomSocketType.WALL,
                FLOOR_SOCKET to RoomSocketType.FLOOR,
            ),
        ),
        origin = origin,
    )

    private fun floorTrap() = TrapDefinition(
        id = "spike_trap",
        displayName = "Spike Trap",
        damage = 5,
        cooldownSeconds = 0.25f,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
    )

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)

    private companion object {
        val WALL_SOCKET = GridPosition(column = 0, row = 1)
        val FLOOR_SOCKET = GridPosition(column = 1, row = 1)
    }
}
