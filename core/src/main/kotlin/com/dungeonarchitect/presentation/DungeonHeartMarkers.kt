package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition

internal data class PlacedDungeonHeartGridMarker(
    val position: GridPosition,
)

internal fun placedDungeonHeartGridMarker(
    grid: DungeonGrid,
): PlacedDungeonHeartGridMarker? = grid.placedHeart?.let { heart ->
    PlacedDungeonHeartGridMarker(position = heart.gridPosition)
}
