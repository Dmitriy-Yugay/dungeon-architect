package com.dungeonarchitect.simulation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.HeroGridPosition

sealed interface SimulationEvent

data class HeroSpawned(
    val heroNumber: Int,
    val heroType: String,
    val position: HeroGridPosition,
    val health: Int,
) : SimulationEvent {
    init {
        requireValidHeroNumber(heroNumber)
        require(heroType.isNotBlank()) {
            "A spawned hero must identify its type."
        }
        require(health > 0) {
            "A spawned hero's health must be positive."
        }
    }
}

data class HeroArrived(
    val heroNumber: Int,
    val position: HeroGridPosition,
) : SimulationEvent {
    init {
        requireValidHeroNumber(heroNumber)
    }
}

data class TrapActivated(
    val heroNumber: Int,
    val trapId: String,
    val trapPosition: GridPosition,
) : SimulationEvent {
    init {
        requireValidHeroNumber(heroNumber)
        requireValidTrapId(trapId)
    }
}

data class HeroDamaged(
    val heroNumber: Int,
    val sourceTrapId: String,
    val damage: Int,
    val remainingHealth: Int,
) : SimulationEvent {
    init {
        requireValidHeroNumber(heroNumber)
        requireValidTrapId(sourceTrapId)
        require(damage > 0) {
            "Hero damage must be positive."
        }
        require(remainingHealth >= 0) {
            "A damaged hero's remaining health must not be negative."
        }
    }
}

data class HeroDied(
    val heroNumber: Int,
    val position: HeroGridPosition,
) : SimulationEvent {
    init {
        requireValidHeroNumber(heroNumber)
    }
}

data class HeartDamaged(
    val heroNumber: Int,
    val damage: Int,
    val remainingHealth: Int,
) : SimulationEvent {
    init {
        requireValidHeroNumber(heroNumber)
        require(damage > 0) {
            "Heart damage must be positive."
        }
        require(remainingHealth >= 0) {
            "Heart remaining health must not be negative."
        }
    }
}

data class WaveResolved(
    val outcome: WaveOutcome,
    val heartHealth: Int,
) : SimulationEvent {
    init {
        require(heartHealth >= 0) {
            "Resolved wave heart health must not be negative."
        }
        require(
            (outcome == WaveOutcome.VICTORY && heartHealth > 0) ||
                (outcome == WaveOutcome.DEFEAT && heartHealth == 0),
        ) {
            "Wave outcome must agree with the remaining heart health."
        }
    }
}

enum class WaveOutcome {
    VICTORY,
    DEFEAT,
}

private fun requireValidHeroNumber(heroNumber: Int) {
    require(heroNumber > 0) {
        "A simulation event hero number must be positive."
    }
}

private fun requireValidTrapId(trapId: String) {
    require(trapId.isNotBlank()) {
        "A simulation event trap ID must not be blank."
    }
}
