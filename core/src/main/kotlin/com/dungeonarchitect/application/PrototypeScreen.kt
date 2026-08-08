package com.dungeonarchitect.application

import com.badlogic.gdx.Gdx
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
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomPlacementPreview
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.presentation.ControlBounds
import com.dungeonarchitect.presentation.DungeonGridRenderer
import com.dungeonarchitect.presentation.RoomChoiceControl
import com.dungeonarchitect.presentation.RoomChoicesLayout
import com.dungeonarchitect.presentation.RoomChoicesRenderer
import com.dungeonarchitect.presentation.RoomChoicesView
import com.dungeonarchitect.presentation.WavePanelLayout
import com.dungeonarchitect.presentation.WavePanelRenderer
import com.dungeonarchitect.presentation.WavePanelView

class PrototypeScreen(
    private val buildState: BuildState = loadBuildState { path ->
        Gdx.files.internal(path).readString("UTF-8")
    },
    private val grid: DungeonGrid = prototypeGrid(
        buildState.selectedRoomBlueprint,
    ),
    private val upcomingWave: UpcomingHeroWave = loadUpcomingWave { path ->
        Gdx.files.internal(path).readString("UTF-8")
    },
    trapDefinition: TrapDefinition = loadTrapDefinition { path ->
        Gdx.files.internal(path).readString("UTF-8")
    },
    runDefinition: PrototypeRunDefinition = loadRunDefinition { path ->
        Gdx.files.internal(path).readString("UTF-8")
    },
) : ScreenAdapter() {
    init {
        check(placePrototypeTrap(grid, trapDefinition)) {
            "The prototype trap could not be placed in its authored socket."
        }
    }

    private val camera = OrthographicCamera()
    private val gridRenderer = DungeonGridRenderer()
    private val roomChoicesRenderer = RoomChoicesRenderer()
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
        gridRenderer.render(
            grid = grid,
            projection = camera.combined,
            placementPreview = placementPreview(
                grid = grid,
                buildState = buildState,
                hoveredPosition = hoveredPosition,
            ),
            heroState = runController.heroState,
            hoveredPosition = hoveredPosition,
            selectedPosition = selectedPosition,
        )
        wavePanelRenderer.render(
            view = WavePanelView.from(
                wave = upcomingWave,
                phase = runController.phase,
                objectiveHealth = runController.objectiveHealth,
                objectiveMaxHealth = runController.objectiveMaxHealth,
                isStartEnabled = runController.isStartEnabled,
            ),
            projection = camera.combined,
            worldWidth = worldWidth,
            panelBottom = gridWorldHeight,
        )
        roomChoicesRenderer.render(
            view = RoomChoicesView.from(buildState),
            projection = camera.combined,
            worldWidth = worldWidth,
            panelBottom = gridWorldHeight,
        )
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        roomChoicesRenderer.dispose()
        wavePanelRenderer.dispose()
        gridRenderer.dispose()
    }

    private fun updatePointerState() {
        pointerCoordinates.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        viewport.unproject(pointerCoordinates)

        hoveredPosition = gridRenderer.gridPositionAt(
            grid = grid,
            worldX = pointerCoordinates.x,
            worldY = pointerCoordinates.y,
        )

        if (Gdx.input.justTouched()) {
            val result = handleClick(
                grid = grid,
                buildState = buildState,
                clickedPosition = hoveredPosition,
                worldX = pointerCoordinates.x,
                worldY = pointerCoordinates.y,
                startButtonBounds = WavePanelLayout.startButtonBounds(
                    worldWidth = worldWidth,
                    panelBottom = gridWorldHeight,
                ),
                roomChoiceControls = RoomChoicesLayout.controls(
                    view = RoomChoicesView.from(buildState),
                    worldWidth = worldWidth,
                    panelBottom = gridWorldHeight,
                ),
                runController = runController,
            )
            if (result == PrototypeClickResult.ROOM_PLACED ||
                result == PrototypeClickResult.IGNORED
            ) {
                selectedPosition = hoveredPosition
            }
        }
    }

    companion object {
        private const val UPCOMING_WAVE_PATH = "content/upcoming-hero-wave.json"
        private const val TRAP_DEFINITION_PATH = "content/spike-trap.json"
        private const val RUN_DEFINITION_PATH = "content/prototype-run.json"
        private const val ROOM_BLUEPRINT_PATH = "content/prototype-room.json"
        private const val LONG_GALLERY_PATH = "content/long-gallery.json"
        private val ROOM_BLUEPRINT_PATHS = listOf(
            ROOM_BLUEPRINT_PATH,
            LONG_GALLERY_PATH,
        )

        private const val BACKGROUND_RED = 0.04f
        private const val BACKGROUND_GREEN = 0.05f
        private const val BACKGROUND_BLUE = 0.07f
        private const val BACKGROUND_ALPHA = 1f

        internal fun prototypeGrid(blueprint: RoomBlueprint) = DungeonGrid(
            width = 16,
            height = 9,
            entrance = GridPosition(column = 0, row = 4),
            objective = GridPosition(column = 15, row = 4),
            placedRooms = listOf(
                PlacedRoom(
                    blueprint = blueprint,
                    origin = GridPosition(column = 6, row = 3),
                ),
            ),
        )

        internal fun placementPreview(
            grid: DungeonGrid,
            buildState: BuildState,
            hoveredPosition: GridPosition?,
        ): RoomPlacementPreview? =
            hoveredPosition?.let { origin ->
                grid.placementPreview(
                    buildState.selectedRoomBlueprint,
                    origin,
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

            return placementPreview(grid, buildState, clickedPosition)
                ?.let { preview -> grid.place(preview.room) }
                ?: false
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

        internal fun placePrototypeTrap(
            grid: DungeonGrid,
            definition: TrapDefinition,
        ): Boolean {
            val room = grid.placedRooms.singleOrNull() ?: return false
            val localSocketPosition =
                room.blueprint.sockets.keys.singleOrNull() ?: return false

            return grid.placeTrap(
                room = room,
                localSocketPosition = localSocketPosition,
                definition = definition,
            )
        }

        internal fun handleClick(
            grid: DungeonGrid,
            buildState: BuildState,
            clickedPosition: GridPosition?,
            worldX: Float,
            worldY: Float,
            startButtonBounds: ControlBounds,
            roomChoiceControls: List<RoomChoiceControl>,
            runController: PrototypeRunController,
        ): PrototypeClickResult {
            if (startButtonBounds.contains(worldX, worldY)) {
                return when {
                    runController.restart() ->
                        PrototypeClickResult.RUN_RESTARTED
                    runController.start() ->
                        PrototypeClickResult.WAVE_STARTED
                    else ->
                        PrototypeClickResult.WAVE_START_REJECTED
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
    ROOM_PLACED,
    IGNORED,
}
