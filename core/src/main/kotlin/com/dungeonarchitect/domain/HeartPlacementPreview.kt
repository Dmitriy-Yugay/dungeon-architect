package com.dungeonarchitect.domain

data class HeartPlacementPreview(
    val room: PlacedRoom?,
    val position: GridPosition,
    val isValid: Boolean,
)
