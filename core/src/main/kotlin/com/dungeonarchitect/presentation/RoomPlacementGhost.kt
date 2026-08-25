package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.RoomAttachmentTarget
import com.dungeonarchitect.domain.RoomAttachmentTargetType
import com.dungeonarchitect.domain.RoomPlacementInvalidReason
import com.dungeonarchitect.domain.RoomPlacementPreview
import com.dungeonarchitect.domain.RoomSocketType

data class RoomAttachmentTargetMarker(
    val position: GridPosition,
    val facing: CardinalDirection,
    val isEntrance: Boolean,
    val isActive: Boolean,
)

internal data class GhostDoorMarker(
    val position: GridPosition,
    val facing: CardinalDirection,
    val isConnecting: Boolean,
)

internal data class RoomPlacementGhost(
    val footprint: Set<GridPosition>,
    val doors: Set<GhostDoorMarker>,
    val sockets: Map<GridPosition, RoomSocketType>,
    val heartAnchor: GridPosition,
    val isValid: Boolean,
    val invalidReason: String?,
)

internal fun roomAttachmentTargetMarkers(
    grid: DungeonGrid,
    activeTarget: RoomAttachmentTarget?,
): List<RoomAttachmentTargetMarker> = grid.roomAttachmentTargets.map { target ->
    RoomAttachmentTargetMarker(
        position = target.position,
        facing = target.facing,
        isEntrance = target.type == RoomAttachmentTargetType.ENTRANCE,
        isActive = target == activeTarget,
    )
}

internal fun roomAttachmentTargetAt(
    grid: DungeonGrid,
    worldX: Float,
    worldY: Float,
    tileSize: Float,
    clickRadius: Float,
): RoomAttachmentTarget? = grid.roomAttachmentTargets
    .map { target ->
        target to roomAttachmentMarkerCenter(
            position = target.position,
            facing = target.facing,
            isEntrance = target.type == RoomAttachmentTargetType.ENTRANCE,
            tileSize = tileSize,
        )
    }
    .filter { (_, center) ->
        center.squaredDistanceTo(worldX, worldY) <= clickRadius * clickRadius
    }
    .minByOrNull { (_, center) -> center.squaredDistanceTo(worldX, worldY) }
    ?.first

internal fun roomAttachmentMarkerCenter(
    marker: RoomAttachmentTargetMarker,
    tileSize: Float,
): Pair<Float, Float> = roomAttachmentMarkerCenter(
    position = marker.position,
    facing = marker.facing,
    isEntrance = marker.isEntrance,
    tileSize = tileSize,
)

private fun roomAttachmentMarkerCenter(
    position: GridPosition,
    facing: CardinalDirection,
    isEntrance: Boolean,
    tileSize: Float,
): Pair<Float, Float> {
    val centerX = (position.column + 0.5f) * tileSize
    val centerY = (position.row + 0.5f) * tileSize
    if (isEntrance) {
        return centerX to centerY
    }
    return when (facing) {
        CardinalDirection.WEST -> position.column * tileSize to centerY
        CardinalDirection.EAST -> (position.column + 1) * tileSize to centerY
        CardinalDirection.SOUTH -> centerX to position.row * tileSize
        CardinalDirection.NORTH -> centerX to (position.row + 1) * tileSize
    }
}

private fun Pair<Float, Float>.squaredDistanceTo(
    x: Float,
    y: Float,
): Float {
    val deltaX = x - first
    val deltaY = y - second
    return deltaX * deltaX + deltaY * deltaY
}

internal fun roomPlacementGhost(
    preview: RoomPlacementPreview,
): RoomPlacementGhost {
    val room = preview.room
    return RoomPlacementGhost(
        footprint = room.gridPositions,
        doors = room.doors.mapTo(mutableSetOf()) { door ->
            GhostDoorMarker(
                position = door.gridPosition,
                facing = door.door.facing,
                isConnecting = door == preview.connectingDoor,
            )
        },
        sockets = room.geometry.sockets.mapKeys { (localPosition) ->
            room.toGridPosition(localPosition)
        },
        heartAnchor = room.toGridPosition(room.geometry.heartAnchor),
        isValid = preview.isValid,
        invalidReason = preview.invalidReason?.displayDescription,
    )
}

internal val RoomPlacementInvalidReason.displayDescription: String
    get() = when (this) {
        RoomPlacementInvalidReason.DOOR_FACES_AWAY -> "Rotate: no door faces target"
        RoomPlacementInvalidReason.OUTSIDE_GRID -> "Room is outside the grid"
        RoomPlacementInvalidReason.COVERS_ENTRANCE -> "Room would cover entrance"
        RoomPlacementInvalidReason.OVERLAPS_ROOM -> "Room overlaps another room"
        RoomPlacementInvalidReason.INVALID_CONNECTION -> "Room makes extra connections"
        RoomPlacementInvalidReason.ENTRANCE_UNAVAILABLE ->
            "Entrance is already occupied"
    }
