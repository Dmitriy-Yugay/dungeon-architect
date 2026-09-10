package com.dungeonarchitect.application

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomOrientation
import com.dungeonarchitect.presentation.ControlBounds
import com.dungeonarchitect.presentation.HeartPlacementControlView
import com.dungeonarchitect.presentation.HeartPlacementLayout
import com.dungeonarchitect.presentation.RoomChoicesLayout
import com.dungeonarchitect.presentation.RoomChoicesView
import com.dungeonarchitect.presentation.RoomRotationDirection
import com.dungeonarchitect.presentation.RoomRotationLayout
import com.dungeonarchitect.presentation.RoomRotationView
import com.dungeonarchitect.presentation.WavePanelLayout
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import com.dungeonarchitect.simulation.HeartDamaged
import com.dungeonarchitect.simulation.HeroArrived
import com.dungeonarchitect.simulation.HeroDamaged
import com.dungeonarchitect.simulation.HeroDied
import com.dungeonarchitect.simulation.TrapActivated
import com.dungeonarchitect.simulation.WaveOutcome
import com.dungeonarchitect.simulation.WaveResolved
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FlexibleDungeonEndToEndTest {
    @Test
    fun `turned dungeon can be corrected armed and defended to its chosen heart`() {
        val fixture = fixture()

        assertEquals(
            PrototypeClickResult.ROOM_CHOICE_SELECTED,
            clickRoomChoice(fixture, "long-gallery"),
        )
        assertEquals(
            PrototypeClickResult.ROOM_PLACED,
            clickGrid(fixture, position(1, 4)),
        )
        val firstRoom = fixture.grid.placedRooms.single()

        assertEquals(
            PrototypeClickResult.ROOM_PLACED,
            clickGrid(fixture, position(5, 4)),
        )
        val mistakenRoom = fixture.grid.placedRooms.last()
        assertEquals(
            PrototypeClickResult.TRAP_PLACED,
            clickGrid(fixture, position(7, 4)),
        )
        assertSame(mistakenRoom, fixture.grid.placedTraps.single().room)

        assertEquals(PrototypeClickResult.ROOM_CANCELED, clickCancel(fixture))
        assertEquals(listOf(firstRoom), fixture.grid.placedRooms)
        assertEquals(emptyList(), fixture.grid.placedTraps)
        assertFalse(fixture.grid.placedRooms.any { room -> room === mistakenRoom })

        assertEquals(
            PrototypeClickResult.ROOM_CHOICE_SELECTED,
            clickRoomChoice(fixture, "corner-room"),
        )
        assertEquals(
            PrototypeClickResult.ROOM_PLACED,
            clickGrid(fixture, position(5, 4)),
        )
        assertEquals(
            PrototypeClickResult.ROOM_CHOICE_SELECTED,
            clickRoomChoice(fixture, "prototype-room"),
        )
        assertEquals(
            PrototypeClickResult.ROOM_ROTATED,
            clickRotation(fixture, RoomRotationDirection.COUNTER_CLOCKWISE),
        )
        assertEquals(
            PrototypeClickResult.ROOM_PLACED,
            clickGrid(fixture, position(6, 6)),
        )
        val heartRoom = fixture.grid.placedRooms.last()
        assertEquals(RoomOrientation.CLOCKWISE_270, heartRoom.orientation)
        assertEquals(
            listOf("long-gallery", "corner-room", "prototype-room"),
            fixture.grid.placedRooms.map { room -> room.blueprint.id },
        )

        assertEquals(
            PrototypeClickResult.HEART_MODE_ACTIVATED,
            clickHeartControl(fixture),
        )
        assertEquals(
            PrototypeClickResult.HEART_PLACED,
            clickGrid(fixture, position(6, 6)),
        )
        val selectedHeart = requireNotNull(fixture.grid.placedHeart)
        assertSame(heartRoom, selectedHeart.room)
        assertEquals(position(5, 7), selectedHeart.gridPosition)

        assertEquals(
            PrototypeClickResult.TRAP_PLACED,
            clickGrid(fixture, position(3, 4)),
        )
        val cornerRoom = fixture.grid.placedRooms[1]
        val cornerSocket = cornerRoom.geometry.sockets.keys.single()
        val cornerTrapPosition = cornerRoom.toGridPosition(cornerSocket)
        assertEquals(
            PrototypeClickResult.TRAP_PLACED,
            clickGrid(fixture, cornerTrapPosition),
        )
        val selectedTraps = fixture.grid.placedTraps
        assertEquals(2, selectedTraps.size)
        assertSame(firstRoom, selectedTraps.first().room)
        assertSame(cornerRoom, selectedTraps.last().room)

        assertEquals(PrototypeClickResult.WAVE_STARTED, clickRunControl(fixture))
        val startedWave = requireNotNull(fixture.controller.startedWave)
        assertEquals(fixture.grid.entrance, startedWave.route.first())
        assertEquals(selectedHeart.gridPosition, startedWave.route.last())
        assertTrue(selectedTraps.all { trap -> trap.gridPosition in startedWave.route })
        assertTrue(startedWave.route.zipWithNext().any { (first, second) ->
            first.row == second.row && first.column != second.column
        })
        assertTrue(startedWave.route.zipWithNext().any { (first, second) ->
            first.column == second.column && first.row != second.row
        })
        assertEquals(selectedTraps, startedWave.traps)

        completeWave(fixture)

        assertEquals(PrototypeRunPhase.RUN_VICTORY, fixture.controller.phase)
        assertEquals(10, fixture.controller.heartHealth)
        assertEquals(
            WaveResolved(WaveOutcome.VICTORY, heartHealth = 10),
            fixture.controller.events.last(),
        )
        assertEquals(4, fixture.controller.events.count { it is HeroDied })
        assertEquals(0, fixture.controller.events.count { it is HeroArrived })
        assertEquals(0, fixture.controller.events.count { it is HeartDamaged })
        assertEquals(12, fixture.controller.events.count { it is TrapActivated })
        assertEquals(
            40,
            fixture.controller.events
                .filterIsInstance<HeroDamaged>()
                .sumOf(HeroDamaged::damage),
        )

        val persistentRooms = fixture.grid.placedRooms
        val persistentTraps = fixture.grid.placedTraps
        assertEquals(PrototypeClickResult.RUN_RESTARTED, clickRunControl(fixture))
        assertEquals(PrototypeRunPhase.DEFENSE_PREPARATION, fixture.controller.phase)
        assertEquals(persistentRooms, fixture.grid.placedRooms)
        assertEquals(persistentTraps, fixture.grid.placedTraps)
        assertSame(selectedHeart, fixture.grid.placedHeart)
        assertTrue(fixture.controller.isStartEnabled)
    }

    private fun completeWave(fixture: Fixture) {
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

    private fun clickGrid(
        fixture: Fixture,
        position: GridPosition,
    ): PrototypeClickResult = click(fixture, clickedPosition = position)

    private fun clickCancel(fixture: Fixture): PrototypeClickResult = click(
        fixture,
        controlBounds = cancelBounds(),
    )

    private fun clickRunControl(fixture: Fixture): PrototypeClickResult = click(
        fixture,
        controlBounds = startBounds(),
    )

    private fun clickHeartControl(fixture: Fixture): PrototypeClickResult = click(
        fixture,
        controlBounds = heartControl(fixture).bounds,
    )

    private fun clickRoomChoice(
        fixture: Fixture,
        blueprintId: String,
    ): PrototypeClickResult {
        val control = roomChoiceControls(fixture.buildState).single { choice ->
            choice.choice.id == blueprintId
        }
        return click(fixture, controlBounds = control.bounds)
    }

    private fun clickRotation(
        fixture: Fixture,
        direction: RoomRotationDirection,
    ): PrototypeClickResult {
        val choicesView = RoomChoicesView.from(fixture.buildState)
        val control = RoomRotationLayout.controls(
            rotationView = RoomRotationView.from(
                fixture.buildState,
                fixture.controller.phase,
            ),
            roomChoicesView = choicesView,
            worldWidth = WORLD_WIDTH,
            panelBottom = PANEL_BOTTOM,
        ).single { rotation -> rotation.view.direction == direction }
        return click(fixture, controlBounds = control.bounds)
    }

    private fun click(
        fixture: Fixture,
        clickedPosition: GridPosition? = null,
        controlBounds: ControlBounds? = null,
    ): PrototypeClickResult {
        val worldX = controlBounds?.let { bounds -> bounds.x + bounds.width / 2f }
            ?: GRID_CLICK_WORLD_COORDINATE
        val worldY = controlBounds?.let { bounds -> bounds.y + bounds.height / 2f }
            ?: GRID_CLICK_WORLD_COORDINATE
        val choicesView = RoomChoicesView.from(fixture.buildState)
        return PrototypeScreen.handleClick(
            grid = fixture.grid,
            buildState = fixture.buildState,
            clickedPosition = clickedPosition,
            worldX = worldX,
            worldY = worldY,
            startButtonBounds = startBounds(),
            cancelButtonBounds = cancelBounds(),
            roomChoiceControls = RoomChoicesLayout.controls(
                view = choicesView,
                worldWidth = WORLD_WIDTH,
                panelBottom = PANEL_BOTTOM,
            ),
            roomRotationControls = RoomRotationLayout.controls(
                rotationView = RoomRotationView.from(
                    fixture.buildState,
                    fixture.controller.phase,
                ),
                roomChoicesView = choicesView,
                worldWidth = WORLD_WIDTH,
                panelBottom = PANEL_BOTTOM,
            ),
            heartPlacementControl = heartControl(fixture),
            runController = fixture.controller,
        )
    }

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
                upcomingWave = PrototypeScreen.loadUpcomingWave(::readInternalText),
                runDefinition = PrototypeScreen.loadRunDefinition(::readInternalText),
            ),
        )
    }

    private fun heartControl(fixture: Fixture) = HeartPlacementLayout.control(
        view = HeartPlacementControlView.from(
            fixture.buildState,
            fixture.controller.phase,
        ),
        worldWidth = WORLD_WIDTH,
        panelBottom = PANEL_BOTTOM,
    )

    private fun roomChoiceControls(buildState: BuildState) =
        RoomChoicesLayout.controls(
            view = RoomChoicesView.from(buildState),
            worldWidth = WORLD_WIDTH,
            panelBottom = PANEL_BOTTOM,
        )

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
        const val GRID_CLICK_WORLD_COORDINATE = -1f
        const val MAX_SIMULATION_STEPS = 10_000
    }
}
