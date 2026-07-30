package com.dungeonarchitect.domain

class StartedHeroWave(
    val wave: UpcomingHeroWave,
    route: List<GridPosition>,
) {
    val route: List<GridPosition> = route.toList()

    init {
        require(this.route.isNotEmpty()) {
            "A started hero wave must have a route."
        }
    }
}
