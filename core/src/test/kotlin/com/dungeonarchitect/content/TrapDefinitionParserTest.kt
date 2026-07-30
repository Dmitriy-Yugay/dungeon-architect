package com.dungeonarchitect.content

import com.dungeonarchitect.domain.RoomSocketType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TrapDefinitionParserTest {
    @Test
    fun `authored spike trap parses without libGDX global state`() {
        val trap = TrapDefinitionParser.parse(authoredTrapJson())

        assertEquals("spike_trap", trap.id)
        assertEquals("Spike Trap", trap.displayName)
        assertEquals(5, trap.damage)
        assertEquals(0.25f, trap.cooldownSeconds)
        assertEquals(setOf(RoomSocketType.FLOOR), trap.compatibleSocketTypes)
    }

    @Test
    fun `parser rejects an unknown room socket type`() {
        val json = """
            {
              "id": "spike_trap",
              "displayName": "Spike Trap",
              "damage": 5,
              "cooldownSeconds": 0.25,
              "compatibleSocketTypes": ["ceiling"]
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            TrapDefinitionParser.parse(json)
        }
    }

    @Test
    fun `parser rejects invalid damage and cooldown data`() {
        listOf(
            "\"damage\": 0, \"cooldownSeconds\": 0.25",
            "\"damage\": 5, \"cooldownSeconds\": 0",
            "\"damage\": 2.5, \"cooldownSeconds\": 0.25",
            "\"damage\": 5, \"cooldownSeconds\": \"slow\"",
        ).forEach { combatFields ->
            val json = """
                {
                  "id": "spike_trap",
                  "displayName": "Spike Trap",
                  $combatFields,
                  "compatibleSocketTypes": ["floor"]
                }
            """.trimIndent()

            assertFailsWith<IllegalArgumentException> {
                TrapDefinitionParser.parse(json)
            }
        }
    }

    private fun authoredTrapJson(): String {
        val config = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("assets/content/spike-trap.json") }
            .firstOrNull { Files.isRegularFile(it) }
            ?: error("Could not locate authored spike trap config.")

        return Files.readString(config)
    }
}
