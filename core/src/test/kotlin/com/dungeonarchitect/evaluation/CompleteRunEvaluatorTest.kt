package com.dungeonarchitect.evaluation

import com.dungeonarchitect.content.PrototypeRunDefinitionParser
import com.dungeonarchitect.content.RoomBlueprintParser
import com.dungeonarchitect.content.TrapDefinitionParser
import com.dungeonarchitect.content.UpcomingHeroWaveParser
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomOrientation
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.simulation.WaveOutcome
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CompleteRunEvaluatorTest {
    @Test
    fun `two explicit authored strategies complete deterministically`() {
        val firstStraight = evaluateStraightCoverage()
        val secondStraight = evaluateStraightCoverage()
        val firstTurned = evaluateTurnedCoverage()
        val secondTurned = evaluateTurnedCoverage()

        assertEquals(firstStraight, secondStraight)
        assertEquals(firstTurned, secondTurned)
        listOf(firstStraight, firstTurned).forEach { evaluation ->
            assertEquals(
                listOf(
                    "opening-recruits",
                    "reinforcement-recruits",
                    "final-recruits",
                ),
                evaluation.waves.map(EvaluatedWave::waveId),
                evaluation.toString(),
            )
            assertTrue(evaluation.waves.all { it.report.outcome == WaveOutcome.VICTORY })
            assertEquals(WaveOutcome.VICTORY, evaluation.aggregate.outcome)
            assertEquals(3, evaluation.aggregate.authoredWaveCount)
            assertEquals(3, evaluation.aggregate.resolvedWaveCount)
            assertEquals(10, evaluation.aggregate.finalHeartHealth)
            assertEquals(12, evaluation.aggregate.heroKills)
            assertEquals(0, evaluation.aggregate.heroArrivals)
            assertEquals(288, evaluation.aggregate.trapDamage)
            assertEquals(72, evaluation.aggregate.trapActivations)
            assertEquals(
                listOf(64, 96, 128),
                evaluation.waves.map { it.report.trapDamage },
            )
            assertEquals(
                listOf(4, 6, 8),
                evaluation.waves.map { it.report.trapActivations / 4 },
            )
        }
        assertEquals(0, firstStraight.aggregate.finalGold)
        assertEquals(0, firstTurned.aggregate.finalGold)
        assertTrue(
            firstStraight.aggregate.elapsedSimulationSeconds !=
                firstTurned.aggregate.elapsedSimulationSeconds,
        )
    }

    @Test
    fun `complete strategy must match authored wave order and cadence`() {
        val fixture = straightCoverageFixture()
        val malformedStrategy = fixture.strategy.copy(
            waves = fixture.strategy.waves.reversed(),
        )

        val error = assertFailsWith<IllegalArgumentException> {
            evaluate(fixture.copy(strategy = malformedStrategy))
        }

        assertTrue(error.message.orEmpty().contains("every authored wave in order"))
    }

    @Test
    fun `defeated run returns the resolved wave and partial aggregate`() {
        val fixture = straightCoverageFixture()
        val defeatedStrategy = fixture.strategy.copy(
            name = "Unarmed gallery",
            description = "Leave the route undefended to verify partial reports.",
            waves = fixture.strategy.waves.mapIndexed { index, wave ->
                if (index == 0) wave.copy(defensePurchases = emptyList()) else wave
            },
        )

        val evaluation = evaluate(fixture.copy(strategy = defeatedStrategy))

        assertEquals(WaveOutcome.DEFEAT, evaluation.aggregate.outcome)
        assertEquals(3, evaluation.aggregate.authoredWaveCount)
        assertEquals(1, evaluation.aggregate.resolvedWaveCount)
        assertEquals(0, evaluation.aggregate.finalHeartHealth)
        assertEquals(2, evaluation.aggregate.finalGold)
        assertEquals(0, evaluation.aggregate.heroKills)
        assertEquals(1, evaluation.aggregate.heroArrivals)
        assertEquals(listOf("opening-recruits"), evaluation.waves.map { it.waveId })
    }

    private fun evaluateStraightCoverage(): CompleteRunEvaluation =
        evaluate(straightCoverageFixture())

    private fun evaluateTurnedCoverage(): CompleteRunEvaluation =
        evaluate(turnedCoverageFixture())

    private fun evaluate(fixture: RunFixture): CompleteRunEvaluation =
        HeadlessScenarioEvaluator.evaluateRun(
            grid = fixture.grid,
            waveContentByPath = fixture.waveContentByPath,
            runDefinition = fixture.runDefinition,
            strategy = fixture.strategy,
        )

    private fun straightCoverageFixture(): RunFixture {
        val content = authoredContent()
        val gallery = content.blueprints.getValue("long-gallery")
        val rooms = listOf(1, 5).map { column ->
            PlacedRoom(gallery, GridPosition(column, 0))
        }
        val grid = DungeonGrid(
            width = 16,
            height = 9,
            entrance = GridPosition(0, 1),
            placedRooms = rooms,
        )
        val firstDraft = PlacedRoom(
            content.blueprints.getValue("long-gallery"),
            GridPosition(9, 0),
        )
        val secondDraft = PlacedRoom(
            content.blueprints.getValue("prototype-room"),
            GridPosition(13, 0),
        )
        return RunFixture(
            grid = grid,
            waveContentByPath = content.waveContentByPath,
            runDefinition = content.runDefinition,
            strategy = CompleteRunStrategy(
                name = "Straight gallery coverage",
                description =
                    "Cover two separated gallery sockets and preserve the inner heart.",
                waves = listOf(
                    WaveStrategy(
                        waveId = "opening-recruits",
                        heartRoom = rooms.last(),
                        defensePurchases = rooms.map { room ->
                            purchase(room, content.trap)
                        },
                    ),
                    WaveStrategy(
                        waveId = "reinforcement-recruits",
                        draftedRoom = firstDraft,
                        heartRoom = firstDraft,
                        defensePurchases = listOf(
                            purchase(firstDraft, content.trap),
                        ),
                    ),
                    WaveStrategy(
                        waveId = "final-recruits",
                        draftedRoom = secondDraft,
                        heartRoom = secondDraft,
                        defensePurchases = listOf(
                            purchase(secondDraft, content.trap),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun turnedCoverageFixture(): RunFixture {
        val content = authoredContent()
        val corner = content.blueprints.getValue("corner-room")
        val rooms = listOf(
            PlacedRoom(corner, GridPosition(1, 0)),
            PlacedRoom(
                corner,
                GridPosition(2, 2),
                RoomOrientation.CLOCKWISE_180,
            ),
            PlacedRoom(corner, GridPosition(4, 3)),
            PlacedRoom(
                corner,
                GridPosition(5, 5),
                RoomOrientation.CLOCKWISE_180,
            ),
        )
        val grid = DungeonGrid(
            width = 16,
            height = 9,
            entrance = GridPosition(0, 0),
            placedRooms = rooms,
        )
        val firstDraft = PlacedRoom(
            content.blueprints.getValue("long-gallery"),
            GridPosition(7, 5),
        )
        val secondDraft = PlacedRoom(
            content.blueprints.getValue("prototype-room"),
            GridPosition(11, 5),
        )
        return RunFixture(
            grid = grid,
            waveContentByPath = content.waveContentByPath,
            runDefinition = content.runDefinition,
            strategy = CompleteRunStrategy(
                name = "Turned corner coverage",
                description =
                    "Cover separated turns and move the heart into each drafted extension.",
                waves = listOf(
                    WaveStrategy(
                        waveId = "opening-recruits",
                        heartRoom = rooms.last(),
                        defensePurchases = listOf(rooms[0], rooms[2]).map { room ->
                            purchase(room, content.trap)
                        },
                    ),
                    WaveStrategy(
                        waveId = "reinforcement-recruits",
                        draftedRoom = firstDraft,
                        heartRoom = firstDraft,
                        defensePurchases = listOf(
                            purchase(firstDraft, content.trap),
                        ),
                    ),
                    WaveStrategy(
                        waveId = "final-recruits",
                        draftedRoom = secondDraft,
                        heartRoom = secondDraft,
                        defensePurchases = listOf(
                            purchase(secondDraft, content.trap),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun purchase(
        room: PlacedRoom,
        trap: TrapDefinition,
    ) = DefensePurchase(
        room = room,
        localSocketPosition = room.geometry.sockets.keys.single(),
        definition = trap,
    )

    private fun authoredContent(): AuthoredContent {
        val blueprints = listOf(
            "prototype-room.json",
            "long-gallery.json",
            "corner-room.json",
        ).associate { fileName ->
            val blueprint = RoomBlueprintParser.parse(readContent(fileName))
            blueprint.id to blueprint
        }
        val waveContentByPath = mapOf(
            "content/opening-hero-wave.json" to "opening-hero-wave.json",
            "content/reinforcement-hero-wave.json" to
                "reinforcement-hero-wave.json",
            "content/final-hero-wave.json" to "final-hero-wave.json",
        ).mapValues { (_, fileName) ->
            UpcomingHeroWaveParser.parse(readContent(fileName))
        }
        return AuthoredContent(
            blueprints = blueprints,
            trap = TrapDefinitionParser.parse(readContent("spike-trap.json")),
            waveContentByPath = waveContentByPath,
            runDefinition = PrototypeRunDefinitionParser.parse(
                json = readContent("prototype-run.json"),
                availableWaveContentPaths = waveContentByPath.keys,
                availableRoomBlueprintIds = blueprints.keys,
            ),
        )
    }

    private fun readContent(fileName: String): String {
        val content = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("assets/content/$fileName") }
            .firstOrNull(Files::isRegularFile)
            ?: error("Could not find authored content file '$fileName'.")
        return Files.readString(content)
    }

    private data class RunFixture(
        val grid: DungeonGrid,
        val waveContentByPath: Map<String, UpcomingHeroWave>,
        val runDefinition: PrototypeRunDefinition,
        val strategy: CompleteRunStrategy,
    )

    private data class AuthoredContent(
        val blueprints: Map<String, RoomBlueprint>,
        val trap: TrapDefinition,
        val waveContentByPath: Map<String, UpcomingHeroWave>,
        val runDefinition: PrototypeRunDefinition,
    )
}
