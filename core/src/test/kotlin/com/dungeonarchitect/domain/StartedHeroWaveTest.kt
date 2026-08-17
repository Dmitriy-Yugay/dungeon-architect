package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class StartedHeroWaveTest {
    @Test
    fun `started wave owns a snapshot of its route`() {
        val entrance = GridPosition(column = 0, row = 0)
        val heart = GridPosition(column = 1, row = 0)
        val sourceRoute = mutableListOf(entrance, heart)
        val startedWave = StartedHeroWave(
            wave = UpcomingHeroWave(
                heroType = "militia_recruit",
                heroDisplayName = "Militia Recruit",
                count = 4,
                heroHealth = 10,
                heartDamage = 10,
                movementSpeedTilesPerSecond = 2f,
                traitDescription = "A straightforward melee fighter.",
            ),
            route = sourceRoute,
        )

        sourceRoute.clear()

        assertEquals(listOf(entrance, heart), startedWave.route)
    }
}
