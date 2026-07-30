package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpcomingHeroWaveTest {
    @Test
    fun `wave stores the hero preview data`() {
        val wave = UpcomingHeroWave(
            heroType = "militia_recruit",
            heroDisplayName = "Militia Recruit",
            count = 4,
            heroHealth = 10,
            objectiveDamage = 10,
            movementSpeedTilesPerSecond = 2f,
            traitDescription = "A straightforward melee fighter.",
        )

        assertEquals("militia_recruit", wave.heroType)
        assertEquals("Militia Recruit", wave.heroDisplayName)
        assertEquals(4, wave.count)
        assertEquals(10, wave.heroHealth)
        assertEquals(10, wave.objectiveDamage)
        assertEquals(2f, wave.movementSpeedTilesPerSecond)
        assertEquals("A straightforward melee fighter.", wave.traitDescription)
    }

    @Test
    fun `wave rejects non-positive hero health`() {
        assertFailsWith<IllegalArgumentException> {
            wave(heroHealth = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            wave(heroHealth = -1)
        }
    }

    @Test
    fun `wave rejects non-positive objective damage`() {
        assertFailsWith<IllegalArgumentException> {
            wave(objectiveDamage = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            wave(objectiveDamage = -1)
        }
    }

    @Test
    fun `wave rejects missing hero preview text`() {
        assertFailsWith<IllegalArgumentException> {
            wave(heroType = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            wave(heroDisplayName = "")
        }
        assertFailsWith<IllegalArgumentException> {
            wave(traitDescription = "\t")
        }
    }

    @Test
    fun `wave rejects non-positive hero counts`() {
        assertFailsWith<IllegalArgumentException> {
            wave(count = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            wave(count = -1)
        }
    }

    @Test
    fun `wave rejects non-positive or non-finite movement speeds`() {
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { speed ->
            assertFailsWith<IllegalArgumentException> {
                wave(movementSpeedTilesPerSecond = speed)
            }
        }
    }

    private fun wave(
        heroType: String = "militia_recruit",
        heroDisplayName: String = "Militia Recruit",
        count: Int = 4,
        heroHealth: Int = 10,
        objectiveDamage: Int = 10,
        movementSpeedTilesPerSecond: Float = 2f,
        traitDescription: String = "A straightforward melee fighter.",
    ) = UpcomingHeroWave(
        heroType = heroType,
        heroDisplayName = heroDisplayName,
        count = count,
        heroHealth = heroHealth,
        objectiveDamage = objectiveDamage,
        movementSpeedTilesPerSecond = movementSpeedTilesPerSecond,
        traitDescription = traitDescription,
    )
}
