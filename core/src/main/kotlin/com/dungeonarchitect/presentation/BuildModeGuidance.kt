package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomAttachmentTarget
import com.dungeonarchitect.domain.RoomPlacementPreview

internal fun buildModeGuidance(
    buildState: BuildState,
    phase: PrototypeRunPhase,
    roomPreview: RoomPlacementPreview?,
    hasTrapPreview: Boolean,
    activeAttachmentTarget: RoomAttachmentTarget?,
): String = when {
    phase != PrototypeRunPhase.BUILDING -> ""
    buildState.isHeartPlacementModeActive -> "HEART: click room"
    hasTrapPreview -> "TRAP: click socket"
    activeAttachmentTarget == null -> "ROOM: choose gold attachment"
    roomPreview?.isValid == false ->
        roomPreview.invalidReason?.displayDescription ?: "ROOM: placement blocked"
    else ->
        "ROOM ${buildState.selectedRoomOrientation.plainLabel} | click ghost | Q/E"
}
