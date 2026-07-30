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
)
