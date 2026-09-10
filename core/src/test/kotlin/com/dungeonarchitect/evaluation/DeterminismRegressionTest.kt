package com.dungeonarchitect.evaluation

import com.dungeonarchitect.application.PrototypeRunController
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.HeroGridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomDoor
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import com.dungeonarchitect.simulation.HeroArrived
import com.dungeonarchitect.simulation.HeroDamaged
import com.dungeonarchitect.simulation.HeroDied
import com.dungeonarchitect.simulation.HeroSpawned
import com.dungeonarchitect.simulation.HeartDamaged
import com.dungeonarchitect.simulation.TrapActivated
import com.dungeonarchitect.simulation.WaveOutcome
import com.dungeonarchitect.simulation.WaveResolved
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeterminismRegressionTest {
    @Test
    fun `identical independently built scenarios produce identical reports`() {
        val first = scenario()
        val second = scenario()

        val firstReport = HeadlessScenarioEvaluator.evaluate(
            first.grid,
            first.wave,
            first.runDefinition,
        )
        val secondReport = HeadlessScenarioEvaluator.evaluate(
            second.grid,
            second.wave,
            second.runDefinition,
        )

        assertEquals(
            WaveEvaluationReport(
                outcome = WaveOutcome.VICTORY,
                heartHealth = 6,
                heroKills = 1,
                heroArrivals = 1,
                elapsedSimulationSeconds = elapsedSteps(4),
                trapActivations = 1,
                trapDamage = 5,
            ),
            firstReport,
        )
        assertEquals(firstReport, secondReport)
    }

    @Test
    fun `identical scenarios produce identical events across elapsed-time chunks`() {
        val first = scenario().controller()
        val second = scenario().controller()
        assertTrue(first.start())
        assertTrue(second.start())

        first.advance(elapsedSeconds = fixedSteps(5))
        repeat(5) {
            second.advance(elapsedSeconds = fixedSteps(1))
        }

        assertEquals(PrototypeRunPhase.VICTORY, first.phase)
        assertEquals(first.phase, second.phase)
        assertEquals(first.events, second.events)
        assertEquals(
            listOf(
                HeroSpawned(1, HERO_TYPE, heroPosition(0), health = 5),
                TrapActivated(1, TRAP_ID, GridPosition(1, 0)),
                HeroDamaged(1, TRAP_ID, damage = 5, remainingHealth = 0),
                HeroDied(1, heroPosition(1)),
                HeroSpawned(2, HERO_TYPE, heroPosition(0), health = 5),
                HeroArrived(2, heroPosition(2)),
                HeartDamaged(2, damage = 4, remainingHealth = 6),
                WaveResolved(WaveOutcome.VICTORY, heartHealth = 6),
            ),
            first.events,
        )
    }

    private fun scenario(): Scenario {
        val socketPosition = GridPosition(0, 0)
        val grid = DungeonGrid(
            width = 4,
            height = 1,
            entrance = GridPosition(0, 0),
            placedRooms = listOf(
                PlacedRoom(
                    blueprint = RoomBlueprint(
                        id = "cooldown-room",
                        displayName = "Cooldown Room",
                        heartAnchor = GridPosition(column = 1, row = 0),
                        footprint = setOf(
                            GridPosition(0, 0),
                            GridPosition(1, 0),
                        ),
                        doors = listOf(
                            RoomDoor(socketPosition, CardinalDirection.WEST),
                            RoomDoor(GridPosition(1, 0), CardinalDirection.EAST),
                        ),
                        sockets = mapOf(socketPosition to RoomSocketType.FLOOR),
                    ),
                    origin = GridPosition(1, 0),
                ),
            ),
        )
        assertTrue(grid.placeOrRelocateHeart(grid.placedRooms.single()))
        assertTrue(
            grid.placeTrap(
                room = grid.placedRooms.single(),
                localSocketPosition = socketPosition,
                definition = TrapDefinition(
                    id = TRAP_ID,
                    displayName = "Spike Trap",
                    damage = 5,
                    cooldownSeconds = 10f,
                    compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
                ),
            ),
        )
        return Scenario(
            grid = grid,
            wave = UpcomingHeroWave(
                heroType = HERO_TYPE,
                heroDisplayName = "Militia Recruit",
                count = 2,
                heroHealth = 5,
                heartDamage = 4,
                movementSpeedTilesPerSecond = 60f,
                traitDescription = "A straightforward melee fighter.",
            ),
            runDefinition = PrototypeRunDefinition.singleWave(
                heartHealth = 10,
                contentPath = "test-wave.json",
            ),
        )
    }

    private fun Scenario.controller() = PrototypeRunController(
        grid = grid,
        upcomingWave = wave,
        runDefinition = runDefinition,
    )

    private fun fixedSteps(count: Int): Float =
        (count * FixedStepHeroSimulation.FIXED_STEP_SECONDS).toFloat()

    private fun elapsedSteps(count: Int): Double =
        count * FixedStepHeroSimulation.FIXED_STEP_SECONDS

    private fun heroPosition(column: Int) = HeroGridPosition(
        column = column.toFloat(),
        row = 0f,
    )

    private data class Scenario(
        val grid: DungeonGrid,
        val wave: UpcomingHeroWave,
        val runDefinition: PrototypeRunDefinition,
    )

    private companion object {
        const val HERO_TYPE = "militia_recruit"
        const val TRAP_ID = "spike_trap"
    }
}
