package com.dungeonarchitect.evaluation

import com.dungeonarchitect.simulation.WaveOutcome

data class WaveEvaluationReport(
    val outcome: WaveOutcome,
    val heartHealth: Int,
    val heroKills: Int,
    val heroArrivals: Int,
    val elapsedSimulationSeconds: Double,
    val trapActivations: Int,
    val trapDamage: Int,
) {
    init {
        require(heartHealth >= 0) {
            "Wave report heart health must not be negative."
        }
        require(heroKills >= 0) {
            "Wave report hero kills must not be negative."
        }
        require(heroArrivals >= 0) {
            "Wave report hero arrivals must not be negative."
        }
        require(
            elapsedSimulationSeconds.isFinite() &&
                elapsedSimulationSeconds >= 0.0,
        ) {
            "Wave report elapsed simulation time must be finite and non-negative."
        }
        require(trapActivations >= 0) {
            "Wave report trap activations must not be negative."
        }
        require(trapDamage >= 0) {
            "Wave report trap damage must not be negative."
        }
        require(
            (outcome == WaveOutcome.VICTORY && heartHealth > 0) ||
                (outcome == WaveOutcome.DEFEAT && heartHealth == 0),
        ) {
            "Wave report outcome must agree with the remaining heart health."
        }
    }
}
