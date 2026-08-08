package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomChoicesViewTest {
    @Test
    fun `view maps available room choices in authored order`() {
        val squareRoom = blueprint(
            id = "square-room",
            displayName = "Square Room",
        )
        val longGallery = blueprint(
            id = "long-gallery",
            displayName = "Long Gallery",
        )
        val buildState = BuildState(
            availableRoomBlueprints = listOf(squareRoom, longGallery),
            selectedRoomBlueprint = squareRoom,
            selectedTrapDefinition = trapDefinition(),
        )

        val view = RoomChoicesView.from(buildState)

        assertEquals(
            listOf(
                RoomChoiceView(
                    id = "square-room",
                    displayName = "Square Room",
                    isSelected = true,
                ),
                RoomChoiceView(
                    id = "long-gallery",
                    displayName = "Long Gallery",
                    isSelected = false,
                ),
            ),
            view.choices,
        )
    }

    @Test
    fun `new view reflects selection changes without mutating an earlier view`() {
        val squareRoom = blueprint("square-room", "Square Room")
        val longGallery = blueprint("long-gallery", "Long Gallery")
        val buildState = BuildState(
            availableRoomBlueprints = listOf(squareRoom, longGallery),
            selectedRoomBlueprint = squareRoom,
            selectedTrapDefinition = trapDefinition(),
        )
        val beforeSelection = RoomChoicesView.from(buildState)

        assertTrue(buildState.selectRoomBlueprint("long-gallery"))
        val afterSelection = RoomChoicesView.from(buildState)

        assertEquals(listOf(true, false), beforeSelection.selectedStates)
        assertEquals(listOf(false, true), afterSelection.selectedStates)
    }

    private val RoomChoicesView.selectedStates: List<Boolean>
        get() = choices.map(RoomChoiceView::isSelected)

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
