package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PlacedTrap
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.TrapSocketHoverResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrapPlacementPreviewTest {
    @Test
    fun `valid socket maps to valid preview at translated hover position`() {
        val room = socketRoom(origin = position(4, 2))
        val hoveredPosition = room.toGridPosition(LOCAL_SOCKET)

        assertEquals(
            TrapPlacementPreview(
                position = position(5, 3),
                isValid = true,
            ),
            trapPlacementPreviewFor(
                hoverResult = TrapSocketHoverResult.Valid(
                    room = room,
                    localSocketPosition = LOCAL_SOCKET,
                ),
                hoveredPosition = hoveredPosition,
            ),
        )
    }

    @Test
    fun `occupied socket maps to invalid preview`() {
        val room = socketRoom(origin = position(4, 2))
        val placedTrap = PlacedTrap(
            definition = floorTrap(),
            room = room,
            localSocketPosition = LOCAL_SOCKET,
        )

        assertEquals(
            TrapPlacementPreview(
                position = position(5, 3),
                isValid = false,
            ),
            trapPlacementPreviewFor(
                hoverResult = TrapSocketHoverResult.Occupied(placedTrap),
                hoveredPosition = placedTrap.gridPosition,
            ),
        )
    }

    @Test
    fun `incompatible socket maps to invalid preview`() {
        val hoveredPosition = position(4, 3)

        assertEquals(
            TrapPlacementPreview(
                position = hoveredPosition,
                isValid = false,
            ),
            trapPlacementPreviewFor(
                hoverResult = TrapSocketHoverResult.Incompatible(
                    RoomSocketType.WALL,
                ),
                hoveredPosition = hoveredPosition,
            ),
        )
    }

    @Test
    fun `non-socket cell does not produce trap preview`() {
        assertNull(
            trapPlacementPreviewFor(
                hoverResult = TrapSocketHoverResult.NonSocket,
                hoveredPosition = position(4, 2),
            ),
        )
    }

    private fun socketRoom(origin: GridPosition) = PlacedRoom(
        blueprint = RoomBlueprint(
            id = "socket-room",
            displayName = "Socket Room",
            footprint = setOf(
                position(0, 0),
                position(1, 0),
                position(0, 1),
                LOCAL_SOCKET,
            ),
            doorPositions = setOf(position(0, 0)),
            sockets = mapOf(LOCAL_SOCKET to RoomSocketType.FLOOR),
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
        val LOCAL_SOCKET = GridPosition(column = 1, row = 1)
    }
}
