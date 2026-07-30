package com.dungeonarchitect.domain

data class PlacedTrap(
    val definition: TrapDefinition,
    val room: PlacedRoom,
    val localSocketPosition: GridPosition,
) {
    val gridPosition: GridPosition = room.toGridPosition(localSocketPosition)

    init {
        val socketType = requireNotNull(
            room.blueprint.sockets[localSocketPosition],
        ) {
            "A placed trap must occupy a declared room socket."
        }
        require(definition.isCompatibleWith(socketType)) {
            "A placed trap must be compatible with its room socket."
        }
    }
}
