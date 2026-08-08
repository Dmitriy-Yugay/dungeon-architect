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
    fun `layout creates two ordered controls left of the start button`() {
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
                        x = 440f,
                        y = 592f,
                        width = 144f,
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
                        x = 592f,
                        y = 592f,
                        width = 144f,
                        height = 48f,
                    ),
                ),
            ),
            controls,
        )
        assertEquals(
            752f,
            WavePanelLayout.startButtonBounds(1_024f, 576f).x,
        )
    }

    private fun blueprint(
        id: String,
        displayName: String,
    ) = RoomBlueprint(
        id = id,
        displayName = displayName,
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
