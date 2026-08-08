package com.dungeonarchitect.application

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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PrototypeScreenTest {
    @Test
    fun `prototype grid starts with one authored room`() {
        val room = PrototypeScreen.prototypeGrid().placedRooms.single()

        assertEquals(GridPosition(column = 6, row = 3), room.origin)
        assertEquals(
            setOf(
                GridPosition(column = 6, row = 3),
                GridPosition(column = 7, row = 3),
                GridPosition(column = 8, row = 3),
                GridPosition(column = 6, row = 4),
                GridPosition(column = 7, row = 4),
                GridPosition(column = 8, row = 4),
                GridPosition(column = 6, row = 5),
                GridPosition(column = 7, row = 5),
                GridPosition(column = 8, row = 5),
            ),
            room.gridPositions,
        )
    }

    @Test
    fun `authored prototype room has one floor trap socket`() {
        val room = PrototypeScreen.prototypeGrid().placedRooms.single()

        assertEquals(
            mapOf(
                GridPosition(column = 1, row = 1) to RoomSocketType.FLOOR,
            ),
            room.blueprint.sockets,
        )
    }

    @Test
    fun `placement preview uses the authored blueprint at the hovered origin`() {
        val grid = PrototypeScreen.prototypeGrid()

        val preview = PrototypeScreen.placementPreview(
            grid = grid,
            hoveredPosition = GridPosition(column = 3, row = 3),
        )!!

        assertEquals(GridPosition(column = 3, row = 3), preview.room.origin)
        assertEquals(grid.placedRooms.single().blueprint, preview.room.blueprint)
        assertTrue(preview.isValid)
    }

    @Test
    fun `placement preview reports invalid candidate without changing placed rooms`() {
        val grid = PrototypeScreen.prototypeGrid()

        val preview = PrototypeScreen.placementPreview(
            grid = grid,
            hoveredPosition = GridPosition(column = 6, row = 3),
        )!!

        assertFalse(preview.isValid)
        assertEquals(1, grid.placedRooms.size)
    }

    @Test
    fun `placement preview is absent when the pointer is outside the grid`() {
        assertNull(
            PrototypeScreen.placementPreview(
                grid = PrototypeScreen.prototypeGrid(),
                hoveredPosition = null,
            ),
        )
    }

    @Test
    fun `click commits the current valid placement preview`() {
        val grid = PrototypeScreen.prototypeGrid()
        val clickedPosition = GridPosition(column = 3, row = 3)

        assertTrue(PrototypeScreen.commitPlacement(grid, clickedPosition))
        assertEquals(2, grid.placedRooms.size)
        assertEquals(clickedPosition, grid.placedRooms.last().origin)
    }

    @Test
    fun `invalid click leaves prototype rooms unchanged`() {
        val grid = PrototypeScreen.prototypeGrid()
        val roomsBeforeClick = grid.placedRooms

        assertFalse(
            PrototypeScreen.commitPlacement(
                grid = grid,
                clickedPosition = GridPosition(column = 6, row = 3),
            ),
        )
        assertEquals(roomsBeforeClick, grid.placedRooms)
    }

    @Test
    fun `application loads the authored wave through the supplied internal text reader`() {
        var requestedPath: String? = null

        val wave = PrototypeScreen.loadUpcomingWave { path ->
            requestedPath = path
            """
                {
                  "heroType": "militia_recruit",
                  "heroDisplayName": "Militia Recruit",
                  "count": 4,
                  "heroHealth": 10,
                  "objectiveDamage": 10,
                  "movementSpeedTilesPerSecond": 2.0,
                  "traitDescription": "A straightforward melee fighter."
                }
            """.trimIndent()
        }

        assertEquals("content/upcoming-hero-wave.json", requestedPath)
        assertEquals("Militia Recruit", wave.heroDisplayName)
    }

    @Test
    fun `application loads the authored trap through the supplied internal text reader`() {
        var requestedPath: String? = null

        val trap = PrototypeScreen.loadTrapDefinition { path ->
            requestedPath = path
            """
                {
                  "id": "spike_trap",
                  "displayName": "Spike Trap",
                  "damage": 5,
                  "cooldownSeconds": 0.25,
                  "compatibleSocketTypes": ["floor"]
                }
            """.trimIndent()
        }

        assertEquals("content/spike-trap.json", requestedPath)
        assertEquals("spike_trap", trap.id)
        assertTrue(trap.isCompatibleWith(RoomSocketType.FLOOR))
    }

    @Test
    fun `application loads authored run rules through the supplied internal text reader`() {
        var requestedPath: String? = null

        val runDefinition = PrototypeScreen.loadRunDefinition { path ->
            requestedPath = path
            """{ "objectiveHealth": 10 }"""
        }

        assertEquals("content/prototype-run.json", requestedPath)
        assertEquals(10, runDefinition.objectiveHealth)
    }

    @Test
    fun `prototype places one authored trap in its translated room socket`() {
        val grid = PrototypeScreen.prototypeGrid()
        val definition = trapDefinition()

        assertTrue(PrototypeScreen.placePrototypeTrap(grid, definition))

        val placedTrap = grid.placedTraps.single()
        assertSame(definition, placedTrap.definition)
        assertEquals(grid.placedRooms.single(), placedTrap.room)
        assertEquals(
            GridPosition(column = 1, row = 1),
            placedTrap.localSocketPosition,
        )
        assertEquals(GridPosition(column = 7, row = 4), placedTrap.gridPosition)
        assertFalse(PrototypeScreen.placePrototypeTrap(grid, definition))
        assertEquals(1, grid.placedTraps.size)
    }

    @Test
    fun `start control click wins over room placement and starts a ready wave`() {
        val grid = readyGrid()
        val controller = runController(grid)
        val roomsBeforeClick = grid.placedRooms
        val bounds = ControlBounds(x = 10f, y = 20f, width = 100f, height = 40f)

        val result = PrototypeScreen.handleClick(
            grid = grid,
            clickedPosition = GridPosition(column = 1, row = 0),
            worldX = 60f,
            worldY = 40f,
            startButtonBounds = bounds,
            runController = controller,
        )

        assertEquals(PrototypeClickResult.WAVE_STARTED, result)
        assertEquals(roomsBeforeClick, grid.placedRooms)
        assertTrue(controller.startedWave != null)
    }

    @Test
    fun `click outside start control remains a placement click`() {
        val grid = PrototypeScreen.prototypeGrid()
        val controller = runController(grid)
        val clickedPosition = GridPosition(column = 3, row = 3)

        val result = PrototypeScreen.handleClick(
            grid = grid,
            clickedPosition = clickedPosition,
            worldX = 9f,
            worldY = 40f,
            startButtonBounds = ControlBounds(
                x = 10f,
                y = 20f,
                width = 100f,
                height = 40f,
            ),
            runController = controller,
        )

        assertEquals(PrototypeClickResult.ROOM_PLACED, result)
        assertEquals(clickedPosition, grid.placedRooms.last().origin)
        assertNull(controller.startedWave)
    }

    @Test
    fun `terminal control click restarts the run`() {
        val grid = readyGrid()
        val controller = runController(grid)
        assertTrue(controller.start())
        controller.advance(elapsedSeconds = 1f)
        assertEquals(PrototypeRunPhase.DEFEAT, controller.phase)

        val result = PrototypeScreen.handleClick(
            grid = grid,
            clickedPosition = null,
            worldX = 60f,
            worldY = 40f,
            startButtonBounds = ControlBounds(
                x = 10f,
                y = 20f,
                width = 100f,
                height = 40f,
            ),
            runController = controller,
        )

        assertEquals(PrototypeClickResult.RUN_RESTARTED, result)
        assertEquals(PrototypeRunPhase.BUILDING, controller.phase)
    }

    private fun readyGrid() = DungeonGrid(
        width = 3,
        height = 1,
        entrance = GridPosition(column = 0, row = 0),
        objective = GridPosition(column = 2, row = 0),
        placedRooms = listOf(
            PlacedRoom(
                blueprint = RoomBlueprint(
                    id = "test-room",
                    displayName = "Test Room",
                    footprint = setOf(GridPosition(column = 0, row = 0)),
                    doorPositions = setOf(GridPosition(column = 0, row = 0)),
                ),
                origin = GridPosition(column = 1, row = 0),
            ),
        ),
    )

    private fun upcomingWave() = UpcomingHeroWave(
        heroType = "militia_recruit",
        heroDisplayName = "Militia Recruit",
        count = 4,
        heroHealth = 10,
        objectiveDamage = 10,
        movementSpeedTilesPerSecond = 2f,
        traitDescription = "A straightforward melee fighter.",
    )

    private fun runController(grid: DungeonGrid) = PrototypeRunController(
        grid = grid,
        upcomingWave = upcomingWave(),
        runDefinition = PrototypeRunDefinition(objectiveHealth = 10),
    )

    private fun trapDefinition() = TrapDefinition(
        id = "spike_trap",
        displayName = "Spike Trap",
        damage = 5,
        cooldownSeconds = 0.25f,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
    )
}
