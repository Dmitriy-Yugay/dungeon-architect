package com.dungeonarchitect.evaluation

import com.dungeonarchitect.simulation.WaveOutcome
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.HeroGridPosition
import com.dungeonarchitect.simulation.HeroArrived
import com.dungeonarchitect.simulation.HeroDamaged
import com.dungeonarchitect.simulation.HeroDied
import com.dungeonarchitect.simulation.TrapActivated
import com.dungeonarchitect.simulation.WaveResolved
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

    @Test
    fun `factory derives every metric from resolved event history`() {
        assertEquals(
            WaveEvaluationReport(
                outcome = WaveOutcome.VICTORY,
                heartHealth = 6,
                heroKills = 1,
                heroArrivals = 1,
                elapsedSimulationSeconds = 1.5,
                trapActivations = 2,
                trapDamage = 7,
            ),
            WaveEvaluationReport.fromEvents(
                events = listOf(
                    TrapActivated(1, "spikes", GridPosition(1, 0)),
                    HeroDamaged(1, "spikes", damage = 4, remainingHealth = 6),
                    TrapActivated(1, "spikes", GridPosition(1, 0)),
                    HeroDamaged(1, "spikes", damage = 3, remainingHealth = 3),
                    HeroDied(1, HeroGridPosition(2f, 0f)),
                    HeroArrived(2, HeroGridPosition(3f, 0f)),
                    WaveResolved(WaveOutcome.VICTORY, heartHealth = 6),
                ),
                elapsedSimulationSeconds = 1.5,
            ),
        )
    }

    @Test
    fun `factory requires exactly one resolution event`() {
        assertFailsWith<IllegalArgumentException> {
            WaveEvaluationReport.fromEvents(emptyList(), 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            WaveEvaluationReport.fromEvents(
                listOf(
                    WaveResolved(WaveOutcome.VICTORY, 10),
                    WaveResolved(WaveOutcome.VICTORY, 10),
                ),
                0.0,
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
