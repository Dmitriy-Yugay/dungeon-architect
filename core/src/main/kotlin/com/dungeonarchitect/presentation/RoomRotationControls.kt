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
import com.dungeonarchitect.domain.RoomOrientation

internal enum class RoomRotationDirection {
    COUNTER_CLOCKWISE,
    CLOCKWISE,
}

internal data class RoomRotationControlView(
    val direction: RoomRotationDirection,
    val label: String,
    val isEnabled: Boolean,
)

internal data class RoomRotationView(
    val orientationLabel: String,
    val controls: List<RoomRotationControlView>,
) {
    companion object {
        fun from(
            buildState: BuildState,
            phase: PrototypeRunPhase,
        ): RoomRotationView {
            val isEnabled = phase == PrototypeRunPhase.DEFENSE_PREPARATION
            return RoomRotationView(
                orientationLabel = buildState.selectedRoomOrientation.plainLabel,
                controls = listOf(
                    RoomRotationControlView(
                        direction = RoomRotationDirection.COUNTER_CLOCKWISE,
                        label = "< Q",
                        isEnabled = isEnabled,
                    ),
                    RoomRotationControlView(
                        direction = RoomRotationDirection.CLOCKWISE,
                        label = "E >",
                        isEnabled = isEnabled,
                    ),
                ),
            )
        }

    }
}

internal val RoomOrientation.plainLabel: String
    get() = when (this) {
        RoomOrientation.UNROTATED -> "Original"
        RoomOrientation.CLOCKWISE_90 -> "90 right"
        RoomOrientation.CLOCKWISE_180 -> "Turned 180"
        RoomOrientation.CLOCKWISE_270 -> "90 left"
    }

internal data class RoomRotationControl(
    val view: RoomRotationControlView,
    val bounds: ControlBounds,
)

internal object RoomRotationLayout {
    fun controls(
        rotationView: RoomRotationView,
        layout: BottomPanelLayout,
    ): List<RoomRotationControl> {
        require(rotationView.controls.size == layout.roomRotationBounds.size) {
            "Rotation view and panel layout must contain the same number of controls."
        }
        return rotationView.controls.zip(layout.roomRotationBounds) { view, bounds ->
            RoomRotationControl(view = view, bounds = bounds)
        }
    }

    fun controls(
        rotationView: RoomRotationView,
        roomChoicesView: RoomChoicesView,
        worldWidth: Float,
        panelBottom: Float,
    ): List<RoomRotationControl> = controls(
        rotationView = rotationView,
        layout = WavePanelLayout.create(
            worldWidth = worldWidth,
            panelBottom = panelBottom,
            roomChoiceCount = roomChoicesView.choices.size,
            roomRotationControlCount = rotationView.controls.size,
        ),
    )
}

internal class RoomRotationRenderer : Disposable {
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val labelLayout = GlyphLayout()

    fun render(
        rotationView: RoomRotationView,
        projection: Matrix4,
        layout: BottomPanelLayout,
    ) {
        val controls = RoomRotationLayout.controls(
            rotationView = rotationView,
            layout = layout,
        )

        shapes.projectionMatrix = projection
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        controls.forEach { control ->
            shapes.color =
                if (control.view.isEnabled) ENABLED_COLOR else DISABLED_COLOR
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
                if (control.view.isEnabled) {
                    ENABLED_LABEL_COLOR
                } else {
                    DISABLED_LABEL_COLOR
                }
            labelLayout.setText(font, control.view.label)
            font.draw(
                batch,
                control.view.label,
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
        val ENABLED_COLOR = Color.valueOf("596A8A")
        val DISABLED_COLOR = Color.valueOf("3F454D")
        val ENABLED_LABEL_COLOR = Color.WHITE
        val DISABLED_LABEL_COLOR = Color.valueOf("AAB0B8")
    }
}
