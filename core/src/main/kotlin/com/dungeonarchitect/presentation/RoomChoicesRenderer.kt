package com.dungeonarchitect.presentation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Align
import com.dungeonarchitect.domain.CardinalDirection
import kotlin.math.min

internal data class RoomChoiceControl(
    val choice: RoomChoiceView,
    val bounds: ControlBounds,
)

internal object RoomChoicesLayout {
    fun controls(
        view: RoomChoicesView,
        layout: BottomPanelLayout,
    ): List<RoomChoiceControl> {
        require(view.choices.size == layout.roomChoiceBounds.size) {
            "Room choice view and panel layout must contain the same number of controls."
        }
        return view.choices.zip(layout.roomChoiceBounds) { choice, bounds ->
            RoomChoiceControl(choice = choice, bounds = bounds)
        }
    }

    fun controls(
        view: RoomChoicesView,
        worldWidth: Float,
        panelBottom: Float,
    ): List<RoomChoiceControl> = controls(
        view = view,
        layout = WavePanelLayout.create(
            worldWidth = worldWidth,
            panelBottom = panelBottom,
            roomChoiceCount = view.choices.size,
            roomRotationControlCount = 2,
        ),
    )
}

class RoomChoicesRenderer : Disposable {
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()

    fun render(
        view: RoomChoicesView,
        projection: Matrix4,
        layout: BottomPanelLayout,
    ) {
        val controls = RoomChoicesLayout.controls(
            view = view,
            layout = layout,
        )

        shapes.projectionMatrix = projection
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        controls.forEach { control ->
            shapes.color =
                if (control.choice.isSelected) SELECTED_COLOR else AVAILABLE_COLOR
            shapes.rect(
                control.bounds.x,
                control.bounds.y,
                control.bounds.width,
                control.bounds.height,
            )
            renderThumbnail(control)
        }
        shapes.end()

        batch.projectionMatrix = projection
        batch.begin()
        controls.forEach { control ->
            font.color =
                if (control.choice.isSelected) SELECTED_LABEL_COLOR else LABEL_COLOR
            font.draw(
                batch,
                control.choice.displayName,
                control.bounds.x + LABEL_LEFT_OFFSET,
                control.bounds.y + LABEL_BASELINE,
                control.bounds.width - LABEL_LEFT_OFFSET - LABEL_RIGHT_PADDING,
                Align.center,
                true,
            )
        }
        batch.end()
    }

    override fun dispose() {
        font.dispose()
        batch.dispose()
        shapes.dispose()
    }

    private fun renderThumbnail(control: RoomChoiceControl) {
        val thumbnail = control.choice.thumbnail
        if (thumbnail.footprint.isEmpty()) {
            return
        }
        val columns = thumbnail.footprint.maxOf { it.column } + 1
        val rows = thumbnail.footprint.maxOf { it.row } + 1
        val tileSize = min(THUMBNAIL_SIZE / columns, THUMBNAIL_SIZE / rows)
        val left = control.bounds.x + THUMBNAIL_LEFT
        val bottom = control.bounds.y +
            (control.bounds.height - rows * tileSize) / 2f

        shapes.color = THUMBNAIL_FOOTPRINT_COLOR
        thumbnail.footprint.forEach { position ->
            shapes.rect(
                left + position.column * tileSize + THUMBNAIL_CELL_GAP,
                bottom + position.row * tileSize + THUMBNAIL_CELL_GAP,
                tileSize - THUMBNAIL_CELL_GAP * 2f,
                tileSize - THUMBNAIL_CELL_GAP * 2f,
            )
        }
        shapes.color = THUMBNAIL_DOOR_COLOR
        thumbnail.doors.forEach { door ->
            val cellLeft = left + door.position.column * tileSize
            val cellBottom = bottom + door.position.row * tileSize
            val doorLength = tileSize * THUMBNAIL_DOOR_LENGTH_FRACTION
            val crossInset = (tileSize - doorLength) / 2f
            when (door.facing) {
                CardinalDirection.WEST -> shapes.rect(
                    cellLeft,
                    cellBottom + crossInset,
                    THUMBNAIL_DOOR_THICKNESS,
                    doorLength,
                )
                CardinalDirection.EAST -> shapes.rect(
                    cellLeft + tileSize - THUMBNAIL_DOOR_THICKNESS,
                    cellBottom + crossInset,
                    THUMBNAIL_DOOR_THICKNESS,
                    doorLength,
                )
                CardinalDirection.SOUTH -> shapes.rect(
                    cellLeft + crossInset,
                    cellBottom,
                    doorLength,
                    THUMBNAIL_DOOR_THICKNESS,
                )
                CardinalDirection.NORTH -> shapes.rect(
                    cellLeft + crossInset,
                    cellBottom + tileSize - THUMBNAIL_DOOR_THICKNESS,
                    doorLength,
                    THUMBNAIL_DOOR_THICKNESS,
                )
            }
        }
        shapes.color = THUMBNAIL_SOCKET_COLOR
        thumbnail.sockets.keys.forEach { socket ->
            shapes.circle(
                left + (socket.column + 0.5f) * tileSize,
                bottom + (socket.row + 0.5f) * tileSize,
                THUMBNAIL_SOCKET_RADIUS,
            )
        }
        thumbnail.heartAnchor?.let { heart ->
            shapes.color = THUMBNAIL_HEART_COLOR
            shapes.circle(
                left + (heart.column + 0.5f) * tileSize,
                bottom + (heart.row + 0.5f) * tileSize,
                THUMBNAIL_HEART_RADIUS,
            )
        }
    }

    private companion object {
        const val THUMBNAIL_SIZE = 32f
        const val THUMBNAIL_LEFT = 5f
        const val THUMBNAIL_CELL_GAP = 1f
        const val THUMBNAIL_DOOR_LENGTH_FRACTION = 0.6f
        const val THUMBNAIL_DOOR_THICKNESS = 2f
        const val THUMBNAIL_SOCKET_RADIUS = 2f
        const val THUMBNAIL_HEART_RADIUS = 1.5f
        const val LABEL_LEFT_OFFSET = 40f
        const val LABEL_RIGHT_PADDING = 4f
        const val LABEL_BASELINE = 38f

        val SELECTED_COLOR = Color.valueOf("4B7CB8")
        val AVAILABLE_COLOR = Color.valueOf("3F454D")
        val SELECTED_LABEL_COLOR = Color.WHITE
        val LABEL_COLOR = Color.valueOf("D4D9E0")
        val THUMBNAIL_FOOTPRINT_COLOR = Color.valueOf("A8B5C5")
        val THUMBNAIL_DOOR_COLOR = Color.valueOf("F0C15B")
        val THUMBNAIL_SOCKET_COLOR = Color.valueOf("47C6B5")
        val THUMBNAIL_HEART_COLOR = Color.valueOf("EF6FAE")
    }
}
