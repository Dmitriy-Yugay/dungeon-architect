package com.dungeonarchitect.domain

import kotlin.math.abs

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

    fun connectsTo(other: PlacedRoom): Boolean =
        !overlaps(other) &&
            blueprint.doorPositions.any { door ->
                other.blueprint.doorPositions.any { otherDoor ->
                    toGridPosition(door).isCardinallyAdjacentTo(
                        other.toGridPosition(otherDoor),
                    )
                }
            }

    fun toGridPosition(localPosition: GridPosition): GridPosition {
        require(localPosition in blueprint.footprint) {
            "Local position $localPosition is not part of the room footprint."
        }

        return GridPosition(
            column = origin.column + localPosition.column,
            row = origin.row + localPosition.row,
        )
    }

    private fun GridPosition.isCardinallyAdjacentTo(other: GridPosition): Boolean =
        abs(column.toLong() - other.column) +
            abs(row.toLong() - other.row) == 1L
}
