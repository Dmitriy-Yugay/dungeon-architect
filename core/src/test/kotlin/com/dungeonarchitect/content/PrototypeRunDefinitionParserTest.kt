package com.dungeonarchitect.content

import com.dungeonarchitect.domain.PrototypeRunDefinition
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrototypeRunDefinitionParserTest {
    @Test
    fun `authored prototype run config parses without libGDX global state`() {
        assertEquals(
            PrototypeRunDefinition(objectiveHealth = 10),
            PrototypeRunDefinitionParser.parse(authoredRunJson()),
        )
    }

    @Test
    fun `parser rejects invalid objective health`() {
        listOf("0", "-1", "2.5", "\"healthy\"").forEach { health ->
            assertFailsWith<IllegalArgumentException> {
                PrototypeRunDefinitionParser.parse(
                    """{ "objectiveHealth": $health }""",
                )
            }
        }
    }

    private fun authoredRunJson(): String {
        val config = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("assets/content/prototype-run.json") }
            .firstOrNull { Files.isRegularFile(it) }
            ?: error("Could not locate authored prototype run config.")

        return Files.readString(config)
    }
}
