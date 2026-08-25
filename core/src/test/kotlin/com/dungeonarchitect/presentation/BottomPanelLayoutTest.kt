package com.dungeonarchitect.presentation

import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomPanelLayoutTest {
    @Test
    fun `status safe areas occupy a separate row above every control`() {
        val layout = defaultLayout()
        val controlBounds = layout.roomChoiceBounds +
            layout.roomRotationBounds +
            layout.heartPlacementBounds +
            layout.cancelBounds +
            layout.startBounds

        assertEquals(ControlBounds(0f, 576f, 1_024f, 128f), layout.panelBounds)
        assertEquals(ControlBounds(16f, 648f, 992f, 48f), layout.statusRegion)
        assertEquals(ControlBounds(16f, 648f, 752f, 48f), layout.waveStatusSafeArea)
        assertEquals(ControlBounds(784f, 648f, 224f, 48f), layout.heartStatusSafeArea)
        assertEquals(ControlBounds(16f, 592f, 992f, 48f), layout.controlsRegion)
        assertFalse(layout.waveStatusSafeArea.overlaps(layout.heartStatusSafeArea))
        controlBounds.forEach { control ->
            assertFalse(layout.waveStatusSafeArea.overlaps(control))
            assertFalse(layout.heartStatusSafeArea.overlaps(control))
        }
    }

    @Test
    fun `room placement and run groups fit without overlap at logical width`() {
        val layout = defaultLayout()
        val allControls = layout.roomChoiceBounds +
            layout.roomRotationBounds +
            layout.heartPlacementBounds +
            layout.cancelBounds +
            layout.startBounds

        assertEquals(listOf(16f, 120f, 224f), layout.roomChoiceBounds.map { it.x })
        assertEquals(listOf(332f, 388f), layout.roomRotationBounds.map { it.x })
        assertEquals(448f, layout.heartPlacementBounds.x)
        assertEquals(676f, layout.cancelBounds.x)
        assertEquals(784f, layout.startBounds.x)
        allControls.forEachIndexed { index, bounds ->
            assertTrue(bounds.x >= layout.controlsRegion.x)
            assertTrue(bounds.right <= layout.controlsRegion.right)
            assertFalse(
                allControls.drop(index + 1).any(bounds::overlaps),
                "Control at index $index overlaps another control.",
            )
        }
    }

    @Test
    fun `fit viewport projection preserves safe layout at default window size`() {
        val layout = defaultLayout()
        val scale = min(
            DEFAULT_WINDOW_WIDTH / LOGICAL_WIDTH,
            DEFAULT_WINDOW_HEIGHT / LOGICAL_HEIGHT,
        )
        val horizontalGutter = (DEFAULT_WINDOW_WIDTH - LOGICAL_WIDTH * scale) / 2f
        val projectedStatus = layout.statusRegion.project(scale, horizontalGutter)
        val projectedControls = layout.controlsRegion.project(scale, horizontalGutter)

        assertFalse(projectedStatus.overlaps(projectedControls))
        assertTrue(projectedStatus.x >= 0f)
        assertTrue(projectedStatus.right <= DEFAULT_WINDOW_WIDTH)
        assertTrue(projectedStatus.top <= DEFAULT_WINDOW_HEIGHT)
        assertTrue(projectedControls.x >= 0f)
        assertTrue(projectedControls.right <= DEFAULT_WINDOW_WIDTH)
    }

    private fun defaultLayout() = WavePanelLayout.create(
        worldWidth = LOGICAL_WIDTH,
        panelBottom = 576f,
        roomChoiceCount = 3,
        roomRotationControlCount = 2,
    )

    private fun ControlBounds.project(
        scale: Float,
        horizontalGutter: Float,
    ) = ControlBounds(
        x = horizontalGutter + x * scale,
        y = y * scale,
        width = width * scale,
        height = height * scale,
    )

    private companion object {
        const val LOGICAL_WIDTH = 1_024f
        const val LOGICAL_HEIGHT = 704f
        const val DEFAULT_WINDOW_WIDTH = 1_280f
        const val DEFAULT_WINDOW_HEIGHT = 720f
    }
}
