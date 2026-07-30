package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.PrototypeHeroState

internal data class HeroWorldMarker(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
)

internal fun heroWorldMarker(
    heroState: PrototypeHeroState?,
    tileSize: Float,
): HeroWorldMarker? =
    heroState?.let { state ->
        HeroWorldMarker(
            centerX = (state.position.column + HALF_TILE) * tileSize,
            centerY = (state.position.row + HALF_TILE) * tileSize,
            radius = tileSize * HERO_RADIUS_IN_TILES,
        )
    }

private const val HALF_TILE = 0.5f
private const val HERO_RADIUS_IN_TILES = 0.25f
