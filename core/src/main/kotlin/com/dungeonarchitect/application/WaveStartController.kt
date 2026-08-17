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
        get() = startedWave == null && grid.entranceToHeartRoute != null

    fun start(): Boolean {
        if (startedWave != null) {
            return false
        }

        val route = grid.entranceToHeartRoute ?: return false
        startedWave = StartedHeroWave(
            wave = upcomingWave,
            route = route,
            traps = grid.placedTraps,
        )
        return true
    }

    fun restart() {
        startedWave = null
    }
}
