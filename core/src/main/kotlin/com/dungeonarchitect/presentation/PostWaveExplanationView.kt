package com.dungeonarchitect.presentation

import com.dungeonarchitect.evaluation.WaveEvaluationReport
import com.dungeonarchitect.simulation.WaveOutcome
import java.util.Locale

data class PostWaveExplanationView(
    val headline: String,
    val explanation: String,
    val heroResultLabel: String,
    val heartLabel: String,
    val trapResultLabel: String,
    val elapsedTimeLabel: String,
) {
    companion object {
        fun from(report: WaveEvaluationReport) = PostWaveExplanationView(
            headline = when (report.outcome) {
                WaveOutcome.VICTORY -> "VICTORY - Heart secured"
                WaveOutcome.DEFEAT -> "DEFEAT - Heart destroyed"
            },
            explanation = report.explanation(),
            heroResultLabel =
                "Heroes: ${report.heroKills} defeated, " +
                    "${report.heroArrivals} arrived",
            heartLabel = "Heart health: ${report.heartHealth}",
            trapResultLabel =
                "Traps: ${report.trapActivations} activations, " +
                    "${report.trapDamage} damage",
            elapsedTimeLabel = "Elapsed simulation time: " +
                "%.2f s".format(
                    Locale.ROOT,
                    report.elapsedSimulationSeconds,
                ),
        )

        private fun WaveEvaluationReport.explanation(): String = when (outcome) {
            WaveOutcome.VICTORY -> if (heroArrivals == 0) {
                "All $heroKills ${heroLabel(heroKills)} were defeated before " +
                    "reaching the heart."
            } else {
                "The heart survived with $heartHealth health after " +
                    "$heroArrivals ${heroLabel(heroArrivals)} arrived; " +
                    "$heroKills ${heroLabel(heroKills)} were defeated."
            }
            WaveOutcome.DEFEAT ->
                "$heroArrivals ${heroLabel(heroArrivals)} reached the heart " +
                    "and reduced its health to 0; $heroKills " +
                    "${heroLabel(heroKills)} were defeated."
        }

        private fun heroLabel(count: Int): String =
            if (count == 1) "hero" else "heroes"
    }
}
