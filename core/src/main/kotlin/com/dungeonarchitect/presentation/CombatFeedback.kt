package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.simulation.HeroDamaged
import com.dungeonarchitect.simulation.HeroSpawned
import com.dungeonarchitect.simulation.SimulationEvent
import com.dungeonarchitect.simulation.TrapActivated

data class CombatFeedbackView(
    val trapPulses: List<TrapActivationPulse>,
    val heroDamageFlash: HeroDamageFlash?,
) {
    companion object {
        val NONE = CombatFeedbackView(
            trapPulses = emptyList(),
            heroDamageFlash = null,
        )
    }
}

data class TrapActivationPulse(
    val position: GridPosition,
    val progress: Float,
) {
    init {
        require(progress in 0f..1f) {
            "A trap activation pulse's progress must be between zero and one."
        }
    }
}

data class HeroDamageFlash(
    val heroNumber: Int,
    val damage: Int,
    val intensity: Float,
) {
    init {
        require(heroNumber > 0) {
            "A hero damage flash must identify a positive hero number."
        }
        require(damage > 0) {
            "A hero damage flash must contain positive damage."
        }
        require(intensity in 0f..1f) {
            "A hero damage flash's intensity must be between zero and one."
        }
    }
}

class CombatFeedbackTracker(
    private val trapPulseDurationSeconds: Float = DEFAULT_TRAP_PULSE_SECONDS,
    private val heroDamageFlashDurationSeconds: Float = DEFAULT_DAMAGE_FLASH_SECONDS,
) {
    private var observedEvents: List<SimulationEvent> = emptyList()
    private val trapPulses = mutableListOf<TimedTrapPulse>()
    private var heroDamageFlash: TimedDamageFlash? = null

    init {
        require(trapPulseDurationSeconds.isFinite() && trapPulseDurationSeconds > 0f) {
            "Trap pulse duration must be finite and positive."
        }
        require(
            heroDamageFlashDurationSeconds.isFinite() &&
                heroDamageFlashDurationSeconds > 0f,
        ) {
            "Hero damage-flash duration must be finite and positive."
        }
    }

    fun update(
        events: List<SimulationEvent>,
        elapsedSeconds: Float,
    ): CombatFeedbackView {
        require(elapsedSeconds.isFinite() && elapsedSeconds >= 0f) {
            "Combat-feedback elapsed time must be finite and non-negative."
        }

        if (!events.continues(observedEvents)) {
            trapPulses.clear()
            heroDamageFlash = null
            observedEvents = emptyList()
        }

        advanceFeedback(elapsedSeconds)
        events.drop(observedEvents.size).forEach(::consume)
        observedEvents = events.toList()
        return currentView()
    }

    private fun advanceFeedback(elapsedSeconds: Float) {
        trapPulses.forEach { pulse ->
            pulse.remainingSeconds -= elapsedSeconds
        }
        trapPulses.removeAll { pulse -> pulse.remainingSeconds <= 0f }

        heroDamageFlash?.let { flash ->
            flash.remainingSeconds -= elapsedSeconds
            if (flash.remainingSeconds <= 0f) {
                heroDamageFlash = null
            }
        }
    }

    private fun consume(event: SimulationEvent) {
        when (event) {
            is TrapActivated -> trapPulses += TimedTrapPulse(
                position = event.trapPosition,
                remainingSeconds = trapPulseDurationSeconds,
            )
            is HeroDamaged -> heroDamageFlash = TimedDamageFlash(
                heroNumber = event.heroNumber,
                damage = event.damage,
                remainingSeconds = heroDamageFlashDurationSeconds,
            )
            is HeroSpawned -> if (heroDamageFlash?.heroNumber != event.heroNumber) {
                heroDamageFlash = null
            }
            else -> Unit
        }
    }

    private fun currentView() = CombatFeedbackView(
        trapPulses = trapPulses.map { pulse ->
            TrapActivationPulse(
                position = pulse.position,
                progress = 1f -
                    pulse.remainingSeconds / trapPulseDurationSeconds,
            )
        },
        heroDamageFlash = heroDamageFlash?.let { flash ->
            HeroDamageFlash(
                heroNumber = flash.heroNumber,
                damage = flash.damage,
                intensity = flash.remainingSeconds /
                    heroDamageFlashDurationSeconds,
            )
        },
    )

    private fun List<SimulationEvent>.continues(
        previous: List<SimulationEvent>,
    ): Boolean = size >= previous.size && subList(0, previous.size) == previous

    private data class TimedTrapPulse(
        val position: GridPosition,
        var remainingSeconds: Float,
    )

    private data class TimedDamageFlash(
        val heroNumber: Int,
        val damage: Int,
        var remainingSeconds: Float,
    )

    private companion object {
        const val DEFAULT_TRAP_PULSE_SECONDS = 0.3f
        const val DEFAULT_DAMAGE_FLASH_SECONDS = 0.22f
    }
}
