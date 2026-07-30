package com.dungeonarchitect.application

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.presentation.DungeonGridRenderer

class PrototypeScreen(
    private val grid: DungeonGrid = prototypeGrid(),
) : ScreenAdapter() {
    private val camera = OrthographicCamera()
    private val gridRenderer = DungeonGridRenderer()
    private val viewport = FitViewport(
        gridRenderer.worldWidth(grid),
        gridRenderer.worldHeight(grid),
        camera,
    )
    private val pointerCoordinates = Vector2()

    private var hoveredPosition: GridPosition? = null
    private var selectedPosition: GridPosition? = null

    override fun render(delta: Float) {
        updatePointerState()
        ScreenUtils.clear(BACKGROUND_RED, BACKGROUND_GREEN, BACKGROUND_BLUE, BACKGROUND_ALPHA)
        gridRenderer.render(grid, camera.combined, hoveredPosition, selectedPosition)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
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
            selectedPosition = hoveredPosition
        }
    }

    companion object {
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
    }
}
