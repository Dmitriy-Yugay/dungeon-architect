package com.dungeonarchitect.simulation

import com.dungeonarchitect.domain.HeroGridPosition
import com.dungeonarchitect.domain.PlacedTrap
import kotlin.math.max

class DeterministicTrapSystem(
    placedTraps: List<PlacedTrap>,
) {
    private val traps = placedTraps.map(::RuntimeTrap)

    fun applyStep(
        heroPosition: HeroGridPosition,
        heroHealth: Int,
        stepSeconds: Double,
    ): Int {
        require(heroHealth >= 0) {
            "Hero health must not be negative."
        }
        require(stepSeconds.isFinite() && stepSeconds > 0.0) {
            "Trap simulation step must be finite and positive."
        }
        if (heroHealth == 0) {
            return 0
        }

        var remainingHealth = heroHealth
        traps.forEach { trap ->
            trap.cooldownRemaining = max(
                0.0,
                trap.cooldownRemaining - stepSeconds,
            )
            if (trap.cooldownRemaining < COOLDOWN_TOLERANCE) {
                trap.cooldownRemaining = 0.0
            }
            if (remainingHealth > 0 &&
                trap.cooldownRemaining == 0.0 &&
                trap.targets(heroPosition)
            ) {
                remainingHealth = max(
                    0,
                    remainingHealth - trap.placed.definition.damage,
                )
                trap.cooldownRemaining =
                    trap.placed.definition.cooldownSeconds.toDouble()
            }
        }

        return remainingHealth
    }

    private data class RuntimeTrap(
        val placed: PlacedTrap,
        var cooldownRemaining: Double = 0.0,
    ) {
        fun targets(heroPosition: HeroGridPosition): Boolean =
            heroPosition.column >= placed.gridPosition.column - HALF_TILE &&
                heroPosition.column < placed.gridPosition.column + HALF_TILE &&
                heroPosition.row >= placed.gridPosition.row - HALF_TILE &&
                heroPosition.row < placed.gridPosition.row + HALF_TILE
    }

    private companion object {
        const val HALF_TILE = 0.5f
        const val COOLDOWN_TOLERANCE = 1e-12
    }
}
