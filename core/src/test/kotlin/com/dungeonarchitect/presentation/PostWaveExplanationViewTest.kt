package com.dungeonarchitect.presentation

import com.dungeonarchitect.evaluation.WaveEvaluationReport
import com.dungeonarchitect.simulation.WaveOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

class PostWaveExplanationViewTest {
    @Test
    fun `victory explains that every hero was defeated before arrival`() {
        val view = PostWaveExplanationView.from(
            report(
                outcome = WaveOutcome.VICTORY,
                objectiveHealth = 10,
                heroKills = 4,
                heroArrivals = 0,
                elapsedSimulationSeconds = 4.066666666666666,
                trapActivations = 8,
                trapDamage = 40,
            ),
        )

        assertEquals("VICTORY - Objective secured", view.headline)
        assertEquals(
            "All 4 heroes were defeated before reaching the objective.",
            view.explanation,
        )
        assertEquals("Heroes: 4 defeated, 0 arrived", view.heroResultLabel)
        assertEquals("Objective health: 10", view.objectiveLabel)
        assertEquals("Traps: 8 activations, 40 damage", view.trapResultLabel)
        assertEquals("Elapsed simulation time: 4.07 s", view.elapsedTimeLabel)
    }

    @Test
    fun `victory explains when arrivals occurred but the objective survived`() {
        val view = PostWaveExplanationView.from(
            report(
                outcome = WaveOutcome.VICTORY,
                objectiveHealth = 6,
                heroKills = 3,
                heroArrivals = 1,
            ),
        )

        assertEquals(
            "The objective survived with 6 health after 1 hero arrived; " +
                "3 heroes were defeated.",
            view.explanation,
        )
    }

    @Test
    fun `defeat explains arrival damage including zero trap metrics`() {
        val report = report(
            outcome = WaveOutcome.DEFEAT,
            objectiveHealth = 0,
            heroKills = 0,
            heroArrivals = 1,
            elapsedSimulationSeconds = 0.0,
            trapActivations = 0,
            trapDamage = 0,
        )

        assertEquals(
            PostWaveExplanationView(
                headline = "DEFEAT - Objective destroyed",
                explanation =
                    "1 hero reached the objective and reduced its health to 0; " +
                        "0 heroes were defeated.",
                heroResultLabel = "Heroes: 0 defeated, 1 arrived",
                objectiveLabel = "Objective health: 0",
                trapResultLabel = "Traps: 0 activations, 0 damage",
                elapsedTimeLabel = "Elapsed simulation time: 0.00 s",
            ),
            PostWaveExplanationView.from(report),
        )
        assertEquals(
            PostWaveExplanationView.from(report),
            PostWaveExplanationView.from(report.copy()),
        )
    }

    private fun report(
        outcome: WaveOutcome,
        objectiveHealth: Int,
        heroKills: Int,
        heroArrivals: Int,
        elapsedSimulationSeconds: Double = 2.5,
        trapActivations: Int = 2,
        trapDamage: Int = 10,
    ) = WaveEvaluationReport(
        outcome = outcome,
        objectiveHealth = objectiveHealth,
        heroKills = heroKills,
        heroArrivals = heroArrivals,
        elapsedSimulationSeconds = elapsedSimulationSeconds,
        trapActivations = trapActivations,
        trapDamage = trapDamage,
    )
}
