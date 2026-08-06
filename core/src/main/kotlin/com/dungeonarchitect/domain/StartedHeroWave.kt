package com.dungeonarchitect.domain

class StartedHeroWave(
    val wave: UpcomingHeroWave,
    route: List<GridPosition>,
    traps: List<PlacedTrap> = emptyList(),
) {
    val route: List<GridPosition> = route.toList()
    val traps: List<PlacedTrap> = traps.toList()

    init {
        require(this.route.isNotEmpty()) {
            "A started hero wave must have a route."
        }
    }
}
