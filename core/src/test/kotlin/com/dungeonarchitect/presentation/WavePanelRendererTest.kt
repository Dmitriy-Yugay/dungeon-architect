package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.simulation.WaveOutcome
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
            gold = 2,
            defenseName = "Spike Trap",
            defenseCostGold = 1,
            upcomingRewardGold = 1,
            waveOutcome = null,
            isWaveRewardClaimed = false,
            isStartEnabled = true,
            isCancelEnabled = true,
        )

        assertEquals("Upcoming wave: 4 x Militia Recruit", view.summary)
        assertEquals("A straightforward melee fighter.", view.traitDescription)
        assertEquals("Heart: 10/10 | Gold: 2", view.runStatus)
        assertEquals("Spike Trap: 1G | Reward: +1G", view.economyStatus)
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
            gold = 0,
            defenseName = "Spike Trap",
            defenseCostGold = 1,
            upcomingRewardGold = 1,
            waveOutcome = null,
            isWaveRewardClaimed = false,
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
        assertEquals("Heart: 10/10 | Gold: 2", victory.runStatus)
        assertEquals("RESTART", victory.controlLabel)
        assertTrue(victory.isControlEnabled)
        assertFalse(victory.isCancelEnabled)

        assertEquals("DEFEAT - Heart destroyed", defeat.summary)
        assertEquals("Heart: 0/10 | Gold: 2", defeat.runStatus)
        assertEquals("RESTART", defeat.controlLabel)
        assertTrue(defeat.isControlEnabled)
        assertFalse(defeat.isCancelEnabled)
    }

    @Test
    fun `panel maps every cadence phase to an action and disabled reason`() {
        val intelligence = view(PrototypeRunPhase.INTELLIGENCE, 10)
        val draft = view(PrototypeRunPhase.ROOM_DRAFT, 10)
        val combat = view(PrototypeRunPhase.COMBAT, 10)
        val unclaimedReport = view(
            phase = PrototypeRunPhase.WAVE_REPORT,
            heartHealth = 10,
            waveOutcome = WaveOutcome.VICTORY,
        )
        val claimedReport = view(
            phase = PrototypeRunPhase.WAVE_REPORT,
            heartHealth = 10,
            waveOutcome = WaveOutcome.VICTORY,
            isWaveRewardClaimed = true,
        )
        val defeatReport = view(
            phase = PrototypeRunPhase.WAVE_REPORT,
            heartHealth = 0,
            waveOutcome = WaveOutcome.DEFEAT,
        )

        assertEquals("REVIEW ROOM OFFER", intelligence.controlLabel)
        assertTrue(intelligence.isControlEnabled)
        assertEquals("PLACE ONE OFFERED ROOM", draft.controlLabel)
        assertFalse(draft.isControlEnabled)
        assertEquals("WAIT - WAVE IN PROGRESS", combat.controlLabel)
        assertFalse(combat.isControlEnabled)
        assertEquals("CLAIM +1 GOLD", unclaimedReport.controlLabel)
        assertTrue(unclaimedReport.isControlEnabled)
        assertEquals("CONTINUE", claimedReport.controlLabel)
        assertEquals("END RUN", defeatReport.controlLabel)
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
        waveOutcome: WaveOutcome? = null,
        isWaveRewardClaimed: Boolean = false,
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
        gold = 2,
        defenseName = "Spike Trap",
        defenseCostGold = 1,
        upcomingRewardGold = 1,
        waveOutcome = waveOutcome,
        isWaveRewardClaimed = isWaveRewardClaimed,
        isStartEnabled = false,
        isCancelEnabled = false,
    )
}
