package com.dungeonarchitect.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PostWaveExplanationRendererTest {
    @Test
    fun `layout centers a readable analysis card over the logical dungeon`() {
        val layout = PostWaveExplanationLayout.centered(
            worldWidth = 1_024f,
            worldHeight = 576f,
        )

        assertEquals(ControlBounds(152f, 156f, 720f, 264f), layout.panelBounds)
        assertEquals(ControlBounds(176f, 180f, 672f, 216f), layout.contentBounds)
        assertTrue(layout.panelBounds.x >= 0f)
        assertTrue(layout.panelBounds.right <= 1_024f)
        assertTrue(layout.panelBounds.top <= 576f)
    }

    @Test
    fun `layout contracts within a smaller render surface`() {
        val layout = PostWaveExplanationLayout.centered(
            worldWidth = 640f,
            worldHeight = 360f,
        )

        assertEquals(ControlBounds(32f, 48f, 576f, 264f), layout.panelBounds)
        assertEquals(ControlBounds(56f, 72f, 528f, 216f), layout.contentBounds)
    }

    @Test
    fun `layout rejects surfaces that cannot contain padded text`() {
        assertFailsWith<IllegalArgumentException> {
            PostWaveExplanationLayout.centered(100f, 576f)
        }
        assertFailsWith<IllegalArgumentException> {
            PostWaveExplanationLayout.centered(1_024f, 100f)
        }
    }
}
