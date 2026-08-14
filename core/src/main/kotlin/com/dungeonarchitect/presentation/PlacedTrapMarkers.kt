package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition

internal data class PlacedTrapGridMarker(
    val position: GridPosition,
    val trapId: String,
    val displayName: String,
)

internal fun placedTrapGridMarkers(
    grid: DungeonGrid,
): List<PlacedTrapGridMarker> = grid.placedTraps.map { trap ->
    PlacedTrapGridMarker(
        position = trap.gridPosition,
        trapId = trap.definition.id,
        displayName = trap.definition.displayName,
    )
}
