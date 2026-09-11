package com.dungeonarchitect.application

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.presentation.ControlBounds
import com.dungeonarchitect.presentation.HeartPlacementControlView
import com.dungeonarchitect.presentation.HeartPlacementLayout
import com.dungeonarchitect.presentation.RoomChoicesLayout
import com.dungeonarchitect.presentation.RoomChoicesView
import com.dungeonarchitect.presentation.WavePanelLayout
import com.dungeonarchitect.presentation.WavePanelView
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import com.dungeonarchitect.simulation.WaveOutcome
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AuthoredRunEndToEndTest {
    @Test
    fun `complete authored run grows and arms the route through the application controls`() {
        val fixture = fixture()

        selectRoom(fixture, "long-gallery")
        placeRoom(fixture, position(1, 4))
        placeRoom(fixture, position(5, 4))
        placeHeart(fixture, position(5, 4))
        placeTrap(fixture, position(3, 4))
        placeTrap(fixture, position(7, 4))
        assertEquals(0, fixture.controller.gold)

        startAndResolveWave(fixture)
        assertWaveReport(fixture, expectedDamage = 64, expectedActivations = 16)
        assertEquals(PrototypeClickResult.REWARD_CLAIMED, clickRunControl(fixture))
        assertEquals(1, fixture.controller.gold)
        assertEquals(
            PrototypeClickResult.WAVE_REPORT_CONTINUED,
            clickRunControl(fixture),
        )

        assertEquals(PrototypeRunPhase.INTELLIGENCE, fixture.controller.phase)
        assertEquals(24, fixture.controller.upcomingWave.heroHealth)
        assertTrue(panelView(fixture).traitDescription.contains("Extend the route"))
        assertEquals(
            PrototypeClickResult.INTELLIGENCE_REVIEWED,
            clickRunControl(fixture),
        )
        selectRoom(fixture, "long-gallery")
        placeRoom(fixture, position(9, 4))
        val reinforcementRoom = fixture.grid.placedRooms.last()
        placeHeart(fixture, position(9, 4))
        assertSame(reinforcementRoom, fixture.grid.placedHeart?.room)
        placeTrap(fixture, position(11, 4))
        assertEquals(0, fixture.controller.gold)

        startAndResolveWave(fixture)
        assertWaveReport(fixture, expectedDamage = 96, expectedActivations = 24)
        assertEquals(PrototypeClickResult.REWARD_CLAIMED, clickRunControl(fixture))
        assertEquals(1, fixture.controller.gold)
        assertEquals(
            PrototypeClickResult.WAVE_REPORT_CONTINUED,
            clickRunControl(fixture),
        )

        assertEquals(PrototypeRunPhase.INTELLIGENCE, fixture.controller.phase)
        assertEquals(32, fixture.controller.upcomingWave.heroHealth)
        assertTrue(panelView(fixture).traitDescription.contains("final room"))
        assertEquals(
            PrototypeClickResult.INTELLIGENCE_REVIEWED,
            clickRunControl(fixture),
        )
        selectRoom(fixture, "prototype-room")
        placeRoom(fixture, position(13, 4))
        val finalRoom = fixture.grid.placedRooms.last()
        placeHeart(fixture, position(13, 4))
        assertSame(finalRoom, fixture.grid.placedHeart?.room)
        placeTrap(fixture, position(14, 4))
        assertEquals(0, fixture.controller.gold)

        startAndResolveWave(fixture)
        assertWaveReport(fixture, expectedDamage = 128, expectedActivations = 32)
        assertEquals(PrototypeClickResult.REWARD_CLAIMED, clickRunControl(fixture))
        assertEquals(
            PrototypeClickResult.WAVE_REPORT_CONTINUED,
            clickRunControl(fixture),
        )

        assertEquals(PrototypeRunPhase.RUN_VICTORY, fixture.controller.phase)
        assertEquals(10, fixture.controller.heartHealth)
        assertEquals(0, fixture.controller.gold)
        assertEquals(4, fixture.grid.placedRooms.size)
        assertEquals(4, fixture.grid.placedTraps.size)
        assertSame(finalRoom, fixture.grid.placedHeart?.room)
    }

    private fun selectRoom(fixture: Fixture, blueprintId: String) {
        val control = roomChoiceControls(fixture).single { it.choice.id == blueprintId }
        assertEquals(
            PrototypeClickResult.ROOM_CHOICE_SELECTED,
            click(fixture, controlBounds = control.bounds),
        )
    }

    private fun placeRoom(fixture: Fixture, position: GridPosition) {
        assertEquals(
            PrototypeClickResult.ROOM_PLACED,
            click(fixture, clickedPosition = position),
        )
    }

    private fun placeHeart(fixture: Fixture, position: GridPosition) {
        assertEquals(
            PrototypeClickResult.HEART_MODE_ACTIVATED,
            click(fixture, controlBounds = heartControl(fixture).bounds),
        )
        assertEquals(
            PrototypeClickResult.HEART_PLACED,
            click(fixture, clickedPosition = position),
        )
    }

    private fun placeTrap(fixture: Fixture, position: GridPosition) {
        assertEquals(
            PrototypeClickResult.TRAP_PLACED,
            click(fixture, clickedPosition = position),
        )
    }

    private fun startAndResolveWave(fixture: Fixture) {
        assertEquals(PrototypeClickResult.WAVE_STARTED, clickRunControl(fixture))
        var steps = 0
        while (fixture.controller.phase == PrototypeRunPhase.COMBAT) {
            fixture.controller.advance(
                FixedStepHeroSimulation.FIXED_STEP_SECONDS.toFloat(),
            )
            steps++
            check(steps < MAX_SIMULATION_STEPS) {
                "Authored wave did not resolve within $MAX_SIMULATION_STEPS steps."
            }
        }
    }

    private fun assertWaveReport(
        fixture: Fixture,
        expectedDamage: Int,
        expectedActivations: Int,
    ) {
        assertEquals(PrototypeRunPhase.WAVE_REPORT, fixture.controller.phase)
        val report = requireNotNull(fixture.controller.evaluationReport)
        assertEquals(WaveOutcome.VICTORY, report.outcome)
        assertEquals(4, report.heroKills)
        assertEquals(0, report.heroArrivals)
        assertEquals(expectedDamage, report.trapDamage)
        assertEquals(expectedActivations, report.trapActivations)
    }

    private fun clickRunControl(fixture: Fixture) =
        click(fixture, controlBounds = startBounds())

    private fun click(
        fixture: Fixture,
        clickedPosition: GridPosition? = null,
        controlBounds: ControlBounds? = null,
    ): PrototypeClickResult {
        val worldX = controlBounds?.let { it.x + it.width / 2f } ?: -1f
        val worldY = controlBounds?.let { it.y + it.height / 2f } ?: -1f
        return PrototypeScreen.handleClick(
            grid = fixture.grid,
            buildState = fixture.buildState,
            clickedPosition = clickedPosition,
            worldX = worldX,
            worldY = worldY,
            startButtonBounds = startBounds(),
            cancelButtonBounds = cancelBounds(),
            roomChoiceControls = roomChoiceControls(fixture),
            heartPlacementControl = heartControl(fixture),
            runController = fixture.controller,
        )
    }

    private fun roomChoiceControls(fixture: Fixture) = RoomChoicesLayout.controls(
        view = RoomChoicesView.forPhase(
            buildState = fixture.buildState,
            phase = fixture.controller.phase,
            offeredBlueprintIds = fixture.controller.offeredRoomBlueprintIds,
        ),
        worldWidth = WORLD_WIDTH,
        panelBottom = PANEL_BOTTOM,
    )

    private fun heartControl(fixture: Fixture) = HeartPlacementLayout.control(
        view = HeartPlacementControlView.from(
            fixture.buildState,
            fixture.controller.phase,
        ),
        worldWidth = WORLD_WIDTH,
        panelBottom = PANEL_BOTTOM,
    )

    private fun panelView(fixture: Fixture) = WavePanelView.from(
        wave = fixture.controller.upcomingWave,
        phase = fixture.controller.phase,
        heartHealth = fixture.controller.heartHealth,
        heartMaxHealth = fixture.controller.heartMaxHealth,
        gold = fixture.controller.gold,
        defenseName = fixture.buildState.selectedTrapDefinition.displayName,
        defenseCostGold = fixture.buildState.selectedTrapDefinition.costGold,
        upcomingRewardGold = fixture.controller.currentWaveDefinition.rewardGold,
        waveOutcome = fixture.controller.evaluationReport?.outcome,
        isWaveRewardClaimed = fixture.controller.isWaveRewardClaimed,
        isStartEnabled = fixture.controller.isStartEnabled,
        isCancelEnabled = fixture.controller.isCancelEnabled,
    )

    private fun fixture(): Fixture {
        val buildState = PrototypeScreen.loadBuildState(::readInternalText)
        val grid = DungeonGrid(
            width = 16,
            height = 9,
            entrance = position(0, 4),
            entranceFacing = CardinalDirection.EAST,
        )
        return Fixture(
            grid = grid,
            buildState = buildState,
            controller = PrototypeRunController(
                grid = grid,
                waveContentByPath = PrototypeScreen.loadWaveContent(::readInternalText),
                runDefinition = PrototypeScreen.loadRunDefinition(::readInternalText),
            ),
        )
    }

    private fun startBounds() = WavePanelLayout.startButtonBounds(
        worldWidth = WORLD_WIDTH,
        panelBottom = PANEL_BOTTOM,
    )

    private fun cancelBounds() = WavePanelLayout.cancelButtonBounds(
        worldWidth = WORLD_WIDTH,
        panelBottom = PANEL_BOTTOM,
    )

    private fun readInternalText(path: String): String {
        val content = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { directory -> directory.resolve("assets/$path") }
            .firstOrNull(Files::isRegularFile)
            ?: error("Could not locate authored content '$path'.")
        return Files.readString(content)
    }

    private fun position(column: Int, row: Int) = GridPosition(column, row)

    private data class Fixture(
        val grid: DungeonGrid,
        val buildState: BuildState,
        val controller: PrototypeRunController,
    )

    private companion object {
        const val WORLD_WIDTH = 1_024f
        const val PANEL_BOTTOM = 768f
        const val MAX_SIMULATION_STEPS = 10_000
    }
}
