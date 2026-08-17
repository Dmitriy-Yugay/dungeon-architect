package com.dungeonarchitect.domain

class RoomGeometry internal constructor(
    footprint: Set<GridPosition>,
    doors: Set<RoomDoor>,
    sockets: Map<GridPosition, RoomSocketType>,
) {
    val footprint: Set<GridPosition> = footprint.toSet()
    val doors: Set<RoomDoor> = doors.toSet()
    val doorPositions: Set<GridPosition> =
        this.doors.mapTo(mutableSetOf(), RoomDoor::position)
    val sockets: Map<GridPosition, RoomSocketType> = sockets.toMap()
}
