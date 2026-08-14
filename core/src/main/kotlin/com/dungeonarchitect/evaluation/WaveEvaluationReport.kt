package com.dungeonarchitect.evaluation

import com.dungeonarchitect.simulation.WaveOutcome

data class WaveEvaluationReport(
    val outcome: WaveOutcome,
    val objectiveHealth: Int,
    val heroKills: Int,
    val heroArrivals: Int,
    val elapsedSimulationSeconds: Double,
    val trapActivations: Int,
    val trapDamage: Int,
) {
    init {
        require(objectiveHealth >= 0) {
            "Wave report objective health must not be negative."
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
            (outcome == WaveOutcome.VICTORY && objectiveHealth > 0) ||
                (outcome == WaveOutcome.DEFEAT && objectiveHealth == 0),
        ) {
            "Wave report outcome must agree with the remaining objective health."
        }
    }
}
