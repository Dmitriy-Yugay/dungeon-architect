package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrototypeRunDefinitionTest {
    @Test
    fun `run definition stores heart health`() {
        assertEquals(
            10,
            PrototypeRunDefinition(heartHealth = 10).heartHealth,
        )
    }

    @Test
    fun `run definition rejects non-positive heart health`() {
        listOf(0, -1).forEach { health ->
            assertFailsWith<IllegalArgumentException> {
                PrototypeRunDefinition(heartHealth = health)
            }
        }
    }
}
