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
            val isEnabled = phase == PrototypeRunPhase.BUILDING
            return RoomRotationView(
                orientationLabel = buildState.selectedRoomOrientation.label,
                controls = listOf(
                    RoomRotationControlView(
                        direction = RoomRotationDirection.COUNTER_CLOCKWISE,
                        label = "CCW",
                        isEnabled = isEnabled,
                    ),
                    RoomRotationControlView(
                        direction = RoomRotationDirection.CLOCKWISE,
                        label = "CW",
                        isEnabled = isEnabled,
                    ),
                ),
            )
        }

        private val RoomOrientation.label: String
            get() = when (this) {
                RoomOrientation.UNROTATED -> "ROT 0"
                RoomOrientation.CLOCKWISE_90 -> "ROT 90"
                RoomOrientation.CLOCKWISE_180 -> "ROT 180"
                RoomOrientation.CLOCKWISE_270 -> "ROT 270"
            }
    }
}

internal data class RoomRotationControl(
    val view: RoomRotationControlView,
    val bounds: ControlBounds,
)

internal object RoomRotationLayout {
    fun controls(
        rotationView: RoomRotationView,
        roomChoicesView: RoomChoicesView,
        worldWidth: Float,
        panelBottom: Float,
    ): List<RoomRotationControl> {
        val groupWidth = rotationView.controls.size * CONTROL_SIZE +
            (rotationView.controls.size - 1).coerceAtLeast(0) * CONTROL_GAP
        val groupRight = RoomChoicesLayout.leftEdge(
            view = roomChoicesView,
            worldWidth = worldWidth,
            panelBottom = panelBottom,
        ) - ROOM_CHOICES_GAP
        val groupLeft = groupRight - groupWidth

        return rotationView.controls.mapIndexed { index, control ->
            RoomRotationControl(
                view = control,
                bounds = ControlBounds(
                    x = groupLeft + index * (CONTROL_SIZE + CONTROL_GAP),
                    y = panelBottom + WavePanelLayout.VERTICAL_PADDING,
                    width = CONTROL_SIZE,
                    height = CONTROL_SIZE,
                ),
            )
        }
    }

    private const val CONTROL_SIZE = 48f
    private const val CONTROL_GAP = 8f
    private const val ROOM_CHOICES_GAP = 16f
}

internal class RoomRotationRenderer : Disposable {
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val labelLayout = GlyphLayout()

    fun render(
        rotationView: RoomRotationView,
        roomChoicesView: RoomChoicesView,
        projection: Matrix4,
        worldWidth: Float,
        panelBottom: Float,
    ) {
        val controls = RoomRotationLayout.controls(
            rotationView = rotationView,
            roomChoicesView = roomChoicesView,
            worldWidth = worldWidth,
            panelBottom = panelBottom,
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
        val left = controls.firstOrNull()?.bounds?.x ?: 0f
        val right = controls.lastOrNull()?.bounds?.let { it.x + it.width } ?: left
        font.color = ORIENTATION_LABEL_COLOR
        labelLayout.setText(font, rotationView.orientationLabel)
        font.draw(
            batch,
            rotationView.orientationLabel,
            left + (right - left - labelLayout.width) / 2f,
            panelBottom + ORIENTATION_LABEL_BASELINE,
        )
        batch.end()
    }

    override fun dispose() {
        font.dispose()
        batch.dispose()
        shapes.dispose()
    }

    private companion object {
        const val ORIENTATION_LABEL_BASELINE = 12f

        val ENABLED_COLOR = Color.valueOf("596A8A")
        val DISABLED_COLOR = Color.valueOf("3F454D")
        val ENABLED_LABEL_COLOR = Color.WHITE
        val DISABLED_LABEL_COLOR = Color.valueOf("AAB0B8")
        val ORIENTATION_LABEL_COLOR = Color.valueOf("B8C0CC")
    }
}
