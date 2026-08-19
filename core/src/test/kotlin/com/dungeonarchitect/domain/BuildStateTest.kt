package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BuildStateTest {
    @Test
    fun `selected room orientation cycles in both directions`() {
        val room = blueprint("square-room")
        val state = BuildState(
            availableRoomBlueprints = listOf(room),
            selectedRoomBlueprint = room,
            selectedTrapDefinition = trapDefinition(),
        )

        assertEquals(RoomOrientation.UNROTATED, state.selectedRoomOrientation)

        val clockwise = RoomOrientation.entries.drop(1) + RoomOrientation.UNROTATED
        clockwise.forEach { expected ->
            state.rotateSelectedRoomClockwise()
            assertEquals(expected, state.selectedRoomOrientation)
        }

        val counterClockwise = listOf(
            RoomOrientation.CLOCKWISE_270,
            RoomOrientation.CLOCKWISE_180,
            RoomOrientation.CLOCKWISE_90,
            RoomOrientation.UNROTATED,
        )
        counterClockwise.forEach { expected ->
            state.rotateSelectedRoomCounterClockwise()
            assertEquals(expected, state.selectedRoomOrientation)
        }
    }

    @Test
    fun `build state accepts an initial selected room orientation`() {
        val room = blueprint("square-room")
        val state = BuildState(
            availableRoomBlueprints = listOf(room),
            selectedRoomBlueprint = room,
            selectedTrapDefinition = trapDefinition(),
            selectedRoomOrientation = RoomOrientation.CLOCKWISE_180,
        )

        assertEquals(
            RoomOrientation.CLOCKWISE_180,
            state.selectedRoomOrientation,
        )
    }

    @Test
    fun `build state stores ordered choices and selected blueprint`() {
        val squareRoom = blueprint("square-room")
        val longGallery = blueprint("long-gallery")

        val state = BuildState(
            availableRoomBlueprints = listOf(squareRoom, longGallery),
            selectedRoomBlueprint = longGallery,
            selectedTrapDefinition = trapDefinition(),
        )

        assertEquals(
            listOf(squareRoom, longGallery),
            state.availableRoomBlueprints,
        )
        assertSame(longGallery, state.selectedRoomBlueprint)
    }

    @Test
    fun `build state snapshots mutable available choices`() {
        val squareRoom = blueprint("square-room")
        val longGallery = blueprint("long-gallery")
        val choices = mutableListOf(squareRoom, longGallery)
        val state = BuildState(
            availableRoomBlueprints = choices,
            selectedRoomBlueprint = squareRoom,
            selectedTrapDefinition = trapDefinition(),
        )

        choices.clear()

        assertEquals(
            listOf(squareRoom, longGallery),
            state.availableRoomBlueprints,
        )
    }

    @Test
    fun `build state resolves selection to the available blueprint with that ID`() {
        val availableBlueprint = blueprint("square-room")
        val equivalentSelection = blueprint("square-room")

        val state = BuildState(
            availableRoomBlueprints = listOf(availableBlueprint),
            selectedRoomBlueprint = equivalentSelection,
            selectedTrapDefinition = trapDefinition(),
        )

        assertSame(availableBlueprint, state.selectedRoomBlueprint)
    }

    @Test
    fun `selecting an available blueprint updates the canonical selection`() {
        val squareRoom = blueprint("square-room")
        val longGallery = blueprint("long-gallery")
        val state = BuildState(
            availableRoomBlueprints = listOf(squareRoom, longGallery),
            selectedRoomBlueprint = squareRoom,
            selectedTrapDefinition = trapDefinition(),
        )
        state.rotateSelectedRoomClockwise()

        assertTrue(state.selectRoomBlueprint("long-gallery"))

        assertSame(longGallery, state.selectedRoomBlueprint)
        assertEquals(
            RoomOrientation.CLOCKWISE_90,
            state.selectedRoomOrientation,
        )
    }

    @Test
    fun `selecting an unknown blueprint leaves the current selection unchanged`() {
        val squareRoom = blueprint("square-room")
        val longGallery = blueprint("long-gallery")
        val state = BuildState(
            availableRoomBlueprints = listOf(squareRoom, longGallery),
            selectedRoomBlueprint = squareRoom,
            selectedTrapDefinition = trapDefinition(),
        )

        assertFalse(state.selectRoomBlueprint("unknown-room"))

        assertSame(squareRoom, state.selectedRoomBlueprint)
    }

    @Test
    fun `build state requires an available room blueprint`() {
        assertFailsWith<IllegalArgumentException> {
            BuildState(
                availableRoomBlueprints = emptyList(),
                selectedRoomBlueprint = blueprint("square-room"),
                selectedTrapDefinition = trapDefinition(),
            )
        }
    }

    @Test
    fun `build state rejects duplicate room blueprint IDs`() {
        assertFailsWith<IllegalArgumentException> {
            BuildState(
                availableRoomBlueprints = listOf(
                    blueprint("repeated-room"),
                    blueprint("repeated-room"),
                ),
                selectedRoomBlueprint = blueprint("repeated-room"),
                selectedTrapDefinition = trapDefinition(),
            )
        }
    }

    @Test
    fun `build state rejects a selected blueprint that is not available`() {
        assertFailsWith<IllegalArgumentException> {
            BuildState(
                availableRoomBlueprints = listOf(blueprint("square-room")),
                selectedRoomBlueprint = blueprint("unknown-room"),
                selectedTrapDefinition = trapDefinition(),
            )
        }
    }

    @Test
    fun `build state stores the selected trap definition`() {
        val selectedTrapDefinition = trapDefinition()

        val state = BuildState(
            availableRoomBlueprints = listOf(blueprint("square-room")),
            selectedRoomBlueprint = blueprint("square-room"),
            selectedTrapDefinition = selectedTrapDefinition,
        )

        assertSame(selectedTrapDefinition, state.selectedTrapDefinition)
    }

    private fun blueprint(id: String) = RoomBlueprint(
        id = id,
        displayName = id,
        heartAnchor = GridPosition(column = 0, row = 0),
        footprint = setOf(GridPosition(column = 0, row = 0)),
        doorPositions = setOf(GridPosition(column = 0, row = 0)),
    )

    private fun trapDefinition() = TrapDefinition(
        id = "spike_trap",
        displayName = "Spike Trap",
        damage = 5,
        cooldownSeconds = 0.25f,
        compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
    )
}
