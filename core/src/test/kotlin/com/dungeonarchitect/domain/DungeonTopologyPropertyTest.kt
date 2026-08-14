package com.dungeonarchitect.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DungeonTopologyPropertyTest : FunSpec({
    test("generated room chains preserve placement and route invariants") {
        checkAll(
            iterations = 500,
            Arb.list(Arb.int(1..6), 1..8),
            Arb.int(0..5),
        ) { roomWidths, routeRow ->
            val routeLength = roomWidths.sum()
            val grid = DungeonGrid(
                width = routeLength + 2,
                height = routeRow + 1,
                entrance = GridPosition(column = 0, row = routeRow),
                objective = GridPosition(column = routeLength + 1, row = routeRow),
            )

            var nextOriginColumn = 1
            val rooms = roomWidths.mapIndexed { index, roomWidth ->
                horizontalRoom(
                    id = "generated-$index",
                    width = roomWidth,
                    origin = GridPosition(nextOriginColumn, routeRow),
                ).also { room ->
                    assertTrue(grid.canPlace(room))
                    assertTrue(grid.place(room))
                    nextOriginColumn += roomWidth
                }
            }

            assertTrue(rooms.all { room -> room.gridPositions.all(grid::contains) })
            assertTrue(
                rooms.withIndex().all { (index, room) ->
                    rooms.drop(index + 1).none(room::overlaps)
                },
            )
            assertEquals(rooms.size - 1, grid.roomDoorConnections.size)

            val route = requireNotNull(grid.entranceToObjectiveRoute)
            assertEquals(grid.entrance, route.first())
            assertEquals(grid.objective, route.last())
            assertEquals(routeLength + 2, route.size)
            assertEquals(grid.walkablePositions, route.drop(1).dropLast(1).toSet())
            assertTrue(
                route.zipWithNext().all { (first, second) ->
                    abs(first.column - second.column) +
                        abs(first.row - second.row) == 1
                },
            )
        }
    }
})

private fun horizontalRoom(
    id: String,
    width: Int,
    origin: GridPosition,
): PlacedRoom {
    val footprint = (0 until width)
        .mapTo(mutableSetOf()) { column -> GridPosition(column, 0) }
    return PlacedRoom(
        blueprint = RoomBlueprint(
            id = id,
            displayName = "Generated room $id",
            footprint = footprint,
            doors = listOf(
                RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                RoomDoor(GridPosition(width - 1, 0), CardinalDirection.EAST),
            ),
        ),
        origin = origin,
    )
}
