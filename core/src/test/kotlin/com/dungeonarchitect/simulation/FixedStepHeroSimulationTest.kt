package com.dungeonarchitect.simulation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.HeroGridPosition
import com.dungeonarchitect.domain.StartedHeroWave
import com.dungeonarchitect.domain.UpcomingHeroWave
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FixedStepHeroSimulationTest {
    @Test
    fun `hero moves partway along a route at the authored speed`() {
        val simulation = simulation(
            route = listOf(position(0, 0), position(2, 0)),
            speed = 2f,
        )

        simulation.advance(elapsedSeconds = 0.25f)

        assertEquals(HeroGridPosition(column = 0.5f, row = 0f), simulation.heroState.position)
        assertFalse(simulation.heroState.hasArrived)
    }

    @Test
    fun `large elapsed time carries hero across a cardinal route corner`() {
        val simulation = simulation(
            route = listOf(
                position(0, 0),
                position(2, 0),
                position(2, 3),
            ),
            speed = 2f,
        )

        simulation.advance(elapsedSeconds = 1.75f)

        assertEquals(HeroGridPosition(column = 2f, row = 1.5f), simulation.heroState.position)
        assertFalse(simulation.heroState.hasArrived)
    }

    @Test
    fun `fixed-step result is deterministic across elapsed-time chunks`() {
        val route = listOf(
            position(0, 0),
            position(2, 0),
            position(2, 3),
        )
        val singleChunk = simulation(route = route, speed = 2f)
        val fourChunks = simulation(route = route, speed = 2f)

        singleChunk.advance(elapsedSeconds = 1f)
        repeat(4) {
            fourChunks.advance(elapsedSeconds = 0.25f)
        }

        assertEquals(singleChunk.heroState, fourChunks.heroState)
    }

    @Test
    fun `simulation rejects invalid routes and elapsed time`() {
        listOf(
            listOf(position(0, 0), position(1, 1)),
            listOf(position(0, 0), position(0, 0)),
        ).forEach { route ->
            assertFailsWith<IllegalArgumentException> {
                simulation(route = route, speed = 2f)
            }
        }

        val simulation = simulation(
            route = listOf(position(0, 0), position(1, 0)),
            speed = 2f,
        )
        listOf(-0.1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { elapsed ->
            assertFailsWith<IllegalArgumentException> {
                simulation.advance(elapsed)
            }
        }
    }

    @Test
    fun `hero clamps to final point and never moves after arrival`() {
        val simulation = simulation(
            route = listOf(
                position(0, 0),
                position(1, 0),
                position(1, 1),
            ),
            speed = 3f,
        )

        simulation.advance(elapsedSeconds = 10f)

        assertEquals(HeroGridPosition(column = 1f, row = 1f), simulation.heroState.position)
        assertTrue(simulation.heroState.hasArrived)

        val arrivedState = simulation.heroState
        simulation.advance(elapsedSeconds = 100f)
        assertEquals(arrivedState, simulation.heroState)
    }

    @Test
    fun `single-point route starts arrived`() {
        val simulation = simulation(
            route = listOf(position(4, 5)),
            speed = 2f,
        )

        assertEquals(HeroGridPosition(column = 4f, row = 5f), simulation.heroState.position)
        assertTrue(simulation.heroState.hasArrived)
    }

    private fun simulation(
        route: List<GridPosition>,
        speed: Float,
    ) = FixedStepHeroSimulation(
        StartedHeroWave(
            wave = UpcomingHeroWave(
                heroType = "militia_recruit",
                heroDisplayName = "Militia Recruit",
                count = 4,
                movementSpeedTilesPerSecond = speed,
                traitDescription = "A straightforward melee fighter.",
            ),
            route = route,
        ),
    )

    private fun position(column: Int, row: Int) = GridPosition(column, row)
}
