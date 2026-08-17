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
    fun leftEdge(
        view: RoomChoicesView,
        worldWidth: Float,
        panelBottom: Float,
    ): Float = controls(view, worldWidth, panelBottom)
        .firstOrNull()
        ?.bounds
        ?.x
        ?: WavePanelLayout.cancelButtonBounds(worldWidth, panelBottom).x

    fun controls(
        view: RoomChoicesView,
        worldWidth: Float,
        panelBottom: Float,
    ): List<RoomChoiceControl> {
        val groupWidth = view.choices.size * CONTROL_WIDTH +
            (view.choices.size - 1).coerceAtLeast(0) * CONTROL_GAP
        val cancelButton = WavePanelLayout.cancelButtonBounds(
            worldWidth = worldWidth,
            panelBottom = panelBottom,
        )
        val groupLeft = cancelButton.x - WAVE_CONTROL_GAP - groupWidth

        return view.choices.mapIndexed { index, choice ->
            RoomChoiceControl(
                choice = choice,
                bounds = ControlBounds(
                    x = groupLeft + index * (CONTROL_WIDTH + CONTROL_GAP),
                    y = panelBottom + WavePanelLayout.VERTICAL_PADDING,
                    width = CONTROL_WIDTH,
                    height = CONTROL_HEIGHT,
                ),
            )
        }
    }

    private const val CONTROL_WIDTH = 144f
    private const val CONTROL_HEIGHT = 48f
    private const val CONTROL_GAP = 8f
    private const val WAVE_CONTROL_GAP = 16f
}

class RoomChoicesRenderer : Disposable {
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val labelLayout = GlyphLayout()

    fun render(
        view: RoomChoicesView,
        projection: Matrix4,
        worldWidth: Float,
        panelBottom: Float,
    ) {
        val controls = RoomChoicesLayout.controls(
            view = view,
            worldWidth = worldWidth,
            panelBottom = panelBottom,
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
