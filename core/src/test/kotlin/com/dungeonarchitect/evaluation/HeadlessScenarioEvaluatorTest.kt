package com.dungeonarchitect.evaluation

import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomDoor
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import com.dungeonarchitect.simulation.WaveOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HeadlessScenarioEvaluatorTest {
    @Test
    fun `lethal trap produces a victory report with actual damage`() {
        val grid = routedGrid(hasSocket = true)
        assertTrue(
            grid.placeTrap(
                room = grid.placedRooms.single(),
                localSocketPosition = GridPosition(0, 0),
                definition = trap(damage = 12),
            ),
        )

        assertEquals(
            WaveEvaluationReport(
                outcome = WaveOutcome.VICTORY,
                objectiveHealth = 10,
                heroKills = 2,
                heroArrivals = 0,
                elapsedSimulationSeconds = fixedSteps(4),
                trapActivations = 2,
                trapDamage = 10,
            ),
            HeadlessScenarioEvaluator.evaluate(
                grid = grid,
                wave = wave(heroCount = 2, heroHealth = 5),
                runDefinition = PrototypeRunDefinition(objectiveHealth = 10),
            ),
        )
    }

    @Test
    fun `hero arrivals produce a defeat report with clamped objective damage`() {
        assertEquals(
            WaveEvaluationReport(
                outcome = WaveOutcome.DEFEAT,
                objectiveHealth = 0,
                heroKills = 0,
                heroArrivals = 2,
                elapsedSimulationSeconds = fixedSteps(4),
                trapActivations = 0,
                trapDamage = 0,
            ),
            HeadlessScenarioEvaluator.evaluate(
                grid = routedGrid(),
                wave = wave(
                    heroCount = 3,
                    heroHealth = 5,
                    objectiveDamage = 10,
                ),
                runDefinition = PrototypeRunDefinition(objectiveHealth = 15),
            ),
        )
    }

    @Test
    fun `elapsed time counts exact fixed steps for a partial final segment`() {
        val report = HeadlessScenarioEvaluator.evaluate(
            grid = routedGrid(),
            wave = wave(
                heroCount = 1,
                heroHealth = 5,
                objectiveDamage = 1,
                speed = 70f,
            ),
            runDefinition = PrototypeRunDefinition(objectiveHealth = 10),
        )

        assertEquals(fixedSteps(2), report.elapsedSimulationSeconds)
        assertEquals(WaveOutcome.VICTORY, report.outcome)
    }

    @Test
    fun `unroutable layout fails before evaluation`() {
        val error = assertFailsWith<IllegalArgumentException> {
            HeadlessScenarioEvaluator.evaluate(
                grid = DungeonGrid(
                    width = 3,
                    height = 1,
                    entrance = GridPosition(0, 0),
                    objective = GridPosition(2, 0),
                ),
                wave = wave(heroCount = 1, heroHealth = 5),
                runDefinition = PrototypeRunDefinition(objectiveHealth = 10),
            )
        }

        assertEquals(
            "A headless scenario requires a route from entrance to objective.",
            error.message,
        )
    }

    private fun routedGrid(hasSocket: Boolean = false) = DungeonGrid(
        width = 3,
        height = 1,
        entrance = GridPosition(0, 0),
        objective = GridPosition(2, 0),
        placedRooms = listOf(
            PlacedRoom(
                blueprint = RoomBlueprint(
                    id = "test-room",
                    displayName = "Test Room",
                    footprint = setOf(GridPosition(0, 0)),
                    doors = listOf(
                        RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                        RoomDoor(GridPosition(0, 0), CardinalDirection.EAST),
                    ),
                    sockets = if (hasSocket) {
                        mapOf(GridPosition(0, 0) to RoomSocketType.FLOOR)
                    } else {
                        emptyMap()
                    },
                ),
                origin = GridPosition(1, 0),
            ),
        ),
    )

    private fun wave(
        heroCount: Int,
        heroHealth: Int,
        objectiveDamage: Int = 10,
        speed: Float = 60f,
    ) = UpcomingHeroWave(
        heroType = "militia_recruit",
        heroDisplayName = "Militia Recruit",
        count = heroCount,
        heroHealth = heroHealth,
        objectiveDamage = objectiveDamage,
        movementSpeedTilesPerSecond = speed,
        traitDescription = "A straightforward melee fighter.",
    )

    private fun trap(damage: Int) = TrapDefinition(
        id = "spike_trap",
        displayName = "Spike Trap",
        damage = damage,
        cooldownSeconds = 0.001f,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
    )

    private fun fixedSteps(count: Int): Double =
        count * FixedStepHeroSimulation.FIXED_STEP_SECONDS
}
