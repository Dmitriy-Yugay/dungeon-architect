package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BuildAttachmentSelectionTest {
    @Test
    fun `selection remains stable while available and falls forward when removed`() {
        val buildState = buildState()
        val entrance = target(0, RoomAttachmentTargetType.ENTRANCE)
        val firstDoor = target(2, RoomAttachmentTargetType.OPEN_DOOR)
        val secondDoor = target(4, RoomAttachmentTargetType.OPEN_DOOR)

        assertEquals(entrance, buildState.retainOrSelectRoomAttachmentTarget(listOf(entrance)))
        assertTrue(buildState.selectRoomAttachmentTarget(secondDoor))
        assertEquals(
            secondDoor,
            buildState.retainOrSelectRoomAttachmentTarget(listOf(firstDoor, secondDoor)),
        )
        assertFalse(buildState.selectRoomAttachmentTarget(secondDoor))
        assertEquals(firstDoor, buildState.retainOrSelectRoomAttachmentTarget(listOf(firstDoor)))
        assertNull(buildState.retainOrSelectRoomAttachmentTarget(emptyList()))
    }

    private fun target(
        column: Int,
        type: RoomAttachmentTargetType,
    ) = RoomAttachmentTarget(
        position = GridPosition(column, 0),
        facing = CardinalDirection.EAST,
        type = type,
    )

    private fun buildState(): BuildState {
        val room = RoomBlueprint(
            id = "room",
            displayName = "Room",
            footprint = setOf(GridPosition(0, 0)),
            heartAnchor = GridPosition(0, 0),
            doorPositions = setOf(GridPosition(0, 0)),
        )
        return BuildState(
            availableRoomBlueprints = listOf(room),
            selectedRoomBlueprint = room,
            selectedTrapDefinition = TrapDefinition(
                id = "trap",
                displayName = "Trap",
                damage = 1,
                cooldownSeconds = 1f,
                compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
            ),
        )
    }
}
