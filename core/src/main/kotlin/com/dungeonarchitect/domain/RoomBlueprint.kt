package com.dungeonarchitect.domain

class RoomBlueprint(
    footprint: Set<GridPosition>,
    doorPositions: Set<GridPosition>,
    sockets: Map<GridPosition, RoomSocketType> = emptyMap(),
) {
    val footprint: Set<GridPosition> = footprint.toSet()
    val doorPositions: Set<GridPosition> = doorPositions.toSet()
    val sockets: Map<GridPosition, RoomSocketType> = sockets.toMap()

    init {
        require(this.footprint.isNotEmpty()) {
            "A room footprint must contain at least one cell."
        }
        require(this.footprint.all { it.column >= 0 && it.row >= 0 }) {
            "Room footprint positions must use non-negative local coordinates."
        }
        require(
            this.footprint.minOf(GridPosition::column) == 0 &&
                this.footprint.minOf(GridPosition::row) == 0,
        ) {
            "A room footprint must be normalized to local column 0 and row 0."
        }
        require(isFourDirectionallyConnected(this.footprint)) {
            "A room footprint must be connected in four directions."
        }
        require(this.doorPositions.isNotEmpty()) {
            "A room blueprint must contain at least one door."
        }
        require(this.doorPositions.all { it in this.footprint }) {
            "Every door position must be part of the room footprint."
        }
        require(this.doorPositions.all(::isOnFootprintEdge)) {
            "Every door position must be on an exposed edge of the room footprint."
        }
        require(this.sockets.keys.all { it in this.footprint }) {
            "Every room socket must occupy a cell in the room footprint."
        }
    }

    private fun isOnFootprintEdge(position: GridPosition): Boolean =
        position.cardinalNeighbors().any { it !in footprint }

    private fun isFourDirectionallyConnected(positions: Set<GridPosition>): Boolean {
        val remaining = positions.toMutableSet()
        val pending = ArrayDeque<GridPosition>()

        pending.addLast(remaining.first())
        while (pending.isNotEmpty()) {
            val position = pending.removeFirst()
            if (!remaining.remove(position)) {
                continue
            }

            position.cardinalNeighbors()
                .filterTo(pending) { it in remaining }
        }

        return remaining.isEmpty()
    }

    private fun GridPosition.cardinalNeighbors(): List<GridPosition> = listOf(
        copy(column = column - 1),
        copy(column = column + 1),
        copy(row = row - 1),
        copy(row = row + 1),
    )
}
