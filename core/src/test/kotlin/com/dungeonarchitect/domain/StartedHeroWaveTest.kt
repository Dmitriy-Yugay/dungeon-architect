package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class StartedHeroWaveTest {
    @Test
    fun `started wave owns a snapshot of its route`() {
        val entrance = GridPosition(column = 0, row = 0)
        val objective = GridPosition(column = 1, row = 0)
        val sourceRoute = mutableListOf(entrance, objective)
        val startedWave = StartedHeroWave(
            wave = UpcomingHeroWave(
                heroType = "militia_recruit",
                heroDisplayName = "Militia Recruit",
                count = 4,
                movementSpeedTilesPerSecond = 2f,
                traitDescription = "A straightforward melee fighter.",
            ),
            route = sourceRoute,
        )

        sourceRoute.clear()

        assertEquals(listOf(entrance, objective), startedWave.route)
    }
}
