package com.dungeonarchitect.application

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.HeartPlacementPreview
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomPlacementPreview
import com.dungeonarchitect.presentation.TrapPlacementPreview
import com.dungeonarchitect.presentation.trapPlacementPreviewFor

internal data class BuildPlacementPreviews(
    val room: RoomPlacementPreview?,
    val trap: TrapPlacementPreview?,
    val heart: HeartPlacementPreview?,
) {
    init {
        require(listOfNotNull(room, trap, heart).size <= 1) {
            "Only one build placement preview may be shown at a time."
        }
    }
}

internal fun buildPlacementPreviews(
    grid: DungeonGrid,
    buildState: BuildState,
    hoveredPosition: GridPosition?,
    runPhase: PrototypeRunPhase = PrototypeRunPhase.BUILDING,
): BuildPlacementPreviews {
    if (hoveredPosition == null) {
        return BuildPlacementPreviews(room = null, trap = null, heart = null)
    }
    if (buildState.isHeartPlacementModeActive) {
        return BuildPlacementPreviews(
            room = null,
            trap = null,
            heart = if (runPhase == PrototypeRunPhase.BUILDING) {
                grid.heartPlacementPreview(hoveredPosition)
            } else {
                null
            },
        )
    }

    val trapPreview = trapPlacementPreviewFor(
        hoverResult = grid.trapSocketHoverResult(
            hoveredPosition = hoveredPosition,
            definition = buildState.selectedTrapDefinition,
        ),
        hoveredPosition = hoveredPosition,
    )
    return if (trapPreview != null) {
        BuildPlacementPreviews(room = null, trap = trapPreview, heart = null)
    } else {
        BuildPlacementPreviews(
            room = grid.snappedPlacementPreview(
                blueprint = buildState.selectedRoomBlueprint,
                hoveredPosition = hoveredPosition,
                orientation = buildState.selectedRoomOrientation,
            ),
            trap = null,
            heart = null,
        )
    }
}
