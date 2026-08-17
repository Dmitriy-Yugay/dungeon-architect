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
import com.dungeonarchitect.domain.RoomOrientation
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.presentation.ControlBounds
import com.dungeonarchitect.presentation.RoomRotationControl
import com.dungeonarchitect.presentation.RoomRotationControlView
import com.dungeonarchitect.presentation.RoomRotationDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomRotationApplicationTest {
    @Test
    fun `rotation updates live preview and committed room orientation`() {
        val buildState = buildState(horizontalBlueprint())
        val grid = placementGrid()
        val hoveredPosition = position(2, 2)

        assertNull(
            buildPlacementPreviews(
                grid = grid,
                buildState = buildState,
                hoveredPosition = hoveredPosition,
            ).room,
        )

        buildState.rotateSelectedRoomClockwise()
        val preview = requireNotNull(
            buildPlacementPreviews(
                grid = grid,
                buildState = buildState,
                hoveredPosition = hoveredPosition,
            ).room,
        )

        assertEquals(RoomOrientation.CLOCKWISE_90, preview.room.orientation)
        assertEquals(position(2, 1), preview.room.origin)
        assertTrue(
            PrototypeScreen.commitPlacement(
                grid = grid,
                buildState = buildState,
                clickedPosition = hoveredPosition,
                runPhase = PrototypeRunPhase.BUILDING,
            ),
        )
        assertEquals(
            RoomOrientation.CLOCKWISE_90,
            grid.placedRooms.single().orientation,
        )
    }

    @Test
    fun `both rotation clicks take precedence over valid room placement`() {
        listOf(
            RoomRotationDirection.CLOCKWISE to RoomOrientation.CLOCKWISE_90,
            RoomRotationDirection.COUNTER_CLOCKWISE to
                RoomOrientation.CLOCKWISE_270,
        ).forEach { (direction, expectedOrientation) ->
            val buildState = buildState(horizontalBlueprint())
            val grid = placementGrid()
            val controller = controller(grid)

            val result = clickRotation(
                grid = grid,
                buildState = buildState,
                controller = controller,
                clickedPosition = position(2, 2),
                direction = direction,
            )

            assertEquals(PrototypeClickResult.ROOM_ROTATED, result)
            assertEquals(expectedOrientation, buildState.selectedRoomOrientation)
            assertEquals(emptyList(), grid.placedRooms)
        }
    }

    @Test
    fun `rotation clicks are consumed and accepted only while building`() {
        PrototypeRunPhase.entries.forEach { phase ->
            val grid = readySocketGrid()
            val buildState = buildState(grid.placedRooms.single().blueprint)
            val controller = controller(grid)
            if (phase == PrototypeRunPhase.VICTORY) {
                assertTrue(
                    grid.placeTrap(
                        room = grid.placedRooms.single(),
                        localSocketPosition = position(0, 0),
                        definition = buildState.selectedTrapDefinition,
                    ),
                )
            }
            if (phase != PrototypeRunPhase.BUILDING) {
                assertTrue(controller.start())
            }
            if (phase == PrototypeRunPhase.VICTORY ||
                phase == PrototypeRunPhase.DEFEAT
            ) {
                controller.advance(elapsedSeconds = 1f)
                assertEquals(phase, controller.phase)
            }
            val trapsBeforeClick = grid.placedTraps

            val result = clickRotation(
                grid = grid,
                buildState = buildState,
                controller = controller,
                clickedPosition = position(1, 0),
                direction = RoomRotationDirection.CLOCKWISE,
            )

            assertEquals(
                if (phase == PrototypeRunPhase.BUILDING) {
                    PrototypeClickResult.ROOM_ROTATED
                } else {
                    PrototypeClickResult.ROOM_ROTATION_REJECTED
                },
                result,
                phase.name,
            )
            assertEquals(
                if (phase == PrototypeRunPhase.BUILDING) {
                    RoomOrientation.CLOCKWISE_90
                } else {
                    RoomOrientation.UNROTATED
                },
                buildState.selectedRoomOrientation,
                phase.name,
            )
            assertEquals(trapsBeforeClick, grid.placedTraps, phase.name)
        }
    }

    private fun clickRotation(
        grid: DungeonGrid,
        buildState: BuildState,
        controller: PrototypeRunController,
        clickedPosition: GridPosition,
        direction: RoomRotationDirection,
    ): PrototypeClickResult {
        val bounds = ControlBounds(x = 0f, y = 0f, width = 10f, height = 10f)
        return PrototypeScreen.handleClick(
            grid = grid,
            buildState = buildState,
            clickedPosition = clickedPosition,
            worldX = 5f,
            worldY = 5f,
            startButtonBounds = ControlBounds(
                x = 20f,
                y = 0f,
                width = 10f,
                height = 10f,
            ),
            cancelButtonBounds = ControlBounds(
                x = 30f,
                y = 0f,
                width = 10f,
                height = 10f,
            ),
            roomChoiceControls = emptyList(),
            roomRotationControls = listOf(
                RoomRotationControl(
                    view = RoomRotationControlView(
                        direction = direction,
                        label = direction.name,
                        isEnabled = controller.phase == PrototypeRunPhase.BUILDING,
                    ),
                    bounds = bounds,
                ),
            ),
            runController = controller,
        )
    }

    private fun placementGrid() = DungeonGrid(
        width = 5,
        height = 5,
        entrance = position(2, 0),
        objective = position(4, 4),
        entranceFacing = CardinalDirection.NORTH,
    )

    private fun readySocketGrid(): DungeonGrid {
        val blueprint = RoomBlueprint(
            id = "ready-room",
            displayName = "Ready Room",
            heartAnchor = GridPosition(column = 0, row = 0),
            footprint = setOf(position(0, 0)),
            doors = listOf(
                RoomDoor(position(0, 0), CardinalDirection.WEST),
                RoomDoor(position(0, 0), CardinalDirection.EAST),
            ),
            sockets = mapOf(position(0, 0) to RoomSocketType.FLOOR),
        )
        return DungeonGrid(
            width = 3,
            height = 1,
            entrance = position(0, 0),
            objective = position(2, 0),
            placedRooms = listOf(PlacedRoom(blueprint, position(1, 0))),
        )
    }

    private fun horizontalBlueprint() = RoomBlueprint(
        id = "horizontal-room",
        displayName = "Horizontal Room",
        heartAnchor = GridPosition(column = 0, row = 0),
        footprint = setOf(position(0, 0), position(1, 0)),
        doors = listOf(
            RoomDoor(position(0, 0), CardinalDirection.WEST),
            RoomDoor(position(1, 0), CardinalDirection.EAST),
        ),
    )

    private fun buildState(blueprint: RoomBlueprint) = BuildState(
        availableRoomBlueprints = listOf(blueprint),
        selectedRoomBlueprint = blueprint,
        selectedTrapDefinition = lethalTrap(),
    )

    private fun controller(grid: DungeonGrid) = PrototypeRunController(
        grid = grid,
        upcomingWave = UpcomingHeroWave(
            heroType = "hero",
            heroDisplayName = "Hero",
            count = 1,
            heroHealth = 5,
            objectiveDamage = 10,
            movementSpeedTilesPerSecond = 60f,
            traitDescription = "Test hero.",
        ),
        runDefinition = PrototypeRunDefinition(objectiveHealth = 10),
    )

    private fun lethalTrap() = TrapDefinition(
        id = "lethal-trap",
        displayName = "Lethal Trap",
        damage = 5,
        cooldownSeconds = 0.001f,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
    )

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)
}
