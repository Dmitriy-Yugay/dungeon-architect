package com.dungeonarchitect.simulation

import com.dungeonarchitect.domain.HeroGridPosition
import com.dungeonarchitect.domain.PlacedTrap
import kotlin.math.max

class DeterministicTrapSystem(
    placedTraps: List<PlacedTrap>,
    private val eventSink: (SimulationEvent) -> Unit = {},
) {
    private val traps = placedTraps.map(::RuntimeTrap)

    fun applyStep(
        heroPosition: HeroGridPosition,
        heroHealth: Int,
        stepSeconds: Double,
        heroNumber: Int = 1,
    ): Int {
        require(heroHealth >= 0) {
            "Hero health must not be negative."
        }
        require(stepSeconds.isFinite() && stepSeconds > 0.0) {
            "Trap simulation step must be finite and positive."
        }
        require(heroNumber > 0) {
            "Trap simulation hero number must be positive."
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
                val healthBeforeDamage = remainingHealth
                remainingHealth = max(
                    0,
                    remainingHealth - trap.placed.definition.damage,
                )
                trap.cooldownRemaining =
                    trap.placed.definition.cooldownSeconds.toDouble()
                eventSink(
                    TrapActivated(
                        heroNumber = heroNumber,
                        trapId = trap.placed.definition.id,
                        trapPosition = trap.placed.gridPosition,
                    ),
                )
                eventSink(
                    HeroDamaged(
                        heroNumber = heroNumber,
                        sourceTrapId = trap.placed.definition.id,
                        damage = healthBeforeDamage - remainingHealth,
                        remainingHealth = remainingHealth,
                    ),
                )
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
