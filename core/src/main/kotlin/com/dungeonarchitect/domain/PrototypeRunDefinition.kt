package com.dungeonarchitect.domain

data class PrototypeRunDefinition(
    val heartHealth: Int,
) {
    init {
        require(heartHealth > 0) {
            "The prototype heart's health must be positive."
        }
    }
}
