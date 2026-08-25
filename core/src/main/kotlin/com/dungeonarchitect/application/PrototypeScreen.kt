package com.dungeonarchitect.application

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.dungeonarchitect.content.PrototypeRunDefinitionParser
import com.dungeonarchitect.content.RoomBlueprintParser
import com.dungeonarchitect.content.TrapDefinitionParser
import com.dungeonarchitect.content.UpcomingHeroWaveParser
import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomAttachmentTarget
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomPlacementPreview
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.TrapSocketHoverResult
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.presentation.ControlBounds
import com.dungeonarchitect.presentation.DungeonGridRenderer
import com.dungeonarchitect.presentation.HeartPlacementControl
import com.dungeonarchitect.presentation.HeartPlacementControlRenderer
import com.dungeonarchitect.presentation.HeartPlacementControlView
import com.dungeonarchitect.presentation.HeartPlacementLayout
import com.dungeonarchitect.presentation.RoomChoiceControl
import com.dungeonarchitect.presentation.RoomChoicesLayout
import com.dungeonarchitect.presentation.RoomChoicesRenderer
import com.dungeonarchitect.presentation.RoomChoicesView
import com.dungeonarchitect.presentation.RoomRotationControl
import com.dungeonarchitect.presentation.RoomRotationDirection
import com.dungeonarchitect.presentation.RoomRotationLayout
import com.dungeonarchitect.presentation.RoomRotationRenderer
import com.dungeonarchitect.presentation.RoomRotationView
import com.dungeonarchitect.presentation.WavePanelLayout
import com.dungeonarchitect.presentation.WavePanelRenderer
import com.dungeonarchitect.presentation.WavePanelView
import com.dungeonarchitect.presentation.buildModeGuidance
import com.dungeonarchitect.presentation.roomAttachmentTargetMarkers

