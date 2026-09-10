package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomAttachmentTarget
import com.dungeonarchitect.domain.RoomAttachmentTargetType
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomPlacementInvalidReason
import com.dungeonarchitect.domain.RoomPlacementPreview
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildModeGuidanceTest {
    @Test
    fun `room guidance exposes placement action and rotation shortcuts`() {
        assertEquals(
            "ROOM Original | click ghost | Q/E",
            buildModeGuidance(
                buildState = buildState(),
                phase = PrototypeRunPhase.DEFENSE_PREPARATION,
                roomPreview = validRoomPreview(),
                hasTrapPreview = false,
                activeAttachmentTarget = target(),
            ),
        )
    }

    @Test
    fun `invalid room guidance gives the domain reason`() {
        val room = PlacedRoom(blueprint(), GridPosition(1, 1))
        assertEquals(
            RoomPlacementInvalidReason.OVERLAPS_ROOM.displayDescription,
            buildModeGuidance(
                buildState = buildState(),
                phase = PrototypeRunPhase.DEFENSE_PREPARATION,
                roomPreview = RoomPlacementPreview(
                    room = room,
                    isValid = false,
                    invalidReason = RoomPlacementInvalidReason.OVERLAPS_ROOM,
                ),
                hasTrapPreview = false,
                activeAttachmentTarget = target(),
            ),
        )
    }

    @Test
    fun `heart and contextual trap modes have explicit feedback`() {
        val heartState = buildState().also(BuildState::toggleHeartPlacementMode)
        assertEquals(
            "HEART: click room",
            buildModeGuidance(
                heartState,
                PrototypeRunPhase.DEFENSE_PREPARATION,
                roomPreview = null,
                hasTrapPreview = false,
                activeAttachmentTarget = target(),
            ),
        )
        assertEquals(
            "TRAP: click socket",
            buildModeGuidance(
                buildState(),
                PrototypeRunPhase.DEFENSE_PREPARATION,
                roomPreview = null,
                hasTrapPreview = true,
                activeAttachmentTarget = target(),
            ),
        )
        assertEquals(
            "TRAP: need 1 more Gold",
            buildModeGuidance(
                buildState(),
                PrototypeRunPhase.DEFENSE_PREPARATION,
                roomPreview = null,
                hasTrapPreview = true,
                activeAttachmentTarget = target(),
                currentGold = 0,
                defenseCostGold = 1,
            ),
        )
    }

    private fun validRoomPreview() = RoomPlacementPreview(
        room = PlacedRoom(blueprint(), GridPosition(1, 1)),
        isValid = true,
    )

    private fun target() = RoomAttachmentTarget(
        position = GridPosition(0, 1),
        facing = CardinalDirection.EAST,
        type = RoomAttachmentTargetType.ENTRANCE,
    )

    private fun buildState(): BuildState {
        val room = blueprint()
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

    private fun blueprint() = RoomBlueprint(
        id = "room",
        displayName = "Room",
        footprint = setOf(GridPosition(0, 0)),
        heartAnchor = GridPosition(0, 0),
        doorPositions = setOf(GridPosition(0, 0)),
    )
}
