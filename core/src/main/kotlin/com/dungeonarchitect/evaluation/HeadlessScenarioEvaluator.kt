package com.dungeonarchitect.evaluation

import com.dungeonarchitect.application.PrototypeRunController
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import com.dungeonarchitect.simulation.HeroArrived
import com.dungeonarchitect.simulation.HeroDamaged
import com.dungeonarchitect.simulation.HeroDied
import com.dungeonarchitect.simulation.TrapActivated
import com.dungeonarchitect.simulation.WaveResolved

object HeadlessScenarioEvaluator {
    fun evaluate(
        grid: DungeonGrid,
        wave: UpcomingHeroWave,
        runDefinition: PrototypeRunDefinition,
    ): WaveEvaluationReport {
        val controller = PrototypeRunController(
            grid = grid,
            upcomingWave = wave,
            runDefinition = runDefinition,
        )
        require(controller.start()) {
            "A headless scenario requires a route from entrance to the heart."
        }

        var completedSteps = 0L
        controller.advance(elapsedSeconds = 0f)
        while (controller.phase == PrototypeRunPhase.RUNNING) {
            controller.advance(
                elapsedSeconds =
                    FixedStepHeroSimulation.FIXED_STEP_SECONDS.toFloat(),
            )
            completedSteps++
        }

        val events = controller.events
        val resolution = events.filterIsInstance<WaveResolved>().single()
        return WaveEvaluationReport(
            outcome = resolution.outcome,
            heartHealth = resolution.heartHealth,
            heroKills = events.count { it is HeroDied },
            heroArrivals = events.count { it is HeroArrived },
            elapsedSimulationSeconds =
                completedSteps * FixedStepHeroSimulation.FIXED_STEP_SECONDS,
            trapActivations = events.count { it is TrapActivated },
            trapDamage = events.filterIsInstance<HeroDamaged>().sumOf { it.damage },
        )
    }
}
