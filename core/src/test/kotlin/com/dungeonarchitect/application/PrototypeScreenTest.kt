package com.dungeonarchitect.application

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomDoor
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
    fun `prototype grid starts with an empty player-built layout`() {
        val grid = prototypeGrid()

        assertEquals(emptyList(), grid.placedRooms)
        assertEquals(emptySet(), grid.walkablePositions)
        assertNull(grid.entranceToHeartRoute)
    }

    @Test
    fun `authored prototype room has one floor trap socket`() {
        val blueprint = authoredBuildState().selectedRoomBlueprint

        assertEquals(
            mapOf(
                GridPosition(column = 1, row = 1) to RoomSocketType.FLOOR,
            ),
            blueprint.sockets,
        )
    }

    @Test
    fun `placement preview uses the authored blueprint at the hovered origin`() {
        val buildState = authoredBuildState()
        val grid = prototypeGrid()

        val preview = PrototypeScreen.placementPreview(
            grid = grid,
            buildState = buildState,
            hoveredPosition = GridPosition(column = 1, row = 4),
        )!!

        assertEquals(GridPosition(column = 1, row = 3), preview.room.origin)
        assertSame(buildState.selectedRoomBlueprint, preview.room.blueprint)
        assertTrue(preview.isValid)
    }

    @Test
    fun `changing build selection changes placement preview blueprint and geometry`() {
        val buildState = authoredBuildState()
        val grid = prototypeGrid()
        val hoveredPosition = GridPosition(column = 1, row = 4)

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
                GridPosition(column = 1, row = 3),
                GridPosition(column = 2, row = 3),
                GridPosition(column = 3, row = 3),
                GridPosition(column = 4, row = 3),
                GridPosition(column = 1, row = 4),
                GridPosition(column = 2, row = 4),
                GridPosition(column = 3, row = 4),
                GridPosition(column = 4, row = 4),
            ),
            preview.room.gridPositions,
        )
    }

    @Test
    fun `placement preview is absent away from an open door`() {
        val buildState = authoredBuildState()
        val grid = prototypeGrid()

        assertNull(
            PrototypeScreen.placementPreview(
                grid = grid,
                buildState = buildState,
                hoveredPosition = GridPosition(column = 10, row = 8),
            ),
        )
        assertEquals(0, grid.placedRooms.size)
    }

    @Test
    fun `placement preview is absent when the pointer is outside the grid`() {
        val buildState = authoredBuildState()

        assertNull(
            PrototypeScreen.placementPreview(
                grid = prototypeGrid(),
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
            val grid = prototypeGrid()
            val roomsBeforeClick = grid.placedRooms

            val wasPlaced = PrototypeScreen.commitPlacement(
                grid = grid,
                buildState = buildState,
                clickedPosition = GridPosition(column = 1, row = 4),
                runPhase = phase,
            )

            assertEquals(expectedPlacement, wasPlaced, phase.name)
            if (expectedPlacement) {
                assertEquals(1, grid.placedRooms.size, phase.name)
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
        val grid = prototypeGrid()
        val clickedPosition = GridPosition(column = 1, row = 4)
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
        val grid = prototypeGrid()
        assertTrue(buildState.selectRoomBlueprint("long-gallery"))
        val roomsBeforeClick = grid.placedRooms

        assertFalse(
            PrototypeScreen.commitPlacement(
                grid = grid,
                buildState = buildState,
                clickedPosition = GridPosition(column = 10, row = 8),
                runPhase = PrototypeRunPhase.BUILDING,
            ),
        )
        assertEquals(roomsBeforeClick, grid.placedRooms)
    }

    @Test
    fun `attachment click selects a stable target and ghost click places there`() {
        val buildState = authoredBuildState()
        val grid = prototypeGrid()
        val controller = runController(grid)

        val selected = PrototypeScreen.handleClick(
            grid = grid,
            buildState = buildState,
            clickedPosition = grid.entrance,
            clickedAttachmentTarget = grid.roomAttachmentTargets.single(),
            worldX = 100f,
            worldY = 100f,
            startButtonBounds = startButtonBounds(),
            cancelButtonBounds = cancelButtonBounds(),
            roomChoiceControls = roomChoiceControls(buildState),
            runController = controller,
        )
        val target = buildState.selectedRoomAttachmentTarget

        assertEquals(PrototypeClickResult.ROOM_ATTACHMENT_SELECTED, selected)
        assertEquals(grid.roomAttachmentTargets.single(), target)
        val preview = requireNotNull(
            grid.targetedPlacementPreview(
                blueprint = buildState.selectedRoomBlueprint,
                target = requireNotNull(target),
                orientation = buildState.selectedRoomOrientation,
            ),
        )
        val clickedGhostCell = preview.room.gridPositions.first()
        val placed = PrototypeScreen.handleClick(
            grid = grid,
            buildState = buildState,
            clickedPosition = clickedGhostCell,
            worldX = 100f,
            worldY = 100f,
            startButtonBounds = startButtonBounds(),
            cancelButtonBounds = cancelButtonBounds(),
            roomChoiceControls = roomChoiceControls(buildState),
            runController = controller,
        )

        assertEquals(PrototypeClickResult.ROOM_PLACED, placed)
        assertEquals(preview.room, grid.placedRooms.single())
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
                  "heartDamage": 10,
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
                "content/corner-room.json",
                "content/spike-trap.json",
            ),
            requestedPaths,
        )
        assertEquals(
            listOf("prototype-room", "long-gallery", "corner-room"),
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
            """{ "heartHealth": 10 }"""
        }

        assertEquals("content/prototype-run.json", requestedPath)
        assertEquals(10, runDefinition.heartHealth)
    }

    @Test
    fun `prototype grid starts without placed traps`() {
        assertEquals(emptyList(), prototypeGrid().placedTraps)
    }

    @Test
    fun `room choice click selects without placing or starting the wave`() {
        val buildState = authoredBuildState()
        val grid = prototypeGrid()
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
            cancelButtonBounds = cancelButtonBounds(),
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
            cancelButtonBounds = cancelButtonBounds(),
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
            cancelButtonBounds = cancelButtonBounds(),
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
            cancelButtonBounds = cancelButtonBounds(),
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
        val grid = prototypeGrid()
        val controller = runController(grid)
        val clickedPosition = GridPosition(column = 1, row = 4)
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
            cancelButtonBounds = cancelButtonBounds(),
            roomChoiceControls = roomChoiceControls(buildState),
            runController = controller,
        )

        assertEquals(PrototypeClickResult.ROOM_PLACED, result)
        assertEquals(GridPosition(column = 1, row = 3), grid.placedRooms.last().origin)
        assertSame(
            buildState.selectedRoomBlueprint,
            grid.placedRooms.last().blueprint,
        )
        assertNull(controller.startedWave)
    }

    @Test
    fun `cancel control consumes repeated clicks without another action`() {
        val buildState = authoredBuildState()
        val grid = cancellableGrid()
        val controller = runController(grid)
        val bounds = ControlBounds(x = 10f, y = 20f, width = 100f, height = 40f)
        val overlappingChoice = roomChoiceControls(buildState)
            .single { control -> control.choice.id == "long-gallery" }
            .copy(bounds = bounds)

        fun clickCancel() = PrototypeScreen.handleClick(
            grid = grid,
            buildState = buildState,
            clickedPosition = GridPosition(column = 2, row = 0),
            worldX = 60f,
            worldY = 40f,
            startButtonBounds = bounds,
            cancelButtonBounds = bounds,
            roomChoiceControls = listOf(overlappingChoice),
            runController = controller,
        )

        assertTrue(controller.isCancelEnabled)
        assertEquals(PrototypeClickResult.ROOM_CANCELED, clickCancel())
        assertEquals(1, grid.placedRooms.size)
        assertEquals("prototype-room", buildState.selectedRoomBlueprint.id)
        assertNull(controller.startedWave)

        assertEquals(PrototypeClickResult.ROOM_CANCELED, clickCancel())
        assertEquals(emptyList(), grid.placedRooms)
        assertFalse(controller.isCancelEnabled)

        assertEquals(PrototypeClickResult.CANCEL_REJECTED, clickCancel())
        assertEquals(emptyList(), grid.placedRooms)
        assertEquals("prototype-room", buildState.selectedRoomBlueprint.id)
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
            cancelButtonBounds = cancelButtonBounds(),
            roomChoiceControls = roomChoiceControls(buildState),
            runController = controller,
        )

        assertEquals(PrototypeClickResult.RUN_RESTARTED, result)
        assertEquals(PrototypeRunPhase.BUILDING, controller.phase)
    }

    private fun readyGrid(): DungeonGrid {
        val room = PlacedRoom(
            blueprint = RoomBlueprint(
                id = "test-room",
                displayName = "Test Room",
                heartAnchor = GridPosition(column = 0, row = 0),
                footprint = setOf(GridPosition(column = 0, row = 0)),
                doors = CardinalDirection.entries.map { facing ->
                    RoomDoor(
                        position = GridPosition(column = 0, row = 0),
                        facing = facing,
                    )
                },
            ),
            origin = GridPosition(column = 1, row = 0),
        )
        return DungeonGrid(
            width = 3,
            height = 2,
            entrance = GridPosition(column = 0, row = 0),
            placedRooms = listOf(room),
        ).also { grid ->
            assertTrue(grid.placeOrRelocateHeart(room))
        }
    }

    private fun cancellableGrid(): DungeonGrid {
        val blueprint = RoomBlueprint(
            id = "cancel-room",
            displayName = "Cancel Room",
            heartAnchor = GridPosition(column = 0, row = 0),
            footprint = setOf(GridPosition(column = 0, row = 0)),
            doors = listOf(
                RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                RoomDoor(GridPosition(0, 0), CardinalDirection.EAST),
            ),
        )
        return DungeonGrid(
            width = 4,
            height = 1,
            entrance = GridPosition(column = 0, row = 0),
            placedRooms = listOf(
                PlacedRoom(blueprint, GridPosition(column = 1, row = 0)),
                PlacedRoom(blueprint, GridPosition(column = 2, row = 0)),
            ),
        )
    }

    private fun prototypeGrid() = PrototypeScreen.prototypeGrid()

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

    private fun cancelButtonBounds() = WavePanelLayout.cancelButtonBounds(
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
        heartDamage = 10,
        movementSpeedTilesPerSecond = 2f,
        traitDescription = "A straightforward melee fighter.",
    )

    private fun runController(grid: DungeonGrid) = PrototypeRunController(
        grid = grid,
        upcomingWave = upcomingWave(),
        runDefinition = PrototypeRunDefinition(heartHealth = 10),
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
