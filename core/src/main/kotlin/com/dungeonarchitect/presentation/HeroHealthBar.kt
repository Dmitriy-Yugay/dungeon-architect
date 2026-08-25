package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.PrototypeHeroState

internal data class HeroHealthBar(
    val left: Float,
    val bottom: Float,
    val width: Float,
    val height: Float,
    val fillFraction: Float,
) {
    init {
        require(left.isFinite() && bottom.isFinite()) {
            "Hero health-bar coordinates must be finite."
        }
        require(width.isFinite() && width > 0f) {
            "Hero health-bar width must be finite and positive."
        }
        require(height.isFinite() && height > 0f) {
            "Hero health-bar height must be finite and positive."
        }
        require(fillFraction in 0f..1f) {
            "Hero health-bar fill must be between zero and one."
        }
    }
}

internal fun heroHealthBar(
    heroState: PrototypeHeroState?,
    tileSize: Float,
): HeroHealthBar? {
    require(tileSize.isFinite() && tileSize > 0f) {
        "Hero health-bar tile size must be finite and positive."
    }

    val marker = heroWorldMarker(heroState, tileSize) ?: return null
    val state = requireNotNull(heroState)
    return HeroHealthBar(
        left = marker.centerX - tileSize * BAR_WIDTH_IN_TILES / 2f,
        bottom = marker.centerY + marker.radius + tileSize * BAR_GAP_IN_TILES,
        width = tileSize * BAR_WIDTH_IN_TILES,
        height = tileSize * BAR_HEIGHT_IN_TILES,
        fillFraction = healthFraction(state.health, state.maxHealth),
    )
}

internal fun healthFraction(health: Int, maxHealth: Int): Float {
    require(maxHealth > 0) {
        "Hero health-bar maximum health must be positive."
    }
    return (health.toFloat() / maxHealth).coerceIn(0f, 1f)
}

private const val BAR_WIDTH_IN_TILES = 0.625f
private const val BAR_HEIGHT_IN_TILES = 0.125f
private const val BAR_GAP_IN_TILES = 0.0625f
