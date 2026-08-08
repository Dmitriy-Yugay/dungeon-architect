package com.dungeonarchitect.application

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.presentation.ControlBounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TrapPlacementCommitTest {
    @Test
    fun `commit places selected trap in canonical translated socket target`() {
        val fixture = fixture()

        val result = PrototypeScreen.commitTrapPlacement(
            grid = fixture.grid,
            buildState = fixture.buildState,
            clickedPosition = position(4, 3),
            runPhase = PrototypeRunPhase.BUILDING,
        )

        assertEquals(TrapPlacementCommitResult.PLACED, result)
        val placedTrap = fixture.grid.placedTraps.single()
        assertSame(fixture.buildState.selectedTrapDefinition, placedTrap.definition)
        assertSame(fixture.room, placedTrap.room)
        assertEquals(FLOOR_SOCKET, placedTrap.localSocketPosition)
        assertEquals(position(4, 3), placedTrap.gridPosition)
    }

    @Test
    fun `commit rejects occupied and incompatible sockets without mutation`() {
        val occupiedFixture = fixture()
        assertTrue(
            occupiedFixture.grid.placeTrap(
                room = occupiedFixture.room,
                localSocketPosition = FLOOR_SOCKET,
                definition = occupiedFixture.buildState.selectedTrapDefinition,
            ),
        )
        val occupiedTraps = occupiedFixture.grid.placedTraps

        assertEquals(
            TrapPlacementCommitResult.REJECTED,
            PrototypeScreen.commitTrapPlacement(
                grid = occupiedFixture.grid,
                buildState = occupiedFixture.buildState,
                clickedPosition = position(4, 3),
                runPhase = PrototypeRunPhase.BUILDING,
            ),
        )
        assertEquals(occupiedTraps, occupiedFixture.grid.placedTraps)

        val incompatibleFixture = fixture()
        assertEquals(
            TrapPlacementCommitResult.REJECTED,
            PrototypeScreen.commitTrapPlacement(
                grid = incompatibleFixture.grid,
                buildState = incompatibleFixture.buildState,
                clickedPosition = position(3, 3),
                runPhase = PrototypeRunPhase.BUILDING,
            ),
        )
        assertEquals(emptyList(), incompatibleFixture.grid.placedTraps)
    }

    @Test
    fun `commit rejects a valid socket outside building phase`() {
        listOf(
            PrototypeRunPhase.RUNNING,
            PrototypeRunPhase.VICTORY,
            PrototypeRunPhase.DEFEAT,
        ).forEach { phase ->
            val fixture = fixture()

            assertEquals(
                TrapPlacementCommitResult.REJECTED,
                PrototypeScreen.commitTrapPlacement(
                    grid = fixture.grid,
                    buildState = fixture.buildState,
                    clickedPosition = position(4, 3),
                    runPhase = phase,
                ),
                phase.name,
            )
            assertEquals(emptyList(), fixture.grid.placedTraps, phase.name)
        }
    }

    @Test
    fun `commit identifies non-socket without changing state`() {
        val fixture = fixture()

        assertEquals(
            TrapPlacementCommitResult.NON_SOCKET,
            PrototypeScreen.commitTrapPlacement(
                grid = fixture.grid,
                buildState = fixture.buildState,
                clickedPosition = position(5, 2),
                runPhase = PrototypeRunPhase.BUILDING,
            ),
        )
        assertEquals(emptyList(), fixture.grid.placedTraps)
    }

    @Test
    fun `compatible socket click returns trap placed and uses selected definition`() {
        val fixture = fixture()
        val clickedPosition = position(4, 3)
        val roomsBeforeClick = fixture.grid.placedRooms
        assertTrue(
            fixture.grid.placementPreview(
                blueprint = fixture.buildState.selectedRoomBlueprint,
                origin = clickedPosition,
            ).isValid,
        )

        val result = handleClick(fixture, clickedPosition = clickedPosition)

        assertEquals(PrototypeClickResult.TRAP_PLACED, result)
        assertEquals(roomsBeforeClick, fixture.grid.placedRooms)
        val placedTrap = fixture.grid.placedTraps.single()
        assertSame(fixture.buildState.selectedTrapDefinition, placedTrap.definition)
        assertSame(fixture.room, placedTrap.room)
        assertEquals(FLOOR_SOCKET, placedTrap.localSocketPosition)
    }

    @Test
    fun `occupied and incompatible socket clicks are consumed without room fallthrough`() {
        val occupiedFixture = fixture()
        assertTrue(
            occupiedFixture.grid.placeTrap(
                room = occupiedFixture.room,
                localSocketPosition = FLOOR_SOCKET,
                definition = occupiedFixture.buildState.selectedTrapDefinition,
            ),
        )
        val occupiedRooms = occupiedFixture.grid.placedRooms
        val occupiedTraps = occupiedFixture.grid.placedTraps
        assertTrue(
            occupiedFixture.grid.placementPreview(
                blueprint = occupiedFixture.buildState.selectedRoomBlueprint,
                origin = position(4, 3),
            ).isValid,
        )

        assertEquals(
            PrototypeClickResult.IGNORED,
            handleClick(occupiedFixture, clickedPosition = position(4, 3)),
        )
        assertEquals(occupiedRooms, occupiedFixture.grid.placedRooms)
        assertEquals(occupiedTraps, occupiedFixture.grid.placedTraps)

        val incompatibleFixture = fixture()
        val incompatibleRooms = incompatibleFixture.grid.placedRooms
        assertEquals(
            PrototypeClickResult.IGNORED,
            handleClick(incompatibleFixture, clickedPosition = position(3, 3)),
        )
        assertEquals(incompatibleRooms, incompatibleFixture.grid.placedRooms)
        assertEquals(emptyList(), incompatibleFixture.grid.placedTraps)
    }

    @Test
    fun `non-socket click retains existing room placement path`() {
        val fixture = fixture()

        val result = handleClick(fixture, clickedPosition = position(5, 2))

        assertEquals(PrototypeClickResult.ROOM_PLACED, result)
        assertEquals(position(5, 2), fixture.grid.placedRooms.last().origin)
        assertSame(
            fixture.buildState.selectedRoomBlueprint,
            fixture.grid.placedRooms.last().blueprint,
        )
        assertEquals(emptyList(), fixture.grid.placedTraps)
    }

    private fun handleClick(
        fixture: Fixture,
        clickedPosition: GridPosition,
    ) = PrototypeScreen.handleClick(
        grid = fixture.grid,
        buildState = fixture.buildState,
        clickedPosition = clickedPosition,
        worldX = 0f,
        worldY = 0f,
        startButtonBounds = ControlBounds(
            x = 100f,
            y = 100f,
            width = 10f,
            height = 10f,
        ),
        roomChoiceControls = emptyList(),
        runController = runController(fixture.grid),
    )

    private fun fixture(): Fixture {
        val room = PlacedRoom(
            blueprint = RoomBlueprint(
                id = "socket-room",
                displayName = "Socket Room",
                footprint = setOf(
                    position(0, 0),
                    position(1, 0),
                    WALL_SOCKET,
                    FLOOR_SOCKET,
                ),
                doorPositions = setOf(FLOOR_SOCKET),
                sockets = mapOf(
                    WALL_SOCKET to RoomSocketType.WALL,
                    FLOOR_SOCKET to RoomSocketType.FLOOR,
                ),
            ),
            origin = position(3, 2),
        )
        val selectedRoomBlueprint = RoomBlueprint(
            id = "one-cell-room",
            displayName = "Corner Room",
            footprint = setOf(
                SELECTED_ROOM_DOOR,
                position(1, 0),
                position(1, 1),
            ),
            doorPositions = setOf(SELECTED_ROOM_DOOR),
        )
        val buildState = BuildState(
            availableRoomBlueprints = listOf(selectedRoomBlueprint),
            selectedRoomBlueprint = selectedRoomBlueprint,
            selectedTrapDefinition = TrapDefinition(
                id = "spike_trap",
                displayName = "Spike Trap",
                damage = 5,
                cooldownSeconds = 0.25f,
                compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
            ),
        )
        return Fixture(
            room = room,
            grid = DungeonGrid(
                width = 10,
                height = 8,
                entrance = position(0, 0),
                objective = position(9, 7),
                placedRooms = listOf(room),
            ),
            buildState = buildState,
        )
    }

    private fun runController(grid: DungeonGrid) = PrototypeRunController(
        grid = grid,
        upcomingWave = UpcomingHeroWave(
            heroType = "militia_recruit",
            heroDisplayName = "Militia Recruit",
            count = 1,
            heroHealth = 10,
            objectiveDamage = 10,
            movementSpeedTilesPerSecond = 2f,
            traitDescription = "A straightforward melee fighter.",
        ),
        runDefinition = PrototypeRunDefinition(objectiveHealth = 10),
    )

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
        val SELECTED_ROOM_DOOR = GridPosition(column = 0, row = 1)
    }
}
