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
    val heartStatus: String,
    val buildGuidance: String,
    val controlLabel: String,
    val isControlEnabled: Boolean,
    val cancelLabel: String,
    val isCancelEnabled: Boolean,
) {
    companion object {
        fun from(
            wave: UpcomingHeroWave,
            phase: PrototypeRunPhase,
            heartHealth: Int,
            heartMaxHealth: Int,
            isStartEnabled: Boolean,
            isCancelEnabled: Boolean,
            buildGuidance: String = "",
        ) = WavePanelView(
            summary = when (phase) {
                PrototypeRunPhase.INTELLIGENCE,
                PrototypeRunPhase.ROOM_DRAFT,
                PrototypeRunPhase.DEFENSE_PREPARATION ->
                    "Upcoming wave: ${wave.count} x ${wave.heroDisplayName}"
                PrototypeRunPhase.COMBAT ->
                    "Wave in progress: ${wave.count} x ${wave.heroDisplayName}"
                PrototypeRunPhase.WAVE_REPORT -> "WAVE COMPLETE"
                PrototypeRunPhase.RUN_VICTORY -> "VICTORY - Heart secured"
                PrototypeRunPhase.RUN_DEFEAT -> "DEFEAT - Heart destroyed"
            },
            traitDescription = wave.traitDescription,
            heartStatus =
                "Heart health: $heartHealth / $heartMaxHealth",
            buildGuidance = buildGuidance,
            controlLabel = when (phase) {
                PrototypeRunPhase.INTELLIGENCE -> "INTELLIGENCE"
                PrototypeRunPhase.ROOM_DRAFT -> "ROOM DRAFT"
                PrototypeRunPhase.DEFENSE_PREPARATION ->
                    if (isStartEnabled) {
                        "START WAVE"
                    } else {
                        "START WAVE - ROUTE REQUIRED"
                    }
                PrototypeRunPhase.COMBAT -> "WAVE IN PROGRESS"
                PrototypeRunPhase.WAVE_REPORT -> "WAVE REPORT"
                PrototypeRunPhase.RUN_VICTORY,
                PrototypeRunPhase.RUN_DEFEAT,
                -> "RESTART"
            },
            isControlEnabled = isStartEnabled ||
                phase == PrototypeRunPhase.RUN_VICTORY ||
                phase == PrototypeRunPhase.RUN_DEFEAT,
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

    val right: Float
        get() = x + width

    val top: Float
        get() = y + height

    fun overlaps(other: ControlBounds): Boolean =
        x < other.right && right > other.x &&
            y < other.top && top > other.y
}

data class BottomPanelLayout(
    val panelBounds: ControlBounds,
    val statusRegion: ControlBounds,
    val waveStatusSafeArea: ControlBounds,
    val heartStatusSafeArea: ControlBounds,
    val controlsRegion: ControlBounds,
    val roomChoiceBounds: List<ControlBounds>,
    val roomRotationBounds: List<ControlBounds>,
    val heartPlacementBounds: ControlBounds,
    val cancelBounds: ControlBounds,
    val startBounds: ControlBounds,
)

object WavePanelLayout {
    const val HEIGHT = 128f

    internal fun create(
        worldWidth: Float,
        panelBottom: Float,
        roomChoiceCount: Int,
        roomRotationControlCount: Int,
    ): BottomPanelLayout {
        require(worldWidth > HORIZONTAL_PADDING * 2f) {
            "Panel width must leave room for horizontal padding."
        }
        require(roomChoiceCount >= 0) { "Room choice count cannot be negative." }
        require(roomRotationControlCount >= 0) {
            "Room rotation control count cannot be negative."
        }

        val controlsRegion = ControlBounds(
            x = HORIZONTAL_PADDING,
            y = panelBottom + ROW_PADDING,
            width = worldWidth - HORIZONTAL_PADDING * 2f,
            height = CONTROL_HEIGHT,
        )
        val statusRegion = ControlBounds(
            x = HORIZONTAL_PADDING,
            y = controlsRegion.y + controlsRegion.height + ROW_GAP,
            width = controlsRegion.width,
            height = STATUS_HEIGHT,
        )
        val heartStatusSafeArea = ControlBounds(
            x = statusRegion.x + statusRegion.width - HEART_STATUS_WIDTH,
            y = statusRegion.y,
            width = HEART_STATUS_WIDTH,
            height = statusRegion.height,
        )
        val waveStatusSafeArea = ControlBounds(
            x = statusRegion.x,
            y = statusRegion.y,
            width = heartStatusSafeArea.x - STATUS_GAP - statusRegion.x,
            height = statusRegion.height,
        )

        val roomChoiceBounds = List(roomChoiceCount) { index ->
            ControlBounds(
                x = controlsRegion.x + index * (ROOM_CHOICE_WIDTH + CONTROL_GAP),
                y = controlsRegion.y,
                width = ROOM_CHOICE_WIDTH,
                height = controlsRegion.height,
            )
        }
        val rotationGroupLeft = roomChoiceBounds.lastOrNull()
            ?.let { it.right + SECTION_GAP }
            ?: controlsRegion.x
        val roomRotationBounds = List(roomRotationControlCount) { index ->
            ControlBounds(
                x = rotationGroupLeft + index * (ROTATION_CONTROL_SIZE + CONTROL_GAP),
                y = controlsRegion.y,
                width = ROTATION_CONTROL_SIZE,
                height = controlsRegion.height,
            )
        }
        val heartControlLeft = roomRotationBounds.lastOrNull()
            ?.let { it.right + SECTION_GAP }
            ?: roomChoiceBounds.lastOrNull()
                ?.let { it.right + SECTION_GAP }
            ?: controlsRegion.x
        val heartPlacementBounds = ControlBounds(
            x = heartControlLeft,
            y = controlsRegion.y,
            width = HEART_CONTROL_WIDTH,
            height = controlsRegion.height,
        )

        val startBounds = ControlBounds(
            x = controlsRegion.x + controlsRegion.width - START_BUTTON_WIDTH,
            y = controlsRegion.y,
            width = START_BUTTON_WIDTH,
            height = controlsRegion.height,
        )
        val cancelBounds = ControlBounds(
            x = startBounds.x - SECTION_GAP - CANCEL_BUTTON_WIDTH,
            y = controlsRegion.y,
            width = CANCEL_BUTTON_WIDTH,
            height = controlsRegion.height,
        )
        require(heartPlacementBounds.right + SECTION_GAP <= cancelBounds.x) {
            "Build and run controls do not fit in a $worldWidth-unit panel."
        }

        return BottomPanelLayout(
            panelBounds = ControlBounds(0f, panelBottom, worldWidth, HEIGHT),
            statusRegion = statusRegion,
            waveStatusSafeArea = waveStatusSafeArea,
            heartStatusSafeArea = heartStatusSafeArea,
            controlsRegion = controlsRegion,
            roomChoiceBounds = roomChoiceBounds,
            roomRotationBounds = roomRotationBounds,
            heartPlacementBounds = heartPlacementBounds,
            cancelBounds = cancelBounds,
            startBounds = startBounds,
        )
    }

    fun startButtonBounds(
        worldWidth: Float,
        panelBottom: Float,
    ) = defaultLayout(worldWidth, panelBottom).startBounds

    fun cancelButtonBounds(
        worldWidth: Float,
        panelBottom: Float,
    ): ControlBounds = defaultLayout(worldWidth, panelBottom).cancelBounds

    internal fun defaultLayout(
        worldWidth: Float,
        panelBottom: Float,
    ) = create(
        worldWidth = worldWidth,
        panelBottom = panelBottom,
        roomChoiceCount = DEFAULT_ROOM_CHOICE_COUNT,
        roomRotationControlCount = DEFAULT_ROTATION_CONTROL_COUNT,
    )

    internal const val HORIZONTAL_PADDING = 16f
    private const val ROW_PADDING = 16f
    private const val ROW_GAP = 8f
    private const val STATUS_HEIGHT = 48f
    private const val HEART_STATUS_WIDTH = 224f
    private const val STATUS_GAP = 16f
    private const val CONTROL_HEIGHT = 48f
    private const val ROOM_CHOICE_WIDTH = 96f
    private const val ROTATION_CONTROL_SIZE = 48f
    private const val HEART_CONTROL_WIDTH = 112f
    private const val START_BUTTON_WIDTH = 224f
    private const val CANCEL_BUTTON_WIDTH = 96f
    private const val CONTROL_GAP = 8f
    private const val SECTION_GAP = 12f
    private const val DEFAULT_ROOM_CHOICE_COUNT = 3
    private const val DEFAULT_ROTATION_CONTROL_COUNT = 2
}

class WavePanelRenderer : Disposable {
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val labelLayout = GlyphLayout()

    fun render(
        view: WavePanelView,
        projection: Matrix4,
        layout: BottomPanelLayout,
    ) {
        val startBounds = layout.startBounds
        val cancelBounds = layout.cancelBounds

        shapes.projectionMatrix = projection
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = PANEL_COLOR
        shapes.rect(
            layout.panelBounds.x,
            layout.panelBounds.y,
            layout.panelBounds.width,
            layout.panelBounds.height,
        )
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
            layout.waveStatusSafeArea.x,
            layout.waveStatusSafeArea.y + SUMMARY_BASELINE_OFFSET,
        )
        font.color = SECONDARY_TEXT_COLOR
        font.draw(
            batch,
            view.traitDescription,
            layout.waveStatusSafeArea.x,
            layout.waveStatusSafeArea.y + DESCRIPTION_BASELINE_OFFSET,
        )
        font.draw(
            batch,
            view.heartStatus,
            layout.heartStatusSafeArea.x,
            layout.heartStatusSafeArea.y + HEART_STATUS_BASELINE_OFFSET,
        )
        font.draw(
            batch,
            view.buildGuidance,
            layout.heartStatusSafeArea.x,
            layout.heartStatusSafeArea.y + GUIDANCE_BASELINE_OFFSET,
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
        const val SUMMARY_BASELINE_OFFSET = 40f
        const val DESCRIPTION_BASELINE_OFFSET = 16f
        const val HEART_STATUS_BASELINE_OFFSET = 40f
        const val GUIDANCE_BASELINE_OFFSET = 16f

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
