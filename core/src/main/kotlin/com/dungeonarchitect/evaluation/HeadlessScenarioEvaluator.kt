package com.dungeonarchitect.evaluation

import com.dungeonarchitect.application.PrototypeRunController
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import com.dungeonarchitect.simulation.WaveOutcome

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

        advanceToReport(controller)

        return requireNotNull(controller.evaluationReport) {
            "A resolved headless scenario must expose an evaluation report."
        }
    }

    fun evaluateRun(
        grid: DungeonGrid,
        waveContentByPath: Map<String, UpcomingHeroWave>,
        runDefinition: PrototypeRunDefinition,
        strategy: CompleteRunStrategy,
    ): CompleteRunEvaluation {
        val authoredWaveIds = runDefinition.waves.map { it.id }
        require(strategy.waves.map { it.waveId } == authoredWaveIds) {
            "Strategy '${strategy.name}' must plan every authored wave in order: " +
                "${authoredWaveIds.joinToString()}."
        }
        require(strategy.waves.first().draftedRoom == null) {
            "Strategy '${strategy.name}' must not draft a room before the opening wave."
        }
        require(strategy.waves.drop(1).all { it.draftedRoom != null }) {
            "Strategy '${strategy.name}' must draft one room before every later wave."
        }

        val controller = PrototypeRunController(
            grid = grid,
            waveContentByPath = waveContentByPath,
            runDefinition = runDefinition,
        )
        val evaluatedWaves = mutableListOf<EvaluatedWave>()

        strategy.waves.forEachIndexed { index, waveStrategy ->
            if (index > 0) {
                require(controller.acknowledgeIntelligence()) {
                    "Strategy '${strategy.name}' cannot review intelligence for " +
                        "'${waveStrategy.waveId}'."
                }
                require(controller.placeRoom(requireNotNull(waveStrategy.draftedRoom))) {
                    "Strategy '${strategy.name}' has an illegal room draft for " +
                        "'${waveStrategy.waveId}'."
                }
            }
            waveStrategy.heartRoom?.let { room ->
                require(controller.placeOrRelocateHeart(room)) {
                    "Strategy '${strategy.name}' cannot select its heart room " +
                        "for '${waveStrategy.waveId}'."
                }
            }
            waveStrategy.defensePurchases.forEachIndexed { purchaseIndex, purchase ->
                require(
                    controller.purchaseDefense(
                        room = purchase.room,
                        localSocketPosition = purchase.localSocketPosition,
                        definition = purchase.definition,
                    ),
                ) {
                    "Strategy '${strategy.name}' has an illegal defense purchase " +
                        "${purchaseIndex + 1} for '${waveStrategy.waveId}'."
                }
            }
            require(controller.start()) {
                "Strategy '${strategy.name}' cannot start '${waveStrategy.waveId}'."
            }
            advanceToReport(controller)

            val report = requireNotNull(controller.evaluationReport)
            evaluatedWaves += EvaluatedWave(
                waveId = waveStrategy.waveId,
                report = report,
            )
            if (report.outcome == WaveOutcome.DEFEAT) {
                check(controller.acknowledgeWaveReport())
                return evaluationResult(
                    outcome = WaveOutcome.DEFEAT,
                    runDefinition = runDefinition,
                    controller = controller,
                    waves = evaluatedWaves,
                )
            }

            check(controller.claimWaveReward())
            check(controller.acknowledgeWaveReport())
        }

        check(controller.phase == PrototypeRunPhase.RUN_VICTORY)
        return evaluationResult(
            outcome = WaveOutcome.VICTORY,
            runDefinition = runDefinition,
            controller = controller,
            waves = evaluatedWaves,
        )
    }

    private fun advanceToReport(controller: PrototypeRunController) {
        controller.advance(elapsedSeconds = 0f)
        while (controller.phase == PrototypeRunPhase.COMBAT) {
            controller.advance(
                elapsedSeconds =
                    FixedStepHeroSimulation.FIXED_STEP_SECONDS.toFloat(),
            )
        }
    }

    private fun evaluationResult(
        outcome: WaveOutcome,
        runDefinition: PrototypeRunDefinition,
        controller: PrototypeRunController,
        waves: List<EvaluatedWave>,
    ) = CompleteRunEvaluation(
        waves = waves.toList(),
        aggregate = AggregateRunEvaluationReport.from(
            outcome = outcome,
            authoredWaveCount = runDefinition.waves.size,
            finalGold = controller.gold,
            waves = waves,
        ),
    )
}
