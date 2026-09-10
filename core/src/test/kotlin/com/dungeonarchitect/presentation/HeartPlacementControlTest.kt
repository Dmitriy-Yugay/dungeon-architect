package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeartPlacementControlTest {
    @Test
    fun `view is enabled only while building and reflects active mode there`() {
        PrototypeRunPhase.entries.forEach { phase ->
            val buildState = buildState()
            buildState.toggleHeartPlacementMode()

            val view = HeartPlacementControlView.from(buildState, phase)

            assertEquals("PLACE HEART", view.label)
            assertEquals(
                phase == PrototypeRunPhase.DEFENSE_PREPARATION,
                view.isEnabled,
                phase.name,
            )
            assertEquals(
                phase == PrototypeRunPhase.DEFENSE_PREPARATION,
                view.isActive,
                phase.name,
            )
        }
    }

    @Test
    fun `heart and existing controls fit without overlap at 1024 width`() {
        val buildState = buildState()
        val roomChoicesView = RoomChoicesView.from(buildState)
        val heart = HeartPlacementLayout.control(
            view = HeartPlacementControlView.from(
                buildState,
                PrototypeRunPhase.DEFENSE_PREPARATION,
            ),
            worldWidth = WORLD_WIDTH,
            panelBottom = PANEL_BOTTOM,
        )
        val roomControls = RoomChoicesLayout.controls(
            roomChoicesView,
            WORLD_WIDTH,
            PANEL_BOTTOM,
        )
        val rotationControls = RoomRotationLayout.controls(
            RoomRotationView.from(buildState, PrototypeRunPhase.DEFENSE_PREPARATION),
            roomChoicesView,
            WORLD_WIDTH,
            PANEL_BOTTOM,
        )
        val cancel = WavePanelLayout.cancelButtonBounds(WORLD_WIDTH, PANEL_BOTTOM)
        val start = WavePanelLayout.startButtonBounds(WORLD_WIDTH, PANEL_BOTTOM)
        val allBounds = rotationControls.map { it.bounds } +
            roomControls.map { it.bounds } + heart.bounds + cancel + start

        assertEquals(ControlBounds(448f, 592f, 112f, 48f), heart.bounds)
        assertTrue(rotationControls.last().bounds.right < heart.bounds.x)
        assertTrue(heart.bounds.right < cancel.x)
        assertTrue(cancel.right < start.x)
        assertTrue(allBounds.all { it.x >= 0f && it.right <= WORLD_WIDTH })
        allBounds.forEachIndexed { index, bounds ->
            assertFalse(
                allBounds.drop(index + 1).any { other ->
                    bounds.overlaps(other)
                },
                "Control at index $index overlaps another control.",
            )
        }
    }

    private fun buildState(): BuildState {
        val rooms = listOf("prototype", "gallery", "corner").map(::blueprint)
        return BuildState(
            availableRoomBlueprints = rooms,
            selectedRoomBlueprint = rooms.first(),
            selectedTrapDefinition = TrapDefinition(
                id = "trap",
                displayName = "Trap",
                damage = 1,
                cooldownSeconds = 1f,
                compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
            ),
        )
    }

    private fun blueprint(id: String) = RoomBlueprint(
        id = id,
        displayName = id,
        heartAnchor = GridPosition(0, 0),
        footprint = setOf(GridPosition(0, 0)),
        doorPositions = setOf(GridPosition(0, 0)),
    )

    private companion object {
        const val WORLD_WIDTH = 1_024f
        const val PANEL_BOTTOM = 576f
    }
}
