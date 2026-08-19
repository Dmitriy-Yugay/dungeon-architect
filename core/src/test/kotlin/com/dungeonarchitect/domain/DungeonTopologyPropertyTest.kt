package com.dungeonarchitect.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DungeonTopologyPropertyTest : FunSpec({
    test("generated turned and branched layouts preserve topology invariants") {
        checkAll(
            iterations = 500,
            Arb.int(1..4),
            Arb.int(2..6),
            Arb.int(1..3),
            Arb.list(Arb.int(1..3), 3..3),
        ) { horizontalTail, verticalLength, branchCount, branchLengthSeeds ->
            val scenario = TopologyScenario(
                horizontalLength = branchCount + 2 + horizontalTail,
                verticalLength = verticalLength,
                branchLengths = branchLengthSeeds.take(branchCount),
            )
            val layout = buildLayout(scenario)

            assertConnectionInvariants(layout.grid)
            assertRouteInvariants(layout.grid, layout.mainRoute)
            assertTurnedAndBranched(layout)

            layout.branchPlacements.asReversed().forEach { placement ->
                assertEquals(placement.room, layout.grid.placedRooms.last())
                assertFalse(placement.parentDoor in layout.grid.openRoomDoors)

                assertTrue(layout.grid.cancelLastPlacedRoom())

                assertFalse(placement.room in layout.grid.placedRooms)
                assertTrue(placement.parentDoor in layout.grid.openRoomDoors)
                assertRouteInvariants(layout.grid, layout.mainRoute)
                assertConnectionInvariants(layout.grid)
            }

            val routeRoom = layout.mainRooms.last()
            val routeParentDoor = layout.grid.roomDoorConnections
                .single { connection -> connection.contains(routeRoom) }
                .otherDoor(routeRoom)
            assertFalse(routeParentDoor in layout.grid.openRoomDoors)

            assertTrue(layout.grid.cancelLastPlacedRoom())

            assertFalse(routeRoom in layout.grid.placedRooms)
            assertTrue(routeParentDoor in layout.grid.openRoomDoors)
            assertNull(layout.grid.placedHeart)
            assertNull(layout.grid.entranceToHeartRoute)
            assertTrue(routeRoom.gridPositions.any { it in layout.mainRoute })
            assertConnectionInvariants(layout.grid)
        }
    }
})

private const val MAIN_ROW = 4

private data class TopologyScenario(
    val horizontalLength: Int,
    val verticalLength: Int,
    val branchLengths: List<Int>,
)

private data class GeneratedLayout(
    val grid: DungeonGrid,
    val mainRooms: List<PlacedRoom>,
    val mainRoute: List<GridPosition>,
    val branchPlacements: List<BranchPlacement>,
)

private data class BranchPlacement(
    val room: PlacedRoom,
    val parentDoor: PlacedRoomDoor,
)

private fun buildLayout(scenario: TopologyScenario): GeneratedLayout {
    val grid = DungeonGrid(
        width = scenario.horizontalLength + 2,
        height = MAIN_ROW + scenario.verticalLength + 2,
        entrance = position(0, MAIN_ROW),
    )
    val mainRooms = buildList {
        (1..scenario.horizontalLength).forEach { column ->
            val doors = buildSet {
                add(CardinalDirection.WEST)
                if (column < scenario.horizontalLength) {
                    add(CardinalDirection.EAST)
                } else {
                    add(CardinalDirection.NORTH)
                }
                if (column <= scenario.branchLengths.size) {
                    add(CardinalDirection.SOUTH)
                }
            }
            addAndPlace(
                grid = grid,
                id = "main-horizontal-$column",
                origin = position(column, MAIN_ROW),
                facings = doors,
            )
        }

        (1..scenario.verticalLength).forEach { rowOffset ->
            addAndPlace(
                grid = grid,
                id = "main-vertical-$rowOffset",
                origin = position(
                    scenario.horizontalLength,
                    MAIN_ROW + rowOffset,
                ),
                facings = setOf(
                    CardinalDirection.SOUTH,
                    CardinalDirection.NORTH,
                ),
            )
        }
    }
    assertTrue(grid.placeOrRelocateHeart(mainRooms.last()))
    val mainRoute = requireNotNull(grid.entranceToHeartRoute)
    val branchPlacements = buildList {
        scenario.branchLengths.forEachIndexed { branchIndex, branchLength ->
            val branchColumn = branchIndex + 1
            var parent = mainRooms[branchIndex]
            (1..branchLength).forEach { segment ->
                val parentDoor = parent.doors.single { door ->
                    door.door.facing == CardinalDirection.SOUTH
                }
                val facings = buildSet {
                    add(CardinalDirection.NORTH)
                    if (segment < branchLength) add(CardinalDirection.SOUTH)
                }
                val room = placeRoom(
                    grid = grid,
                    id = "branch-$branchIndex-$segment",
                    origin = position(branchColumn, MAIN_ROW - segment),
                    facings = facings,
                )
                add(BranchPlacement(room = room, parentDoor = parentDoor))
                parent = room
            }
        }
    }

    return GeneratedLayout(
        grid = grid,
        mainRooms = mainRooms,
        mainRoute = mainRoute,
        branchPlacements = branchPlacements,
    )
}

