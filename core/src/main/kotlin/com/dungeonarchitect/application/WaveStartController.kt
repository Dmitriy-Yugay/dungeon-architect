package com.dungeonarchitect.application

import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.StartedHeroWave
import com.dungeonarchitect.domain.UpcomingHeroWave

class WaveStartController(
    private val grid: DungeonGrid,
    private val upcomingWave: UpcomingHeroWave,
) {
    var startedWave: StartedHeroWave? = null
        private set

    val isStartEnabled: Boolean
        get() = startedWave == null && grid.entranceToObjectiveRoute != null

    fun start(): Boolean {
        if (startedWave != null) {
            return false
        }

        val route = grid.entranceToObjectiveRoute ?: return false
        startedWave = StartedHeroWave(
            wave = upcomingWave,
            route = route,
        )
        return true
    }
}
