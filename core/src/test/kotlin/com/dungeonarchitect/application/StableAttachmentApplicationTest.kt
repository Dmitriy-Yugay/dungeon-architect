package com.dungeonarchitect.application

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomDoor
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StableAttachmentApplicationTest {
    @Test
    fun `clicking another candidate cannot bypass the selected attachment`() {
        val grid = DungeonGrid(
            width = 7,
            height = 6,
            entrance = GridPosition(1, 2),
            entranceFacing = CardinalDirection.EAST,
        )
        val blueprint = branchingRoom()
        val entrancePreview = assertNotNull(
            grid.targetedPlacementPreview(
                blueprint,
                grid.roomAttachmentTargets.single(),
            ),
        )
        assertTrue(grid.place(entrancePreview.room))
        val eastTarget = grid.roomAttachmentTargets.single { target ->
            target.facing == CardinalDirection.EAST
        }
        val northTarget = grid.roomAttachmentTargets.single { target ->
            target.facing == CardinalDirection.NORTH
        }
        val northPreview = assertNotNull(
            grid.targetedPlacementPreview(blueprint, northTarget),
        )
        val buildState = buildState(blueprint)
        assertTrue(buildState.selectRoomAttachmentTarget(eastTarget))

        assertFalse(
            PrototypeScreen.commitPlacement(
                grid = grid,
                buildState = buildState,
                clickedPosition = northPreview.room.gridPositions.single(),
                runPhase = PrototypeRunPhase.DEFENSE_PREPARATION,
            ),
        )
    }

    private fun branchingRoom() = RoomBlueprint(
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

    private fun buildState(blueprint: RoomBlueprint) = BuildState(
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
}
