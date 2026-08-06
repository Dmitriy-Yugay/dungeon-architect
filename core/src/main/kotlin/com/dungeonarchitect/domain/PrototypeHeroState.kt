package com.dungeonarchitect.domain

data class HeroGridPosition(
    val column: Float,
    val row: Float,
) {
    init {
        require(column.isFinite() && row.isFinite()) {
            "A hero grid position must contain finite coordinates."
        }
    }
}

data class PrototypeHeroState(
    val position: HeroGridPosition,
    val hasArrived: Boolean,
    val health: Int,
) {
    init {
        require(health >= 0) {
            "A prototype hero's health must not be negative."
        }
        require(!hasArrived || health > 0) {
            "A dead prototype hero cannot have arrived."
        }
    }

    val isDead: Boolean
        get() = health == 0
}
