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
import com.dungeonarchitect.domain.RunCompletionCondition
import com.dungeonarchitect.domain.RunWaveDefinition
import com.dungeonarchitect.domain.TrapDefinition
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.presentation.ControlBounds
import com.dungeonarchitect.presentation.RoomChoiceControl
import com.dungeonarchitect.presentation.RoomChoicesLayout
import com.dungeonarchitect.presentation.RoomChoicesView
import com.dungeonarchitect.presentation.WavePanelLayout
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrototypeIntermissionCadenceTest {
    @Test
    fun `playable controls traverse report reward intelligence draft and preparation`() {
        val openingRoom = routeRoom()
        val grid = DungeonGrid(
            width = 6,
            height = 1,
            entrance = position(0, 0),
            placedRooms = listOf(openingRoom),
        )
        val defense = defense()
        assertTrue(
            grid.placeTrap(openingRoom, position(0, 0), defense),
        )
        assertTrue(grid.placeOrRelocateHeart(openingRoom))
        val offeredRooms = listOf(
            draftRoom("draft-a"),
            draftRoom("draft-b"),
            draftRoom("draft-c"),
        )
        val buildState = BuildState(
            availableRoomBlueprints = offeredRooms,
            selectedRoomBlueprint = offeredRooms.first(),
            selectedTrapDefinition = defense,
        )
        val wave = wave()
        val controller = PrototypeRunController(
            grid = grid,
            waveContentByPath = mapOf(
                "content/opening.json" to wave,
                "content/finale.json" to wave,
            ),
            runDefinition = PrototypeRunDefinition(
                heartHealth = 10,
                startingGold = 2,
                waves = listOf(
                    RunWaveDefinition(
                        id = "opening",
                        contentPath = "content/opening.json",
                        rewardGold = 1,
                    ),
                    RunWaveDefinition(
                        id = "finale",
                        contentPath = "content/finale.json",
                        rewardGold = 0,
                        roomOfferBlueprintIds = offeredRooms.map { it.id },
                    ),
                ),
                completionCondition = RunCompletionCondition.CLEAR_ALL_WAVES,
            ),
        )

        assertEquals(
            PrototypeClickResult.WAVE_STARTED,
            clickPrimary(grid, buildState, controller),
        )
        assertEquals(
            PrototypeClickResult.PRIMARY_CONTROL_REJECTED,
            clickPrimary(grid, buildState, controller),
        )
        controller.advance(fixedSteps(2))
        assertEquals(PrototypeRunPhase.WAVE_REPORT, controller.phase)

        assertEquals(
            PrototypeClickResult.REWARD_CLAIMED,
            clickPrimary(grid, buildState, controller),
        )
        assertEquals(3, controller.gold)
        assertEquals(
            PrototypeClickResult.WAVE_REPORT_CONTINUED,
            clickPrimary(grid, buildState, controller),
        )
        assertEquals(PrototypeRunPhase.INTELLIGENCE, controller.phase)
        assertEquals(
            PrototypeClickResult.INTELLIGENCE_REVIEWED,
            clickPrimary(grid, buildState, controller),
        )
        assertEquals(PrototypeRunPhase.ROOM_DRAFT, controller.phase)
        assertEquals(
            PrototypeClickResult.PRIMARY_CONTROL_REJECTED,
            clickPrimary(grid, buildState, controller),
        )

        val choices = RoomChoicesView.forPhase(
            buildState,
            controller.phase,
            controller.offeredRoomBlueprintIds,
        )
        val layout = WavePanelLayout.create(
            worldWidth = 1_024f,
            panelBottom = 576f,
            roomChoiceCount = choices.choices.size,
            roomRotationControlCount = 0,
        )
        val choiceControls = RoomChoicesLayout.controls(choices, layout)
        val secondChoice = choiceControls[1]
        assertEquals(
            PrototypeClickResult.ROOM_CHOICE_SELECTED,
            click(
                grid = grid,
                buildState = buildState,
                controller = controller,
                worldX = secondChoice.bounds.x + 1f,
                worldY = secondChoice.bounds.y + 1f,
                roomChoiceControls = choiceControls,
            ),
        )
        assertEquals(
            PrototypeClickResult.ROOM_PLACED,
            click(
                grid = grid,
                buildState = buildState,
                controller = controller,
                clickedPosition = position(3, 0),
            ),
        )

        assertEquals(PrototypeRunPhase.DEFENSE_PREPARATION, controller.phase)
        assertFalse(controller.isRoomPlacementEnabled)
        assertEquals(
            emptyList(),
            RoomChoicesView.forPhase(
                buildState,
                controller.phase,
                controller.offeredRoomBlueprintIds,
            ).choices,
        )
        assertFalse(
            PrototypeScreen.applyBuildShortcut(
                buildState,
                BuildShortcut.ROTATE_CLOCKWISE,
                controller.phase,
                controller.isRoomPlacementEnabled,
            ),
        )
        assertEquals(
            PrototypeClickResult.WAVE_STARTED,
            clickPrimary(grid, buildState, controller),
        )
    }

    private fun clickPrimary(
        grid: DungeonGrid,
        buildState: BuildState,
        controller: PrototypeRunController,
    ) = click(
        grid = grid,
        buildState = buildState,
        controller = controller,
        worldX = PRIMARY.x + 1f,
        worldY = PRIMARY.y + 1f,
    )

    private fun click(
        grid: DungeonGrid,
        buildState: BuildState,
        controller: PrototypeRunController,
        worldX: Float = -1f,
        worldY: Float = -1f,
        clickedPosition: GridPosition? = null,
        roomChoiceControls: List<RoomChoiceControl> = emptyList(),
    ) = PrototypeScreen.handleClick(
        grid = grid,
        buildState = buildState,
        clickedPosition = clickedPosition,
        worldX = worldX,
        worldY = worldY,
        startButtonBounds = PRIMARY,
        cancelButtonBounds = CANCEL,
        roomChoiceControls = roomChoiceControls,
        runController = controller,
    )

    private fun routeRoom() = PlacedRoom(
        blueprint = RoomBlueprint(
            id = "opening-room",
            displayName = "Opening Room",
            footprint = setOf(position(0, 0), position(1, 0)),
            heartAnchor = position(1, 0),
            doors = listOf(
                RoomDoor(position(0, 0), CardinalDirection.WEST),
                RoomDoor(position(1, 0), CardinalDirection.EAST),
            ),
            sockets = mapOf(position(0, 0) to RoomSocketType.FLOOR),
        ),
        origin = position(1, 0),
    )

    private fun draftRoom(id: String) = RoomBlueprint(
        id = id,
        displayName = id,
        footprint = setOf(position(0, 0)),
        heartAnchor = position(0, 0),
        doors = listOf(
            RoomDoor(position(0, 0), CardinalDirection.WEST),
            RoomDoor(position(0, 0), CardinalDirection.EAST),
        ),
    )

    private fun defense() = TrapDefinition(
        id = "spike",
        displayName = "Spike Trap",
        damage = 5,
        cooldownSeconds = 0.001f,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
        costGold = 1,
    )

    private fun wave() = UpcomingHeroWave(
        heroType = "recruit",
        heroDisplayName = "Recruit",
        count = 1,
        heroHealth = 5,
        heartDamage = 1,
        movementSpeedTilesPerSecond = 60f,
        heroRole = "Frontline attacker",
        defenseImplication = "Cover multiple on-route sockets.",
        traitDescription = "A straightforward fighter.",
    )

    private fun position(column: Int, row: Int) = GridPosition(column, row)

    private fun fixedSteps(count: Int): Float =
        (FixedStepHeroSimulation.FIXED_STEP_SECONDS * count).toFloat()

    private companion object {
        val PRIMARY = ControlBounds(100f, 100f, 80f, 40f)
        val CANCEL = ControlBounds(0f, 100f, 80f, 40f)
    }
}
