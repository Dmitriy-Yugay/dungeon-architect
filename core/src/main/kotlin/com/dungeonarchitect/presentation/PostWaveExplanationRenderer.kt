package com.dungeonarchitect.presentation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import kotlin.math.min

data class PostWaveExplanationLayout(
    val panelBounds: ControlBounds,
    val contentBounds: ControlBounds,
) {
    companion object {
        fun centered(
            worldWidth: Float,
            worldHeight: Float,
        ): PostWaveExplanationLayout {
            require(worldWidth > OUTER_MARGIN * 2f + CONTENT_PADDING * 2f) {
                "Post-wave world width must leave room for margins."
            }
            require(worldHeight > OUTER_MARGIN * 2f + CONTENT_PADDING * 2f) {
                "Post-wave world height must leave room for margins."
            }
            val panelWidth = min(MAX_PANEL_WIDTH, worldWidth - OUTER_MARGIN * 2f)
            val panelHeight = min(PANEL_HEIGHT, worldHeight - OUTER_MARGIN * 2f)
            val panelBounds = ControlBounds(
                x = (worldWidth - panelWidth) / 2f,
                y = (worldHeight - panelHeight) / 2f,
                width = panelWidth,
                height = panelHeight,
            )
            return PostWaveExplanationLayout(
                panelBounds = panelBounds,
                contentBounds = ControlBounds(
                    x = panelBounds.x + CONTENT_PADDING,
                    y = panelBounds.y + CONTENT_PADDING,
                    width = panelBounds.width - CONTENT_PADDING * 2f,
                    height = panelBounds.height - CONTENT_PADDING * 2f,
                ),
            )
        }

        private const val OUTER_MARGIN = 32f
        private const val CONTENT_PADDING = 24f
        private const val MAX_PANEL_WIDTH = 720f
        private const val PANEL_HEIGHT = 264f
    }
}

class PostWaveExplanationRenderer : Disposable {
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()

    fun render(
        view: PostWaveExplanationView,
        projection: Matrix4,
        worldWidth: Float,
        worldHeight: Float,
    ) {
        val layout = PostWaveExplanationLayout.centered(worldWidth, worldHeight)
        shapes.projectionMatrix = projection
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = SCRIM_COLOR
        shapes.rect(0f, 0f, worldWidth, worldHeight)
        shapes.color = PANEL_COLOR
        shapes.rect(
            layout.panelBounds.x,
            layout.panelBounds.y,
            layout.panelBounds.width,
            layout.panelBounds.height,
        )
        shapes.end()

        val content = layout.contentBounds
        val top = content.top
        batch.projectionMatrix = projection
        batch.begin()
        font.color = HEADLINE_COLOR
        font.draw(
            batch,
            view.headline,
            content.x,
            top,
            content.width,
            Align.center,
            false,
        )
        font.color = EXPLANATION_COLOR
        font.draw(
            batch,
            view.explanation,
            content.x,
            top - EXPLANATION_TOP_OFFSET,
            content.width,
            Align.left,
            true,
        )
        font.color = METRIC_COLOR
        listOf(
            view.heroResultLabel,
            view.heartLabel,
            view.trapResultLabel,
            view.elapsedTimeLabel,
        ).forEachIndexed { index, label ->
            font.draw(
                batch,
                label,
                content.x,
                top - METRICS_TOP_OFFSET - index * METRIC_LINE_HEIGHT,
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
        const val EXPLANATION_TOP_OFFSET = 40f
        const val METRICS_TOP_OFFSET = 104f
        const val METRIC_LINE_HEIGHT = 28f

        val SCRIM_COLOR = Color.valueOf("101318")
        val PANEL_COLOR = Color.valueOf("20262E")
        val HEADLINE_COLOR = Color.valueOf("F2F2F2")
        val EXPLANATION_COLOR = Color.valueOf("D4D9E0")
        val METRIC_COLOR = Color.valueOf("B8C0CC")
    }
}
