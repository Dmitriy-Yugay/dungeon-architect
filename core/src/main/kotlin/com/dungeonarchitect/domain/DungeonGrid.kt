package com.dungeonarchitect.domain

import com.dungeonarchitect.simulation.FourDirectionalPathfinder

class DungeonGrid(
    val width: Int,
    val height: Int,
    val entrance: GridPosition,
    val objective: GridPosition,
    val entranceFacing: CardinalDirection = CardinalDirection.EAST,
    val objectiveFacing: CardinalDirection = CardinalDirection.WEST,
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
    val roomDoorConnections: List<RoomDoorConnection>
        get() = buildList {
            mutablePlacedRooms.forEachIndexed { index, room ->
                mutablePlacedRooms.drop(index + 1).forEach { otherRoom ->
                    room.doors.forEach { door ->
                        otherRoom.doors
                            .filter(door::connectsTo)
                            .forEach { otherDoor ->
                                add(RoomDoorConnection(door, otherDoor))
                            }
                    }
                }
            }
        }
    val entranceDoor: PlacedRoomDoor?
        get() = doorsConnectedToPort(entrance, entranceFacing).singleOrNull()
    val objectiveDoor: PlacedRoomDoor?
        get() = doorsConnectedToPort(objective, objectiveFacing).singleOrNull()
    val openRoomDoors: Set<PlacedRoomDoor>
        get() {
            val connectedDoors = roomDoorConnections.flatMapTo(mutableSetOf()) {
                listOf(it.first, it.second)
            }
            entranceDoor?.let(connectedDoors::add)
            objectiveDoor?.let(connectedDoors::add)
            return mutablePlacedRooms
                .flatMapTo(mutableSetOf(), PlacedRoom::doors) - connectedDoors
        }
    val entranceToObjectiveRoute: List<GridPosition>?
        get() = FourDirectionalPathfinder.findPath(
            start = entrance,
            end = objective,
            neighbors = ::traversalNeighbors,
        )

    init {
        require(width > 0) { "Grid width must be positive." }
        require(height > 0) { "Grid height must be positive." }
        require(contains(entrance)) { "Entrance must be inside the grid." }
        require(contains(objective)) { "Objective must be inside the grid." }
        require(entrance != objective) { "Entrance and objective must occupy different tiles." }
        require(mutablePlacedRooms.none { entrance in it.gridPositions }) {
            "Placed rooms must not cover the entrance."
        }
        require(mutablePlacedRooms.none { objective in it.gridPositions }) {
            "Placed rooms must not cover the objective."
        }
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
        require(doorsConnectedToPort(entrance, entranceFacing).size <= 1) {
            "At most one room door may connect to the entrance port."
        }
        require(doorsConnectedToPort(objective, objectiveFacing).size <= 1) {
            "At most one room door may connect to the objective port."
        }
    }

    fun contains(position: GridPosition): Boolean =
        contains(position.column, position.row)

    fun contains(column: Int, row: Int): Boolean =
        column in 0 until width && row in 0 until height

    fun canPlace(room: PlacedRoom): Boolean =
        room.fitsInside(this) &&
            entrance !in room.gridPositions &&
            objective !in room.gridPositions &&
            mutablePlacedRooms.none(room::overlaps) &&
            hasOneFrontierConnection(room) &&
            portRemainsAvailable(room, entrance, entranceFacing) &&
            portRemainsAvailable(room, objective, objectiveFacing)

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

    fun trapSocketHoverResult(
        hoveredPosition: GridPosition,
        definition: TrapDefinition,
    ): TrapSocketHoverResult {
        val (room, localSocketPosition) = mutablePlacedRooms
            .firstNotNullOfOrNull { placedRoom ->
                placedRoom.blueprint.sockets.keys
                    .firstOrNull { localPosition ->
                        placedRoom.toGridPosition(localPosition) == hoveredPosition
                    }
                    ?.let { localPosition -> placedRoom to localPosition }
            }
            ?: return TrapSocketHoverResult.NonSocket

        val socketType = requireNotNull(
            room.blueprint.sockets[localSocketPosition],
        )
        if (!definition.isCompatibleWith(socketType)) {
            return TrapSocketHoverResult.Incompatible(socketType)
        }

        val placedTrap = mutablePlacedTraps.firstOrNull { trap ->
            trap.gridPosition == hoveredPosition
        }
        if (placedTrap != null) {
            return TrapSocketHoverResult.Occupied(placedTrap)
        }

        return TrapSocketHoverResult.Valid(
            room = room,
            localSocketPosition = localSocketPosition,
        )
    }

    fun placementPreview(
        blueprint: RoomBlueprint,
        origin: GridPosition,
    ): RoomPlacementPreview {
        val room = PlacedRoom(blueprint, origin)
        return RoomPlacementPreview(room, canPlace(room))
    }

    fun snappedPlacementPreview(
        blueprint: RoomBlueprint,
        hoveredPosition: GridPosition,
    ): RoomPlacementPreview? = snapCandidates(blueprint)
        .filter { hoveredPosition in it.gridPositions }
        .sortedWith(
            compareByDescending<PlacedRoom>(::canPlace)
                .thenBy { it.origin.row }
                .thenBy { it.origin.column },
        )
        .firstOrNull()
        ?.let { room -> RoomPlacementPreview(room, canPlace(room)) }

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

    private fun hasOneFrontierConnection(room: PlacedRoom): Boolean {
        if (mutablePlacedRooms.isEmpty()) {
            return room.doors.count { door ->
                doorConnectsToPort(door, entrance, entranceFacing)
            } == 1
        }

        val connections = room.doors.flatMap { candidateDoor ->
            mutablePlacedRooms.flatMap { existingRoom ->
                existingRoom.doors
                    .filter(candidateDoor::connectsTo)
                    .map { existingDoor -> candidateDoor to existingDoor }
            }
        }
        return connections.size == 1 && connections.single().second in openRoomDoors
    }

    private fun portRemainsAvailable(
        room: PlacedRoom,
        portPosition: GridPosition,
        portFacing: CardinalDirection,
    ): Boolean {
        val existingConnections = doorsConnectedToPort(portPosition, portFacing).size
        val candidateConnections = room.doors.count { door ->
            doorConnectsToPort(door, portPosition, portFacing)
        }
        return existingConnections + candidateConnections <= 1
    }

    private fun snapCandidates(blueprint: RoomBlueprint): List<PlacedRoom> {
        val origins = if (mutablePlacedRooms.isEmpty()) {
            blueprint.doors
                .filter { it.facing == entranceFacing.opposite }
                .map { door ->
                    originForDoor(
                        targetDoorPosition = entranceFacing.move(entrance),
                        localDoorPosition = door.position,
                    )
                }
        } else {
            openRoomDoors.flatMap { openDoor ->
                blueprint.doors
                    .filter { it.facing == openDoor.door.facing.opposite }
                    .map { door ->
                        originForDoor(
                            targetDoorPosition = openDoor.outsidePosition,
                            localDoorPosition = door.position,
                        )
                    }
            }
        }

        return origins.distinct().map { origin -> PlacedRoom(blueprint, origin) }
    }

    private fun originForDoor(
        targetDoorPosition: GridPosition,
        localDoorPosition: GridPosition,
    ): GridPosition = GridPosition(
        column = targetDoorPosition.column - localDoorPosition.column,
        row = targetDoorPosition.row - localDoorPosition.row,
    )

    private fun traversalNeighbors(position: GridPosition): Iterable<GridPosition> {
        val room = mutablePlacedRooms.firstOrNull { position in it.gridPositions }
        if (room == null) {
            return when (position) {
                entrance -> listOfNotNull(entranceDoor?.gridPosition)
                objective -> listOfNotNull(objectiveDoor?.gridPosition)
                else -> emptyList()
            }
        }

        return buildSet {
            CardinalDirection.entries
                .map { direction -> direction.move(position) }
                .filterTo(this) { neighbor -> neighbor in room.gridPositions }

            roomDoorConnections.forEach { connection ->
                when (position) {
                    connection.first.gridPosition -> add(connection.second.gridPosition)
                    connection.second.gridPosition -> add(connection.first.gridPosition)
                }
            }

            room.doors
                .filter { it.gridPosition == position }
                .forEach { door ->
                    if (door == entranceDoor) {
                        add(entrance)
                    }
                    if (door == objectiveDoor) {
                        add(objective)
                    }
                }
        }
    }

    private fun doorsConnectedToPort(
        portPosition: GridPosition,
        portFacing: CardinalDirection,
    ): List<PlacedRoomDoor> = mutablePlacedRooms.flatMap(PlacedRoom::doors)
        .filter { door -> doorConnectsToPort(door, portPosition, portFacing) }

    private fun doorConnectsToPort(
        door: PlacedRoomDoor,
        portPosition: GridPosition,
        portFacing: CardinalDirection,
    ): Boolean =
        door.door.facing == portFacing.opposite &&
            door.outsidePosition == portPosition &&
            portFacing.move(portPosition) == door.gridPosition
}
