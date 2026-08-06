package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlacedTrapTest {
    @Test
    fun `placed trap translates its local socket to the grid`() {
        val room = roomWithSocket(RoomSocketType.FLOOR)

        val placedTrap = PlacedTrap(
            definition = trapFor(RoomSocketType.FLOOR),
            room = room,
            localSocketPosition = GridPosition(column = 1, row = 0),
        )

        assertEquals(GridPosition(column = 3, row = 4), placedTrap.gridPosition)
    }

    @Test
    fun `placed trap requires a declared compatible socket`() {
        val room = roomWithSocket(RoomSocketType.FLOOR)

        assertFailsWith<IllegalArgumentException> {
            PlacedTrap(
                definition = trapFor(RoomSocketType.FLOOR),
                room = room,
                localSocketPosition = GridPosition(column = 0, row = 0),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PlacedTrap(
                definition = trapFor(RoomSocketType.WALL),
                room = room,
                localSocketPosition = GridPosition(column = 1, row = 0),
            )
        }
    }

    private fun roomWithSocket(socketType: RoomSocketType) = PlacedRoom(
        blueprint = RoomBlueprint(
            footprint = setOf(
                GridPosition(column = 0, row = 0),
                GridPosition(column = 1, row = 0),
            ),
            doorPositions = setOf(GridPosition(column = 0, row = 0)),
            sockets = mapOf(
                GridPosition(column = 1, row = 0) to socketType,
            ),
        ),
        origin = GridPosition(column = 2, row = 4),
    )

    private fun trapFor(socketType: RoomSocketType) = TrapDefinition(
        id = "spike_trap",
        displayName = "Spike Trap",
        damage = 5,
        cooldownSeconds = 0.25f,
        compatibleSocketTypes = setOf(socketType),
    )
}
