package com.dungeonarchitect.domain

data class RoomDoor(
    val position: GridPosition,
    val facing: CardinalDirection,
)

data class PlacedRoomDoor(
    val room: PlacedRoom,
    val door: RoomDoor,
) {
    val gridPosition: GridPosition = room.toGridPosition(door.position)
    val outsidePosition: GridPosition = door.facing.move(gridPosition)

    fun connectsTo(other: PlacedRoomDoor): Boolean =
        door.facing.opposite == other.door.facing &&
            outsidePosition == other.gridPosition &&
            other.outsidePosition == gridPosition
}

data class RoomDoorConnection(
    val first: PlacedRoomDoor,
    val second: PlacedRoomDoor,
) {
    init {
        require(first.connectsTo(second)) {
            "A room-door connection requires two facing adjacent doors."
        }
    }
}
