package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomSocketType

internal data class RoomSocketGridMarker(
    val position: GridPosition,
    val type: RoomSocketType,
)

internal fun roomSocketGridMarkers(
    placedRooms: Iterable<PlacedRoom>,
): Set<RoomSocketGridMarker> = buildSet {
    placedRooms.forEach { room ->
        room.blueprint.sockets.forEach { (localPosition, type) ->
            add(
                RoomSocketGridMarker(
                    position = room.toGridPosition(localPosition),
                    type = type,
                ),
            )
        }
    }
}
