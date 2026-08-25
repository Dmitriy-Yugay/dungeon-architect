package com.dungeonarchitect.presentation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable

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
    private val labelLayout = GlyphLayout()

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
        }
        shapes.end()

        batch.projectionMatrix = projection
        batch.begin()
        controls.forEach { control ->
            font.color =
                if (control.choice.isSelected) SELECTED_LABEL_COLOR else LABEL_COLOR
            labelLayout.setText(font, control.choice.displayName)
            font.draw(
                batch,
                control.choice.displayName,
                control.bounds.x +
                    (control.bounds.width - labelLayout.width) / 2f,
                control.bounds.y +
                    (control.bounds.height + labelLayout.height) / 2f,
            )
        }
        batch.end()
    }

    override fun dispose() {
        font.dispose()
        batch.dispose()
        shapes.dispose()
    }

    private companion object {
        val SELECTED_COLOR = Color.valueOf("4B7CB8")
        val AVAILABLE_COLOR = Color.valueOf("3F454D")
        val SELECTED_LABEL_COLOR = Color.WHITE
        val LABEL_COLOR = Color.valueOf("D4D9E0")
    }
}
