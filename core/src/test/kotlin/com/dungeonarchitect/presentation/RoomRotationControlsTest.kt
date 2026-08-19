package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomOrientation
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoomRotationControlsTest {
    @Test
    fun `view exposes selected orientation and ordered build controls`() {
        val buildState = buildState()
        buildState.rotateSelectedRoomClockwise()

        val view = RoomRotationView.from(
            buildState = buildState,
            phase = PrototypeRunPhase.BUILDING,
        )

        assertEquals("ROT 90", view.orientationLabel)
        assertEquals(
            listOf(
                RoomRotationControlView(
                    direction = RoomRotationDirection.COUNTER_CLOCKWISE,
                    label = "CCW",
                    isEnabled = true,
                ),
                RoomRotationControlView(
                    direction = RoomRotationDirection.CLOCKWISE,
                    label = "CW",
                    isEnabled = true,
                ),
            ),
            view.controls,
        )
    }

    @Test
    fun `rotation controls are enabled only while building`() {
        PrototypeRunPhase.entries.forEach { phase ->
            val view = RoomRotationView.from(buildState(), phase)

            view.controls.forEach { control ->
                assertEquals(
                    phase == PrototypeRunPhase.BUILDING,
                    control.isEnabled,
                    phase.name,
                )
            }
        }
    }

    @Test
    fun `compact rotation controls do not overlap room cancel or start controls`() {
        val roomChoicesView = RoomChoicesView.from(buildState())
        val rotationControls = RoomRotationLayout.controls(
            rotationView = RoomRotationView.from(
                buildState(),
                PrototypeRunPhase.BUILDING,
            ),
            roomChoicesView = roomChoicesView,
            worldWidth = WORLD_WIDTH,
            panelBottom = PANEL_BOTTOM,
        )
        val roomControls = RoomChoicesLayout.controls(
            roomChoicesView,
            WORLD_WIDTH,
            PANEL_BOTTOM,
        )
        val cancel = WavePanelLayout.cancelButtonBounds(WORLD_WIDTH, PANEL_BOTTOM)
        val start = WavePanelLayout.startButtonBounds(WORLD_WIDTH, PANEL_BOTTOM)

        assertEquals(listOf(8f, 64f), rotationControls.map { it.bounds.x })
        assertTrue(
            rotationControls.last().bounds.right < roomControls.first().bounds.x,
        )
        val heart = HeartPlacementLayout.bounds(WORLD_WIDTH, PANEL_BOTTOM)
        assertTrue(roomControls.last().bounds.right < heart.x)
        assertTrue(heart.right < cancel.x)
        assertTrue(cancel.right < start.x)
        assertFalse(rotationControls[0].bounds.overlaps(rotationControls[1].bounds))
        val allBounds = rotationControls.map { it.bounds } +
            roomControls.map { it.bounds } + listOf(heart, cancel, start)
        assertTrue(allBounds.all { bounds ->
            bounds.x >= 0f && bounds.right <= WORLD_WIDTH
        })
    }

    private val ControlBounds.right: Float
        get() = x + width

    private fun ControlBounds.overlaps(other: ControlBounds): Boolean =
        x < other.right && right > other.x &&
            y < other.y + other.height && y + height > other.y

    private fun buildState(): BuildState {
        val rooms = listOf(
            blueprint("square-room"),
            blueprint("long-gallery"),
            blueprint("corner-room"),
        )
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
        heartAnchor = GridPosition(column = 0, row = 0),
        footprint = setOf(GridPosition(0, 0)),
        doorPositions = setOf(GridPosition(0, 0)),
    )

    private companion object {
        const val WORLD_WIDTH = 1_024f
        const val PANEL_BOTTOM = 576f
    }
}
