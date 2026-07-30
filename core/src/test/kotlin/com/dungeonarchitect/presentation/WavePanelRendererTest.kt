package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.UpcomingHeroWave
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WavePanelRendererTest {
    @Test
    fun `panel displays wave count name and trait description`() {
        val view = WavePanelView.from(
            wave = UpcomingHeroWave(
                heroType = "militia_recruit",
                heroDisplayName = "Militia Recruit",
                count = 4,
                movementSpeedTilesPerSecond = 2f,
                traitDescription = "A straightforward melee fighter.",
            ),
            isStartEnabled = true,
            hasStarted = false,
        )

        assertEquals("Upcoming wave: 4 x Militia Recruit", view.summary)
        assertEquals("A straightforward melee fighter.", view.traitDescription)
        assertEquals("START WAVE", view.startLabel)
        assertTrue(view.isStartEnabled)
    }

    @Test
    fun `panel explains why start is disabled before a route exists`() {
        val view = WavePanelView.from(
            wave = UpcomingHeroWave(
                heroType = "militia_recruit",
                heroDisplayName = "Militia Recruit",
                count = 4,
                movementSpeedTilesPerSecond = 2f,
                traitDescription = "A straightforward melee fighter.",
            ),
            isStartEnabled = false,
            hasStarted = false,
        )

        assertEquals("START WAVE - ROUTE REQUIRED", view.startLabel)
        assertFalse(view.isStartEnabled)
    }

    @Test
    fun `start button bounds include lower edge and exclude upper and right edges`() {
        val bounds = WavePanelLayout.startButtonBounds(
            worldWidth = 1_024f,
            panelBottom = 576f,
        )

        assertTrue(bounds.contains(bounds.x, bounds.y))
        assertTrue(
            bounds.contains(
                worldX = bounds.x + bounds.width / 2f,
                worldY = bounds.y + bounds.height / 2f,
            ),
        )
        assertFalse(bounds.contains(bounds.x + bounds.width, bounds.y))
        assertFalse(bounds.contains(bounds.x, bounds.y + bounds.height))
    }
}
