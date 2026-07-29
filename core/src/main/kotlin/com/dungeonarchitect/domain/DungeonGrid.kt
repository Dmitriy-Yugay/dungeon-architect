package com.dungeonarchitect.domain

class DungeonGrid(
    val width: Int,
    val height: Int,
    val entrance: GridPosition,
    val objective: GridPosition,
) {
    init {
        require(width > 0) { "Grid width must be positive." }
        require(height > 0) { "Grid height must be positive." }
        require(contains(entrance)) { "Entrance must be inside the grid." }
        require(contains(objective)) { "Objective must be inside the grid." }
        require(entrance != objective) { "Entrance and objective must occupy different tiles." }
    }

    fun contains(position: GridPosition): Boolean =
        contains(position.column, position.row)

    fun contains(column: Int, row: Int): Boolean =
        column in 0 until width && row in 0 until height

    fun tileAt(position: GridPosition): TileType =
        tileAt(position.column, position.row)

    fun tileAt(column: Int, row: Int): TileType {
        require(contains(column, row)) {
            "Tile ($column, $row) is outside a $width x $height grid."
        }

        return when {
            entrance.column == column && entrance.row == row -> TileType.ENTRANCE
            objective.column == column && objective.row == row -> TileType.OBJECTIVE
            else -> TileType.EMPTY
        }
    }
}
