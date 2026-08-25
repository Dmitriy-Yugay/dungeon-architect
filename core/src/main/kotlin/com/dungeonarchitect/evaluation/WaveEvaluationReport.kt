package com.dungeonarchitect.evaluation

import com.dungeonarchitect.simulation.HeroArrived
import com.dungeonarchitect.simulation.HeroDamaged
import com.dungeonarchitect.simulation.HeroDied
import com.dungeonarchitect.simulation.SimulationEvent
import com.dungeonarchitect.simulation.TrapActivated
import com.dungeonarchitect.simulation.WaveOutcome
import com.dungeonarchitect.simulation.WaveResolved

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

    companion object {
        fun fromEvents(
            events: Iterable<SimulationEvent>,
            elapsedSimulationSeconds: Double,
        ): WaveEvaluationReport {
            val eventSnapshot = events.toList()
            val resolutions = eventSnapshot.filterIsInstance<WaveResolved>()
            require(resolutions.size == 1) {
                "A wave report requires exactly one resolution event."
            }
            val resolution = resolutions.single()
            return WaveEvaluationReport(
                outcome = resolution.outcome,
                heartHealth = resolution.heartHealth,
                heroKills = eventSnapshot.count { it is HeroDied },
                heroArrivals = eventSnapshot.count { it is HeroArrived },
                elapsedSimulationSeconds = elapsedSimulationSeconds,
                trapActivations = eventSnapshot.count { it is TrapActivated },
                trapDamage = eventSnapshot
                    .filterIsInstance<HeroDamaged>()
                    .sumOf(HeroDamaged::damage),
            )
        }
    }
}
