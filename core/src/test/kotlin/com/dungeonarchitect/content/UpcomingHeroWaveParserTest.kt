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
                heroHealth = 10,
                heartDamage = 10,
                movementSpeedTilesPerSecond = 2f,
                heroRole = "Frontline attacker",
                defenseImplication = "Cover multiple on-route sockets.",
                traitDescription = "No special defenses.",
            ),
            wave,
        )
    }

    @Test
    fun `parser rejects missing malformed and blank intelligence guidance`() {
        listOf(
            validIntelligenceJson(heroRole = "null"),
            validIntelligenceJson(heroRole = "\" \""),
            validIntelligenceJson(defenseImplication = "42"),
            validIntelligenceJson(defenseImplication = "\"\t\""),
        ).forEach { json ->
            assertFailsWith<IllegalArgumentException> {
                UpcomingHeroWaveParser.parse(json)
            }
        }
    }

    @Test
    fun `parser rejects a non-positive authored count`() {
        val json = """
            {
              "heroType": "militia_recruit",
              "heroDisplayName": "Militia Recruit",
              "count": 0,
              "heroHealth": 10,
              "heartDamage": 10,
              "movementSpeedTilesPerSecond": 2.0,
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
              "heroHealth": 10,
              "heartDamage": 10,
              "movementSpeedTilesPerSecond": 2.0,
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
              "count": 4,
              "heroHealth": 10,
              "heartDamage": 10,
              "movementSpeedTilesPerSecond": 2.0
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            UpcomingHeroWaveParser.parse(json)
        }
    }

    @Test
    fun `parser rejects invalid movement speed data`() {
        listOf("0", "-1", "\"fast\"").forEach { speed ->
            val json = """
                {
                  "heroType": "militia_recruit",
                  "heroDisplayName": "Militia Recruit",
                  "count": 4,
                  "heroHealth": 10,
                  "heartDamage": 10,
                  "movementSpeedTilesPerSecond": $speed,
                  "traitDescription": "A straightforward melee fighter."
                }
            """.trimIndent()

            assertFailsWith<IllegalArgumentException> {
                UpcomingHeroWaveParser.parse(json)
            }
        }
    }

    @Test
    fun `parser rejects non-positive hero health`() {
        val json = """
            {
              "heroType": "militia_recruit",
              "heroDisplayName": "Militia Recruit",
              "count": 4,
              "heroHealth": 0,
              "heartDamage": 10,
              "movementSpeedTilesPerSecond": 2.0,
              "traitDescription": "A straightforward melee fighter."
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            UpcomingHeroWaveParser.parse(json)
        }
    }

    @Test
    fun `parser rejects non-positive heart damage`() {
        val json = """
            {
              "heroType": "militia_recruit",
              "heroDisplayName": "Militia Recruit",
              "count": 4,
              "heroHealth": 10,
              "heartDamage": 0,
              "movementSpeedTilesPerSecond": 2.0,
              "traitDescription": "A straightforward melee fighter."
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

    private fun validIntelligenceJson(
        heroRole: String = "\"Frontline attacker\"",
        defenseImplication: String = "\"Cover multiple on-route sockets.\"",
    ) = """
        {
          "heroType": "militia_recruit",
          "heroDisplayName": "Militia Recruit",
          "count": 4,
          "heroHealth": 10,
          "heartDamage": 10,
          "movementSpeedTilesPerSecond": 2.0,
          "heroRole": $heroRole,
          "traitDescription": "No special defenses.",
          "defenseImplication": $defenseImplication
        }
    """.trimIndent()
}
