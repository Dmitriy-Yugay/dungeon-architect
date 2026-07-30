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
            damage = 5,
            cooldownSeconds = 0.25f,
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
            damage = 5,
            cooldownSeconds = 0.25f,
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
                damage = 5,
                cooldownSeconds = 0.25f,
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
                damage = 5,
                cooldownSeconds = 0.25f,
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
                damage = 5,
                cooldownSeconds = 0.25f,
                compatibleSocketTypes = emptySet(),
            )
        }
    }

    @Test
    fun `trap rejects invalid damage or cooldown`() {
        assertFailsWith<IllegalArgumentException> {
            trap(damage = 0)
        }
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { cooldown ->
            assertFailsWith<IllegalArgumentException> {
                trap(cooldownSeconds = cooldown)
            }
        }
    }

    private fun trap(
        damage: Int = 5,
        cooldownSeconds: Float = 0.25f,
    ) = TrapDefinition(
        id = "spike_trap",
        displayName = "Spike Trap",
        damage = damage,
        cooldownSeconds = cooldownSeconds,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
    )
}
