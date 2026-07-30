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
                objectiveDamage = 10,
                movementSpeedTilesPerSecond = 2f,
                traitDescription = "A straightforward melee fighter.",
            ),
            phase = PrototypeRunPhase.BUILDING,
            objectiveHealth = 10,
            objectiveMaxHealth = 10,
            isStartEnabled = true,
        )

        assertEquals("Upcoming wave: 4 x Militia Recruit", view.summary)
        assertEquals("A straightforward melee fighter.", view.traitDescription)
        assertEquals("Objective health: 10 / 10", view.objectiveStatus)
        assertEquals("START WAVE", view.controlLabel)
        assertTrue(view.isControlEnabled)
    }

    @Test
    fun `panel explains why start is disabled before a route exists`() {
        val view = WavePanelView.from(
            wave = UpcomingHeroWave(
                heroType = "militia_recruit",
                heroDisplayName = "Militia Recruit",
                count = 4,
                heroHealth = 10,
                objectiveDamage = 10,
                movementSpeedTilesPerSecond = 2f,
                traitDescription = "A straightforward melee fighter.",
            ),
            phase = PrototypeRunPhase.BUILDING,
            objectiveHealth = 10,
            objectiveMaxHealth = 10,
            isStartEnabled = false,
        )

        assertEquals("START WAVE - ROUTE REQUIRED", view.controlLabel)
        assertFalse(view.isControlEnabled)
    }

    @Test
    fun `panel gives clear terminal feedback and enables restart`() {
        val victory = view(PrototypeRunPhase.VICTORY, objectiveHealth = 10)
        val defeat = view(PrototypeRunPhase.DEFEAT, objectiveHealth = 0)

        assertEquals("VICTORY - Objective secured", victory.summary)
        assertEquals("Objective health: 10 / 10", victory.objectiveStatus)
        assertEquals("RESTART", victory.controlLabel)
        assertTrue(victory.isControlEnabled)

        assertEquals("DEFEAT - Objective destroyed", defeat.summary)
        assertEquals("Objective health: 0 / 10", defeat.objectiveStatus)
        assertEquals("RESTART", defeat.controlLabel)
        assertTrue(defeat.isControlEnabled)
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

    private fun view(
        phase: PrototypeRunPhase,
        objectiveHealth: Int,
    ) = WavePanelView.from(
        wave = UpcomingHeroWave(
            heroType = "militia_recruit",
            heroDisplayName = "Militia Recruit",
            count = 4,
            heroHealth = 10,
            objectiveDamage = 10,
            movementSpeedTilesPerSecond = 2f,
            traitDescription = "A straightforward melee fighter.",
        ),
        phase = phase,
        objectiveHealth = objectiveHealth,
        objectiveMaxHealth = 10,
        isStartEnabled = false,
    )
}
