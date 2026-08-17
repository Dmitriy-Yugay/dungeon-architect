package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DungeonGridTest {
    @Test
    fun `grid exposes its entrance`() {
        val grid = dungeonGrid()

        assertEquals(TileType.ENTRANCE, grid.tileAt(grid.entrance))
        assertEquals(TileType.EMPTY, grid.tileAt(column = 1, row = 1))
    }

    @Test
    fun `grid classifies placed room cells as room tiles`() {
        val grid = dungeonGrid(
            placedRooms = listOf(
                placedRoom(origin = GridPosition(column = 1, row = 0)),
            ),
        )

        assertEquals(TileType.ROOM, grid.tileAt(column = 1, row = 0))
        assertEquals(TileType.ROOM, grid.tileAt(column = 2, row = 0))
        assertEquals(TileType.EMPTY, grid.tileAt(column = 1, row = 1))
    }

    @Test
    fun `grid rejects a room covering the entrance`() {
        assertFailsWith<IllegalArgumentException> {
            dungeonGrid(
                entrance = GridPosition(column = 1, row = 0),
                placedRooms = listOf(
                    placedRoom(origin = GridPosition(column = 1, row = 0)),
                ),
            )
        }
    }

    @Test
    fun `grid contains only positions inside its bounds`() {
        val grid = dungeonGrid()

        assertTrue(grid.contains(column = 0, row = 0))
        assertTrue(grid.contains(column = grid.width - 1, row = grid.height - 1))
        assertFalse(grid.contains(column = -1, row = 0))
        assertFalse(grid.contains(column = grid.width, row = 0))
        assertFalse(grid.contains(column = 0, row = grid.height))
    }

    @Test
    fun `grid rejects invalid dimensions`() {
        assertFailsWith<IllegalArgumentException> {
            dungeonGrid(width = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            dungeonGrid(height = 0)
        }
    }

    @Test
    fun `grid rejects an invalid entrance position`() {
        assertFailsWith<IllegalArgumentException> {
            dungeonGrid(entrance = GridPosition(column = -1, row = 0))
        }
    }

    @Test
    fun `grid rejects tile access outside its bounds`() {
        val grid = dungeonGrid()

        assertFailsWith<IllegalArgumentException> {
            grid.tileAt(column = grid.width, row = 0)
        }
    }

    @Test
    fun `grid owns a snapshot of its placed rooms`() {
        val room = placedRoom(origin = GridPosition(column = 1, row = 0))
        val sourceRooms = mutableListOf(room)

        val grid = dungeonGrid(placedRooms = sourceRooms)
        sourceRooms.clear()

        assertEquals(listOf(room), grid.placedRooms)
    }

    @Test
    fun `grid has no walkable positions without placed rooms`() {
        val grid = dungeonGrid()

        assertEquals(emptySet(), grid.walkablePositions)
    }

    @Test
    fun `walkable positions are the deduplicated union of placed room footprints`() {
        val firstRoom = placedRoom(origin = GridPosition(column = 0, row = 0))
        val secondRoom = placedRoom(origin = GridPosition(column = 2, row = 2))
        val grid = dungeonGrid(placedRooms = listOf(firstRoom, secondRoom))

        assertEquals(
            setOf(
                GridPosition(column = 0, row = 0),
                GridPosition(column = 1, row = 0),
                GridPosition(column = 2, row = 2),
                GridPosition(column = 3, row = 2),
            ),
            grid.walkablePositions,
        )
    }

    @Test
    fun `route connects entrance to the placed heart through room positions`() {
        val room = placedRoom(
            origin = GridPosition(column = 1, row = 1),
            footprint = setOf(
                GridPosition(column = 0, row = 0),
                GridPosition(column = 1, row = 0),
                GridPosition(column = 2, row = 0),
            ),
            heartAnchor = GridPosition(column = 2, row = 0),
            doors = listOf(
                RoomDoor(
                    position = GridPosition(column = 0, row = 0),
                    facing = CardinalDirection.WEST,
                ),
                RoomDoor(
                    position = GridPosition(column = 2, row = 0),
                    facing = CardinalDirection.EAST,
                ),
            ),
        )
        val grid = dungeonGrid(
            width = 5,
            entrance = GridPosition(column = 0, row = 1),
            placedRooms = listOf(room),
        )
        assertTrue(grid.placeOrRelocateHeart(room))

        assertEquals(
            listOf(
                grid.entrance,
                GridPosition(column = 1, row = 1),
                GridPosition(column = 2, row = 1),
                GridPosition(column = 3, row = 1),
            ),
            grid.entranceToHeartRoute,
        )
    }

    @Test
    fun `route is absent when room connections leave a gap to the heart`() {
        val entranceRoom = placedRoom(origin = GridPosition(column = 1, row = 1))
        val heartRoom = directionalRoom(
            origin = GridPosition(column = 4, row = 1),
            doors = listOf(
                RoomDoor(GridPosition(column = 0, row = 0), CardinalDirection.WEST),
            ),
        )
        val grid = dungeonGrid(
            width = 6,
            entrance = GridPosition(column = 0, row = 1),
            placedRooms = listOf(entranceRoom, heartRoom),
        )
        assertTrue(grid.placeOrRelocateHeart(heartRoom))

        assertNull(grid.entranceToHeartRoute)
    }

    @Test
    fun `route is absent before a heart is placed`() {
        assertNull(dungeonGrid().entranceToHeartRoute)
    }

    @Test
    fun `grid rejects a placed room outside its bounds`() {
        assertFailsWith<IllegalArgumentException> {
            dungeonGrid(
                placedRooms = listOf(
                    placedRoom(origin = GridPosition(column = 3, row = 0)),
                ),
            )
        }
    }

    @Test
    fun `grid rejects overlapping placed rooms`() {
        assertFailsWith<IllegalArgumentException> {
            dungeonGrid(
                placedRooms = listOf(
                    placedRoom(origin = GridPosition(column = 0, row = 0)),
                    placedRoom(origin = GridPosition(column = 1, row = 0)),
                ),
            )
        }
    }

    @Test
    fun `grid accepts placement that fits without overlap and connects through doors`() {
        val existingRoom = placedRoom(
            origin = GridPosition(column = 2, row = 1),
            doorPosition = GridPosition(column = 1, row = 0),
        )
        val grid = dungeonGrid(
            width = 7,
            height = 4,
            placedRooms = listOf(existingRoom),
        )
        val candidate = placedRoom(
            origin = GridPosition(column = 4, row = 1),
            doorPosition = GridPosition(column = 0, row = 0),
        )

        assertTrue(grid.canPlace(candidate))
    }

    @Test
    fun `grid rejects placement outside its bounds`() {
        val grid = dungeonGrid(
            placedRooms = listOf(
                placedRoom(origin = GridPosition(column = 1, row = 0)),
            ),
        )
        val candidate = placedRoom(origin = GridPosition(column = 3, row = 1))

        assertFalse(grid.canPlace(candidate))
    }

    @Test
    fun `grid rejects placement that overlaps an existing room`() {
        val grid = dungeonGrid(
            placedRooms = listOf(
                placedRoom(origin = GridPosition(column = 1, row = 0)),
            ),
        )
        val candidate = placedRoom(origin = GridPosition(column = 2, row = 0))

        assertFalse(grid.canPlace(candidate))
    }

    @Test
    fun `grid rejects placement without a compatible adjacent door`() {
        val existingRoom = placedRoom(
            origin = GridPosition(column = 0, row = 0),
            doorPosition = GridPosition(column = 0, row = 0),
        )
        val grid = dungeonGrid(placedRooms = listOf(existingRoom))
        val candidate = placedRoom(
            origin = GridPosition(column = 2, row = 0),
            doorPosition = GridPosition(column = 1, row = 0),
        )

        assertFalse(grid.canPlace(candidate))
    }

    @Test
    fun `adjacent room walls do not create a traversable connection`() {
        val entranceRoom = directionalRoom(
            origin = GridPosition(column = 1, row = 1),
            doors = listOf(RoomDoor(GridPosition(0, 0), CardinalDirection.WEST)),
        )
        val heartRoom = directionalRoom(
            origin = GridPosition(column = 2, row = 1),
            doors = listOf(RoomDoor(GridPosition(0, 0), CardinalDirection.EAST)),
        )
        val grid = dungeonGrid(
            width = 4,
            entrance = GridPosition(column = 0, row = 1),
            placedRooms = listOf(entranceRoom, heartRoom),
        )

        assertEquals(emptyList(), grid.roomDoorConnections)
        assertTrue(grid.placeOrRelocateHeart(heartRoom))
        assertNull(grid.entranceToHeartRoute)
    }

    @Test
    fun `route crosses room boundaries only through facing door connections`() {
        val firstRoom = directionalRoom(
            origin = GridPosition(column = 1, row = 1),
            doors = listOf(
                RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                RoomDoor(GridPosition(0, 0), CardinalDirection.EAST),
            ),
        )
        val secondRoom = directionalRoom(
            origin = GridPosition(column = 2, row = 1),
            doors = listOf(
                RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                RoomDoor(GridPosition(0, 0), CardinalDirection.EAST),
            ),
        )
        val grid = dungeonGrid(
            width = 4,
            entrance = GridPosition(column = 0, row = 1),
            placedRooms = listOf(firstRoom, secondRoom),
        )
        assertTrue(grid.placeOrRelocateHeart(secondRoom))

        assertEquals(
            listOf(
                GridPosition(column = 0, row = 1),
                GridPosition(column = 1, row = 1),
                GridPosition(column = 2, row = 1),
            ),
            grid.entranceToHeartRoute,
        )
    }

    @Test
    fun `only opposite facing adjacent doors connect and become consumed`() {
        val firstRoom = directionalRoom(
            origin = GridPosition(column = 1, row = 1),
            doors = listOf(
                RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                RoomDoor(GridPosition(0, 0), CardinalDirection.EAST),
            ),
        )
        val grid = dungeonGrid(
            width = 5,
            entrance = GridPosition(column = 0, row = 1),
            placedRooms = listOf(firstRoom),
        )
        val secondRoom = directionalRoom(
            origin = GridPosition(column = 2, row = 1),
            doors = listOf(
                RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                RoomDoor(GridPosition(0, 0), CardinalDirection.EAST),
            ),
        )

        assertTrue(grid.canPlace(secondRoom))
        assertTrue(grid.place(secondRoom))
        assertEquals(1, grid.roomDoorConnections.size)
        assertEquals(
            setOf(secondRoom.doors.single { it.door.facing == CardinalDirection.EAST }),
            grid.openRoomDoors,
        )
    }

    @Test
    fun `same-facing adjacent doors remain open and disconnected`() {
        val firstRoom = directionalRoom(
            origin = GridPosition(column = 1, row = 1),
            doors = listOf(RoomDoor(GridPosition(0, 0), CardinalDirection.EAST)),
        )
        val secondRoom = directionalRoom(
            origin = GridPosition(column = 2, row = 1),
            doors = listOf(RoomDoor(GridPosition(0, 0), CardinalDirection.EAST)),
        )
        val grid = dungeonGrid(
            placedRooms = listOf(firstRoom, secondRoom),
        )

        assertEquals(emptyList(), grid.roomDoorConnections)
        assertEquals(firstRoom.doors + secondRoom.doors, grid.openRoomDoors)
    }

    @Test
    fun `grid rejects a room that would join two open doors at once`() {
        val leftRoom = directionalRoom(
            origin = GridPosition(column = 1, row = 1),
            doors = listOf(RoomDoor(GridPosition(0, 0), CardinalDirection.EAST)),
        )
        val rightRoom = directionalRoom(
            origin = GridPosition(column = 3, row = 1),
            doors = listOf(RoomDoor(GridPosition(0, 0), CardinalDirection.WEST)),
        )
        val grid = dungeonGrid(
            width = 6,
            entrance = GridPosition(column = 0, row = 0),
            placedRooms = listOf(leftRoom, rightRoom),
        )
        val bridge = directionalRoom(
            origin = GridPosition(column = 2, row = 1),
            doors = listOf(
                RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                RoomDoor(GridPosition(0, 0), CardinalDirection.EAST),
            ),
        )

        assertFalse(grid.canPlace(bridge))
    }

    @Test
    fun `placement preview snaps a candidate door to the hovered frontier`() {
        val existingRoom = directionalRoom(
            origin = GridPosition(column = 1, row = 1),
            doors = listOf(RoomDoor(GridPosition(0, 0), CardinalDirection.EAST)),
        )
        val grid = dungeonGrid(
            width = 6,
            placedRooms = listOf(existingRoom),
        )
        val blueprint = RoomBlueprint(
            id = "candidate",
            displayName = "Candidate",
            heartAnchor = GridPosition(column = 0, row = 0),
            footprint = setOf(GridPosition(0, 0), GridPosition(1, 0)),
            doors = listOf(RoomDoor(GridPosition(0, 0), CardinalDirection.WEST)),
        )

        val preview = grid.snappedPlacementPreview(
            blueprint = blueprint,
            hoveredPosition = GridPosition(column = 3, row = 1),
        )

        assertEquals(GridPosition(column = 2, row = 1), preview?.room?.origin)
        assertTrue(preview?.isValid == true)
    }

    @Test
    fun `grid rejects placement when there is no existing room to connect to`() {
        val grid = dungeonGrid()
        val candidate = placedRoom(origin = GridPosition(column = 1, row = 0))

        assertFalse(grid.canPlace(candidate))
    }

    @Test
    fun `grid commits a valid room placement`() {
        val existingRoom = placedRoom(
            origin = GridPosition(column = 2, row = 1),
            doorPosition = GridPosition(column = 1, row = 0),
        )
        val grid = dungeonGrid(
            width = 7,
            height = 4,
            placedRooms = listOf(existingRoom),
        )
        val roomsBeforePlacement = grid.placedRooms
        val candidate = placedRoom(
            origin = GridPosition(column = 4, row = 1),
            doorPosition = GridPosition(column = 0, row = 0),
        )

        assertTrue(grid.place(candidate))
        assertEquals(listOf(existingRoom, candidate), grid.placedRooms)
        assertEquals(listOf(existingRoom), roomsBeforePlacement)
    }

    @Test
    fun `walkable positions update after a room is placed`() {
        val existingRoom = placedRoom(
            origin = GridPosition(column = 2, row = 1),
            doorPosition = GridPosition(column = 1, row = 0),
        )
        val grid = dungeonGrid(
            width = 7,
            height = 4,
            placedRooms = listOf(existingRoom),
        )
        val positionsBeforePlacement = grid.walkablePositions
        val candidate = placedRoom(
            origin = GridPosition(column = 4, row = 1),
            doorPosition = GridPosition(column = 0, row = 0),
        )

        assertTrue(grid.place(candidate))
        assertEquals(
            existingRoom.gridPositions + candidate.gridPositions,
            grid.walkablePositions,
        )
        assertEquals(existingRoom.gridPositions, positionsBeforePlacement)
    }

    @Test
    fun `grid leaves placed rooms unchanged when placement is invalid`() {
        val existingRoom = placedRoom(origin = GridPosition(column = 1, row = 0))
        val grid = dungeonGrid(placedRooms = listOf(existingRoom))
        val overlappingCandidate = placedRoom(origin = GridPosition(column = 2, row = 0))

        assertFalse(grid.place(overlappingCandidate))
        assertEquals(listOf(existingRoom), grid.placedRooms)
    }

    @Test
    fun `cancel on an empty grid leaves derived state empty`() {
        val grid = dungeonGrid()

        assertFalse(grid.cancelLastPlacedRoom())

        assertEquals(emptyList(), grid.placedRooms)
        assertEquals(emptySet(), grid.walkablePositions)
        assertEquals(emptySet(), grid.openRoomDoors)
        assertNull(grid.entranceToHeartRoute)
    }

    @Test
    fun `cancel removes the only placed room`() {
        val room = directionalRoom(
            origin = GridPosition(column = 1, row = 1),
            doors = listOf(
                RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                RoomDoor(GridPosition(0, 0), CardinalDirection.EAST),
            ),
        )
        val grid = dungeonGrid(placedRooms = listOf(room))

        assertTrue(grid.cancelLastPlacedRoom())

        assertEquals(emptyList(), grid.placedRooms)
        assertEquals(emptySet(), grid.walkablePositions)
        assertEquals(TileType.EMPTY, grid.tileAt(room.origin))
    }

    @Test
    fun `cancel removes only the newest room and restores its frontier`() {
        val firstRoom = directionalRoom(
            origin = GridPosition(column = 1, row = 1),
            doors = listOf(
                RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                RoomDoor(GridPosition(0, 0), CardinalDirection.EAST),
            ),
        )
        val secondRoom = directionalRoom(
            origin = GridPosition(column = 2, row = 1),
            doors = listOf(
                RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                RoomDoor(GridPosition(0, 0), CardinalDirection.EAST),
            ),
        )
        val grid = dungeonGrid(
            width = 4,
            entrance = GridPosition(column = 0, row = 1),
            placedRooms = listOf(firstRoom, secondRoom),
        )
        assertTrue(grid.placeOrRelocateHeart(secondRoom))
        assertTrue(grid.entranceToHeartRoute != null)
        assertEquals(
            setOf(
                secondRoom.doors.single {
                    it.door.facing == CardinalDirection.EAST
                },
            ),
            grid.openRoomDoors,
        )

        assertTrue(grid.cancelLastPlacedRoom())

        assertEquals(listOf(firstRoom), grid.placedRooms)
        assertEquals(firstRoom.gridPositions, grid.walkablePositions)
        assertEquals(
            setOf(
                firstRoom.doors.single {
                    it.door.facing == CardinalDirection.EAST
                },
            ),
            grid.openRoomDoors,
        )
        assertEquals(emptyList(), grid.roomDoorConnections)
        assertNull(grid.entranceToHeartRoute)
    }

    @Test
    fun `cancel removes traps in the newest room and preserves earlier traps`() {
        val firstRoom = socketRoom(
            origin = GridPosition(column = 1, row = 1),
            doors = listOf(
                RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                RoomDoor(GridPosition(0, 0), CardinalDirection.EAST),
            ),
        )
        val secondRoom = socketRoom(
            origin = GridPosition(column = 2, row = 1),
            doors = listOf(
                RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                RoomDoor(GridPosition(0, 0), CardinalDirection.EAST),
            ),
        )
        val grid = dungeonGrid(
            width = 4,
            entrance = GridPosition(column = 0, row = 1),
            placedRooms = listOf(firstRoom, secondRoom),
        )
        assertTrue(grid.placeTrap(firstRoom, GridPosition(0, 0), trapDefinition()))
        assertTrue(grid.placeTrap(secondRoom, GridPosition(0, 0), trapDefinition()))

        assertTrue(grid.cancelLastPlacedRoom())

        assertEquals(listOf(firstRoom), grid.placedRooms)
        assertEquals(listOf(firstRoom), grid.placedTraps.map(PlacedTrap::room))
    }

    @Test
    fun `grid places a compatible trap in a declared room socket`() {
        val room = socketRoom(origin = GridPosition(column = 1, row = 0))
        val grid = dungeonGrid(placedRooms = listOf(room))
        val definition = trapDefinition()
        val trapsBeforePlacement = grid.placedTraps

        assertTrue(
            grid.placeTrap(
                room = room,
                localSocketPosition = GridPosition(column = 0, row = 0),
                definition = definition,
            ),
        )

        val placedTrap = grid.placedTraps.single()
        assertSame(definition, placedTrap.definition)
        assertEquals(room, placedTrap.room)
        assertEquals(
            GridPosition(column = 0, row = 0),
            placedTrap.localSocketPosition,
        )
        assertEquals(GridPosition(column = 1, row = 0), placedTrap.gridPosition)
        assertEquals(emptyList(), trapsBeforePlacement)
    }

    @Test
    fun `grid rejects a trap for a foreign room missing socket or incompatible socket`() {
        val room = socketRoom(origin = GridPosition(column = 1, row = 0))
        val foreignRoom = socketRoom(origin = GridPosition(column = 2, row = 0))
        val grid = dungeonGrid(placedRooms = listOf(room))

        assertFalse(
            grid.placeTrap(
                room = foreignRoom,
                localSocketPosition = GridPosition(column = 0, row = 0),
                definition = trapDefinition(),
            ),
        )
        assertFalse(
            grid.placeTrap(
                room = room,
                localSocketPosition = GridPosition(column = 1, row = 0),
                definition = trapDefinition(),
            ),
        )
        assertFalse(
            grid.placeTrap(
                room = room,
                localSocketPosition = GridPosition(column = 0, row = 0),
                definition = trapDefinition(
                    compatibleSocketTypes = setOf(RoomSocketType.WALL),
                ),
            ),
        )
        assertEquals(emptyList(), grid.placedTraps)
    }

    @Test
    fun `grid rejects a trap when the socket is already occupied`() {
        val room = socketRoom(origin = GridPosition(column = 1, row = 0))
        val grid = dungeonGrid(placedRooms = listOf(room))
        val localSocketPosition = GridPosition(column = 0, row = 0)

        assertTrue(
            grid.placeTrap(room, localSocketPosition, trapDefinition()),
        )
        assertFalse(
            grid.placeTrap(room, localSocketPosition, trapDefinition()),
        )
        assertEquals(1, grid.placedTraps.size)
    }

    private fun dungeonGrid(
        width: Int = 4,
        height: Int = 3,
        entrance: GridPosition = GridPosition(column = 0, row = 1),
        placedRooms: List<PlacedRoom> = emptyList(),
    ) = DungeonGrid(
        width = width,
        height = height,
        entrance = entrance,
        placedRooms = placedRooms,
    )

    private fun placedRoom(
        origin: GridPosition,
        doorPosition: GridPosition = GridPosition(column = 0, row = 0),
        heartAnchor: GridPosition = GridPosition(column = 0, row = 0),
        footprint: Set<GridPosition> = setOf(
            GridPosition(column = 0, row = 0),
            GridPosition(column = 1, row = 0),
        ),
        doors: List<RoomDoor>? = null,
    ) = PlacedRoom(
        blueprint = doors?.let {
            RoomBlueprint(
                id = "test-room",
                displayName = "Test Room",
                heartAnchor = heartAnchor,
                footprint = footprint,
                doors = it,
            )
        } ?: RoomBlueprint(
            id = "test-room",
            displayName = "Test Room",
            heartAnchor = heartAnchor,
            footprint = footprint,
            doorPositions = setOf(doorPosition),
        ),
        origin = origin,
    )

    private fun socketRoom(
        origin: GridPosition,
        socketType: RoomSocketType = RoomSocketType.FLOOR,
        doors: List<RoomDoor>? = null,
    ) = PlacedRoom(
        blueprint = RoomBlueprint(
            id = "socket-room",
            displayName = "Socket Room",
            heartAnchor = GridPosition(column = 0, row = 0),
            footprint = setOf(GridPosition(column = 0, row = 0)),
            doors = doors ?: listOf(
                RoomDoor(
                    position = GridPosition(column = 0, row = 0),
                    facing = CardinalDirection.WEST,
                ),
            ),
            sockets = mapOf(
                GridPosition(column = 0, row = 0) to socketType,
            ),
        ),
        origin = origin,
    )

    private fun directionalRoom(
        origin: GridPosition,
        doors: List<RoomDoor>,
    ) = PlacedRoom(
        blueprint = RoomBlueprint(
            id = "directional-room",
            displayName = "Directional Room",
            heartAnchor = GridPosition(column = 0, row = 0),
            footprint = setOf(GridPosition(column = 0, row = 0)),
            doors = doors,
        ),
        origin = origin,
    )

    private fun trapDefinition(
        compatibleSocketTypes: Set<RoomSocketType> =
            setOf(RoomSocketType.FLOOR),
    ) = TrapDefinition(
        id = "spike_trap",
        displayName = "Spike Trap",
        damage = 5,
        cooldownSeconds = 0.25f,
        compatibleSocketTypes = compatibleSocketTypes,
    )
}
