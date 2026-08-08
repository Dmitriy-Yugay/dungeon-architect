package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.TrapSocketHoverResult

data class TrapPlacementPreview(
    val position: GridPosition,
    val isValid: Boolean,
)

internal fun trapPlacementPreviewFor(
    hoverResult: TrapSocketHoverResult,
    hoveredPosition: GridPosition,
): TrapPlacementPreview? = when (hoverResult) {
    is TrapSocketHoverResult.Valid -> TrapPlacementPreview(
        position = hoveredPosition,
        isValid = true,
    )
    is TrapSocketHoverResult.Occupied,
    is TrapSocketHoverResult.Incompatible,
    -> TrapPlacementPreview(
        position = hoveredPosition,
        isValid = false,
    )
    TrapSocketHoverResult.NonSocket -> null
}
