package com.dungeonarchitect.domain

class TrapDefinition(
    val id: String,
    val displayName: String,
    compatibleSocketTypes: Set<RoomSocketType>,
) {
    val compatibleSocketTypes: Set<RoomSocketType> =
        compatibleSocketTypes.toSet()

    init {
        require(id.isNotBlank()) {
            "A trap definition must provide an id."
        }
        require(displayName.isNotBlank()) {
            "A trap definition must provide a display name."
        }
        require(this.compatibleSocketTypes.isNotEmpty()) {
            "A trap definition must support at least one room socket type."
        }
    }

    fun isCompatibleWith(socketType: RoomSocketType): Boolean =
        socketType in compatibleSocketTypes
}
