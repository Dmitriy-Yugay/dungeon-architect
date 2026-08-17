package com.dungeonarchitect.presentation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.UpcomingHeroWave

data class WavePanelView(
    val summary: String,
    val traitDescription: String,
    val objectiveStatus: String,
    val controlLabel: String,
    val isControlEnabled: Boolean,
    val cancelLabel: String,
    val isCancelEnabled: Boolean,
) {
    companion object {
        fun from(
            wave: UpcomingHeroWave,
            phase: PrototypeRunPhase,
            objectiveHealth: Int,
            objectiveMaxHealth: Int,
            isStartEnabled: Boolean,
            isCancelEnabled: Boolean,
        ) = WavePanelView(
            summary = when (phase) {
                PrototypeRunPhase.BUILDING ->
                    "Upcoming wave: ${wave.count} x ${wave.heroDisplayName}"
                PrototypeRunPhase.RUNNING ->
                    "Wave in progress: ${wave.count} x ${wave.heroDisplayName}"
                PrototypeRunPhase.VICTORY -> "VICTORY - Objective secured"
                PrototypeRunPhase.DEFEAT -> "DEFEAT - Objective destroyed"
            },
            traitDescription = wave.traitDescription,
            objectiveStatus =
                "Objective health: $objectiveHealth / $objectiveMaxHealth",
            controlLabel = when (phase) {
                PrototypeRunPhase.BUILDING ->
                    if (isStartEnabled) {
                        "START WAVE"
                    } else {
                        "START WAVE - ROUTE REQUIRED"
                    }
                PrototypeRunPhase.RUNNING -> "WAVE IN PROGRESS"
                PrototypeRunPhase.VICTORY,
                PrototypeRunPhase.DEFEAT,
                -> "RESTART"
            },
            isControlEnabled = isStartEnabled ||
                phase == PrototypeRunPhase.VICTORY ||
                phase == PrototypeRunPhase.DEFEAT,
            cancelLabel = "CANCEL",
            isCancelEnabled = isCancelEnabled,
        )
    }
}

data class ControlBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    init {
        require(width > 0f) { "Control width must be positive." }
        require(height > 0f) { "Control height must be positive." }
    }

    fun contains(worldX: Float, worldY: Float): Boolean =
        worldX >= x &&
            worldX < x + width &&
            worldY >= y &&
            worldY < y + height
}

object WavePanelLayout {
    const val HEIGHT = 128f

    fun startButtonBounds(
        worldWidth: Float,
        panelBottom: Float,
    ) = ControlBounds(
        x = worldWidth - HORIZONTAL_PADDING - START_BUTTON_WIDTH,
        y = panelBottom + VERTICAL_PADDING,
        width = START_BUTTON_WIDTH,
        height = START_BUTTON_HEIGHT,
    )

    fun cancelButtonBounds(
        worldWidth: Float,
        panelBottom: Float,
    ): ControlBounds {
        val startButton = startButtonBounds(worldWidth, panelBottom)
        return ControlBounds(
            x = startButton.x - CONTROL_GAP - CANCEL_BUTTON_WIDTH,
            y = panelBottom + VERTICAL_PADDING,
            width = CANCEL_BUTTON_WIDTH,
            height = START_BUTTON_HEIGHT,
        )
    }

    internal const val HORIZONTAL_PADDING = 16f
    internal const val VERTICAL_PADDING = 16f
    internal const val START_BUTTON_WIDTH = 256f
    internal const val START_BUTTON_HEIGHT = 48f
    internal const val CONTROL_GAP = 16f
    internal const val CANCEL_BUTTON_WIDTH = 112f
}

class WavePanelRenderer : Disposable {
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val labelLayout = GlyphLayout()

    fun render(
        view: WavePanelView,
        projection: Matrix4,
        worldWidth: Float,
        panelBottom: Float,
    ) {
        val startBounds = WavePanelLayout.startButtonBounds(worldWidth, panelBottom)
        val cancelBounds = WavePanelLayout.cancelButtonBounds(worldWidth, panelBottom)

        shapes.projectionMatrix = projection
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = PANEL_COLOR
        shapes.rect(0f, panelBottom, worldWidth, WavePanelLayout.HEIGHT)
        shapes.color =
            if (view.isControlEnabled) ENABLED_BUTTON_COLOR else DISABLED_BUTTON_COLOR
        shapes.rect(startBounds.x, startBounds.y, startBounds.width, startBounds.height)
        shapes.color =
            if (view.isCancelEnabled) ENABLED_CANCEL_COLOR else DISABLED_BUTTON_COLOR
        shapes.rect(
            cancelBounds.x,
            cancelBounds.y,
            cancelBounds.width,
            cancelBounds.height,
        )
        shapes.end()

        batch.projectionMatrix = projection
        batch.begin()
        font.color = PRIMARY_TEXT_COLOR
        font.draw(
            batch,
            view.summary,
            WavePanelLayout.HORIZONTAL_PADDING,
            panelBottom + WavePanelLayout.HEIGHT - WavePanelLayout.VERTICAL_PADDING,
        )
        font.color = SECONDARY_TEXT_COLOR
        font.draw(
            batch,
            view.traitDescription,
            WavePanelLayout.HORIZONTAL_PADDING,
            panelBottom + WavePanelLayout.HEIGHT - DESCRIPTION_OFFSET,
        )
        font.draw(
            batch,
            view.objectiveStatus,
            WavePanelLayout.HORIZONTAL_PADDING,
            panelBottom + WavePanelLayout.HEIGHT - OBJECTIVE_STATUS_OFFSET,
        )

        font.color =
            if (view.isControlEnabled) ENABLED_LABEL_COLOR else DISABLED_LABEL_COLOR
        labelLayout.setText(font, view.controlLabel)
        font.draw(
            batch,
            view.controlLabel,
            startBounds.x + (startBounds.width - labelLayout.width) / 2f,
            startBounds.y + (startBounds.height + labelLayout.height) / 2f,
        )
        font.color =
            if (view.isCancelEnabled) ENABLED_LABEL_COLOR else DISABLED_LABEL_COLOR
        labelLayout.setText(font, view.cancelLabel)
        font.draw(
            batch,
            view.cancelLabel,
            cancelBounds.x + (cancelBounds.width - labelLayout.width) / 2f,
            cancelBounds.y + (cancelBounds.height + labelLayout.height) / 2f,
        )
        batch.end()
    }

    override fun dispose() {
        font.dispose()
        batch.dispose()
        shapes.dispose()
    }

    private companion object {
        const val DESCRIPTION_OFFSET = 48f
        const val OBJECTIVE_STATUS_OFFSET = 80f

        val PANEL_COLOR = Color.valueOf("171B20")
        val ENABLED_BUTTON_COLOR = Color.valueOf("3A9D5D")
        val ENABLED_CANCEL_COLOR = Color.valueOf("9D613A")
        val DISABLED_BUTTON_COLOR = Color.valueOf("3F454D")
        val PRIMARY_TEXT_COLOR = Color.valueOf("F2F2F2")
        val SECONDARY_TEXT_COLOR = Color.valueOf("B8C0CC")
        val ENABLED_LABEL_COLOR = Color.WHITE
        val DISABLED_LABEL_COLOR = Color.valueOf("AAB0B8")
    }
}
