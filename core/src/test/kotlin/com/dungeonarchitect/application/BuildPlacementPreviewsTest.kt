package com.dungeonarchitect.application

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomDoor
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.presentation.TrapPlacementPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class BuildPlacementPreviewsTest {
    @Test
    fun `compatible socket shows only valid trap preview`() {
        val fixture = fixture()

        val previews = buildPlacementPreviews(
            grid = fixture.grid,
            buildState = fixture.buildState,
            hoveredPosition = position(5, 3),
        )

        assertNull(previews.room)
        assertEquals(
            TrapPlacementPreview(position(5, 3), isValid = true),
            previews.trap,
        )
    }

    @Test
    fun `occupied socket shows only invalid trap preview`() {
        val fixture = fixture()
        assertTrue(
            fixture.grid.placeTrap(
                room = fixture.room,
                localSocketPosition = FLOOR_SOCKET,
                definition = fixture.buildState.selectedTrapDefinition,
            ),
        )

        val previews = buildPlacementPreviews(
            grid = fixture.grid,
            buildState = fixture.buildState,
            hoveredPosition = position(5, 3),
        )

        assertNull(previews.room)
        assertEquals(
            TrapPlacementPreview(position(5, 3), isValid = false),
            previews.trap,
        )
    }

    @Test
    fun `incompatible socket shows only invalid trap preview`() {
        val fixture = fixture()

        val previews = buildPlacementPreviews(
            grid = fixture.grid,
            buildState = fixture.buildState,
            hoveredPosition = position(4, 3),
        )

        assertNull(previews.room)
        assertEquals(
            TrapPlacementPreview(position(4, 3), isValid = false),
            previews.trap,
        )
    }

    @Test
    fun `non-socket cell shows only existing room preview`() {
        val fixture = fixture()

        val previews = buildPlacementPreviews(
            grid = fixture.grid,
            buildState = fixture.buildState,
            hoveredPosition = position(6, 2),
        )

        assertNull(previews.trap)
        assertEquals(position(6, 2), previews.room?.room?.origin)
        assertTrue(previews.room!!.isValid)
    }

    @Test
    fun `heart mode replaces trap and room previews with the room anchor preview`() {
        val fixture = fixture()
        fixture.buildState.toggleHeartPlacementMode()

        val previews = buildPlacementPreviews(
            grid = fixture.grid,
            buildState = fixture.buildState,
            hoveredPosition = position(5, 3),
        )

        assertNull(previews.room)
        assertNull(previews.trap)
        assertEquals(position(4, 2), previews.heart?.position)
        assertEquals(fixture.room, previews.heart?.room)
        assertTrue(previews.heart!!.isValid)
    }

    @Test
    fun `heart preview is hidden outside building even if mode remains selected`() {
        val fixture = fixture()
        fixture.buildState.toggleHeartPlacementMode()

        val previews = buildPlacementPreviews(
            grid = fixture.grid,
            buildState = fixture.buildState,
            hoveredPosition = position(5, 3),
            runPhase = PrototypeRunPhase.COMBAT,
        )

        assertNull(previews.room)
        assertNull(previews.trap)
        assertNull(previews.heart)
    }

    @Test
    fun `post-draft preparation hides room construction but keeps defense preview`() {
        val fixture = fixture()

        val defensePreview = buildPlacementPreviews(
            grid = fixture.grid,
            buildState = fixture.buildState,
            hoveredPosition = position(5, 3),
            runPhase = PrototypeRunPhase.DEFENSE_PREPARATION,
            isRoomPlacementEnabled = false,
        )
        val emptyCellPreview = buildPlacementPreviews(
            grid = fixture.grid,
            buildState = fixture.buildState,
            hoveredPosition = position(6, 2),
            runPhase = PrototypeRunPhase.DEFENSE_PREPARATION,
            isRoomPlacementEnabled = false,
        )

        assertNotNull(defensePreview.trap)
        assertNull(defensePreview.room)
        assertNull(emptyCellPreview.room)
        assertNull(emptyCellPreview.trap)
    }

    @Test
    fun `selected attachment keeps room ghost visible without a hovered cell`() {
        val grid = DungeonGrid(
            width = 8,
            height = 5,
            entrance = position(0, 2),
            entranceFacing = CardinalDirection.EAST,
        )
        val blueprint = RoomBlueprint(
            id = "corridor",
            displayName = "Corridor",
            footprint = setOf(position(0, 0), position(1, 0)),
            heartAnchor = position(1, 0),
            doors = listOf(
                RoomDoor(position(0, 0), CardinalDirection.WEST),
                RoomDoor(position(1, 0), CardinalDirection.EAST),
            ),
        )
        val buildState = BuildState(
            availableRoomBlueprints = listOf(blueprint),
            selectedRoomBlueprint = blueprint,
            selectedTrapDefinition = TrapDefinition(
                id = "trap",
                displayName = "Trap",
                damage = 1,
                cooldownSeconds = 1f,
                compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
            ),
        )
        val target = grid.roomAttachmentTargets.single()

        val previews = buildPlacementPreviews(
            grid = grid,
            buildState = buildState,
            hoveredPosition = null,
            attachmentTarget = target,
        )

        val roomPreview = assertNotNull(previews.room)
        assertTrue(roomPreview.isValid)
        assertEquals(target, roomPreview.attachmentTarget)
    }

    private fun fixture(): Fixture {
        val room = PlacedRoom(
            blueprint = RoomBlueprint(
                id = "socket-room",
                displayName = "Socket Room",
                heartAnchor = GridPosition(column = 0, row = 0),
                footprint = setOf(
                    position(0, 0),
                    position(1, 0),
                    WALL_SOCKET,
                    FLOOR_SOCKET,
                ),
                doors = listOf(
                    RoomDoor(position(0, 0), CardinalDirection.WEST),
                    RoomDoor(position(1, 0), CardinalDirection.EAST),
                ),
                sockets = mapOf(
                    WALL_SOCKET to RoomSocketType.WALL,
                    FLOOR_SOCKET to RoomSocketType.FLOOR,
                ),
            ),
            origin = position(4, 2),
        )
        val trapDefinition = TrapDefinition(
            id = "spike_trap",
            displayName = "Spike Trap",
            damage = 5,
            cooldownSeconds = 0.25f,
            compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
        )
        return Fixture(
            room = room,
            grid = DungeonGrid(
                width = 10,
                height = 8,
                entrance = position(0, 0),
                placedRooms = listOf(room),
            ),
            buildState = BuildState(
                availableRoomBlueprints = listOf(room.blueprint),
                selectedRoomBlueprint = room.blueprint,
                selectedTrapDefinition = trapDefinition,
            ),
        )
    }

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)

    private data class Fixture(
        val room: PlacedRoom,
        val grid: DungeonGrid,
        val buildState: BuildState,
    )

    private companion object {
        val WALL_SOCKET = GridPosition(column = 0, row = 1)
        val FLOOR_SOCKET = GridPosition(column = 1, row = 1)
    }
}
