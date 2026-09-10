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
            heartDamage = 10,
            movementSpeedTilesPerSecond = 2f,
            heroRole = "Frontline attacker",
            defenseImplication = "Cover multiple on-route sockets.",
            traitDescription = "A straightforward melee fighter.",
        )

        assertEquals("militia_recruit", wave.heroType)
        assertEquals("Militia Recruit", wave.heroDisplayName)
        assertEquals(4, wave.count)
        assertEquals(10, wave.heroHealth)
        assertEquals(10, wave.heartDamage)
        assertEquals(2f, wave.movementSpeedTilesPerSecond)
        assertEquals("Frontline attacker", wave.heroRole)
        assertEquals("A straightforward melee fighter.", wave.traitDescription)
        assertEquals(
            "Cover multiple on-route sockets.",
            wave.defenseImplication,
        )
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
    fun `wave rejects non-positive heart damage`() {
        assertFailsWith<IllegalArgumentException> {
            wave(heartDamage = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            wave(heartDamage = -1)
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
        assertFailsWith<IllegalArgumentException> {
            wave(heroRole = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            wave(defenseImplication = "\n")
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
        heartDamage: Int = 10,
        movementSpeedTilesPerSecond: Float = 2f,
        heroRole: String = "Frontline attacker",
        traitDescription: String = "A straightforward melee fighter.",
        defenseImplication: String = "Cover multiple on-route sockets.",
    ) = UpcomingHeroWave(
        heroType = heroType,
        heroDisplayName = heroDisplayName,
        count = count,
        heroHealth = heroHealth,
        heartDamage = heartDamage,
        movementSpeedTilesPerSecond = movementSpeedTilesPerSecond,
        heroRole = heroRole,
        defenseImplication = defenseImplication,
        traitDescription = traitDescription,
    )
}
