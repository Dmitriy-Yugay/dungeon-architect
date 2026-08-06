package com.dungeonarchitect.domain

data class UpcomingHeroWave(
    val heroType: String,
    val heroDisplayName: String,
    val count: Int,
    val heroHealth: Int,
    val objectiveDamage: Int,
    val movementSpeedTilesPerSecond: Float,
    val traitDescription: String,
) {
    init {
        require(heroType.isNotBlank()) {
            "A hero wave must identify its hero type."
        }
        require(heroDisplayName.isNotBlank()) {
            "A hero wave must provide a hero display name."
        }
        require(count > 0) {
            "A hero wave must contain at least one hero."
        }
        require(heroHealth > 0) {
            "A hero wave's hero health must be positive."
        }
        require(objectiveDamage > 0) {
            "A hero wave's objective damage must be positive."
        }
        require(movementSpeedTilesPerSecond.isFinite() &&
            movementSpeedTilesPerSecond > 0f
        ) {
            "A hero wave's movement speed must be finite and positive."
        }
        require(traitDescription.isNotBlank()) {
            "A hero wave must describe the hero's important trait."
        }
    }
}
