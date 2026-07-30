package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrapDefinitionTest {
    @Test
    fun `trap snapshots compatible room socket types`() {
        val compatibleSocketTypes = mutableSetOf(RoomSocketType.FLOOR)
        val trap = TrapDefinition(
            id = "spike_trap",
            displayName = "Spike Trap",
            compatibleSocketTypes = compatibleSocketTypes,
        )

        compatibleSocketTypes += RoomSocketType.WALL

        assertEquals(setOf(RoomSocketType.FLOOR), trap.compatibleSocketTypes)
    }

    @Test
    fun `trap reports compatible and incompatible room socket types`() {
        val trap = TrapDefinition(
            id = "spike_trap",
            displayName = "Spike Trap",
            compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
        )

        assertTrue(trap.isCompatibleWith(RoomSocketType.FLOOR))
        assertFalse(trap.isCompatibleWith(RoomSocketType.WALL))
    }

    @Test
    fun `trap rejects a blank id`() {
        assertFailsWith<IllegalArgumentException> {
            TrapDefinition(
                id = " ",
                displayName = "Spike Trap",
                compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
            )
        }
    }

    @Test
    fun `trap rejects a blank display name`() {
        assertFailsWith<IllegalArgumentException> {
            TrapDefinition(
                id = "spike_trap",
                displayName = " ",
                compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
            )
        }
    }

    @Test
    fun `trap requires a compatible room socket type`() {
        assertFailsWith<IllegalArgumentException> {
            TrapDefinition(
                id = "spike_trap",
                displayName = "Spike Trap",
                compatibleSocketTypes = emptySet(),
            )
        }
    }
}
