package com.dungeonarchitect.simulation

import com.dungeonarchitect.domain.GridPosition

object FourDirectionalPathfinder {
    fun findPath(
        walkablePositions: Set<GridPosition>,
        start: GridPosition,
        end: GridPosition,
    ): List<GridPosition>? {
        if (start !in walkablePositions || end !in walkablePositions) {
            return null
        }
        return findPath(
            start = start,
            end = end,
            neighbors = { position ->
                position.cardinalNeighbors().filter { it in walkablePositions }
            },
        )
    }

    fun findPath(
        start: GridPosition,
        end: GridPosition,
        neighbors: (GridPosition) -> Iterable<GridPosition>,
    ): List<GridPosition>? {
        val pending = ArrayDeque<GridPosition>()
        val previousPosition = mutableMapOf<GridPosition, GridPosition?>()
        pending.addLast(start)
        previousPosition[start] = null

        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (current == end) {
                return buildPath(end, previousPosition)
            }

            neighbors(current)
                .filter { it !in previousPosition }
                .forEach { neighbor ->
                    previousPosition[neighbor] = current
                    pending.addLast(neighbor)
                }
        }

        return null
    }

    private fun buildPath(
        end: GridPosition,
        previousPosition: Map<GridPosition, GridPosition?>,
    ): List<GridPosition> =
        generateSequence(end) { previousPosition[it] }
            .toList()
            .asReversed()

    private fun GridPosition.cardinalNeighbors(): List<GridPosition> = buildList {
        if (column < Int.MAX_VALUE) {
            add(copy(column = column + 1))
        }
        if (row < Int.MAX_VALUE) {
            add(copy(row = row + 1))
        }
        if (column > Int.MIN_VALUE) {
            add(copy(column = column - 1))
        }
        if (row > Int.MIN_VALUE) {
            add(copy(row = row - 1))
        }
    }
}
