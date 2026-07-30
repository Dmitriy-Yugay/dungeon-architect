package com.dungeonarchitect.domain

import com.dungeonarchitect.simulation.FourDirectionalPathfinder

class DungeonGrid(
    val width: Int,
    val height: Int,
    val entrance: GridPosition,
    val objective: GridPosition,
    placedRooms: List<PlacedRoom> = emptyList(),
) {
    private val mutablePlacedRooms = placedRooms.toMutableList()
    private val mutablePlacedTraps = mutableListOf<PlacedTrap>()

    val placedRooms: List<PlacedRoom>
        get() = mutablePlacedRooms.toList()
    val placedTraps: List<PlacedTrap>
        get() = mutablePlacedTraps.toList()
    val walkablePositions: Set<GridPosition>
        get() = buildSet {
            mutablePlacedRooms.forEach { addAll(it.gridPositions) }
        }
    val entranceToObjectiveRoute: List<GridPosition>?
        get() = FourDirectionalPathfinder.findPath(
            walkablePositions = walkablePositions + entrance + objective,
            start = entrance,
            end = objective,
        )

    init {
        require(width > 0) { "Grid width must be positive." }
        require(height > 0) { "Grid height must be positive." }
        require(contains(entrance)) { "Entrance must be inside the grid." }
        require(contains(objective)) { "Objective must be inside the grid." }
        require(entrance != objective) { "Entrance and objective must occupy different tiles." }
        require(mutablePlacedRooms.all { it.fitsInside(this) }) {
            "Every placed room must fit inside the grid."
        }
        require(
            mutablePlacedRooms.withIndex().all { (index, room) ->
                mutablePlacedRooms.drop(index + 1).none(room::overlaps)
            },
        ) {
            "Placed rooms must not overlap."
        }
    }

    fun contains(position: GridPosition): Boolean =
        contains(position.column, position.row)

    fun contains(column: Int, row: Int): Boolean =
        column in 0 until width && row in 0 until height

    fun canPlace(room: PlacedRoom): Boolean =
        room.fitsInside(this) &&
            mutablePlacedRooms.none(room::overlaps) &&
            mutablePlacedRooms.any(room::connectsTo)

    fun place(room: PlacedRoom): Boolean {
        if (!canPlace(room)) {
            return false
        }

        mutablePlacedRooms += room
        return true
    }

    fun placeTrap(
        room: PlacedRoom,
        localSocketPosition: GridPosition,
        definition: TrapDefinition,
    ): Boolean {
        if (room !in mutablePlacedRooms) {
            return false
        }

        val socketType =
            room.blueprint.sockets[localSocketPosition] ?: return false
        if (!definition.isCompatibleWith(socketType)) {
            return false
        }

        val gridPosition = room.toGridPosition(localSocketPosition)
        if (mutablePlacedTraps.any { it.gridPosition == gridPosition }) {
            return false
        }

        mutablePlacedTraps += PlacedTrap(
            definition = definition,
            room = room,
            localSocketPosition = localSocketPosition,
        )
        return true
    }

    fun placementPreview(
        blueprint: RoomBlueprint,
        origin: GridPosition,
    ): RoomPlacementPreview {
        val room = PlacedRoom(blueprint, origin)
        return RoomPlacementPreview(room, canPlace(room))
    }

    fun tileAt(position: GridPosition): TileType =
        tileAt(position.column, position.row)

    fun tileAt(column: Int, row: Int): TileType {
        require(contains(column, row)) {
            "Tile ($column, $row) is outside a $width x $height grid."
        }

        return when {
            entrance.column == column && entrance.row == row -> TileType.ENTRANCE
            objective.column == column && objective.row == row -> TileType.OBJECTIVE
            mutablePlacedRooms.any { GridPosition(column, row) in it.gridPositions } -> TileType.ROOM
            else -> TileType.EMPTY
        }
    }
}
