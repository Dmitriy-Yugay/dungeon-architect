package com.dungeonarchitect.domain

class RoomBlueprint(
    val id: String,
    val displayName: String,
    footprint: Set<GridPosition>,
    doors: List<RoomDoor>,
    sockets: Map<GridPosition, RoomSocketType> = emptyMap(),
) {
    val footprint: Set<GridPosition> = footprint.toSet()
    val doors: Set<RoomDoor> = doors.toSet()
    val doorPositions: Set<GridPosition> =
        this.doors.mapTo(mutableSetOf(), RoomDoor::position)
    val sockets: Map<GridPosition, RoomSocketType> = sockets.toMap()

    constructor(
        id: String,
        displayName: String,
        footprint: Set<GridPosition>,
        doorPositions: Set<GridPosition>,
        doorFacings: Map<GridPosition, CardinalDirection> = emptyMap(),
        sockets: Map<GridPosition, RoomSocketType> = emptyMap(),
    ) : this(
        id = id,
        displayName = displayName,
        footprint = footprint,
        doors = inferDoors(footprint, doorPositions, doorFacings),
        sockets = sockets,
    )

    init {
        require(id.isNotBlank()) {
            "A room blueprint ID must not be blank."
        }
        require(displayName.isNotBlank()) {
            "A room blueprint display name must not be blank."
        }
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
        require(this.doors.isNotEmpty()) {
            "A room blueprint must contain at least one door."
        }
        require(this.doors.size == doors.size) {
            "A room blueprint must not repeat the same directional door."
        }
        require(this.doors.all { it.position in this.footprint }) {
            "Every door position must be part of the room footprint."
        }
        require(this.doors.all { door ->
            door.facing.move(door.position) !in this.footprint
        }) {
            "Every room door must face an exposed side of its footprint cell."
        }
        require(this.sockets.keys.all { it in this.footprint }) {
            "Every room socket must occupy a cell in the room footprint."
        }
    }

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

    private companion object {
        fun inferDoors(
            footprint: Set<GridPosition>,
            doorPositions: Set<GridPosition>,
            doorFacings: Map<GridPosition, CardinalDirection>,
        ): List<RoomDoor> = doorPositions.map { position ->
            RoomDoor(
                position = position,
                facing = doorFacings[position] ?: CardinalDirection.entries
                    .firstOrNull { direction ->
                        direction.move(position) !in footprint
                    }
                    ?: CardinalDirection.WEST,
            )
        }
    }
}
