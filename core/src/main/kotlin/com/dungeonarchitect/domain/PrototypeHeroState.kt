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
    val maxHealth: Int,
) {
    init {
        require(maxHealth > 0) {
            "A prototype hero's maximum health must be positive."
        }
        require(health >= 0) {
            "A prototype hero's health must not be negative."
        }
        require(health <= maxHealth) {
            "A prototype hero's health must not exceed its maximum health."
        }
        require(!hasArrived || health > 0) {
            "A dead prototype hero cannot have arrived."
        }
    }

    val isDead: Boolean
        get() = health == 0
}
