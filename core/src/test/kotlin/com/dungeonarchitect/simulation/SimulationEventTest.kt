package com.dungeonarchitect.simulation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.HeroGridPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SimulationEventTest {
    @Test
    fun `events have structural value semantics`() {
        val position = HeroGridPosition(column = 2.5f, row = 3f)
        val trapPosition = GridPosition(column = 2, row = 3)
        val events: List<SimulationEvent> = listOf(
            HeroSpawned(1, "fighter", position, 10),
            HeroArrived(1, position),
            TrapActivated(1, "spike-trap", trapPosition),
            HeroDamaged(1, "spike-trap", 3, 7),
            HeroDied(1, position),
            HeartDamaged(1, 4, 6),
            WaveResolved(WaveOutcome.VICTORY, 6),
        )

        assertEquals(
            events,
            listOf(
                HeroSpawned(1, "fighter", position.copy(), 10),
                HeroArrived(1, position.copy()),
                TrapActivated(1, "spike-trap", trapPosition.copy()),
                HeroDamaged(1, "spike-trap", 3, 7),
                HeroDied(1, position.copy()),
                HeartDamaged(1, 4, 6),
                WaveResolved(WaveOutcome.VICTORY, 6),
            ),
        )
    }

    @Test
    fun `hero events reject non-positive hero numbers`() {
        val position = HeroGridPosition(column = 0f, row = 0f)
        val invalidEvents = listOf<() -> SimulationEvent>(
            { HeroSpawned(0, "fighter", position, 10) },
            { HeroArrived(0, position) },
            { TrapActivated(0, "spike-trap", GridPosition(0, 0)) },
            { HeroDamaged(0, "spike-trap", 3, 7) },
            { HeroDied(0, position) },
            { HeartDamaged(0, 4, 6) },
        )

        invalidEvents.forEach { event ->
            assertFailsWith<IllegalArgumentException> { event() }
        }
    }

    @Test
    fun `events reject invalid identifiers health and damage`() {
        val position = HeroGridPosition(column = 0f, row = 0f)

        listOf(
            { HeroSpawned(1, " ", position, 10) },
            { HeroSpawned(1, "fighter", position, 0) },
            { TrapActivated(1, " ", GridPosition(0, 0)) },
            { HeroDamaged(1, " ", 3, 7) },
            { HeroDamaged(1, "spike-trap", 0, 7) },
            { HeroDamaged(1, "spike-trap", 3, -1) },
            { HeartDamaged(1, 0, 6) },
            { HeartDamaged(1, 4, -1) },
        ).forEach { event ->
            assertFailsWith<IllegalArgumentException> { event() }
        }
    }

    @Test
    fun `wave outcome must agree with heart health`() {
        assertEquals(
            WaveResolved(WaveOutcome.VICTORY, heartHealth = 1),
            WaveResolved(WaveOutcome.VICTORY, heartHealth = 1),
        )
        assertEquals(
            WaveResolved(WaveOutcome.DEFEAT, heartHealth = 0),
            WaveResolved(WaveOutcome.DEFEAT, heartHealth = 0),
        )

        assertFailsWith<IllegalArgumentException> {
            WaveResolved(WaveOutcome.VICTORY, heartHealth = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            WaveResolved(WaveOutcome.DEFEAT, heartHealth = 1)
        }
        assertFailsWith<IllegalArgumentException> {
            WaveResolved(WaveOutcome.DEFEAT, heartHealth = -1)
        }
    }
}
