package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrototypeRunDefinitionTest {
    @Test
    fun `run definition stores ordered waves resources and completion rule`() {
        val waves = listOf(
            RunWaveDefinition(
                id = "opening",
                contentPath = "content/opening.json",
                rewardResources = 1,
            ),
            RunWaveDefinition(
                id = "finale",
                contentPath = "content/finale.json",
                rewardResources = 0,
                roomOfferBlueprintIds = offer(),
            ),
        )
        val definition = PrototypeRunDefinition(
            heartHealth = 10,
            startingResources = 2,
            waves = waves,
            completionCondition = RunCompletionCondition.CLEAR_ALL_WAVES,
        )

        assertEquals(10, definition.heartHealth)
        assertEquals(2, definition.startingResources)
        assertEquals(waves, definition.waves)
        assertEquals(
            RunCompletionCondition.CLEAR_ALL_WAVES,
            definition.completionCondition,
        )
    }

    @Test
    fun `run definition rejects invalid health resources and waves`() {
        listOf(0, -1).forEach { health ->
            assertFailsWith<IllegalArgumentException> {
                definition(heartHealth = health)
            }
        }
        assertFailsWith<IllegalArgumentException> {
            definition(startingResources = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            definition(waves = emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            definition(waves = listOf(wave("duplicate"), wave("duplicate")))
        }
    }

    @Test
    fun `run wave rejects blank references and negative rewards`() {
        assertFailsWith<IllegalArgumentException> {
            wave(id = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            wave(contentPath = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            wave(rewardResources = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            wave(roomOfferBlueprintIds = listOf("room", " ", "corner"))
        }
        assertFailsWith<IllegalArgumentException> {
            wave(roomOfferBlueprintIds = listOf("room", "room", "corner"))
        }
    }

    @Test
    fun `run requires one three-room offer before every wave after opening`() {
        listOf(
            listOf(wave(roomOfferBlueprintIds = offer())),
            listOf(wave(), wave("finale")),
            listOf(wave(), wave("finale", roomOfferBlueprintIds = listOf("a", "b"))),
        ).forEach { waves ->
            assertFailsWith<IllegalArgumentException> {
                definition(waves = waves)
            }
        }

        definition(
            waves = listOf(
                wave(),
                wave("finale", roomOfferBlueprintIds = offer()),
            ),
        )
    }

    private fun definition(
        heartHealth: Int = 10,
        startingResources: Int = 0,
        waves: List<RunWaveDefinition> = listOf(wave()),
    ) = PrototypeRunDefinition(
        heartHealth = heartHealth,
        startingResources = startingResources,
        waves = waves,
        completionCondition = RunCompletionCondition.CLEAR_ALL_WAVES,
    )

    private fun wave(
        id: String = "test-wave",
        contentPath: String = "content/test-wave.json",
        rewardResources: Int = 0,
        roomOfferBlueprintIds: List<String> = emptyList(),
    ) = RunWaveDefinition(
        id = id,
        contentPath = contentPath,
        rewardResources = rewardResources,
        roomOfferBlueprintIds = roomOfferBlueprintIds,
    )

    private fun offer() = listOf("prototype", "gallery", "corner")
}
