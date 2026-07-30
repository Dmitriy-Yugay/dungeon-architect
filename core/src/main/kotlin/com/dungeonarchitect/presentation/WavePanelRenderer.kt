package com.dungeonarchitect.presentation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.dungeonarchitect.domain.UpcomingHeroWave

data class WavePanelView(
    val summary: String,
    val traitDescription: String,
    val startLabel: String,
    val isStartEnabled: Boolean,
) {
    companion object {
        fun from(
            wave: UpcomingHeroWave,
            isStartEnabled: Boolean,
            hasStarted: Boolean,
        ) = WavePanelView(
            summary = "Upcoming wave: ${wave.count} x ${wave.heroDisplayName}",
            traitDescription = wave.traitDescription,
            startLabel = when {
                hasStarted -> "WAVE STARTED"
                isStartEnabled -> "START WAVE"
                else -> "START WAVE - ROUTE REQUIRED"
            },
            isStartEnabled = isStartEnabled,
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

    internal const val HORIZONTAL_PADDING = 16f
    internal const val VERTICAL_PADDING = 16f
    internal const val START_BUTTON_WIDTH = 256f
    internal const val START_BUTTON_HEIGHT = 48f
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

        shapes.projectionMatrix = projection
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = PANEL_COLOR
        shapes.rect(0f, panelBottom, worldWidth, WavePanelLayout.HEIGHT)
        shapes.color = if (view.isStartEnabled) ENABLED_BUTTON_COLOR else DISABLED_BUTTON_COLOR
        shapes.rect(startBounds.x, startBounds.y, startBounds.width, startBounds.height)
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

        font.color = if (view.isStartEnabled) ENABLED_LABEL_COLOR else DISABLED_LABEL_COLOR
        labelLayout.setText(font, view.startLabel)
        font.draw(
            batch,
            view.startLabel,
            startBounds.x + (startBounds.width - labelLayout.width) / 2f,
            startBounds.y + (startBounds.height + labelLayout.height) / 2f,
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

        val PANEL_COLOR = Color.valueOf("171B20")
        val ENABLED_BUTTON_COLOR = Color.valueOf("3A9D5D")
        val DISABLED_BUTTON_COLOR = Color.valueOf("3F454D")
        val PRIMARY_TEXT_COLOR = Color.valueOf("F2F2F2")
        val SECONDARY_TEXT_COLOR = Color.valueOf("B8C0CC")
        val ENABLED_LABEL_COLOR = Color.WHITE
        val DISABLED_LABEL_COLOR = Color.valueOf("AAB0B8")
    }
}
