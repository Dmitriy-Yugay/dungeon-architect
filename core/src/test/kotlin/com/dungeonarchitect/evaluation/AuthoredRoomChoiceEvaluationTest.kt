package com.dungeonarchitect.evaluation

import com.dungeonarchitect.content.PrototypeRunDefinitionParser
import com.dungeonarchitect.content.RoomBlueprintParser
import com.dungeonarchitect.content.TrapDefinitionParser
import com.dungeonarchitect.content.UpcomingHeroWaveParser
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import com.dungeonarchitect.simulation.WaveOutcome
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthoredRoomChoiceEvaluationTest {
    @Test
    fun `authored room choices produce deterministic heart-route evaluations`() {
        val prototypeRoomReport = evaluateAuthoredRoom("prototype-room.json")
        val longGalleryReport = evaluateAuthoredRoom("long-gallery.json")

        assertEquals(
            WaveEvaluationReport(
                outcome = WaveOutcome.VICTORY,
                objectiveHealth = 10,
                heroKills = 4,
                heroArrivals = 0,
                elapsedSimulationSeconds = fixedSteps(244),
                trapActivations = 8,
                trapDamage = 40,
            ),
            prototypeRoomReport,
        )
        assertEquals(
            prototypeRoomReport.copy(elapsedSimulationSeconds = fixedSteps(364)),
            longGalleryReport,
        )
    }

    private fun evaluateAuthoredRoom(fileName: String): WaveEvaluationReport {
        val blueprint = RoomBlueprintParser.parse(readContent(fileName))
        val trap = TrapDefinitionParser.parse(readContent("spike-trap.json"))
        val placedRoom = PlacedRoom(
            blueprint = blueprint,
            origin = GridPosition(column = 1, row = 0),
        )
        val entranceDoor = blueprint.door(CardinalDirection.WEST)
        val entrance = CardinalDirection.WEST.move(
            placedRoom.toGridPosition(entranceDoor.position),
        )
        val occupiedPositions = placedRoom.gridPositions + entrance
        val grid = DungeonGrid(
            width = occupiedPositions.maxOf(GridPosition::column) + 1,
            height = occupiedPositions.maxOf(GridPosition::row) + 1,
            entrance = entrance,
            placedRooms = listOf(placedRoom),
        )
        assertTrue(grid.placeOrRelocateHeart(placedRoom))
        assertTrue(
            grid.placeTrap(
                room = placedRoom,
                localSocketPosition = blueprint.sockets.keys.single(),
                definition = trap,
            ),
        )

        return HeadlessScenarioEvaluator.evaluate(
            grid = grid,
            wave = UpcomingHeroWaveParser.parse(
                readContent("upcoming-hero-wave.json"),
            ),
            runDefinition = PrototypeRunDefinitionParser.parse(
                readContent("prototype-run.json"),
            ),
        )
    }

    private fun RoomBlueprint.door(
        facing: CardinalDirection,
    ) = doors.single { it.facing == facing }

    private fun readContent(fileName: String): String {
        val content = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("assets/content/$fileName") }
            .firstOrNull(Files::isRegularFile)
            ?: error("Could not find authored content file '$fileName'.")
        return Files.readString(content)
    }

    private fun fixedSteps(count: Int): Double =
        count * FixedStepHeroSimulation.FIXED_STEP_SECONDS
}
