package com.dungeonarchitect.domain

enum class CardinalDirection(
    val columnDelta: Int,
    val rowDelta: Int,
) {
    WEST(columnDelta = -1, rowDelta = 0),
    EAST(columnDelta = 1, rowDelta = 0),
    SOUTH(columnDelta = 0, rowDelta = -1),
    NORTH(columnDelta = 0, rowDelta = 1),
    ;

    val opposite: CardinalDirection
        get() = when (this) {
            WEST -> EAST
            EAST -> WEST
            SOUTH -> NORTH
            NORTH -> SOUTH
        }

    fun move(position: GridPosition): GridPosition = position.copy(
        column = position.column + columnDelta,
        row = position.row + rowDelta,
    )
}
