package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.RoomDoor
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

        assertEquals(listOf("square-room", "long-gallery"), view.choices.map { it.id })
        assertEquals(listOf("Square Room", "Long Gallery"), view.choices.map { it.displayName })
        assertEquals(listOf(true, false), view.selectedStates)
        view.choices.forEach { choice ->
            assertEquals(setOf(GridPosition(0, 0)), choice.thumbnail.footprint)
            assertEquals(
                setOf(RoomDoor(GridPosition(0, 0), CardinalDirection.WEST)),
                choice.thumbnail.doors,
            )
            assertEquals(GridPosition(0, 0), choice.thumbnail.heartAnchor)
        }
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

    @Test
    fun `card thumbnail follows current room orientation`() {
        val room = RoomBlueprint(
            id = "gallery",
            displayName = "Gallery",
            footprint = setOf(GridPosition(0, 0), GridPosition(1, 0)),
            heartAnchor = GridPosition(1, 0),
            doors = listOf(
                RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                RoomDoor(GridPosition(1, 0), CardinalDirection.EAST),
            ),
            sockets = mapOf(GridPosition(1, 0) to RoomSocketType.FLOOR),
        )
        val buildState = BuildState(
            availableRoomBlueprints = listOf(room),
            selectedRoomBlueprint = room,
            selectedTrapDefinition = trapDefinition(),
        )
        buildState.rotateSelectedRoomClockwise()

        val thumbnail = RoomChoicesView.from(buildState).choices.single().thumbnail

        assertEquals(setOf(GridPosition(0, 0), GridPosition(0, 1)), thumbnail.footprint)
        assertEquals(GridPosition(0, 0), thumbnail.heartAnchor)
        assertEquals(mapOf(GridPosition(0, 0) to RoomSocketType.FLOOR), thumbnail.sockets)
        assertEquals(
            setOf(
                RoomDoor(GridPosition(0, 1), CardinalDirection.NORTH),
                RoomDoor(GridPosition(0, 0), CardinalDirection.SOUTH),
            ),
            thumbnail.doors,
        )
    }

    private val RoomChoicesView.selectedStates: List<Boolean>
        get() = choices.map(RoomChoiceView::isSelected)

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
