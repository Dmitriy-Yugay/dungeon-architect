package com.dungeonarchitect.application

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.presentation.ControlBounds
import com.dungeonarchitect.presentation.RoomChoicesLayout
import com.dungeonarchitect.presentation.RoomChoicesView
import com.dungeonarchitect.presentation.WavePanelLayout
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PrototypeScreenTest {
    @Test
    fun `prototype grid starts with one authored room`() {
        val room = prototypeGrid().placedRooms.single()

        assertEquals("prototype-room", room.blueprint.id)
        assertEquals("Prototype Room", room.blueprint.displayName)
        assertEquals(GridPosition(column = 6, row = 3), room.origin)
        assertEquals(
            setOf(
                GridPosition(column = 6, row = 3),
                GridPosition(column = 7, row = 3),
                GridPosition(column = 8, row = 3),
                GridPosition(column = 6, row = 4),
                GridPosition(column = 7, row = 4),
                GridPosition(column = 8, row = 4),
                GridPosition(column = 6, row = 5),
                GridPosition(column = 7, row = 5),
                GridPosition(column = 8, row = 5),
            ),
            room.gridPositions,
        )
    }

    @Test
    fun `authored prototype room has one floor trap socket`() {
        val room = prototypeGrid().placedRooms.single()

        assertEquals(
            mapOf(
                GridPosition(column = 1, row = 1) to RoomSocketType.FLOOR,
            ),
            room.blueprint.sockets,
        )
    }

    @Test
    fun `placement preview uses the authored blueprint at the hovered origin`() {
        val buildState = authoredBuildState()
        val grid = prototypeGrid(buildState)

        val preview = PrototypeScreen.placementPreview(
            grid = grid,
            buildState = buildState,
            hoveredPosition = GridPosition(column = 3, row = 3),
        )!!

        assertEquals(GridPosition(column = 3, row = 3), preview.room.origin)
        assertEquals(grid.placedRooms.single().blueprint, preview.room.blueprint)
        assertTrue(preview.isValid)
    }

    @Test
    fun `changing build selection changes placement preview blueprint and geometry`() {
        val buildState = authoredBuildState()
        val grid = prototypeGrid(buildState)
        val hoveredPosition = GridPosition(column = 1, row = 1)

        assertTrue(buildState.selectRoomBlueprint("long-gallery"))
        val preview = PrototypeScreen.placementPreview(
            grid = grid,
            buildState = buildState,
            hoveredPosition = hoveredPosition,
        )!!

        assertSame(buildState.selectedRoomBlueprint, preview.room.blueprint)
        assertEquals("long-gallery", preview.room.blueprint.id)
        assertEquals(
            setOf(
                GridPosition(column = 1, row = 1),
                GridPosition(column = 2, row = 1),
                GridPosition(column = 3, row = 1),
                GridPosition(column = 4, row = 1),
                GridPosition(column = 1, row = 2),
                GridPosition(column = 2, row = 2),
                GridPosition(column = 3, row = 2),
                GridPosition(column = 4, row = 2),
            ),
            preview.room.gridPositions,
        )
    }

    @Test
    fun `placement preview reports invalid candidate without changing placed rooms`() {
        val buildState = authoredBuildState()
        val grid = prototypeGrid(buildState)

        val preview = PrototypeScreen.placementPreview(
            grid = grid,
            buildState = buildState,
            hoveredPosition = GridPosition(column = 6, row = 3),
        )!!

        assertFalse(preview.isValid)
        assertEquals(1, grid.placedRooms.size)
    }

    @Test
    fun `placement preview is absent when the pointer is outside the grid`() {
        val buildState = authoredBuildState()

        assertNull(
            PrototypeScreen.placementPreview(
                grid = prototypeGrid(buildState),
                buildState = buildState,
                hoveredPosition = null,
            ),
        )
    }

    @Test
    fun `room placement is allowed only during the building phase`() {
        listOf(
            PrototypeRunPhase.BUILDING to true,
            PrototypeRunPhase.RUNNING to false,
            PrototypeRunPhase.VICTORY to false,
            PrototypeRunPhase.DEFEAT to false,
        ).forEach { (phase, expectedPlacement) ->
            val buildState = authoredBuildState()
            val grid = prototypeGrid(buildState)
            val roomsBeforeClick = grid.placedRooms

            val wasPlaced = PrototypeScreen.commitPlacement(
                grid = grid,
                buildState = buildState,
                clickedPosition = GridPosition(column = 3, row = 3),
                runPhase = phase,
            )

            assertEquals(expectedPlacement, wasPlaced, phase.name)
            if (expectedPlacement) {
                assertEquals(2, grid.placedRooms.size, phase.name)
                assertSame(
                    buildState.selectedRoomBlueprint,
                    grid.placedRooms.last().blueprint,
                    phase.name,
                )
            } else {
                assertEquals(roomsBeforeClick, grid.placedRooms, phase.name)
            }
        }
    }

    @Test
    fun `commit placement uses the newly selected long gallery`() {
        val buildState = authoredBuildState()
        val grid = prototypeGrid(buildState)
        val clickedPosition = GridPosition(column = 2, row = 3)
        assertTrue(buildState.selectRoomBlueprint("long-gallery"))

        val preview = PrototypeScreen.placementPreview(
            grid = grid,
            buildState = buildState,
            hoveredPosition = clickedPosition,
        )!!
        assertEquals("long-gallery", preview.room.blueprint.id)
        assertTrue(preview.isValid)

        assertTrue(
            PrototypeScreen.commitPlacement(
                grid = grid,
                buildState = buildState,
                clickedPosition = clickedPosition,
                runPhase = PrototypeRunPhase.BUILDING,
            ),
        )
        assertSame(
            buildState.selectedRoomBlueprint,
            grid.placedRooms.last().blueprint,
        )
    }

    @Test
    fun `invalid click leaves prototype rooms unchanged`() {
        val buildState = authoredBuildState()
        val grid = prototypeGrid(buildState)
        assertTrue(buildState.selectRoomBlueprint("long-gallery"))
        val roomsBeforeClick = grid.placedRooms

        assertFalse(
            PrototypeScreen.commitPlacement(
                grid = grid,
                buildState = buildState,
                clickedPosition = GridPosition(column = 6, row = 3),
                runPhase = PrototypeRunPhase.BUILDING,
            ),
        )
        assertEquals(roomsBeforeClick, grid.placedRooms)
    }

    @Test
    fun `application loads the authored wave through the supplied internal text reader`() {
        var requestedPath: String? = null

        val wave = PrototypeScreen.loadUpcomingWave { path ->
            requestedPath = path
            """
                {
                  "heroType": "militia_recruit",
                  "heroDisplayName": "Militia Recruit",
                  "count": 4,
                  "heroHealth": 10,
                  "objectiveDamage": 10,
                  "movementSpeedTilesPerSecond": 2.0,
                  "traitDescription": "A straightforward melee fighter."
                }
            """.trimIndent()
        }

        assertEquals("content/upcoming-hero-wave.json", requestedPath)
        assertEquals("Militia Recruit", wave.heroDisplayName)
    }

    @Test
    fun `application loads the authored room through the supplied internal text reader`() {
        var requestedPath: String? = null

        val blueprint = PrototypeScreen.loadRoomBlueprint { path ->
            requestedPath = path
            authoredRoomJson()
        }

        assertEquals("content/prototype-room.json", requestedPath)
        assertEquals("prototype-room", blueprint.id)
        assertEquals("Prototype Room", blueprint.displayName)
    }

    @Test
    fun `application loads authored room choices and selected trap into build state`() {
        val requestedPaths = mutableListOf<String>()

        val buildState = PrototypeScreen.loadBuildState { path ->
            requestedPaths += path
            authoredContentJson(path)
        }

        assertEquals(
            listOf(
                "content/prototype-room.json",
                "content/long-gallery.json",
                "content/spike-trap.json",
            ),
            requestedPaths,
        )
        assertEquals(
            listOf("prototype-room", "long-gallery"),
            buildState.availableRoomBlueprints.map(RoomBlueprint::id),
        )
        assertEquals("prototype-room", buildState.selectedRoomBlueprint.id)
        assertEquals("spike_trap", buildState.selectedTrapDefinition.id)
        assertEquals("Spike Trap", buildState.selectedTrapDefinition.displayName)
    }

    @Test
    fun `application loads the authored trap through the supplied internal text reader`() {
        var requestedPath: String? = null

        val trap = PrototypeScreen.loadTrapDefinition { path ->
            requestedPath = path
            """
                {
                  "id": "spike_trap",
                  "displayName": "Spike Trap",
                  "damage": 5,
                  "cooldownSeconds": 0.25,
                  "compatibleSocketTypes": ["floor"]
                }
            """.trimIndent()
        }

        assertEquals("content/spike-trap.json", requestedPath)
        assertEquals("spike_trap", trap.id)
        assertTrue(trap.isCompatibleWith(RoomSocketType.FLOOR))
    }

    @Test
    fun `application loads authored run rules through the supplied internal text reader`() {
        var requestedPath: String? = null

        val runDefinition = PrototypeScreen.loadRunDefinition { path ->
            requestedPath = path
            """{ "objectiveHealth": 10 }"""
        }

        assertEquals("content/prototype-run.json", requestedPath)
        assertEquals(10, runDefinition.objectiveHealth)
    }

    @Test
    fun `prototype grid starts without placed traps`() {
        assertEquals(emptyList(), prototypeGrid().placedTraps)
    }

    @Test
    fun `room choice click selects without placing or starting the wave`() {
        val buildState = authoredBuildState()
        val grid = prototypeGrid(buildState)
        val controller = runController(grid)
        val roomsBeforeClick = grid.placedRooms
        val controls = roomChoiceControls(buildState)
        val longGalleryControl = controls.single {
            it.choice.id == "long-gallery"
        }

        val result = PrototypeScreen.handleClick(
            grid = grid,
            buildState = buildState,
            clickedPosition = GridPosition(column = 3, row = 3),
            worldX = longGalleryControl.bounds.x + 1f,
            worldY = longGalleryControl.bounds.y + 1f,
            startButtonBounds = startButtonBounds(),
            roomChoiceControls = controls,
            runController = controller,
        )

        assertEquals(PrototypeClickResult.ROOM_CHOICE_SELECTED, result)
        assertEquals("long-gallery", buildState.selectedRoomBlueprint.id)
        assertEquals(roomsBeforeClick, grid.placedRooms)
        assertEquals(PrototypeRunPhase.BUILDING, controller.phase)
        assertNull(controller.startedWave)
    }

    @Test
    fun `room choice click does not restart a terminal run`() {
        val buildState = authoredBuildState()
        val grid = readyGrid()
        val controller = runController(grid)
        assertTrue(controller.start())
        controller.advance(elapsedSeconds = 1f)
        assertEquals(PrototypeRunPhase.DEFEAT, controller.phase)
        val roomsBeforeClick = grid.placedRooms
        val controls = roomChoiceControls(buildState)
        val longGalleryControl = controls.single {
            it.choice.id == "long-gallery"
        }

        val result = PrototypeScreen.handleClick(
            grid = grid,
            buildState = buildState,
            clickedPosition = GridPosition(column = 1, row = 0),
            worldX = longGalleryControl.bounds.x + 1f,
            worldY = longGalleryControl.bounds.y + 1f,
            startButtonBounds = startButtonBounds(),
            roomChoiceControls = controls,
            runController = controller,
        )

        assertEquals(PrototypeClickResult.ROOM_CHOICE_SELECTED, result)
        assertEquals(PrototypeRunPhase.DEFEAT, controller.phase)
        assertEquals(roomsBeforeClick, grid.placedRooms)
    }

    @Test
    fun `placement click cannot add a room after the wave starts`() {
        val grid = readyGrid()
        val blueprint = grid.placedRooms.single().blueprint
        val buildState = BuildState(
            availableRoomBlueprints = listOf(blueprint),
            selectedRoomBlueprint = blueprint,
            selectedTrapDefinition = trapDefinition(),
        )
        val clickedPosition = GridPosition(column = 1, row = 1)
        assertTrue(
            PrototypeScreen.placementPreview(
                grid = grid,
                buildState = buildState,
                hoveredPosition = clickedPosition,
            )!!.isValid,
        )
        val controller = runController(grid)
        assertTrue(controller.start())
        assertEquals(PrototypeRunPhase.RUNNING, controller.phase)
        val roomsBeforeClick = grid.placedRooms

        val result = PrototypeScreen.handleClick(
            grid = grid,
            buildState = buildState,
            clickedPosition = clickedPosition,
            worldX = 0f,
            worldY = 0f,
            startButtonBounds = startButtonBounds(),
            roomChoiceControls = roomChoiceControls(buildState),
            runController = controller,
        )

        assertEquals(PrototypeClickResult.IGNORED, result)
        assertEquals(roomsBeforeClick, grid.placedRooms)
        assertEquals(PrototypeRunPhase.RUNNING, controller.phase)
    }

    @Test
    fun `start control click wins over room placement and starts a ready wave`() {
        val buildState = authoredBuildState()
        val grid = readyGrid()
        val controller = runController(grid)
        val roomsBeforeClick = grid.placedRooms
        val bounds = ControlBounds(x = 10f, y = 20f, width = 100f, height = 40f)

        val result = PrototypeScreen.handleClick(
            grid = grid,
            buildState = buildState,
            clickedPosition = GridPosition(column = 1, row = 0),
            worldX = 60f,
            worldY = 40f,
            startButtonBounds = bounds,
            roomChoiceControls = roomChoiceControls(buildState),
            runController = controller,
        )

        assertEquals(PrototypeClickResult.WAVE_STARTED, result)
        assertEquals(roomsBeforeClick, grid.placedRooms)
        assertTrue(controller.startedWave != null)
    }

    @Test
    fun `click outside start control remains a placement click`() {
        val buildState = authoredBuildState()
        val grid = prototypeGrid(buildState)
        val controller = runController(grid)
        val clickedPosition = GridPosition(column = 2, row = 3)
        assertTrue(buildState.selectRoomBlueprint("long-gallery"))

        val result = PrototypeScreen.handleClick(
            grid = grid,
            buildState = buildState,
            clickedPosition = clickedPosition,
            worldX = 9f,
            worldY = 40f,
            startButtonBounds = ControlBounds(
                x = 10f,
                y = 20f,
                width = 100f,
                height = 40f,
            ),
            roomChoiceControls = roomChoiceControls(buildState),
            runController = controller,
        )

        assertEquals(PrototypeClickResult.ROOM_PLACED, result)
        assertEquals(clickedPosition, grid.placedRooms.last().origin)
        assertSame(
            buildState.selectedRoomBlueprint,
            grid.placedRooms.last().blueprint,
        )
        assertNull(controller.startedWave)
    }

    @Test
    fun `terminal control click restarts the run`() {
        val buildState = authoredBuildState()
        val grid = readyGrid()
        val controller = runController(grid)
        assertTrue(controller.start())
        controller.advance(elapsedSeconds = 1f)
        assertEquals(PrototypeRunPhase.DEFEAT, controller.phase)

        val result = PrototypeScreen.handleClick(
            grid = grid,
            buildState = buildState,
            clickedPosition = null,
            worldX = 60f,
            worldY = 40f,
            startButtonBounds = ControlBounds(
                x = 10f,
                y = 20f,
                width = 100f,
                height = 40f,
            ),
            roomChoiceControls = roomChoiceControls(buildState),
            runController = controller,
        )

        assertEquals(PrototypeClickResult.RUN_RESTARTED, result)
        assertEquals(PrototypeRunPhase.BUILDING, controller.phase)
    }

    private fun readyGrid() = DungeonGrid(
        width = 3,
        height = 2,
        entrance = GridPosition(column = 0, row = 0),
        objective = GridPosition(column = 2, row = 0),
        placedRooms = listOf(
            PlacedRoom(
                blueprint = RoomBlueprint(
                    id = "test-room",
                    displayName = "Test Room",
                    footprint = setOf(GridPosition(column = 0, row = 0)),
                    doorPositions = setOf(GridPosition(column = 0, row = 0)),
                ),
                origin = GridPosition(column = 1, row = 0),
            ),
        ),
    )

    private fun prototypeGrid() = PrototypeScreen.prototypeGrid(
        authoredBuildState().selectedRoomBlueprint,
    )

    private fun prototypeGrid(buildState: BuildState) =
        PrototypeScreen.prototypeGrid(buildState.selectedRoomBlueprint)

    private fun authoredBuildState() = PrototypeScreen.loadBuildState(
        ::authoredContentJson,
    )

    private fun roomChoiceControls(buildState: BuildState) =
        RoomChoicesLayout.controls(
            view = RoomChoicesView.from(buildState),
            worldWidth = WORLD_WIDTH,
            panelBottom = PANEL_BOTTOM,
        )

    private fun startButtonBounds() = WavePanelLayout.startButtonBounds(
        worldWidth = WORLD_WIDTH,
        panelBottom = PANEL_BOTTOM,
    )

    private fun authoredRoomJson(): String =
        authoredContentJson("content/prototype-room.json")

    private fun authoredContentJson(path: String): String {
        val config = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("assets/$path") }
            .firstOrNull { Files.isRegularFile(it) }
            ?: error("Could not locate authored content '$path'.")

        return Files.readString(config)
    }

    private fun upcomingWave() = UpcomingHeroWave(
        heroType = "militia_recruit",
        heroDisplayName = "Militia Recruit",
        count = 4,
        heroHealth = 10,
        objectiveDamage = 10,
        movementSpeedTilesPerSecond = 2f,
        traitDescription = "A straightforward melee fighter.",
    )

    private fun runController(grid: DungeonGrid) = PrototypeRunController(
        grid = grid,
        upcomingWave = upcomingWave(),
        runDefinition = PrototypeRunDefinition(objectiveHealth = 10),
    )

    private fun trapDefinition() = TrapDefinition(
        id = "spike_trap",
        displayName = "Spike Trap",
        damage = 5,
        cooldownSeconds = 0.25f,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
    )

    private companion object {
        const val WORLD_WIDTH = 1_024f
        const val PANEL_BOTTOM = 576f
    }
}
