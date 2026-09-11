package com.dungeonarchitect.evaluation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.simulation.WaveOutcome

data class CompleteRunStrategy(
    val name: String,
    val description: String,
    val waves: List<WaveStrategy>,
) {
    init {
        require(name.isNotBlank()) {
            "A complete-run strategy must have a name."
        }
        require(description.isNotBlank()) {
            "A complete-run strategy must explain its plan."
        }
        require(waves.isNotEmpty()) {
            "A complete-run strategy must plan at least one wave."
        }
        require(waves.map(WaveStrategy::waveId).distinct().size == waves.size) {
            "A complete-run strategy must not repeat wave IDs."
        }
    }
}

data class WaveStrategy(
    val waveId: String,
    val draftedRoom: PlacedRoom? = null,
    val heartRoom: PlacedRoom? = null,
    val defensePurchases: List<DefensePurchase> = emptyList(),
) {
    init {
        require(waveId.isNotBlank()) {
            "A wave strategy must reference a wave ID."
        }
    }
}

data class DefensePurchase(
    val room: PlacedRoom,
    val localSocketPosition: GridPosition,
    val definition: TrapDefinition,
)

data class EvaluatedWave(
    val waveId: String,
    val report: WaveEvaluationReport,
)

data class CompleteRunEvaluation(
    val waves: List<EvaluatedWave>,
    val aggregate: AggregateRunEvaluationReport,
)

data class AggregateRunEvaluationReport(
    val outcome: WaveOutcome,
    val authoredWaveCount: Int,
    val resolvedWaveCount: Int,
    val finalHeartHealth: Int,
    val finalGold: Int,
    val heroKills: Int,
    val heroArrivals: Int,
    val elapsedSimulationSeconds: Double,
    val trapActivations: Int,
    val trapDamage: Int,
) {
    companion object {
        fun from(
            outcome: WaveOutcome,
            authoredWaveCount: Int,
            finalGold: Int,
            waves: List<EvaluatedWave>,
        ): AggregateRunEvaluationReport {
            val reports = waves.map(EvaluatedWave::report)
            return AggregateRunEvaluationReport(
                outcome = outcome,
                authoredWaveCount = authoredWaveCount,
                resolvedWaveCount = reports.size,
                finalHeartHealth = reports.last().heartHealth,
                finalGold = finalGold,
                heroKills = reports.sumOf(WaveEvaluationReport::heroKills),
                heroArrivals = reports.sumOf(WaveEvaluationReport::heroArrivals),
                elapsedSimulationSeconds = reports.sumOf(
                    WaveEvaluationReport::elapsedSimulationSeconds,
                ),
                trapActivations = reports.sumOf(
                    WaveEvaluationReport::trapActivations,
                ),
                trapDamage = reports.sumOf(WaveEvaluationReport::trapDamage),
            )
        }
    }
}
