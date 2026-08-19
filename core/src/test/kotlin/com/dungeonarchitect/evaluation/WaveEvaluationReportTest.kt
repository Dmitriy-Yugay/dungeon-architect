package com.dungeonarchitect.evaluation

import com.dungeonarchitect.simulation.WaveOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WaveEvaluationReportTest {
    @Test
    fun `report has structural value semantics`() {
        val report = report()

        assertEquals(report, report.copy())
        assertEquals(report.hashCode(), report.copy().hashCode())
    }

    @Test
    fun `report accepts zero-valued metrics`() {
        assertEquals(
            WaveEvaluationReport(
                outcome = WaveOutcome.VICTORY,
                heartHealth = 10,
                heroKills = 0,
                heroArrivals = 0,
                elapsedSimulationSeconds = 0.0,
                trapActivations = 0,
                trapDamage = 0,
            ),
            WaveEvaluationReport(
                outcome = WaveOutcome.VICTORY,
                heartHealth = 10,
                heroKills = 0,
                heroArrivals = 0,
                elapsedSimulationSeconds = 0.0,
                trapActivations = 0,
                trapDamage = 0,
            ),
        )
    }

    @Test
    fun `report rejects negative metrics and invalid elapsed time`() {
        listOf(
            { report(heartHealth = -1) },
            { report(heroKills = -1) },
            { report(heroArrivals = -1) },
            { report(elapsedSimulationSeconds = -0.1) },
            { report(elapsedSimulationSeconds = Double.NaN) },
            { report(elapsedSimulationSeconds = Double.POSITIVE_INFINITY) },
            { report(trapActivations = -1) },
            { report(trapDamage = -1) },
        ).forEach { invalidReport ->
            assertFailsWith<IllegalArgumentException> { invalidReport() }
        }
    }

    @Test
    fun `report outcome must agree with heart health`() {
        assertFailsWith<IllegalArgumentException> {
            report(
                outcome = WaveOutcome.VICTORY,
                heartHealth = 0,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            report(
                outcome = WaveOutcome.DEFEAT,
                heartHealth = 1,
            )
        }
    }

    private fun report(
        outcome: WaveOutcome = WaveOutcome.VICTORY,
        heartHealth: Int = 4,
        heroKills: Int = 3,
        heroArrivals: Int = 1,
        elapsedSimulationSeconds: Double = 2.5,
        trapActivations: Int = 5,
        trapDamage: Int = 12,
    ) = WaveEvaluationReport(
        outcome = outcome,
        heartHealth = heartHealth,
        heroKills = heroKills,
        heroArrivals = heroArrivals,
        elapsedSimulationSeconds = elapsedSimulationSeconds,
        trapActivations = trapActivations,
        trapDamage = trapDamage,
    )
}
