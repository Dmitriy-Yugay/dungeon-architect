package com.dungeonarchitect.application

import com.dungeonarchitect.domain.GridPosition
import kotlin.test.Test
import kotlin.test.assertEquals

class PrototypeScreenTest {
    @Test
    fun `prototype grid starts with one authored room`() {
        val room = PrototypeScreen.prototypeGrid().placedRooms.single()

        assertEquals(GridPosition(column = 6, row = 3), room.origin)
        assertEquals(
            setOf(
                GridPosition(column = 6, row = 3),
                GridPosition(column = 7, row = 3),
                GridPosition(column = 8, row = 3),
                GridPosition(column = 6, row = 4),
                GridPosition(column = 7, row = 4),
                GridPosition(column = 8, row = 4),
                GridPosition(column = 6, row = 5),
                GridPosition(column = 7, row = 5),
                GridPosition(column = 8, row = 5),
            ),
            room.gridPositions,
        )
    }
}
