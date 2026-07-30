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
    fun `grid exposes its entrance and objective`() {
        val grid = dungeonGrid()

        assertEquals(TileType.ENTRANCE, grid.tileAt(grid.entrance))
        assertEquals(TileType.OBJECTIVE, grid.tileAt(grid.objective))
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
    fun `entrance and objective remain special when covered by a placed room`() {
        val grid = dungeonGrid(
            entrance = GridPosition(column = 1, row = 0),
            objective = GridPosition(column = 2, row = 0),
            placedRooms = listOf(
                placedRoom(origin = GridPosition(column = 1, row = 0)),
            ),
        )

        assertEquals(TileType.ENTRANCE, grid.tileAt(grid.entrance))
        assertEquals(TileType.OBJECTIVE, grid.tileAt(grid.objective))
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
    fun `grid rejects invalid special tile positions`() {
        assertFailsWith<IllegalArgumentException> {
            dungeonGrid(entrance = GridPosition(column = -1, row = 0))
        }
        assertFailsWith<IllegalArgumentException> {
            dungeonGrid(objective = GridPosition(column = 4, row = 2))
        }
        assertFailsWith<IllegalArgumentException> {
            dungeonGrid(
                entrance = GridPosition(column = 0, row = 1),
                objective = GridPosition(column = 0, row = 1),
            )
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
    fun `route connects entrance to objective through room positions including endpoints`() {
        val grid = dungeonGrid(
            width = 5,
            entrance = GridPosition(column = 0, row = 1),
            objective = GridPosition(column = 4, row = 1),
            placedRooms = listOf(
                placedRoom(
                    origin = GridPosition(column = 1, row = 1),
                    footprint = setOf(
                        GridPosition(column = 0, row = 0),
                        GridPosition(column = 1, row = 0),
                        GridPosition(column = 2, row = 0),
                    ),
                ),
            ),
        )

        assertEquals(
            listOf(
                grid.entrance,
                GridPosition(column = 1, row = 1),
                GridPosition(column = 2, row = 1),
                GridPosition(column = 3, row = 1),
                grid.objective,
            ),
            grid.entranceToObjectiveRoute,
        )
    }

    @Test
    fun `route is absent when room positions leave a gap`() {
        val grid = dungeonGrid(
            width = 5,
            entrance = GridPosition(column = 0, row = 1),
            objective = GridPosition(column = 4, row = 1),
            placedRooms = listOf(
                placedRoom(origin = GridPosition(column = 1, row = 1)),
            ),
        )

        assertNull(grid.entranceToObjectiveRoute)
    }

    @Test
    fun `route is absent before rooms connect its endpoints`() {
        assertNull(dungeonGrid().entranceToObjectiveRoute)
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
            width = 6,
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
            width = 6,
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
            width = 6,
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
        objective: GridPosition = GridPosition(column = width - 1, row = 1),
        placedRooms: List<PlacedRoom> = emptyList(),
    ) = DungeonGrid(
        width = width,
        height = height,
        entrance = entrance,
        objective = objective,
        placedRooms = placedRooms,
    )

    private fun placedRoom(
        origin: GridPosition,
        doorPosition: GridPosition = GridPosition(column = 0, row = 0),
        footprint: Set<GridPosition> = setOf(
            GridPosition(column = 0, row = 0),
            GridPosition(column = 1, row = 0),
        ),
    ) = PlacedRoom(
        blueprint = RoomBlueprint(
            footprint = footprint,
            doorPositions = setOf(doorPosition),
        ),
        origin = origin,
    )

    private fun socketRoom(
        origin: GridPosition,
        socketType: RoomSocketType = RoomSocketType.FLOOR,
    ) = PlacedRoom(
        blueprint = RoomBlueprint(
            footprint = setOf(GridPosition(column = 0, row = 0)),
            doorPositions = setOf(GridPosition(column = 0, row = 0)),
            sockets = mapOf(
                GridPosition(column = 0, row = 0) to socketType,
            ),
        ),
        origin = origin,
    )

    private fun trapDefinition(
        compatibleSocketTypes: Set<RoomSocketType> =
            setOf(RoomSocketType.FLOOR),
    ) = TrapDefinition(
        id = "spike_trap",
        displayName = "Spike Trap",
        compatibleSocketTypes = compatibleSocketTypes,
    )
}
