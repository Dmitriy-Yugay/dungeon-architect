package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.PrototypeRunPhase
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
                heroHealth = 10,
                heartDamage = 10,
                movementSpeedTilesPerSecond = 2f,
                traitDescription = "A straightforward melee fighter.",
            ),
            phase = PrototypeRunPhase.DEFENSE_PREPARATION,
            heartHealth = 10,
            heartMaxHealth = 10,
            isStartEnabled = true,
            isCancelEnabled = true,
        )

        assertEquals("Upcoming wave: 4 x Militia Recruit", view.summary)
        assertEquals("A straightforward melee fighter.", view.traitDescription)
        assertEquals("Heart health: 10 / 10", view.heartStatus)
        assertEquals("START WAVE", view.controlLabel)
        assertTrue(view.isControlEnabled)
        assertEquals("CANCEL", view.cancelLabel)
        assertTrue(view.isCancelEnabled)
    }

    @Test
    fun `panel explains why start is disabled before a route exists`() {
        val view = WavePanelView.from(
            wave = UpcomingHeroWave(
                heroType = "militia_recruit",
                heroDisplayName = "Militia Recruit",
                count = 4,
                heroHealth = 10,
                heartDamage = 10,
                movementSpeedTilesPerSecond = 2f,
                traitDescription = "A straightforward melee fighter.",
            ),
            phase = PrototypeRunPhase.DEFENSE_PREPARATION,
            heartHealth = 10,
            heartMaxHealth = 10,
            isStartEnabled = false,
            isCancelEnabled = false,
        )

        assertEquals("START WAVE - ROUTE REQUIRED", view.controlLabel)
        assertFalse(view.isControlEnabled)
        assertEquals("CANCEL", view.cancelLabel)
        assertFalse(view.isCancelEnabled)
    }

    @Test
    fun `panel gives clear terminal feedback and enables restart`() {
        val victory = view(PrototypeRunPhase.RUN_VICTORY, heartHealth = 10)
        val defeat = view(PrototypeRunPhase.RUN_DEFEAT, heartHealth = 0)

        assertEquals("VICTORY - Heart secured", victory.summary)
        assertEquals("Heart health: 10 / 10", victory.heartStatus)
        assertEquals("RESTART", victory.controlLabel)
        assertTrue(victory.isControlEnabled)
        assertFalse(victory.isCancelEnabled)

        assertEquals("DEFEAT - Heart destroyed", defeat.summary)
        assertEquals("Heart health: 0 / 10", defeat.heartStatus)
        assertEquals("RESTART", defeat.controlLabel)
        assertTrue(defeat.isControlEnabled)
        assertFalse(defeat.isCancelEnabled)
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

    @Test
    fun `cancel button sits left of start without overlap`() {
        val cancel = WavePanelLayout.cancelButtonBounds(
            worldWidth = 1_024f,
            panelBottom = 576f,
        )
        val start = WavePanelLayout.startButtonBounds(
            worldWidth = 1_024f,
            panelBottom = 576f,
        )

        assertEquals(676f, cancel.x)
        assertEquals(592f, cancel.y)
        assertEquals(96f, cancel.width)
        assertEquals(48f, cancel.height)
        assertTrue(cancel.x + cancel.width < start.x)
    }

    private fun view(
        phase: PrototypeRunPhase,
        heartHealth: Int,
    ) = WavePanelView.from(
        wave = UpcomingHeroWave(
            heroType = "militia_recruit",
            heroDisplayName = "Militia Recruit",
            count = 4,
            heroHealth = 10,
            heartDamage = 10,
            movementSpeedTilesPerSecond = 2f,
            traitDescription = "A straightforward melee fighter.",
        ),
        phase = phase,
        heartHealth = heartHealth,
        heartMaxHealth = 10,
        isStartEnabled = false,
        isCancelEnabled = false,
    )
}
