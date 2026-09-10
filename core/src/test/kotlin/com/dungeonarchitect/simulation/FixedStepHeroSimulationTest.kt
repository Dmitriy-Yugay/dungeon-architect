package com.dungeonarchitect.simulation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.HeroGridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PlacedTrap
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.StartedHeroWave
import com.dungeonarchitect.domain.TrapDefinition
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
        assertEquals(10, simulation.heroState.health)
        assertEquals(10, simulation.heroState.maxHealth)
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
        val events = mutableListOf<SimulationEvent>()
        val simulation = simulation(
            route = listOf(
                position(0, 0),
                position(1, 0),
                position(1, 1),
            ),
            speed = 3f,
            heroNumber = 2,
            eventSink = events::add,
        )

        simulation.advance(elapsedSeconds = 10f)

        assertEquals(HeroGridPosition(column = 1f, row = 1f), simulation.heroState.position)
        assertTrue(simulation.heroState.hasArrived)

        val arrivedState = simulation.heroState
        simulation.advance(elapsedSeconds = 100f)
        assertEquals(arrivedState, simulation.heroState)
        assertEquals(
            listOf(
                HeroSpawned(
                    heroNumber = 2,
                    heroType = "militia_recruit",
                    position = HeroGridPosition(column = 0f, row = 0f),
                    health = 10,
                ),
                HeroArrived(
                    heroNumber = 2,
                    position = HeroGridPosition(column = 1f, row = 1f),
                ),
            ),
            events,
        )
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

    @Test
    fun `lethal trap damage stops the hero before movement`() {
        val trap = placedTrap(
            trapPosition = position(0, 0),
            damage = 10,
        )
        val simulation = simulation(
            route = listOf(position(0, 0), position(2, 0)),
            speed = 2f,
            traps = listOf(trap),
        )

        simulation.advance(
            elapsedSeconds = FixedStepHeroSimulation.FIXED_STEP_SECONDS.toFloat(),
        )

        assertEquals(0, simulation.heroState.health)
        assertEquals(10, simulation.heroState.maxHealth)
        assertTrue(simulation.heroState.isDead)
        assertFalse(simulation.heroState.hasArrived)
        assertEquals(HeroGridPosition(column = 0f, row = 0f), simulation.heroState.position)

        val deadState = simulation.heroState
        simulation.advance(elapsedSeconds = 10f)
        assertEquals(deadState, simulation.heroState)
    }

    private fun simulation(
        route: List<GridPosition>,
        speed: Float,
        traps: List<PlacedTrap> = emptyList(),
        heroNumber: Int = 1,
        eventSink: (SimulationEvent) -> Unit = {},
    ) = FixedStepHeroSimulation(
        StartedHeroWave(
            wave = UpcomingHeroWave(
                heroType = "militia_recruit",
                heroDisplayName = "Militia Recruit",
                count = 4,
                heroHealth = 10,
                heartDamage = 10,
                movementSpeedTilesPerSecond = speed,
                heroRole = "Frontline attacker",
                defenseImplication = "Cover multiple on-route sockets.",
                traitDescription = "A straightforward melee fighter.",
            ),
            route = route,
            traps = traps,
        ),
        heroNumber = heroNumber,
        eventSink = eventSink,
    )

    private fun placedTrap(
        trapPosition: GridPosition,
        damage: Int,
    ): PlacedTrap {
        val localSocketPosition = position(0, 0)
        val room = PlacedRoom(
            blueprint = RoomBlueprint(
                id = "socket-room",
                displayName = "Socket Room",
                heartAnchor = GridPosition(column = 0, row = 0),
                footprint = setOf(localSocketPosition),
                doorPositions = setOf(localSocketPosition),
                sockets = mapOf(
                    localSocketPosition to RoomSocketType.FLOOR,
                ),
            ),
            origin = trapPosition,
        )
        return PlacedTrap(
            definition = TrapDefinition(
                id = "spike_trap",
                displayName = "Spike Trap",
                damage = damage,
                cooldownSeconds = 0.25f,
                compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
            ),
            room = room,
            localSocketPosition = localSocketPosition,
        )
    }

    private fun position(column: Int, row: Int) = GridPosition(column, row)
}
