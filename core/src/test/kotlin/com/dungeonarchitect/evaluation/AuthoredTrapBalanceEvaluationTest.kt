package com.dungeonarchitect.evaluation

import com.dungeonarchitect.content.PrototypeRunDefinitionParser
import com.dungeonarchitect.content.RoomBlueprintParser
import com.dungeonarchitect.content.TrapDefinitionParser
import com.dungeonarchitect.content.UpcomingHeroWaveParser
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomOrientation
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import com.dungeonarchitect.simulation.WaveOutcome
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthoredTrapBalanceEvaluationTest {
    @Test
    fun `authored rooms reward two well-spaced spike traps`() {
        authoredLayouts().forEach { layout ->
            (0..2).forEach { trapCount ->
                val report = evaluate(layout, trapCount)
                assertEquals(
                    layout.expectedReports[trapCount],
                    report,
                    "${layout.roomFileName} with $trapCount traps",
                )
            }
        }
    }

    private fun evaluate(
        layout: AuthoredLayout,
        trapCount: Int,
    ): WaveEvaluationReport {
        val blueprint = RoomBlueprintParser.parse(
            readContent(layout.roomFileName),
        )
        val rooms = layout.placements.map { placement ->
            PlacedRoom(
                blueprint = blueprint,
                origin = placement.origin,
                orientation = placement.orientation,
            )
        }
        val grid = DungeonGrid(
            width = layout.width,
            height = layout.height,
            entrance = layout.entrance,
            placedRooms = rooms,
        )
        assertTrue(grid.placeOrRelocateHeart(rooms.last()))

        val trap = TrapDefinitionParser.parse(readContent("spike-trap.json"))
        val trapPositions = layout.trapRoomIndexes
            .take(trapCount)
            .map { roomIndex ->
                val room = rooms[roomIndex]
                val socket = room.geometry.sockets.keys.single()
                assertTrue(grid.placeTrap(room, socket, trap))
                room.toGridPosition(socket)
            }
        assertWellSpacedOnRoute(grid, trapPositions)

        return evaluate(grid)
    }

    private fun assertWellSpacedOnRoute(
        grid: DungeonGrid,
        trapPositions: List<GridPosition>,
    ) {
        val route = requireNotNull(grid.entranceToHeartRoute)
        assertTrue(
            trapPositions.all(route::contains),
            "Expected traps $trapPositions on route $route.",
        )
        trapPositions.zipWithNext().forEach { (first, second) ->
            assertTrue(
                abs(first.column - second.column) +
                    abs(first.row - second.row) >= MIN_TRAP_SPACING_TILES,
            )
        }
    }

    private fun evaluate(grid: DungeonGrid): WaveEvaluationReport =
        HeadlessScenarioEvaluator.evaluate(
            grid = grid,
            wave = UpcomingHeroWaveParser.parse(
                readContent("upcoming-hero-wave.json"),
            ),
            runDefinition = PrototypeRunDefinitionParser.parse(
                readContent("prototype-run.json"),
            ),
        )

    private fun authoredLayouts(): List<AuthoredLayout> = listOf(
        AuthoredLayout(
            roomFileName = "prototype-room.json",
            entrance = GridPosition(0, 1),
            width = 11,
            height = 4,
            placements = listOf(1, 4, 7).map { column ->
                RoomPlacement(GridPosition(column, 0))
            },
            expectedReports = balanceReports(
                defeatSteps = 270,
                victorySteps = 544,
            ),
        ),
        AuthoredLayout(
            roomFileName = "long-gallery.json",
            entrance = GridPosition(0, 1),
            width = 14,
            height = 3,
            placements = listOf(1, 5, 9).map { column ->
                RoomPlacement(GridPosition(column, 0))
            },
            expectedReports = balanceReports(
                defeatSteps = 390,
                victorySteps = 784,
            ),
        ),
        AuthoredLayout(
            roomFileName = "corner-room.json",
            entrance = GridPosition(0, 0),
            width = 8,
            height = 8,
            placements = listOf(
                RoomPlacement(GridPosition(1, 0)),
                RoomPlacement(
                    origin = GridPosition(2, 2),
                    orientation = RoomOrientation.CLOCKWISE_180,
                ),
                RoomPlacement(GridPosition(4, 3)),
                RoomPlacement(
                    origin = GridPosition(5, 5),
                    orientation = RoomOrientation.CLOCKWISE_180,
                ),
            ),
            trapRoomIndexes = listOf(0, 2),
            expectedReports = balanceReports(
                defeatSteps = 330,
                victorySteps = 904,
            ),
        ),
    )

    private fun balanceReports(
        defeatSteps: Int,
        victorySteps: Int,
    ): List<WaveEvaluationReport> = listOf(
        WaveEvaluationReport(
            outcome = WaveOutcome.DEFEAT,
            heartHealth = 0,
            heroKills = 0,
            heroArrivals = 1,
            elapsedSimulationSeconds = fixedSteps(defeatSteps),
            trapActivations = 0,
            trapDamage = 0,
        ),
        WaveEvaluationReport(
            outcome = WaveOutcome.DEFEAT,
            heartHealth = 0,
            heroKills = 0,
            heroArrivals = 1,
            elapsedSimulationSeconds = fixedSteps(defeatSteps),
            trapActivations = 2,
            trapDamage = 8,
        ),
        WaveEvaluationReport(
            outcome = WaveOutcome.VICTORY,
            heartHealth = 10,
            heroKills = 4,
            heroArrivals = 0,
            elapsedSimulationSeconds = fixedSteps(victorySteps),
            trapActivations = 12,
            trapDamage = 40,
        ),
    )

    private fun fixedSteps(count: Int): Double =
        count * FixedStepHeroSimulation.FIXED_STEP_SECONDS

    private fun readContent(fileName: String): String {
        val content = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("assets/content/$fileName") }
            .firstOrNull(Files::isRegularFile)
            ?: error("Could not find authored content file '$fileName'.")
        return Files.readString(content)
    }

    private data class AuthoredLayout(
        val roomFileName: String,
        val entrance: GridPosition,
        val width: Int,
        val height: Int,
        val placements: List<RoomPlacement>,
        val expectedReports: List<WaveEvaluationReport>,
        val trapRoomIndexes: List<Int> = listOf(0, 1),
    )

    private data class RoomPlacement(
        val origin: GridPosition,
        val orientation: RoomOrientation = RoomOrientation.UNROTATED,
    )

    private companion object {
        const val MIN_TRAP_SPACING_TILES = 2
    }
}
