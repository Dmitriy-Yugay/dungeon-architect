package com.dungeonarchitect.presentation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.TileType
import kotlin.math.floor

class DungeonGridRenderer : Disposable {
    private val shapes = ShapeRenderer()

    fun worldWidth(grid: DungeonGrid): Float = grid.width * TILE_SIZE

    fun worldHeight(grid: DungeonGrid): Float = grid.height * TILE_SIZE

    fun gridPositionAt(
        grid: DungeonGrid,
        worldX: Float,
        worldY: Float,
    ): GridPosition? {
        val column = floor(worldX / TILE_SIZE).toInt()
        val row = floor(worldY / TILE_SIZE).toInt()

        return GridPosition(column, row).takeIf(grid::contains)
    }

    fun render(
        grid: DungeonGrid,
        projection: Matrix4,
        hoveredPosition: GridPosition?,
        selectedPosition: GridPosition?,
    ) {
        shapes.projectionMatrix = projection
        renderTiles(grid)
        renderHighlights(hoveredPosition, selectedPosition)
    }

    override fun dispose() {
        shapes.dispose()
    }

    private fun renderTiles(grid: DungeonGrid) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)

        for (row in 0 until grid.height) {
            for (column in 0 until grid.width) {
                shapes.color = colorFor(grid.tileAt(column, row))
                shapes.rect(
                    column * TILE_SIZE + TILE_GAP,
                    row * TILE_SIZE + TILE_GAP,
                    TILE_SIZE - TILE_GAP * 2,
                    TILE_SIZE - TILE_GAP * 2,
                )
            }
        }

        shapes.end()
    }

    private fun renderHighlights(
        hoveredPosition: GridPosition?,
        selectedPosition: GridPosition?,
    ) {
        shapes.begin(ShapeRenderer.ShapeType.Line)

        hoveredPosition?.let { position ->
            shapes.color = HOVER_COLOR
            renderOutline(position, HOVER_INSET)
        }

        selectedPosition?.let { position ->
            shapes.color = SELECTION_COLOR
            renderOutline(position, SELECTION_INSET)
        }

        shapes.end()
    }

    private fun renderOutline(position: GridPosition, inset: Float) {
        shapes.rect(
            position.column * TILE_SIZE + inset,
            position.row * TILE_SIZE + inset,
            TILE_SIZE - inset * 2,
            TILE_SIZE - inset * 2,
        )
    }

    private fun colorFor(tileType: TileType): Color =
        when (tileType) {
            TileType.EMPTY -> EMPTY_TILE_COLOR
            TileType.ROOM -> ROOM_COLOR
            TileType.ENTRANCE -> ENTRANCE_COLOR
            TileType.OBJECTIVE -> OBJECTIVE_COLOR
        }

    private companion object {
        const val TILE_SIZE = 64f
        const val TILE_GAP = 2f
        const val HOVER_INSET = 4f
        const val SELECTION_INSET = 8f

        val EMPTY_TILE_COLOR = Color.valueOf("252B33")
        val ROOM_COLOR = Color.valueOf("5D6D7E")
        val ENTRANCE_COLOR = Color.valueOf("3A9D5D")
        val OBJECTIVE_COLOR = Color.valueOf("B84B4B")
        val HOVER_COLOR = Color.valueOf("E0B84B")
        val SELECTION_COLOR = Color.valueOf("F2F2F2")
    }
}
