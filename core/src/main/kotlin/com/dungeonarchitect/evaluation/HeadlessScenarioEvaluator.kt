package com.dungeonarchitect.evaluation

import com.dungeonarchitect.application.PrototypeRunController
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.simulation.FixedStepHeroSimulation

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

        controller.advance(elapsedSeconds = 0f)
        while (controller.phase == PrototypeRunPhase.COMBAT) {
            controller.advance(
                elapsedSeconds =
                    FixedStepHeroSimulation.FIXED_STEP_SECONDS.toFloat(),
            )
        }

        return requireNotNull(controller.evaluationReport) {
            "A resolved headless scenario must expose an evaluation report."
        }
    }
}
