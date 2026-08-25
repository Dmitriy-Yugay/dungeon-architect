package com.dungeonarchitect.domain

data class RoomPlacementPreview(
    val room: PlacedRoom,
    val isValid: Boolean,
    val attachmentTarget: RoomAttachmentTarget? = null,
    val connectingDoor: PlacedRoomDoor? = null,
    val invalidReason: RoomPlacementInvalidReason? = null,
)
