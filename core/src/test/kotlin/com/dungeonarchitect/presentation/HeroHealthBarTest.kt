package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.HeroGridPosition
import com.dungeonarchitect.domain.PrototypeHeroState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class HeroHealthBarTest {
    @Test
    fun `full-health hero has a full bar above its marker`() {
        val bar = heroHealthBar(heroState(health = 10), TILE_SIZE)

        assertEquals(
            HeroHealthBar(
                left = 76f,
                bottom = 116f,
                width = 40f,
                height = 8f,
                fillFraction = 1f,
            ),
            bar,
        )
    }

    @Test
    fun `damaged hero bar reflects authored maximum health`() {
        val bar = heroHealthBar(
            heroState(health = 3, maxHealth = 12),
            TILE_SIZE,
        )

        assertEquals(0.25f, bar?.fillFraction)
    }

    @Test
    fun `zero-health hero has no health bar`() {
        assertNull(heroHealthBar(heroState(health = 0), TILE_SIZE))
    }

    @Test
    fun `health fraction clamps values outside display bounds`() {
        assertEquals(0f, healthFraction(health = -1, maxHealth = 10))
        assertEquals(1f, healthFraction(health = 11, maxHealth = 10))
    }

    @Test
    fun `health bar rejects invalid maximum and geometry boundaries`() {
        assertFailsWith<IllegalArgumentException> {
            healthFraction(health = 1, maxHealth = 0)
        }
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { tileSize ->
            assertFailsWith<IllegalArgumentException> {
                heroHealthBar(heroState(health = 10), tileSize)
            }
        }
        assertFailsWith<IllegalArgumentException> {
            HeroHealthBar(
                left = 0f,
                bottom = 0f,
                width = 10f,
                height = 2f,
                fillFraction = 1.1f,
            )
        }
    }

    private fun heroState(
        health: Int,
        maxHealth: Int = 10,
    ) = PrototypeHeroState(
        position = HeroGridPosition(column = 1f, row = 1f),
        hasArrived = false,
        health = health,
        maxHealth = maxHealth,
    )

    private companion object {
        const val TILE_SIZE = 64f
    }
}
