package com.dungeonarchitect.content

import com.dungeonarchitect.domain.UpcomingHeroWave
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpcomingHeroWaveParserTest {
    @Test
    fun `authored upcoming wave config parses without libGDX global state`() {
        val wave = UpcomingHeroWaveParser.parse(authoredWaveJson())

        assertEquals(
            UpcomingHeroWave(
                heroType = "militia_recruit",
                heroDisplayName = "Militia Recruit",
                count = 4,
                traitDescription = "A straightforward melee fighter with no special defenses.",
            ),
            wave,
        )
    }

    @Test
    fun `parser rejects a non-positive authored count`() {
        val json = """
            {
              "heroType": "militia_recruit",
              "heroDisplayName": "Militia Recruit",
              "count": 0,
              "traitDescription": "A straightforward melee fighter."
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            UpcomingHeroWaveParser.parse(json)
        }
    }

    @Test
    fun `parser rejects fractional hero counts`() {
        val json = """
            {
              "heroType": "militia_recruit",
              "heroDisplayName": "Militia Recruit",
              "count": 2.5,
              "traitDescription": "A straightforward melee fighter."
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            UpcomingHeroWaveParser.parse(json)
        }
    }

    @Test
    fun `parser rejects missing preview data`() {
        val json = """
            {
              "heroType": "militia_recruit",
              "heroDisplayName": "Militia Recruit",
              "count": 4
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            UpcomingHeroWaveParser.parse(json)
        }
    }

    private fun authoredWaveJson(): String {
        val config = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("assets/content/upcoming-hero-wave.json") }
            .firstOrNull { Files.isRegularFile(it) }
            ?: error("Could not locate authored upcoming hero wave config.")

        return Files.readString(config)
    }
}
