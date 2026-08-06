package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrototypeRunDefinitionTest {
    @Test
    fun `run definition stores objective health`() {
        assertEquals(
            10,
            PrototypeRunDefinition(objectiveHealth = 10).objectiveHealth,
        )
    }

    @Test
    fun `run definition rejects non-positive objective health`() {
        listOf(0, -1).forEach { health ->
            assertFailsWith<IllegalArgumentException> {
                PrototypeRunDefinition(objectiveHealth = health)
            }
        }
    }
}
