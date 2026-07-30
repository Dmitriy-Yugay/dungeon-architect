package com.dungeonarchitect.domain

class DungeonGrid(
    val width: Int,
    val height: Int,
    val entrance: GridPosition,
    val objective: GridPosition,
    placedRooms: List<PlacedRoom> = emptyList(),
) {
    val placedRooms: List<PlacedRoom> = placedRooms.toList()

    init {
        require(width > 0) { "Grid width must be positive." }
        require(height > 0) { "Grid height must be positive." }
        require(contains(entrance)) { "Entrance must be inside the grid." }
        require(contains(objective)) { "Objective must be inside the grid." }
        require(entrance != objective) { "Entrance and objective must occupy different tiles." }
        require(this.placedRooms.all { it.fitsInside(this) }) {
            "Every placed room must fit inside the grid."
        }
        require(
            this.placedRooms.withIndex().all { (index, room) ->
                this.placedRooms.drop(index + 1).none(room::overlaps)
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
            placedRooms.none(room::overlaps) &&
            placedRooms.any(room::connectsTo)

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
            placedRooms.any { GridPosition(column, row) in it.gridPositions } -> TileType.ROOM
            else -> TileType.EMPTY
        }
    }
}
