package com.dungeonarchitect.presentation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.HeartPlacementPreview
import com.dungeonarchitect.domain.PrototypeHeroState
import com.dungeonarchitect.domain.RoomPlacementPreview
import com.dungeonarchitect.domain.RoomSocketType
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
        placementPreview: RoomPlacementPreview?,
        trapPlacementPreview: TrapPlacementPreview?,
        heartPlacementPreview: HeartPlacementPreview?,
        heroState: PrototypeHeroState?,
        hoveredPosition: GridPosition?,
        selectedPosition: GridPosition?,
    ) {
        shapes.projectionMatrix = projection
        renderTiles(grid)
        placementPreview?.let(::renderPlacementPreview)
        renderDoorMarkers(roomDoorGridMarkers(grid))
        renderSocketMarkers(roomSocketGridMarkers(grid.placedRooms))
        renderTrapMarkers(placedTrapGridMarkers(grid))
        trapPlacementPreview?.let(::renderTrapPlacementPreview)
        placedDungeonHeartGridMarker(grid)?.let(::renderHeartMarker)
        heartPlacementPreview?.let(::renderHeartPlacementPreview)
        heroWorldMarker(heroState, TILE_SIZE)?.let(::renderHero)
        heroHealthBar(heroState, TILE_SIZE)?.let(::renderHeroHealthBar)
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

    private fun renderPlacementPreview(preview: RoomPlacementPreview) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = if (preview.isValid) VALID_PREVIEW_COLOR else INVALID_PREVIEW_COLOR

        preview.room.gridPositions.forEach { position ->
            shapes.rect(
                position.column * TILE_SIZE + TILE_GAP,
                position.row * TILE_SIZE + TILE_GAP,
                TILE_SIZE - TILE_GAP * 2,
                TILE_SIZE - TILE_GAP * 2,
            )
        }

        shapes.end()
    }

    private fun renderHero(marker: HeroWorldMarker) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = HERO_COLOR
        shapes.circle(marker.centerX, marker.centerY, marker.radius)
        shapes.end()
    }

    private fun renderHeroHealthBar(bar: HeroHealthBar) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = HERO_HEALTH_BORDER_COLOR
        shapes.rect(bar.left, bar.bottom, bar.width, bar.height)

        val innerLeft = bar.left + HERO_HEALTH_BAR_BORDER
        val innerBottom = bar.bottom + HERO_HEALTH_BAR_BORDER
        val innerWidth = bar.width - HERO_HEALTH_BAR_BORDER * 2f
        val innerHeight = bar.height - HERO_HEALTH_BAR_BORDER * 2f
        shapes.color = HERO_HEALTH_BACKGROUND_COLOR
        shapes.rect(innerLeft, innerBottom, innerWidth, innerHeight)
        shapes.color = HERO_HEALTH_FILL_COLOR
        shapes.rect(
            innerLeft,
            innerBottom,
            innerWidth * bar.fillFraction,
            innerHeight,
        )
        shapes.end()
    }

    private fun renderDoorMarkers(markers: Set<RoomDoorGridMarker>) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)

        markers.forEach { marker ->
            shapes.color = if (marker.isOpen) {
                OPEN_DOOR_MARKER_COLOR
            } else {
                CONNECTED_DOOR_MARKER_COLOR
            }
            val (offsetX, offsetY) = doorMarkerOffset(marker.facing)
            val isHorizontalEdge = marker.facing == CardinalDirection.NORTH ||
                marker.facing == CardinalDirection.SOUTH
            shapes.rect(
                marker.position.column * TILE_SIZE + offsetX,
                marker.position.row * TILE_SIZE + offsetY,
                if (isHorizontalEdge) DOOR_MARKER_LENGTH else DOOR_MARKER_THICKNESS,
                if (isHorizontalEdge) DOOR_MARKER_THICKNESS else DOOR_MARKER_LENGTH,
            )
        }

        shapes.end()
    }

    private fun doorMarkerOffset(
        facing: CardinalDirection,
    ): Pair<Float, Float> = when (facing) {
        CardinalDirection.WEST -> 0f to DOOR_MARKER_CROSS_INSET
        CardinalDirection.EAST ->
            TILE_SIZE - DOOR_MARKER_THICKNESS to DOOR_MARKER_CROSS_INSET
        CardinalDirection.SOUTH -> DOOR_MARKER_CROSS_INSET to 0f
        CardinalDirection.NORTH ->
            DOOR_MARKER_CROSS_INSET to TILE_SIZE - DOOR_MARKER_THICKNESS
    }

    private fun renderSocketMarkers(markers: Set<RoomSocketGridMarker>) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)

        markers.forEach { marker ->
            shapes.color = when (marker.type) {
                RoomSocketType.FLOOR -> FLOOR_SOCKET_MARKER_COLOR
                RoomSocketType.WALL -> WALL_SOCKET_MARKER_COLOR
            }
            shapes.circle(
                (marker.position.column + HALF_TILE) * TILE_SIZE,
                (marker.position.row + HALF_TILE) * TILE_SIZE,
                SOCKET_MARKER_RADIUS,
            )
        }

        shapes.end()
    }

    private fun renderTrapMarkers(markers: List<PlacedTrapGridMarker>) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = TRAP_MARKER_COLOR

        markers.forEach { marker ->
            val centerX = (marker.position.column + HALF_TILE) * TILE_SIZE
            val centerY = (marker.position.row + HALF_TILE) * TILE_SIZE

            shapes.triangle(
                centerX,
                centerY + TRAP_MARKER_RADIUS,
                centerX + TRAP_MARKER_RADIUS,
                centerY,
                centerX,
                centerY - TRAP_MARKER_RADIUS,
            )
            shapes.triangle(
                centerX,
                centerY + TRAP_MARKER_RADIUS,
                centerX,
                centerY - TRAP_MARKER_RADIUS,
                centerX - TRAP_MARKER_RADIUS,
                centerY,
            )
        }

        shapes.end()
    }

    private fun renderTrapPlacementPreview(preview: TrapPlacementPreview) {
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = if (preview.isValid) {
            VALID_PREVIEW_COLOR
        } else {
            INVALID_PREVIEW_COLOR
        }
        shapes.circle(
            (preview.position.column + HALF_TILE) * TILE_SIZE,
            (preview.position.row + HALF_TILE) * TILE_SIZE,
            TRAP_PREVIEW_RADIUS,
        )
        shapes.end()
    }

    private fun renderHeartMarker(marker: PlacedDungeonHeartGridMarker) {
        val centerX = (marker.position.column + HALF_TILE) * TILE_SIZE
        val centerY = (marker.position.row + HALF_TILE) * TILE_SIZE
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = HEART_MARKER_COLOR
        shapes.circle(
            centerX - HEART_LOBE_OFFSET,
            centerY + HEART_LOBE_OFFSET,
            HEART_LOBE_RADIUS,
        )
        shapes.circle(
            centerX + HEART_LOBE_OFFSET,
            centerY + HEART_LOBE_OFFSET,
            HEART_LOBE_RADIUS,
        )
        shapes.triangle(
            centerX - HEART_MARKER_RADIUS,
            centerY + HEART_LOBE_OFFSET,
            centerX + HEART_MARKER_RADIUS,
            centerY + HEART_LOBE_OFFSET,
            centerX,
            centerY - HEART_MARKER_RADIUS,
        )
        shapes.end()
    }

    private fun renderHeartPlacementPreview(preview: HeartPlacementPreview) {
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = if (preview.isValid) {
            VALID_PREVIEW_COLOR
        } else {
            INVALID_PREVIEW_COLOR
        }
        shapes.rect(
            preview.position.column * TILE_SIZE + HEART_PREVIEW_INSET,
            preview.position.row * TILE_SIZE + HEART_PREVIEW_INSET,
            TILE_SIZE - HEART_PREVIEW_INSET * 2,
            TILE_SIZE - HEART_PREVIEW_INSET * 2,
        )
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
        }

    private companion object {
        const val TILE_SIZE = 64f
        const val TILE_GAP = 2f
        const val HOVER_INSET = 4f
        const val SELECTION_INSET = 8f
        const val DOOR_MARKER_LENGTH = 20f
        const val DOOR_MARKER_THICKNESS = 6f
        const val DOOR_MARKER_CROSS_INSET =
            (TILE_SIZE - DOOR_MARKER_LENGTH) / 2f
        const val HALF_TILE = 0.5f
        const val SOCKET_MARKER_RADIUS = 8f
        const val TRAP_MARKER_RADIUS = 12f
        const val TRAP_PREVIEW_RADIUS = 18f
        const val HEART_LOBE_OFFSET = 6f
        const val HEART_LOBE_RADIUS = 10f
        const val HEART_MARKER_RADIUS = 16f
        const val HEART_PREVIEW_INSET = 12f
        const val HERO_HEALTH_BAR_BORDER = 2f

        val EMPTY_TILE_COLOR = Color.valueOf("252B33")
        val ROOM_COLOR = Color.valueOf("5D6D7E")
        val ENTRANCE_COLOR = Color.valueOf("3A9D5D")
        val VALID_PREVIEW_COLOR = Color.valueOf("4EA86B")
        val INVALID_PREVIEW_COLOR = Color.valueOf("D85C5C")
        val OPEN_DOOR_MARKER_COLOR = Color.valueOf("F0A44B")
        val CONNECTED_DOOR_MARKER_COLOR = Color.valueOf("8A6A45")
        val FLOOR_SOCKET_MARKER_COLOR = Color.valueOf("47C6B5")
        val WALL_SOCKET_MARKER_COLOR = Color.valueOf("B779D0")
        val TRAP_MARKER_COLOR = Color.valueOf("E84A5F")
        val HEART_MARKER_COLOR = Color.valueOf("EF6FAE")
        val HERO_COLOR = Color.valueOf("4BA3D3")
        val HERO_HEALTH_BORDER_COLOR = Color.valueOf("F2F2F2")
        val HERO_HEALTH_BACKGROUND_COLOR = Color.valueOf("301C25")
        val HERO_HEALTH_FILL_COLOR = Color.valueOf("63D471")
        val HOVER_COLOR = Color.valueOf("E0B84B")
        val SELECTION_COLOR = Color.valueOf("F2F2F2")
    }
}
