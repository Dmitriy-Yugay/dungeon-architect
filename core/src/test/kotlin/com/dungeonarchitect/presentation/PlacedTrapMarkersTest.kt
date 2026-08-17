package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomOrientation
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlacedTrapMarkersTest {
    @Test
    fun `rotated trap marker uses the oriented socket position`() {
        val room = placedRoom(
            id = "rotated-room",
            origin = position(4, 6),
            orientation = RoomOrientation.CLOCKWISE_90,
        )
        val grid = gridWith(room)
        val orientedSocket = position(0, 0)

        assertTrue(grid.placeTrap(room, orientedSocket, spikeTrap()))

        assertEquals(
            listOf(
                PlacedTrapGridMarker(
                    position = position(4, 6),
                    trapId = "spike_trap",
                    displayName = "Spike Trap",
                ),
            ),
            placedTrapGridMarkers(grid),
        )
    }

    @Test
    fun `trap marker translates its socket and preserves stable identity`() {
        val room = placedRoom(
            id = "west-room",
            origin = position(4, 6),
        )
        val grid = gridWith(room)

        assertTrue(grid.placeTrap(room, LOCAL_SOCKET, spikeTrap()))

        assertEquals(
            listOf(
                PlacedTrapGridMarker(
                    position = position(5, 6),
                    trapId = "spike_trap",
                    displayName = "Spike Trap",
                ),
            ),
            placedTrapGridMarkers(grid),
        )
    }

    @Test
    fun `trap markers include every placed trap`() {
        val westRoom = placedRoom(
            id = "west-room",
            origin = position(1, 1),
        )
        val eastRoom = placedRoom(
            id = "east-room",
            origin = position(6, 4),
        )
        val grid = gridWith(westRoom, eastRoom)

        assertTrue(grid.placeTrap(westRoom, LOCAL_SOCKET, spikeTrap()))
        assertTrue(grid.placeTrap(eastRoom, LOCAL_SOCKET, frostTrap()))

        assertEquals(
            listOf(
                PlacedTrapGridMarker(
                    position = position(2, 1),
                    trapId = "spike_trap",
                    displayName = "Spike Trap",
                ),
                PlacedTrapGridMarker(
                    position = position(7, 4),
                    trapId = "frost_trap",
                    displayName = "Frost Trap",
                ),
            ),
            placedTrapGridMarkers(grid),
        )
    }

    private fun gridWith(vararg rooms: PlacedRoom) = DungeonGrid(
        width = 10,
        height = 8,
        entrance = position(0, 0),
        objective = position(9, 7),
        placedRooms = rooms.toList(),
    )

    private fun placedRoom(
        id: String,
        origin: GridPosition,
        orientation: RoomOrientation = RoomOrientation.UNROTATED,
    ) = PlacedRoom(
        blueprint = RoomBlueprint(
            id = id,
            displayName = "Test Room",
            heartAnchor = GridPosition(column = 0, row = 0),
            footprint = setOf(position(0, 0), LOCAL_SOCKET),
            doorPositions = setOf(position(0, 0)),
            sockets = mapOf(LOCAL_SOCKET to RoomSocketType.FLOOR),
        ),
        origin = origin,
        orientation = orientation,
    )

    private fun spikeTrap() = trapDefinition(
        id = "spike_trap",
        displayName = "Spike Trap",
    )

    private fun frostTrap() = trapDefinition(
        id = "frost_trap",
        displayName = "Frost Trap",
    )

    private fun trapDefinition(
        id: String,
        displayName: String,
    ) = TrapDefinition(
        id = id,
        displayName = displayName,
        damage = 5,
        cooldownSeconds = 0.25f,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
    )

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)

    private companion object {
        val LOCAL_SOCKET = GridPosition(column = 1, row = 0)
    }
}
