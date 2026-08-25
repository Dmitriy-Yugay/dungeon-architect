package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomChoicesRendererTest {
    @Test
    fun `layout creates two ordered controls in the room selection group`() {
        val squareRoom = blueprint("square-room", "Square Room")
        val longGallery = blueprint("long-gallery", "Long Gallery")
        val view = RoomChoicesView.from(
            BuildState(
                availableRoomBlueprints = listOf(squareRoom, longGallery),
                selectedRoomBlueprint = squareRoom,
                selectedTrapDefinition = trapDefinition(),
            ),
        )

        val controls = RoomChoicesLayout.controls(
            view = view,
            worldWidth = 1_024f,
            panelBottom = 576f,
        )

        assertEquals(
            listOf(
                RoomChoiceControl(
                    choice = RoomChoiceView(
                        id = "square-room",
                        displayName = "Square Room",
                        isSelected = true,
                    ),
                    bounds = ControlBounds(
                        x = 16f,
                        y = 592f,
                        width = 96f,
                        height = 48f,
                    ),
                ),
                RoomChoiceControl(
                    choice = RoomChoiceView(
                        id = "long-gallery",
                        displayName = "Long Gallery",
                        isSelected = false,
                    ),
                    bounds = ControlBounds(
                        x = 120f,
                        y = 592f,
                        width = 96f,
                        height = 48f,
                    ),
                ),
            ),
            controls,
        )
        assertEquals(
            676f,
            WavePanelLayout.cancelButtonBounds(1_024f, 576f).x,
        )
    }

    private fun blueprint(
        id: String,
        displayName: String,
    ) = RoomBlueprint(
        id = id,
        displayName = displayName,
        heartAnchor = GridPosition(column = 0, row = 0),
        footprint = setOf(GridPosition(column = 0, row = 0)),
        doorPositions = setOf(GridPosition(column = 0, row = 0)),
    )

    private fun trapDefinition() = TrapDefinition(
        id = "spike_trap",
        displayName = "Spike Trap",
        damage = 5,
        cooldownSeconds = 0.25f,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
    )
}
