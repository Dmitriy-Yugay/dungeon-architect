package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.HeroGridPosition
import com.dungeonarchitect.simulation.HeroDamaged
import com.dungeonarchitect.simulation.HeroSpawned
import com.dungeonarchitect.simulation.SimulationEvent
import com.dungeonarchitect.simulation.TrapActivated
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CombatFeedbackTrackerTest {
    @Test
    fun `trap event starts one pulse and snapshot reuse does not retrigger it`() {
        val tracker = tracker()
        val events = listOf<SimulationEvent>(trapActivated())

        assertEquals(
            listOf(TrapActivationPulse(TRAP_POSITION, progress = 0f)),
            tracker.update(events, elapsedSeconds = 0f).trapPulses,
        )
        assertEquals(
            listOf(TrapActivationPulse(TRAP_POSITION, progress = 0.5f)),
            tracker.update(events, elapsedSeconds = 0.15f).trapPulses,
        )
        assertEquals(
            emptyList(),
            tracker.update(events, elapsedSeconds = 0.15f).trapPulses,
        )
    }

    @Test
    fun `damage event flashes the current hero health presentation`() {
        val tracker = tracker()
        val events = listOf<SimulationEvent>(
            trapActivated(),
            HeroDamaged(
                heroNumber = 1,
                sourceTrapId = "spike_trap",
                damage = 4,
                remainingHealth = 6,
            ),
        )

        assertEquals(
            HeroDamageFlash(heroNumber = 1, damage = 4, intensity = 1f),
            tracker.update(events, elapsedSeconds = 0f).heroDamageFlash,
        )
        assertEquals(
            HeroDamageFlash(heroNumber = 1, damage = 4, intensity = 0.5f),
            tracker.update(events, elapsedSeconds = 0.1f).heroDamageFlash,
        )
        assertNull(
            tracker.update(events, elapsedSeconds = 0.1f).heroDamageFlash,
        )
    }

    @Test
    fun `new hero clears damage flash belonging to the previous hero`() {
        val tracker = tracker()
        val damagedEvents = listOf<SimulationEvent>(
            HeroDamaged(
                heroNumber = 1,
                sourceTrapId = "spike_trap",
                damage = 4,
                remainingHealth = 0,
            ),
        )
        tracker.update(damagedEvents, elapsedSeconds = 0f)

        val view = tracker.update(
            damagedEvents + HeroSpawned(
                heroNumber = 2,
                heroType = "militia_recruit",
                position = HeroGridPosition(0f, 0f),
                health = 10,
            ),
            elapsedSeconds = 0f,
        )

        assertNull(view.heroDamageFlash)
    }

    @Test
    fun `replacement event snapshot clears feedback from an earlier run`() {
        val tracker = tracker()
        tracker.update(
            listOf(trapActivated()),
            elapsedSeconds = 0f,
        )

        assertEquals(
            CombatFeedbackView.NONE,
            tracker.update(emptyList(), elapsedSeconds = 0f),
        )
    }

    @Test
    fun `tracker rejects invalid timing boundaries`() {
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { duration ->
            assertFailsWith<IllegalArgumentException> {
                CombatFeedbackTracker(trapPulseDurationSeconds = duration)
            }
            assertFailsWith<IllegalArgumentException> {
                CombatFeedbackTracker(heroDamageFlashDurationSeconds = duration)
            }
        }

        val tracker = tracker()
        listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { elapsed ->
            assertFailsWith<IllegalArgumentException> {
                tracker.update(emptyList(), elapsed)
            }
        }
    }

    private fun tracker() = CombatFeedbackTracker(
        trapPulseDurationSeconds = 0.3f,
        heroDamageFlashDurationSeconds = 0.2f,
    )

    private fun trapActivated() = TrapActivated(
        heroNumber = 1,
        trapId = "spike_trap",
        trapPosition = TRAP_POSITION,
    )

    private companion object {
        val TRAP_POSITION = GridPosition(2, 3)
    }
}
