package com.dungeonarchitect.domain

enum class RoomOrientation(
    private val clockwiseQuarterTurns: Int,
) {
    UNROTATED(clockwiseQuarterTurns = 0),
    CLOCKWISE_90(clockwiseQuarterTurns = 1),
    CLOCKWISE_180(clockwiseQuarterTurns = 2),
    CLOCKWISE_270(clockwiseQuarterTurns = 3),
    ;

    fun rotateClockwise(): RoomOrientation = when (this) {
        UNROTATED -> CLOCKWISE_90
        CLOCKWISE_90 -> CLOCKWISE_180
        CLOCKWISE_180 -> CLOCKWISE_270
        CLOCKWISE_270 -> UNROTATED
    }

    fun rotateCounterClockwise(): RoomOrientation = when (this) {
        UNROTATED -> CLOCKWISE_270
        CLOCKWISE_90 -> UNROTATED
        CLOCKWISE_180 -> CLOCKWISE_90
        CLOCKWISE_270 -> CLOCKWISE_180
    }

    internal fun transform(
        position: GridPosition,
        width: Int,
        height: Int,
    ): GridPosition {
        var transformed = position
        var transformedWidth = width
        var transformedHeight = height

        repeat(clockwiseQuarterTurns) {
            transformed = GridPosition(
                column = transformed.row,
                row = transformedWidth - 1 - transformed.column,
            )
            val previousWidth = transformedWidth
            transformedWidth = transformedHeight
            transformedHeight = previousWidth
        }

        return transformed
    }

    internal fun transform(direction: CardinalDirection): CardinalDirection {
        var transformed = direction
        repeat(clockwiseQuarterTurns) {
            transformed = when (transformed) {
                CardinalDirection.WEST -> CardinalDirection.NORTH
                CardinalDirection.NORTH -> CardinalDirection.EAST
                CardinalDirection.EAST -> CardinalDirection.SOUTH
                CardinalDirection.SOUTH -> CardinalDirection.WEST
            }
        }
        return transformed
    }
}
