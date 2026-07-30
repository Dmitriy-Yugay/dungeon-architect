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
            traitDescription = "A straightforward melee fighter.",
        )

        assertEquals("militia_recruit", wave.heroType)
        assertEquals("Militia Recruit", wave.heroDisplayName)
        assertEquals(4, wave.count)
        assertEquals("A straightforward melee fighter.", wave.traitDescription)
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

    private fun wave(
        heroType: String = "militia_recruit",
        heroDisplayName: String = "Militia Recruit",
        count: Int = 4,
        traitDescription: String = "A straightforward melee fighter.",
    ) = UpcomingHeroWave(
        heroType = heroType,
        heroDisplayName = heroDisplayName,
        count = count,
        traitDescription = traitDescription,
    )
}
