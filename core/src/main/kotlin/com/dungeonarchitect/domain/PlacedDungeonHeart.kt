package com.dungeonarchitect.domain

data class PlacedDungeonHeart(
    val room: PlacedRoom,
) {
    val localAnchorPosition: GridPosition = room.geometry.heartAnchor
    val gridPosition: GridPosition = room.toGridPosition(localAnchorPosition)
}
