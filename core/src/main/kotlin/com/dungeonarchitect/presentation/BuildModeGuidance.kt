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
    currentGold: Int = Int.MAX_VALUE,
    defenseCostGold: Int = 0,
): String = when {
    phase != PrototypeRunPhase.DEFENSE_PREPARATION &&
        phase != PrototypeRunPhase.ROOM_DRAFT -> ""
    phase == PrototypeRunPhase.ROOM_DRAFT && activeAttachmentTarget == null ->
        "DRAFT: choose a highlighted attachment"
    phase == PrototypeRunPhase.ROOM_DRAFT && roomPreview?.isValid == false ->
        roomPreview.invalidReason?.displayDescription ?: "DRAFT: placement blocked"
    phase == PrototypeRunPhase.ROOM_DRAFT ->
        "DRAFT ${buildState.selectedRoomOrientation.plainLabel} | click ghost | Q/E"
    buildState.isHeartPlacementModeActive -> "HEART: click room"
    hasTrapPreview && currentGold < defenseCostGold ->
        "TRAP: need ${defenseCostGold - currentGold} more Gold"
    hasTrapPreview -> "TRAP: click socket"
    activeAttachmentTarget == null -> "ROOM: choose highlighted attachment"
    roomPreview?.isValid == false ->
        roomPreview.invalidReason?.displayDescription ?: "ROOM: placement blocked"
    else ->
        "ROOM ${buildState.selectedRoomOrientation.plainLabel} | click ghost | Q/E"
}
