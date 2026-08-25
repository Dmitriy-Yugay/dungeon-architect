package com.dungeonarchitect.domain

enum class RoomAttachmentTargetType {
    ENTRANCE,
    OPEN_DOOR,
}

data class RoomAttachmentTarget(
    val position: GridPosition,
    val facing: CardinalDirection,
    val type: RoomAttachmentTargetType,
) {
    val roomPosition: GridPosition
        get() = facing.move(position)
}

enum class RoomPlacementInvalidReason {
    DOOR_FACES_AWAY,
    OUTSIDE_GRID,
    COVERS_ENTRANCE,
    OVERLAPS_ROOM,
    INVALID_CONNECTION,
    ENTRANCE_UNAVAILABLE,
}
