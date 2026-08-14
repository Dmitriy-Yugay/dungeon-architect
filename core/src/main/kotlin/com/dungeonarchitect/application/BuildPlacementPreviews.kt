package com.dungeonarchitect.application

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.RoomPlacementPreview
import com.dungeonarchitect.presentation.TrapPlacementPreview
import com.dungeonarchitect.presentation.trapPlacementPreviewFor

internal data class BuildPlacementPreviews(
    val room: RoomPlacementPreview?,
    val trap: TrapPlacementPreview?,
) {
    init {
        require(room == null || trap == null) {
            "Room and trap placement previews must not be shown together."
        }
    }
}

internal fun buildPlacementPreviews(
    grid: DungeonGrid,
    buildState: BuildState,
    hoveredPosition: GridPosition?,
): BuildPlacementPreviews {
    if (hoveredPosition == null) {
        return BuildPlacementPreviews(room = null, trap = null)
    }

    val trapPreview = trapPlacementPreviewFor(
        hoverResult = grid.trapSocketHoverResult(
            hoveredPosition = hoveredPosition,
            definition = buildState.selectedTrapDefinition,
        ),
        hoveredPosition = hoveredPosition,
    )
    return if (trapPreview != null) {
        BuildPlacementPreviews(room = null, trap = trapPreview)
    } else {
        BuildPlacementPreviews(
            room = grid.snappedPlacementPreview(
                blueprint = buildState.selectedRoomBlueprint,
                hoveredPosition = hoveredPosition,
            ),
            trap = null,
        )
    }
}
