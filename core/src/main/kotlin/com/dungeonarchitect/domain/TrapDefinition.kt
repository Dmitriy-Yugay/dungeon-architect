package com.dungeonarchitect.domain

class TrapDefinition(
    val id: String,
    val displayName: String,
    val damage: Int,
    val cooldownSeconds: Float,
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
        require(damage > 0) {
            "A trap definition's damage must be positive."
        }
        require(cooldownSeconds.isFinite() && cooldownSeconds > 0f) {
            "A trap definition's cooldown must be finite and positive."
        }
        require(this.compatibleSocketTypes.isNotEmpty()) {
            "A trap definition must support at least one room socket type."
        }
    }

    fun isCompatibleWith(socketType: RoomSocketType): Boolean =
        socketType in compatibleSocketTypes
}
