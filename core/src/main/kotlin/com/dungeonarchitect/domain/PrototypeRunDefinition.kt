package com.dungeonarchitect.domain

data class PrototypeRunDefinition(
    val objectiveHealth: Int,
) {
    init {
        require(objectiveHealth > 0) {
            "The prototype objective's health must be positive."
        }
    }
}
