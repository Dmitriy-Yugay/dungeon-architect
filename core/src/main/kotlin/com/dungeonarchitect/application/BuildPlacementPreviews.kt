package com.dungeonarchitect.application

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.HeartPlacementPreview
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomAttachmentTarget
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
    runPhase: PrototypeRunPhase = PrototypeRunPhase.DEFENSE_PREPARATION,
    attachmentTarget: RoomAttachmentTarget? = null,
): BuildPlacementPreviews {
    if (runPhase != PrototypeRunPhase.DEFENSE_PREPARATION) {
        return BuildPlacementPreviews(room = null, trap = null, heart = null)
    }
    if (buildState.isHeartPlacementModeActive) {
        return BuildPlacementPreviews(
            room = null,
            trap = null,
            heart = hoveredPosition?.let(grid::heartPlacementPreview),
        )
    }

    val trapPreview = hoveredPosition?.let { position ->
        trapPlacementPreviewFor(
            hoverResult = grid.trapSocketHoverResult(
                hoveredPosition = position,
                definition = buildState.selectedTrapDefinition,
            ),
            hoveredPosition = position,
        )
    }
    return if (trapPreview != null) {
        BuildPlacementPreviews(room = null, trap = trapPreview, heart = null)
    } else {
        BuildPlacementPreviews(
            room = if (attachmentTarget != null) {
                grid.targetedPlacementPreview(
                    blueprint = buildState.selectedRoomBlueprint,
                    target = attachmentTarget,
                    orientation = buildState.selectedRoomOrientation,
                )
            } else {
                hoveredPosition?.let { position ->
                    grid.snappedPlacementPreview(
                        blueprint = buildState.selectedRoomBlueprint,
                        hoveredPosition = position,
                        orientation = buildState.selectedRoomOrientation,
                    )
                }
            },
            trap = null,
            heart = null,
        )
    }
}
