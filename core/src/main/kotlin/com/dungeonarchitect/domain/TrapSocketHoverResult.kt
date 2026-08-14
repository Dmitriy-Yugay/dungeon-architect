package com.dungeonarchitect.domain

sealed interface TrapSocketHoverResult {
    data class Valid(
        val room: PlacedRoom,
        val localSocketPosition: GridPosition,
    ) : TrapSocketHoverResult

    data class Occupied(
        val placedTrap: PlacedTrap,
    ) : TrapSocketHoverResult

    data class Incompatible(
        val socketType: RoomSocketType,
    ) : TrapSocketHoverResult

    data object NonSocket : TrapSocketHoverResult
}
