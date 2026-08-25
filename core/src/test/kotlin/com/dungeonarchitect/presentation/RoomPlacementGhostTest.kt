package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.RoomAttachmentTargetType
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomDoor
import com.dungeonarchitect.domain.RoomSocketType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertNull

class RoomPlacementGhostTest {
    @Test
    fun `ghost exposes footprint connecting and open doors socket and heart anchor`() {
        val grid = DungeonGrid(
            width = 8,
            height = 5,
            entrance = GridPosition(0, 2),
            entranceFacing = CardinalDirection.EAST,
        )
        val target = grid.roomAttachmentTargets.single()
        val preview = assertNotNull(grid.targetedPlacementPreview(blueprint(), target))

        val ghost = roomPlacementGhost(preview)

        assertTrue(ghost.isValid)
        assertEquals(null, ghost.invalidReason)
        assertEquals(setOf(GridPosition(1, 2), GridPosition(2, 2)), ghost.footprint)
        assertEquals(1, ghost.doors.count(GhostDoorMarker::isConnecting))
        assertEquals(
            setOf(GridPosition(1, 2), GridPosition(2, 2)),
            ghost.doors.mapTo(mutableSetOf(), GhostDoorMarker::position),
        )
        assertEquals(
            mapOf(GridPosition(2, 2) to RoomSocketType.FLOOR),
            ghost.sockets,
        )
        assertEquals(GridPosition(2, 2), ghost.heartAnchor)
    }

    @Test
    fun `target markers distinguish entrance open doors and active selection`() {
        val grid = DungeonGrid(
            width = 8,
            height = 5,
            entrance = GridPosition(0, 2),
            entranceFacing = CardinalDirection.EAST,
        )
        val entrance = grid.roomAttachmentTargets.single()
        val entranceMarker = roomAttachmentTargetMarkers(grid, entrance).single()
        val preview = assertNotNull(grid.targetedPlacementPreview(blueprint(), entrance))
        assertTrue(grid.place(preview.room))
        val openDoor = grid.roomAttachmentTargets.single()
        val doorMarker = roomAttachmentTargetMarkers(grid, openDoor).single()

        assertTrue(entranceMarker.isEntrance)
        assertTrue(entranceMarker.isActive)
        assertEquals(RoomAttachmentTargetType.OPEN_DOOR, openDoor.type)
        assertEquals(false, doorMarker.isEntrance)
        assertTrue(doorMarker.isActive)
    }

    @Test
    fun `edge hit testing distinguishes open doors sharing one room cell`() {
        val grid = DungeonGrid(
            width = 6,
            height = 5,
            entrance = GridPosition(0, 2),
            entranceFacing = CardinalDirection.EAST,
        )
        val branchingRoom = RoomBlueprint(
            id = "branch",
            displayName = "Branch",
            footprint = setOf(GridPosition(0, 0)),
            heartAnchor = GridPosition(0, 0),
            doors = listOf(
                RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
                RoomDoor(GridPosition(0, 0), CardinalDirection.EAST),
                RoomDoor(GridPosition(0, 0), CardinalDirection.NORTH),
            ),
        )
        val entrance = grid.roomAttachmentTargets.single()
        val preview = assertNotNull(
            grid.targetedPlacementPreview(branchingRoom, entrance),
        )
        assertTrue(grid.place(preview.room))

        val east = roomAttachmentTargetAt(
            grid,
            worldX = 128f,
            worldY = 160f,
            tileSize = 64f,
            clickRadius = 26f,
        )
        val north = roomAttachmentTargetAt(
            grid,
            worldX = 96f,
            worldY = 192f,
            tileSize = 64f,
            clickRadius = 26f,
        )

        assertEquals(CardinalDirection.EAST, east?.facing)
        assertEquals(CardinalDirection.NORTH, north?.facing)
        assertNull(
            roomAttachmentTargetAt(
                grid,
                worldX = 96f,
                worldY = 160f,
                tileSize = 64f,
                clickRadius = 26f,
            ),
        )
    }

    private fun blueprint() = RoomBlueprint(
        id = "corridor",
        displayName = "Corridor",
        footprint = setOf(GridPosition(0, 0), GridPosition(1, 0)),
        heartAnchor = GridPosition(1, 0),
        doors = listOf(
            RoomDoor(GridPosition(0, 0), CardinalDirection.WEST),
            RoomDoor(GridPosition(1, 0), CardinalDirection.EAST),
        ),
        sockets = mapOf(GridPosition(1, 0) to RoomSocketType.FLOOR),
    )
}
