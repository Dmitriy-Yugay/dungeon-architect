package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class BuildStateTest {
    @Test
    fun `build state stores ordered choices and selected blueprint`() {
        val squareRoom = blueprint("square-room")
        val longGallery = blueprint("long-gallery")

        val state = BuildState(
            availableRoomBlueprints = listOf(squareRoom, longGallery),
            selectedRoomBlueprint = longGallery,
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
        )

        assertSame(availableBlueprint, state.selectedRoomBlueprint)
    }

    @Test
    fun `build state requires an available room blueprint`() {
        assertFailsWith<IllegalArgumentException> {
            BuildState(
                availableRoomBlueprints = emptyList(),
                selectedRoomBlueprint = blueprint("square-room"),
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
            )
        }
    }

    @Test
    fun `build state rejects a selected blueprint that is not available`() {
        assertFailsWith<IllegalArgumentException> {
            BuildState(
                availableRoomBlueprints = listOf(blueprint("square-room")),
                selectedRoomBlueprint = blueprint("unknown-room"),
            )
        }
    }

    private fun blueprint(id: String) = RoomBlueprint(
        id = id,
        displayName = id,
        footprint = setOf(GridPosition(column = 0, row = 0)),
        doorPositions = setOf(GridPosition(column = 0, row = 0)),
    )
}
