package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition

internal data class RoomDoorGridMarker(
    val position: GridPosition,
    val facing: CardinalDirection,
    val isOpen: Boolean,
)

internal fun roomDoorGridMarkers(grid: DungeonGrid): Set<RoomDoorGridMarker> {
    val openDoors = grid.openRoomDoors
    return grid.placedRooms.flatMapTo(mutableSetOf()) { room ->
        room.doors.map { door ->
            RoomDoorGridMarker(
                position = door.gridPosition,
                facing = door.door.facing,
                isOpen = door in openDoors,
            )
        }
    }
}
