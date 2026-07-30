package com.dungeonarchitect.domain

data class UpcomingHeroWave(
    val heroType: String,
    val heroDisplayName: String,
    val count: Int,
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
        require(traitDescription.isNotBlank()) {
            "A hero wave must describe the hero's important trait."
        }
    }
}
