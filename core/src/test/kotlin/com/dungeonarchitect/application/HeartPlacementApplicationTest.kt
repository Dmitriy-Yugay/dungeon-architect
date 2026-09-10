package com.dungeonarchitect.application

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomDoor
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.TrapSocketHoverResult
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.presentation.ControlBounds
import com.dungeonarchitect.presentation.HeartPlacementControl
import com.dungeonarchitect.presentation.HeartPlacementControlView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HeartPlacementApplicationTest {
    @Test
    fun `heart mode places and relocates before trap placement then exits`() {
        val firstRoom = socketHeartRoom("first", origin = position(3, 2))
        val secondRoom = socketHeartRoom("second", origin = position(7, 2))
        val fixture = buildFixture(firstRoom, secondRoom)
        val firstAnchor = firstRoom.toGridPosition(HEART_ANCHOR)
        assertIs<TrapSocketHoverResult.Valid>(
            fixture.grid.trapSocketHoverResult(firstAnchor, fixture.trap),
        )

        fixture.buildState.toggleHeartPlacementMode()
        assertEquals(
            PrototypeClickResult.HEART_PLACED,
            clickGrid(fixture, clickedPosition = firstRoom.origin),
        )
        assertSame(firstRoom, fixture.grid.placedHeart?.room)
        assertEquals(firstAnchor, fixture.grid.placedHeart?.gridPosition)
        assertEquals(emptyList(), fixture.grid.placedTraps)
        assertFalse(fixture.buildState.isHeartPlacementModeActive)

        fixture.buildState.toggleHeartPlacementMode()
        assertEquals(
            PrototypeClickResult.HEART_PLACED,
            clickGrid(fixture, clickedPosition = secondRoom.origin),
        )
        assertSame(secondRoom, fixture.grid.placedHeart?.room)
        assertEquals(emptyList(), fixture.grid.placedTraps)
        assertFalse(fixture.buildState.isHeartPlacementModeActive)
    }

    @Test
    fun `invalid heart clicks stay active and cannot fall through to trap or room placement`() {
        val trappedRoom = socketHeartRoom("trapped", origin = position(3, 2))
        val trappedFixture = buildFixture(trappedRoom)
        assertTrue(
            trappedFixture.grid.placeTrap(
                trappedRoom,
                HEART_ANCHOR,
                trappedFixture.trap,
            ),
        )
        trappedFixture.buildState.toggleHeartPlacementMode()

        assertEquals(
            PrototypeClickResult.HEART_PLACEMENT_REJECTED,
            clickGrid(trappedFixture, clickedPosition = trappedRoom.origin),
        )
        assertNull(trappedFixture.grid.placedHeart)
        assertEquals(1, trappedFixture.grid.placedTraps.size)
        assertTrue(trappedFixture.buildState.isHeartPlacementModeActive)

        val frontierRoom = frontierRoom(origin = position(1, 0))
        val frontierFixture = buildFixture(frontierRoom)
        val validRoomPosition = position(2, 0)
        assertTrue(
            requireNotNull(
                PrototypeScreen.placementPreview(
                    frontierFixture.grid,
                    frontierFixture.buildState,
                    validRoomPosition,
                ),
            ).isValid,
        )
        frontierFixture.buildState.toggleHeartPlacementMode()
        val roomsBeforeClick = frontierFixture.grid.placedRooms

        assertEquals(
            PrototypeClickResult.HEART_PLACEMENT_REJECTED,
            clickGrid(frontierFixture, clickedPosition = validRoomPosition),
        )
        assertEquals(roomsBeforeClick, frontierFixture.grid.placedRooms)
        assertTrue(frontierFixture.buildState.isHeartPlacementModeActive)
    }

    @Test
    fun `heart control is consumed in every phase and toggles only while building`() {
        PrototypeRunPhase.entries.forEach { phase ->
            val fixture = phaseFixture(phase)
            val control = heartControl(
                HeartPlacementControlView.from(fixture.buildState, phase),
            )

            val result = PrototypeScreen.handleClick(
                grid = fixture.grid,
                buildState = fixture.buildState,
                clickedPosition = null,
                worldX = 15f,
                worldY = 15f,
                startButtonBounds = OFFSCREEN_BOUNDS,
                cancelButtonBounds = OFFSCREEN_BOUNDS,
                roomChoiceControls = emptyList(),
                heartPlacementControl = control,
                runController = fixture.controller,
            )

            assertEquals(
                if (phase == PrototypeRunPhase.BUILDING) {
                    PrototypeClickResult.HEART_MODE_ACTIVATED
                } else {
                    PrototypeClickResult.HEART_MODE_REJECTED
                },
                result,
                phase.name,
            )
            assertEquals(
                phase == PrototypeRunPhase.BUILDING,
                fixture.buildState.isHeartPlacementModeActive,
                phase.name,
            )
        }
    }

    @Test
    fun `mode toggles off explicitly and when a wave starts`() {
        val fixture = phaseFixture(PrototypeRunPhase.BUILDING)
        val control = heartControl(
            HeartPlacementControlView.from(
                fixture.buildState,
                PrototypeRunPhase.BUILDING,
            ),
        )
        assertEquals(
            PrototypeClickResult.HEART_MODE_ACTIVATED,
            clickControl(fixture, control),
        )
        assertEquals(
            PrototypeClickResult.HEART_MODE_DEACTIVATED,
            clickControl(fixture, control),
        )
        assertFalse(fixture.buildState.isHeartPlacementModeActive)

        fixture.buildState.toggleHeartPlacementMode()
        assertEquals(
            PrototypeClickResult.WAVE_STARTED,
            PrototypeScreen.handleClick(
                grid = fixture.grid,
                buildState = fixture.buildState,
                clickedPosition = null,
                worldX = 55f,
                worldY = 55f,
                startButtonBounds = ControlBounds(50f, 50f, 20f, 20f),
                cancelButtonBounds = OFFSCREEN_BOUNDS,
                roomChoiceControls = emptyList(),
                runController = fixture.controller,
            ),
        )
        assertFalse(fixture.buildState.isHeartPlacementModeActive)
    }

    private fun phaseFixture(phase: PrototypeRunPhase): Fixture {
        val room = routeRoom()
        val fixture = buildFixture(room, width = 3, height = 1, entrance = position(0, 0))
        assertTrue(fixture.grid.placeOrRelocateHeart(room))
        if (phase == PrototypeRunPhase.VICTORY) {
            assertTrue(fixture.grid.placeTrap(room, TRAP_SOCKET, fixture.trap))
        }
        if (phase != PrototypeRunPhase.BUILDING) {
            assertTrue(fixture.controller.start())
        }
        if (phase == PrototypeRunPhase.VICTORY || phase == PrototypeRunPhase.DEFEAT) {
            fixture.controller.advance(1f)
            assertEquals(phase, fixture.controller.phase)
        }
        return fixture
    }

    private fun clickGrid(
        fixture: Fixture,
        clickedPosition: GridPosition,
    ) = PrototypeScreen.handleClick(
        grid = fixture.grid,
        buildState = fixture.buildState,
        clickedPosition = clickedPosition,
        worldX = 0f,
        worldY = 0f,
        startButtonBounds = OFFSCREEN_BOUNDS,
        cancelButtonBounds = OFFSCREEN_BOUNDS,
        roomChoiceControls = emptyList(),
        runController = fixture.controller,
    )

    private fun clickControl(
        fixture: Fixture,
        control: HeartPlacementControl,
    ) = PrototypeScreen.handleClick(
        grid = fixture.grid,
        buildState = fixture.buildState,
        clickedPosition = null,
        worldX = 15f,
        worldY = 15f,
        startButtonBounds = OFFSCREEN_BOUNDS,
        cancelButtonBounds = OFFSCREEN_BOUNDS,
        roomChoiceControls = emptyList(),
        heartPlacementControl = control,
        runController = fixture.controller,
    )

    private fun buildFixture(
        vararg rooms: PlacedRoom,
        width: Int = 12,
        height: Int = 8,
        entrance: GridPosition = position(0, 0),
    ): Fixture {
        val trap = floorTrap()
        val grid = DungeonGrid(
            width = width,
            height = height,
            entrance = entrance,
            placedRooms = rooms.toList(),
        )
        val selectedBlueprint = frontierBlueprint("selected")
        val buildState = BuildState(
            availableRoomBlueprints = listOf(selectedBlueprint),
            selectedRoomBlueprint = selectedBlueprint,
            selectedTrapDefinition = trap,
        )
        return Fixture(
            grid = grid,
            buildState = buildState,
            trap = trap,
            controller = PrototypeRunController(
                grid = grid,
                upcomingWave = UpcomingHeroWave(
                    heroType = "hero",
                    heroDisplayName = "Hero",
                    count = 1,
                    heroHealth = 1,
                    heartDamage = 10,
                    movementSpeedTilesPerSecond = 60f,
                    traitDescription = "Test hero.",
                ),
                runDefinition = PrototypeRunDefinition.singleWave(
                    heartHealth = 10,
                    contentPath = "test-wave.json",
                ),
            ),
        )
    }

    private fun socketHeartRoom(id: String, origin: GridPosition) = PlacedRoom(
        blueprint = RoomBlueprint(
            id = id,
            displayName = id,
            heartAnchor = HEART_ANCHOR,
            footprint = setOf(position(0, 0), HEART_ANCHOR),
            doors = listOf(RoomDoor(position(0, 0), CardinalDirection.WEST)),
            sockets = mapOf(HEART_ANCHOR to RoomSocketType.FLOOR),
        ),
        origin = origin,
    )

    private fun frontierRoom(origin: GridPosition) = PlacedRoom(
        blueprint = frontierBlueprint("frontier"),
        origin = origin,
    )

    private fun frontierBlueprint(id: String) = RoomBlueprint(
        id = id,
        displayName = id,
        heartAnchor = position(0, 0),
        footprint = setOf(position(0, 0)),
        doors = listOf(
            RoomDoor(position(0, 0), CardinalDirection.WEST),
            RoomDoor(position(0, 0), CardinalDirection.EAST),
        ),
    )

    private fun routeRoom(): PlacedRoom {
        val blueprint = RoomBlueprint(
            id = "route-room",
            displayName = "Route Room",
            heartAnchor = HEART_ANCHOR,
            footprint = setOf(TRAP_SOCKET, HEART_ANCHOR),
            doors = listOf(
                RoomDoor(TRAP_SOCKET, CardinalDirection.WEST),
                RoomDoor(HEART_ANCHOR, CardinalDirection.EAST),
            ),
            sockets = mapOf(TRAP_SOCKET to RoomSocketType.FLOOR),
        )
        return PlacedRoom(blueprint, position(1, 0))
    }

    private fun heartControl(view: HeartPlacementControlView) =
        HeartPlacementControl(
            view = view,
            bounds = ControlBounds(10f, 10f, 20f, 20f),
        )

    private fun floorTrap() = TrapDefinition(
        id = "trap",
        displayName = "Trap",
        damage = 1,
        cooldownSeconds = 0.01f,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
    )

    private fun position(column: Int, row: Int) = GridPosition(column, row)

    private data class Fixture(
        val grid: DungeonGrid,
        val buildState: BuildState,
        val trap: TrapDefinition,
        val controller: PrototypeRunController,
    )

    private companion object {
        val TRAP_SOCKET = GridPosition(0, 0)
        val HEART_ANCHOR = GridPosition(1, 0)
        val OFFSCREEN_BOUNDS = ControlBounds(100f, 100f, 10f, 10f)
    }
}
