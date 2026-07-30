package com.dungeonarchitect.domain

data class PlacedRoom(
    val blueprint: RoomBlueprint,
    val origin: GridPosition,
) {
    val gridPositions: Set<GridPosition> =
        blueprint.footprint.mapTo(mutableSetOf(), ::toGridPosition)

    fun fitsInside(grid: DungeonGrid): Boolean =
        gridPositions.all(grid::contains)

    fun overlaps(other: PlacedRoom): Boolean =
        gridPositions.any { it in other.gridPositions }

    fun toGridPosition(localPosition: GridPosition): GridPosition {
        require(localPosition in blueprint.footprint) {
            "Local position $localPosition is not part of the room footprint."
        }

        return GridPosition(
            column = origin.column + localPosition.column,
            row = origin.row + localPosition.row,
        )
    }
}