class PrototypeScreen(
    private val buildState: BuildState = loadBuildState { path ->
        Gdx.files.internal(path).readString("UTF-8")
    },
    private val grid: DungeonGrid = prototypeGrid(),
    private val upcomingWave: UpcomingHeroWave = loadUpcomingWave { path ->
        Gdx.files.internal(path).readString("UTF-8")
    },
    runDefinition: PrototypeRunDefinition = loadRunDefinition { path ->
        Gdx.files.internal(path).readString("UTF-8")
    },
) : ScreenAdapter() {
    private val camera = OrthographicCamera()
    private val gridRenderer = DungeonGridRenderer()
    private val roomChoicesRenderer = RoomChoicesRenderer()
    private val roomRotationRenderer = RoomRotationRenderer()
    private val heartPlacementControlRenderer = HeartPlacementControlRenderer()
    private val wavePanelRenderer = WavePanelRenderer()
    private val worldWidth = gridRenderer.worldWidth(grid)
    private val gridWorldHeight = gridRenderer.worldHeight(grid)
    private val viewport = FitViewport(
        worldWidth,
        gridWorldHeight + WavePanelLayout.HEIGHT,
        camera,
    )
    private val pointerCoordinates = Vector2()
    private val runController = PrototypeRunController(
        grid = grid,
        upcomingWave = upcomingWave,
        runDefinition = runDefinition,
    )

    private var hoveredPosition: GridPosition? = null
    private var selectedPosition: GridPosition? = null

    override fun render(delta: Float) {
        updatePointerState()
        runController.advance(delta)
        ScreenUtils.clear(BACKGROUND_RED, BACKGROUND_GREEN, BACKGROUND_BLUE, BACKGROUND_ALPHA)
        val attachmentTargets = if (runController.phase == PrototypeRunPhase.BUILDING) {
            grid.roomAttachmentTargets
        } else {
            emptyList()
        }
        val activeAttachmentTarget = buildState.retainOrSelectRoomAttachmentTarget(
            attachmentTargets,
        )
        val placementPreviews = buildPlacementPreviews(
            grid = grid,
            buildState = buildState,
            hoveredPosition = hoveredPosition,
            runPhase = runController.phase,
            attachmentTarget = activeAttachmentTarget,
        )
        gridRenderer.render(
            grid = grid,
            projection = camera.combined,
            placementPreview = placementPreviews.room,
            trapPlacementPreview = placementPreviews.trap,
            heartPlacementPreview = placementPreviews.heart,
            heroState = runController.heroState,
            hoveredPosition = hoveredPosition,
            selectedPosition = selectedPosition,
            roomAttachmentTargets = roomAttachmentTargetMarkers(
                grid = grid,
                activeTarget = activeAttachmentTarget,
            ).takeIf { runController.phase == PrototypeRunPhase.BUILDING }
                ?: emptyList(),
        )
        val roomChoicesView = RoomChoicesView.from(buildState)
        val roomRotationView = RoomRotationView.from(buildState, runController.phase)
        val heartPlacementView = HeartPlacementControlView.from(
            buildState,
            runController.phase,
        )
        val panelLayout = WavePanelLayout.create(
            worldWidth = worldWidth,
            panelBottom = gridWorldHeight,
            roomChoiceCount = roomChoicesView.choices.size,
            roomRotationControlCount = roomRotationView.controls.size,
        )
        wavePanelRenderer.render(
            view = WavePanelView.from(
                wave = upcomingWave,
                phase = runController.phase,
                heartHealth = runController.heartHealth,
                heartMaxHealth = runController.heartMaxHealth,
                isStartEnabled = runController.isStartEnabled,
                isCancelEnabled = runController.isCancelEnabled,
                buildGuidance = buildModeGuidance(
                    buildState = buildState,
                    phase = runController.phase,
                    roomPreview = placementPreviews.room,
                    hasTrapPreview = placementPreviews.trap != null,
                    activeAttachmentTarget = activeAttachmentTarget,
                ),
            ),
            projection = camera.combined,
            layout = panelLayout,
        )
        roomChoicesRenderer.render(
            view = roomChoicesView,
            projection = camera.combined,
            layout = panelLayout,
        )
        roomRotationRenderer.render(
            rotationView = roomRotationView,
            projection = camera.combined,
            layout = panelLayout,
        )
        heartPlacementControlRenderer.render(
            view = heartPlacementView,
            projection = camera.combined,
            layout = panelLayout,
        )
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        heartPlacementControlRenderer.dispose()
        roomRotationRenderer.dispose()
        roomChoicesRenderer.dispose()
        wavePanelRenderer.dispose()
        gridRenderer.dispose()
    }

    private fun updatePointerState() {
        updateKeyboardShortcuts()
        pointerCoordinates.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        viewport.unproject(pointerCoordinates)

        hoveredPosition = gridRenderer.gridPositionAt(
            grid = grid,
            worldX = pointerCoordinates.x,
            worldY = pointerCoordinates.y,
        )

        if (Gdx.input.justTouched()) {
            buildState.retainOrSelectRoomAttachmentTarget(grid.roomAttachmentTargets)
            val roomChoicesView = RoomChoicesView.from(buildState)
            val roomRotationView = RoomRotationView.from(
                buildState,
                runController.phase,
            )
            val heartPlacementView = HeartPlacementControlView.from(
                buildState,
                runController.phase,
            )
            val panelLayout = WavePanelLayout.create(
                worldWidth = worldWidth,
                panelBottom = gridWorldHeight,
                roomChoiceCount = roomChoicesView.choices.size,
                roomRotationControlCount = roomRotationView.controls.size,
            )
            val result = handleClick(
                grid = grid,
                buildState = buildState,
                clickedPosition = hoveredPosition,
                clickedAttachmentTarget = gridRenderer.roomAttachmentTargetAt(
                    grid = grid,
                    worldX = pointerCoordinates.x,
                    worldY = pointerCoordinates.y,
                ),
                worldX = pointerCoordinates.x,
                worldY = pointerCoordinates.y,
                startButtonBounds = panelLayout.startBounds,
                cancelButtonBounds = panelLayout.cancelBounds,
                heartPlacementControl = HeartPlacementLayout.control(
                    view = heartPlacementView,
                    layout = panelLayout,
                ),
                roomChoiceControls = RoomChoicesLayout.controls(
                    view = roomChoicesView,
                    layout = panelLayout,
                ),
                roomRotationControls = RoomRotationLayout.controls(
                    rotationView = roomRotationView,
                    layout = panelLayout,
                ),
                runController = runController,
            )
            if (result == PrototypeClickResult.ROOM_PLACED ||
                result == PrototypeClickResult.HEART_PLACED ||
                result == PrototypeClickResult.IGNORED
            ) {
                selectedPosition = hoveredPosition
            }
        }
    }

    private fun updateKeyboardShortcuts() {
        when {
            Gdx.input.isKeyJustPressed(Input.Keys.Q) ->
                applyBuildShortcut(
                    buildState,
                    BuildShortcut.ROTATE_COUNTER_CLOCKWISE,
                    runController.phase,
                )
            Gdx.input.isKeyJustPressed(Input.Keys.E) ->
                applyBuildShortcut(
                    buildState,
                    BuildShortcut.ROTATE_CLOCKWISE,
                    runController.phase,
                )
        }
    }

    companion object {
        private const val UPCOMING_WAVE_PATH = "content/upcoming-hero-wave.json"
        private const val TRAP_DEFINITION_PATH = "content/spike-trap.json"
        private const val RUN_DEFINITION_PATH = "content/prototype-run.json"
        private const val ROOM_BLUEPRINT_PATH = "content/prototype-room.json"
        private const val LONG_GALLERY_PATH = "content/long-gallery.json"
        private const val CORNER_ROOM_PATH = "content/corner-room.json"
        private val ROOM_BLUEPRINT_PATHS = listOf(
            ROOM_BLUEPRINT_PATH,
            LONG_GALLERY_PATH,
            CORNER_ROOM_PATH,
        )

        private const val BACKGROUND_RED = 0.04f
        private const val BACKGROUND_GREEN = 0.05f
        private const val BACKGROUND_BLUE = 0.07f
        private const val BACKGROUND_ALPHA = 1f

        internal fun prototypeGrid() = DungeonGrid(
            width = 16,
            height = 9,
            entrance = GridPosition(column = 0, row = 4),
            entranceFacing = CardinalDirection.EAST,
        )

        internal fun placementPreview(
            grid: DungeonGrid,
            buildState: BuildState,
            hoveredPosition: GridPosition?,
        ): RoomPlacementPreview? =
            hoveredPosition?.let { position ->
                grid.snappedPlacementPreview(
                    blueprint = buildState.selectedRoomBlueprint,
                    hoveredPosition = position,
                    orientation = buildState.selectedRoomOrientation,
                )
            }

        internal fun commitPlacement(
            grid: DungeonGrid,
            buildState: BuildState,
            clickedPosition: GridPosition?,
            runPhase: PrototypeRunPhase,
        ): Boolean {
            if (runPhase != PrototypeRunPhase.BUILDING) {
                return false
            }

            val target = buildState.selectedRoomAttachmentTarget
            val preview = if (target != null) {
                grid.targetedPlacementPreview(
                    blueprint = buildState.selectedRoomBlueprint,
                    target = target,
                    orientation = buildState.selectedRoomOrientation,
                )?.takeIf { candidate ->
                    clickedPosition in candidate.room.gridPositions
                }
            } else {
                placementPreview(grid, buildState, clickedPosition)
            }
            return preview
                ?.let { preview -> grid.place(preview.room) }
                ?: false
        }

        internal fun commitTrapPlacement(
            grid: DungeonGrid,
            buildState: BuildState,
            clickedPosition: GridPosition?,
            runPhase: PrototypeRunPhase,
        ): TrapPlacementCommitResult {
            val hoverResult = clickedPosition?.let { position ->
                grid.trapSocketHoverResult(
                    hoveredPosition = position,
                    definition = buildState.selectedTrapDefinition,
                )
            } ?: return TrapPlacementCommitResult.NON_SOCKET

            return when (hoverResult) {
                is TrapSocketHoverResult.Valid ->
                    if (runPhase == PrototypeRunPhase.BUILDING &&
                        grid.placeTrap(
                            room = hoverResult.room,
                            localSocketPosition = hoverResult.localSocketPosition,
                            definition = buildState.selectedTrapDefinition,
                        )
                    ) {
                        TrapPlacementCommitResult.PLACED
                    } else {
                        TrapPlacementCommitResult.REJECTED
                    }
                is TrapSocketHoverResult.Occupied,
                is TrapSocketHoverResult.HeartOccupied,
                is TrapSocketHoverResult.Incompatible,
                -> TrapPlacementCommitResult.REJECTED
                TrapSocketHoverResult.NonSocket ->
                    TrapPlacementCommitResult.NON_SOCKET
            }
        }

        internal fun commitHeartPlacement(
            grid: DungeonGrid,
            buildState: BuildState,
            clickedPosition: GridPosition?,
            runController: PrototypeRunController,
        ): HeartPlacementCommitResult {
            if (!buildState.isHeartPlacementModeActive ||
                runController.phase != PrototypeRunPhase.BUILDING
            ) {
                return HeartPlacementCommitResult.REJECTED
            }
            val preview = clickedPosition?.let(grid::heartPlacementPreview)
                ?: return HeartPlacementCommitResult.REJECTED
            val room = preview.room
            if (!preview.isValid || room == null ||
                !runController.placeOrRelocateHeart(room)
            ) {
                return HeartPlacementCommitResult.REJECTED
            }

            buildState.deactivateHeartPlacementMode()
            return HeartPlacementCommitResult.PLACED
        }

        internal fun rotateSelectedRoom(
            buildState: BuildState,
            direction: RoomRotationDirection,
            runPhase: PrototypeRunPhase,
        ): Boolean {
            if (runPhase != PrototypeRunPhase.BUILDING) {
                return false
            }

            when (direction) {
                RoomRotationDirection.COUNTER_CLOCKWISE ->
                    buildState.rotateSelectedRoomCounterClockwise()
                RoomRotationDirection.CLOCKWISE ->
                    buildState.rotateSelectedRoomClockwise()
            }
            return true
        }

        internal fun applyBuildShortcut(
            buildState: BuildState,
            shortcut: BuildShortcut,
            runPhase: PrototypeRunPhase,
        ): Boolean = when (shortcut) {
            BuildShortcut.ROTATE_COUNTER_CLOCKWISE -> rotateSelectedRoom(
                buildState,
                RoomRotationDirection.COUNTER_CLOCKWISE,
                runPhase,
            )
            BuildShortcut.ROTATE_CLOCKWISE -> rotateSelectedRoom(
                buildState,
                RoomRotationDirection.CLOCKWISE,
                runPhase,
            )
        }

        internal fun loadUpcomingWave(
            readInternalText: (String) -> String,
        ): UpcomingHeroWave =
            UpcomingHeroWaveParser.parse(readInternalText(UPCOMING_WAVE_PATH))

        internal fun loadRoomBlueprint(
            readInternalText: (String) -> String,
        ): RoomBlueprint =
            RoomBlueprintParser.parse(
                readInternalText(ROOM_BLUEPRINT_PATH),
            )

        internal fun loadBuildState(
            readInternalText: (String) -> String,
        ): BuildState {
            val blueprints = ROOM_BLUEPRINT_PATHS.map { path ->
                RoomBlueprintParser.parse(readInternalText(path))
            }
            return BuildState(
                availableRoomBlueprints = blueprints,
                selectedRoomBlueprint = blueprints.first(),
                selectedTrapDefinition = loadTrapDefinition(readInternalText),
            )
        }

        internal fun loadTrapDefinition(
            readInternalText: (String) -> String,
        ): TrapDefinition =
            TrapDefinitionParser.parse(
                readInternalText(TRAP_DEFINITION_PATH),
            )

        internal fun loadRunDefinition(
            readInternalText: (String) -> String,
        ): PrototypeRunDefinition =
            PrototypeRunDefinitionParser.parse(
                readInternalText(RUN_DEFINITION_PATH),
            )

        internal fun handleClick(
            grid: DungeonGrid,
            buildState: BuildState,
            clickedPosition: GridPosition?,
            clickedAttachmentTarget: RoomAttachmentTarget? = null,
            worldX: Float,
            worldY: Float,
            startButtonBounds: ControlBounds,
            cancelButtonBounds: ControlBounds,
            roomChoiceControls: List<RoomChoiceControl>,
            roomRotationControls: List<RoomRotationControl> = emptyList(),
            heartPlacementControl: HeartPlacementControl? = null,
            runController: PrototypeRunController,
        ): PrototypeClickResult {
            if (heartPlacementControl?.bounds?.contains(worldX, worldY) == true) {
                return if (runController.phase == PrototypeRunPhase.BUILDING) {
                    buildState.toggleHeartPlacementMode()
                    if (buildState.isHeartPlacementModeActive) {
                        PrototypeClickResult.HEART_MODE_ACTIVATED
                    } else {
                        PrototypeClickResult.HEART_MODE_DEACTIVATED
                    }
                } else {
                    PrototypeClickResult.HEART_MODE_REJECTED
                }
            }

            if (cancelButtonBounds.contains(worldX, worldY)) {
                return if (runController.cancelLastPlacedRoom()) {
                    PrototypeClickResult.ROOM_CANCELED
                } else {
                    PrototypeClickResult.CANCEL_REJECTED
                }
            }

            if (startButtonBounds.contains(worldX, worldY)) {
                return when {
                    runController.restart() ->
                        PrototypeClickResult.RUN_RESTARTED
                    runController.start() -> {
                        buildState.deactivateHeartPlacementMode()
                        PrototypeClickResult.WAVE_STARTED
                    }
                    else ->
                        PrototypeClickResult.WAVE_START_REJECTED
                }
            }

            val clickedRotation = roomRotationControls.firstOrNull { control ->
                control.bounds.contains(worldX, worldY)
            }
            if (clickedRotation != null) {
                return if (
                    rotateSelectedRoom(
                        buildState = buildState,
                        direction = clickedRotation.view.direction,
                        runPhase = runController.phase,
                    )
                ) {
                    PrototypeClickResult.ROOM_ROTATED
                } else {
                    PrototypeClickResult.ROOM_ROTATION_REJECTED
                }
            }

            val clickedChoice = roomChoiceControls.firstOrNull { control ->
                control.bounds.contains(worldX, worldY)
            }
            if (clickedChoice != null) {
                val wasSelected = buildState.selectRoomBlueprint(
                    clickedChoice.choice.id,
                )
                return if (wasSelected) {
                    PrototypeClickResult.ROOM_CHOICE_SELECTED
                } else {
                    PrototypeClickResult.IGNORED
                }
            }
            if (buildState.isHeartPlacementModeActive) {
                return when (
                    commitHeartPlacement(
                        grid = grid,
                        buildState = buildState,
                        clickedPosition = clickedPosition,
                        runController = runController,
                    )
                ) {
                    HeartPlacementCommitResult.PLACED ->
                        PrototypeClickResult.HEART_PLACED
                    HeartPlacementCommitResult.REJECTED ->
                        PrototypeClickResult.HEART_PLACEMENT_REJECTED
                }
            }

            when (
                commitTrapPlacement(
                    grid = grid,
                    buildState = buildState,
                    clickedPosition = clickedPosition,
                    runPhase = runController.phase,
                )
            ) {
                TrapPlacementCommitResult.PLACED ->
                    return PrototypeClickResult.TRAP_PLACED
                TrapPlacementCommitResult.REJECTED ->
                    return PrototypeClickResult.IGNORED
                TrapPlacementCommitResult.NON_SOCKET -> Unit
            }

            if (clickedAttachmentTarget != null) {
                buildState.selectRoomAttachmentTarget(clickedAttachmentTarget)
                return PrototypeClickResult.ROOM_ATTACHMENT_SELECTED
            }

            val wasPlaced = commitPlacement(
                grid = grid,
                buildState = buildState,
                clickedPosition = clickedPosition,
                runPhase = runController.phase,
            )
            return if (wasPlaced) {
                PrototypeClickResult.ROOM_PLACED
            } else {
                PrototypeClickResult.IGNORED
            }
        }
    }
}

internal enum class PrototypeClickResult {
    WAVE_STARTED,
    WAVE_START_REJECTED,
    RUN_RESTARTED,
    ROOM_CHOICE_SELECTED,
    ROOM_ATTACHMENT_SELECTED,
    ROOM_PLACED,
    ROOM_CANCELED,
    CANCEL_REJECTED,
    ROOM_ROTATED,
    ROOM_ROTATION_REJECTED,
    HEART_MODE_ACTIVATED,
    HEART_MODE_DEACTIVATED,
    HEART_MODE_REJECTED,
    HEART_PLACED,
    HEART_PLACEMENT_REJECTED,
    TRAP_PLACED,
    IGNORED,
}

internal enum class BuildShortcut {
    ROTATE_COUNTER_CLOCKWISE,
    ROTATE_CLOCKWISE,
}

internal enum class HeartPlacementCommitResult {
    PLACED,
    REJECTED,
}

internal enum class TrapPlacementCommitResult {
    PLACED,
    REJECTED,
    NON_SOCKET,
}
