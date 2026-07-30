package com.dungeonarchitect.application

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.dungeonarchitect.content.UpcomingHeroWaveParser
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomPlacementPreview
import com.dungeonarchitect.domain.StartedHeroWave
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.presentation.ControlBounds
import com.dungeonarchitect.presentation.DungeonGridRenderer
import com.dungeonarchitect.presentation.WavePanelLayout
import com.dungeonarchitect.presentation.WavePanelRenderer
import com.dungeonarchitect.presentation.WavePanelView
import com.dungeonarchitect.simulation.FixedStepHeroSimulation

class PrototypeScreen(
    private val grid: DungeonGrid = prototypeGrid(),
    private val upcomingWave: UpcomingHeroWave = loadUpcomingWave { path ->
        Gdx.files.internal(path).readString("UTF-8")
    },
) : ScreenAdapter() {
    private val camera = OrthographicCamera()
    private val gridRenderer = DungeonGridRenderer()
    private val wavePanelRenderer = WavePanelRenderer()
    private val worldWidth = gridRenderer.worldWidth(grid)
    private val gridWorldHeight = gridRenderer.worldHeight(grid)
    private val viewport = FitViewport(
        worldWidth,
        gridWorldHeight + WavePanelLayout.HEIGHT,
        camera,
    )
    private val pointerCoordinates = Vector2()
    private val waveStartController = WaveStartController(grid, upcomingWave)

    private var hoveredPosition: GridPosition? = null
    private var selectedPosition: GridPosition? = null
    private var heroSimulation: FixedStepHeroSimulation? = null

    override fun render(delta: Float) {
        updatePointerState()
        heroSimulation = advanceHeroSimulation(
            simulation = heroSimulation,
            startedWave = waveStartController.startedWave,
            elapsedSeconds = delta,
        )
        ScreenUtils.clear(BACKGROUND_RED, BACKGROUND_GREEN, BACKGROUND_BLUE, BACKGROUND_ALPHA)
        gridRenderer.render(
            grid = grid,
            projection = camera.combined,
            placementPreview = placementPreview(grid, hoveredPosition),
            heroState = heroSimulation?.heroState,
            hoveredPosition = hoveredPosition,
            selectedPosition = selectedPosition,
        )
        wavePanelRenderer.render(
            view = WavePanelView.from(
                wave = upcomingWave,
                isStartEnabled = waveStartController.isStartEnabled,
                hasStarted = waveStartController.startedWave != null,
            ),
            projection = camera.combined,
            worldWidth = worldWidth,
            panelBottom = gridWorldHeight,
        )
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
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
                clickedPosition = hoveredPosition,
                worldX = pointerCoordinates.x,
                worldY = pointerCoordinates.y,
                startButtonBounds = WavePanelLayout.startButtonBounds(
                    worldWidth = worldWidth,
                    panelBottom = gridWorldHeight,
                ),
                waveStartController = waveStartController,
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

        private val prototypeRoom = PlacedRoom(
            blueprint = RoomBlueprint(
                footprint = setOf(
                    GridPosition(column = 0, row = 0),
                    GridPosition(column = 1, row = 0),
                    GridPosition(column = 2, row = 0),
                    GridPosition(column = 0, row = 1),
                    GridPosition(column = 1, row = 1),
                    GridPosition(column = 2, row = 1),
                    GridPosition(column = 0, row = 2),
                    GridPosition(column = 1, row = 2),
                    GridPosition(column = 2, row = 2),
                ),
                doorPositions = setOf(
                    GridPosition(column = 0, row = 1),
                    GridPosition(column = 2, row = 1),
                ),
            ),
            origin = GridPosition(column = 6, row = 3),
        )

        private const val BACKGROUND_RED = 0.04f
        private const val BACKGROUND_GREEN = 0.05f
        private const val BACKGROUND_BLUE = 0.07f
        private const val BACKGROUND_ALPHA = 1f

        internal fun prototypeGrid() = DungeonGrid(
            width = 16,
            height = 9,
            entrance = GridPosition(column = 0, row = 4),
            objective = GridPosition(column = 15, row = 4),
            placedRooms = listOf(prototypeRoom),
        )

        internal fun placementPreview(
            grid: DungeonGrid,
            hoveredPosition: GridPosition?,
        ): RoomPlacementPreview? =
            hoveredPosition?.let { origin ->
                grid.placementPreview(prototypeRoom.blueprint, origin)
            }

        internal fun commitPlacement(
            grid: DungeonGrid,
            clickedPosition: GridPosition?,
        ): Boolean =
            placementPreview(grid, clickedPosition)
                ?.let { preview -> grid.place(preview.room) }
                ?: false

        internal fun loadUpcomingWave(
            readInternalText: (String) -> String,
        ): UpcomingHeroWave =
            UpcomingHeroWaveParser.parse(readInternalText(UPCOMING_WAVE_PATH))

        internal fun advanceHeroSimulation(
            simulation: FixedStepHeroSimulation?,
            startedWave: StartedHeroWave?,
            elapsedSeconds: Float,
        ): FixedStepHeroSimulation? =
            (simulation ?: startedWave?.let(::FixedStepHeroSimulation))
                ?.also { it.advance(elapsedSeconds) }

        internal fun handleClick(
            grid: DungeonGrid,
            clickedPosition: GridPosition?,
            worldX: Float,
            worldY: Float,
            startButtonBounds: ControlBounds,
            waveStartController: WaveStartController,
        ): PrototypeClickResult =
            if (startButtonBounds.contains(worldX, worldY)) {
                if (waveStartController.start()) {
                    PrototypeClickResult.WAVE_STARTED
                } else {
                    PrototypeClickResult.WAVE_START_REJECTED
                }
            } else if (commitPlacement(grid, clickedPosition)) {
                PrototypeClickResult.ROOM_PLACED
            } else {
                PrototypeClickResult.IGNORED
            }
    }
}

internal enum class PrototypeClickResult {
    WAVE_STARTED,
    WAVE_START_REJECTED,
    ROOM_PLACED,
    IGNORED,
}
