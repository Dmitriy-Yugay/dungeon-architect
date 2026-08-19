package com.dungeonarchitect.presentation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.PrototypeRunPhase

internal data class HeartPlacementControlView(
    val label: String,
    val isEnabled: Boolean,
    val isActive: Boolean,
) {
    companion object {
        fun from(
            buildState: BuildState,
            phase: PrototypeRunPhase,
        ) = HeartPlacementControlView(
            label = "PLACE HEART",
            isEnabled = phase == PrototypeRunPhase.BUILDING,
            isActive = phase == PrototypeRunPhase.BUILDING &&
                buildState.isHeartPlacementModeActive,
        )
    }
}

internal data class HeartPlacementControl(
    val view: HeartPlacementControlView,
    val bounds: ControlBounds,
)

internal object HeartPlacementLayout {
    fun control(
        view: HeartPlacementControlView,
        worldWidth: Float,
        panelBottom: Float,
    ): HeartPlacementControl = HeartPlacementControl(
        view = view,
        bounds = bounds(worldWidth, panelBottom),
    )

    fun bounds(
        worldWidth: Float,
        panelBottom: Float,
    ): ControlBounds {
        val cancel = WavePanelLayout.cancelButtonBounds(worldWidth, panelBottom)
        return ControlBounds(
            x = cancel.x - CONTROL_GAP - CONTROL_WIDTH,
            y = panelBottom + WavePanelLayout.VERTICAL_PADDING,
            width = CONTROL_WIDTH,
            height = CONTROL_HEIGHT,
        )
    }

    private const val CONTROL_WIDTH = 112f
    private const val CONTROL_HEIGHT = 48f
    private const val CONTROL_GAP = 16f
}

internal class HeartPlacementControlRenderer : Disposable {
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val labelLayout = GlyphLayout()

    fun render(
        view: HeartPlacementControlView,
        projection: Matrix4,
        worldWidth: Float,
        panelBottom: Float,
    ) {
        val bounds = HeartPlacementLayout.control(
            view,
            worldWidth,
            panelBottom,
        ).bounds
        shapes.projectionMatrix = projection
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = when {
            view.isActive -> ACTIVE_COLOR
            view.isEnabled -> ENABLED_COLOR
            else -> DISABLED_COLOR
        }
        shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height)
        shapes.end()

        batch.projectionMatrix = projection
        batch.begin()
        font.color = if (view.isEnabled) ENABLED_LABEL_COLOR else DISABLED_LABEL_COLOR
        labelLayout.setText(font, view.label)
        font.draw(
            batch,
            view.label,
            bounds.x + (bounds.width - labelLayout.width) / 2f,
            bounds.y + (bounds.height + labelLayout.height) / 2f,
        )
        batch.end()
    }

    override fun dispose() {
        font.dispose()
        batch.dispose()
        shapes.dispose()
    }

    private companion object {
        val ACTIVE_COLOR = Color.valueOf("9E467B")
        val ENABLED_COLOR = Color.valueOf("67405B")
        val DISABLED_COLOR = Color.valueOf("3F454D")
        val ENABLED_LABEL_COLOR = Color.WHITE
        val DISABLED_LABEL_COLOR = Color.valueOf("AAB0B8")
    }
}
