package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.HeroGridPosition
import com.dungeonarchitect.domain.PrototypeHeroState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeroWorldMarkerTest {
    @Test
    fun `marker is absent without a hero state`() {
        assertNull(heroWorldMarker(heroState = null, tileSize = TILE_SIZE))
    }

    @Test
    fun `marker is present at the center of its tile`() {
        val marker = heroWorldMarker(
            heroState = heroState(column = 2f, row = 3f),
            tileSize = TILE_SIZE,
        )

        assertEquals(
            HeroWorldMarker(
                centerX = 160f,
                centerY = 224f,
                radius = 16f,
            ),
            marker,
        )
    }

    @Test
    fun `marker preserves interpolated grid coordinates`() {
        val marker = heroWorldMarker(
            heroState = heroState(column = 1.25f, row = 2.5f),
            tileSize = TILE_SIZE,
        )

        assertEquals(112f, marker?.centerX)
        assertEquals(192f, marker?.centerY)
    }

    @Test
    fun `arrived hero remains centered at its final position`() {
        val marker = heroWorldMarker(
            heroState = heroState(
                column = 15f,
                row = 4f,
                hasArrived = true,
            ),
            tileSize = TILE_SIZE,
        )

        assertEquals(992f, marker?.centerX)
        assertEquals(288f, marker?.centerY)
    }

    @Test
    fun `marker is absent after the hero dies`() {
        assertNull(
            heroWorldMarker(
                heroState = heroState(
                    column = 2f,
                    row = 3f,
                    health = 0,
                ),
                tileSize = TILE_SIZE,
            ),
        )
    }

    private fun heroState(
        column: Float,
        row: Float,
        hasArrived: Boolean = false,
        health: Int = 10,
    ) = PrototypeHeroState(
        position = HeroGridPosition(column = column, row = row),
        hasArrived = hasArrived,
        health = health,
    )

    private companion object {
        const val TILE_SIZE = 64f
    }
}
