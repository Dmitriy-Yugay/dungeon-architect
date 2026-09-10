package com.dungeonarchitect.content

import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.RunCompletionCondition
import com.dungeonarchitect.domain.RunWaveDefinition
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrototypeRunDefinitionParserTest {
    @Test
    fun `authored multi-wave run parses without libGDX global state`() {
        assertEquals(
            PrototypeRunDefinition(
                heartHealth = 10,
                startingResources = 2,
                completionCondition = RunCompletionCondition.CLEAR_ALL_WAVES,
                waves = listOf(
                    wave("opening-recruits", rewardResources = 1),
                    wave(
                        "reinforcement-recruits",
                        rewardResources = 1,
                        roomOfferBlueprintIds = listOf(
                            "prototype-room",
                            "long-gallery",
                            "corner-room",
                        ),
                    ),
                    wave(
                        "final-recruits",
                        rewardResources = 0,
                        roomOfferBlueprintIds = listOf(
                            "corner-room",
                            "prototype-room",
                            "long-gallery",
                        ),
                    ),
                ),
            ),
            parse(authoredRunJson()),
        )
    }

    @Test
    fun `parser rejects invalid scalar boundaries`() {
        listOf("0", "-1", "2.5", "\"healthy\"").forEach { health ->
            assertFailsWith<IllegalArgumentException> {
                parse(validJson(heartHealth = health))
            }
        }
        listOf("-1", "2.5", "\"many\"").forEach { resources ->
            assertFailsWith<IllegalArgumentException> {
                parse(validJson(startingResources = resources))
            }
            assertFailsWith<IllegalArgumentException> {
                parse(validJson(rewardResources = resources))
            }
        }
    }

    @Test
    fun `parser rejects missing duplicate and malformed wave references`() {
        listOf(
            "[]",
            """[{"id":" ","contentPath":"$WAVE_PATH","rewardResources":0}]""",
            """[{"id":"wave","contentPath":" ","rewardResources":0}]""",
            """[{"id":"wave","contentPath":"content/missing.json","rewardResources":0}]""",
            """[
                {"id":"duplicate","contentPath":"$WAVE_PATH","rewardResources":0},
                {"id":"duplicate","contentPath":"$WAVE_PATH","rewardResources":0}
            ]""".trimIndent(),
        ).forEach { waves ->
            assertFailsWith<IllegalArgumentException> {
                parse(validJson(waves = waves))
            }
        }
    }

    @Test
    fun `parser rejects unknown completion condition`() {
        assertFailsWith<IllegalArgumentException> {
            parse(validJson(completionCondition = "survive_forever"))
        }
    }

    @Test
    fun `parser rejects malformed and unknown room offers`() {
        listOf(
            "[]",
            "[\"prototype-room\", \"long-gallery\"]",
            "[\"prototype-room\", \"prototype-room\", \"corner-room\"]",
            "[\"prototype-room\", \"long-gallery\", \"missing-room\"]",
            "[\"prototype-room\", 42, \"corner-room\"]",
        ).forEach { offer ->
            val waves = """[
                {
                  "id": "opening",
                  "contentPath": "$WAVE_PATH",
                  "rewardResources": 1
                },
                {
                  "id": "finale",
                  "contentPath": "$WAVE_PATH",
                  "rewardResources": 0,
                  "roomOfferBlueprintIds": $offer
                }
            ]""".trimIndent()
            assertFailsWith<IllegalArgumentException> {
                parse(validJson(waves = waves))
            }
        }
    }

    private fun parse(json: String): PrototypeRunDefinition =
        PrototypeRunDefinitionParser.parse(
            json = json,
            availableWaveContentPaths = setOf(WAVE_PATH),
            availableRoomBlueprintIds = setOf(
                "prototype-room",
                "long-gallery",
                "corner-room",
            ),
        )

    private fun validJson(
        heartHealth: String = "10",
        startingResources: String = "2",
        rewardResources: String = "1",
        completionCondition: String = "clear_all_waves",
        waves: String = """[
            {
              "id": "opening",
              "contentPath": "$WAVE_PATH",
              "rewardResources": $rewardResources
            }
        ]""".trimIndent(),
    ) = """
        {
          "heartHealth": $heartHealth,
          "startingResources": $startingResources,
          "completionCondition": "$completionCondition",
          "waves": $waves
        }
    """.trimIndent()

    private fun wave(
        id: String,
        rewardResources: Int,
        roomOfferBlueprintIds: List<String> = emptyList(),
    ) = RunWaveDefinition(
        id = id,
        contentPath = WAVE_PATH,
        rewardResources = rewardResources,
        roomOfferBlueprintIds = roomOfferBlueprintIds,
    )

    private fun authoredRunJson(): String {
        val config = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("assets/content/prototype-run.json") }
            .firstOrNull { Files.isRegularFile(it) }
            ?: error("Could not locate authored prototype run config.")

        return Files.readString(config)
    }

    private companion object {
        const val WAVE_PATH = "content/upcoming-hero-wave.json"
    }
}
