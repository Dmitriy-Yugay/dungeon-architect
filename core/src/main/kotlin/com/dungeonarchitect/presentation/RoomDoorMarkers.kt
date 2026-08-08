package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom

internal fun roomDoorGridPositions(
    placedRooms: Iterable<PlacedRoom>,
): Set<GridPosition> = buildSet {
    placedRooms.forEach { room ->
        room.blueprint.doorPositions.forEach { localDoorPosition ->
            add(room.toGridPosition(localDoorPosition))
        }
    }
}