private fun MutableList<PlacedRoom>.addAndPlace(
    grid: DungeonGrid,
    id: String,
    origin: GridPosition,
    facings: Set<CardinalDirection>,
) {
    add(placeRoom(grid, id, origin, facings))
}

private fun placeRoom(
    grid: DungeonGrid,
    id: String,
    origin: GridPosition,
    facings: Set<CardinalDirection>,
): PlacedRoom {
    val room = oneCellRoom(id, origin, facings)
    assertTrue(grid.canPlace(room), "$id should be a legal placement")
    assertTrue(grid.place(room), "$id should be committed")
    return room
}

private fun oneCellRoom(
    id: String,
    origin: GridPosition,
    facings: Set<CardinalDirection>,
) = PlacedRoom(
    blueprint = RoomBlueprint(
        id = id,
        displayName = "Generated ${fixtureName(facings)} $id",
        heartAnchor = GridPosition(column = 0, row = 0),
        footprint = setOf(position(0, 0)),
        doors = facings.map { facing ->
            RoomDoor(position = position(0, 0), facing = facing)
        },
    ),
    origin = origin,
)

private fun fixtureName(facings: Set<CardinalDirection>): String = when {
    facings.size >= 3 -> "junction"
    facings.size == 1 -> "dead end"
    facings.any { it.opposite in facings } -> "straight room"
    else -> "corner room"
}

private fun assertConnectionInvariants(grid: DungeonGrid) {
    assertEquals(grid.placedRooms.size - 1, grid.roomDoorConnections.size)
    grid.roomDoorConnections.forEach { connection ->
        assertEquals(
            connection.first.door.facing.opposite,
            connection.second.door.facing,
        )
        assertEquals(connection.first.outsidePosition, connection.second.gridPosition)
        assertEquals(connection.second.outsidePosition, connection.first.gridPosition)
        assertEquals(
            1,
            manhattanDistance(
                connection.first.gridPosition,
                connection.second.gridPosition,
            ),
        )
    }
}

private fun assertRouteInvariants(
    grid: DungeonGrid,
    expectedRoute: List<GridPosition>,
) {
    val route = requireNotNull(grid.entranceToHeartRoute)
    assertEquals(expectedRoute, route)
    assertEquals(grid.entrance, route.first())
    assertEquals(grid.placedHeart?.gridPosition, route.last())
    assertTrue(route.drop(1).all { it in grid.walkablePositions })
    assertTrue(
        route.zipWithNext().all { (first, second) ->
            manhattanDistance(first, second) == 1
        },
    )
}

private fun assertTurnedAndBranched(layout: GeneratedLayout) {
    val routeDirections = layout.mainRoute.zipWithNext().map { (first, second) ->
        directionFrom(first, second)
    }
    assertTrue(CardinalDirection.EAST in routeDirections)
    assertTrue(CardinalDirection.NORTH in routeDirections)
    assertTrue(
        routeDirections.zipWithNext().any { (first, second) -> first != second },
    )
    assertTrue(layout.branchPlacements.isNotEmpty())
    assertTrue(
        layout.branchPlacements.any { placement ->
            placement.room.gridPositions.none { it in layout.mainRoute }
        },
    )
}

private fun RoomDoorConnection.contains(room: PlacedRoom): Boolean =
    first.room == room || second.room == room

private fun RoomDoorConnection.otherDoor(room: PlacedRoom): PlacedRoomDoor =
    if (first.room == room) second else first

private fun directionFrom(
    first: GridPosition,
    second: GridPosition,
): CardinalDirection = CardinalDirection.entries.single { direction ->
    direction.move(first) == second
}

private fun manhattanDistance(
    first: GridPosition,
    second: GridPosition,
): Int = abs(first.column - second.column) + abs(first.row - second.row)

private fun position(column: Int, row: Int) = GridPosition(column, row)
