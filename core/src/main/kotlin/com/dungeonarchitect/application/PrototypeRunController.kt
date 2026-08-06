package com.dungeonarchitect.application

import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.PrototypeHeroState
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.StartedHeroWave
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.simulation.DeterministicTrapSystem
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import kotlin.math.max

class PrototypeRunController(
    grid: DungeonGrid,
    private val upcomingWave: UpcomingHeroWave,
    runDefinition: PrototypeRunDefinition,
) {
    private val waveStartController = WaveStartController(grid, upcomingWave)
    private var heroSimulation: FixedStepHeroSimulation? = null
    private var trapSystem: DeterministicTrapSystem? = null

    val objectiveMaxHealth: Int = runDefinition.objectiveHealth

    var phase: PrototypeRunPhase = PrototypeRunPhase.BUILDING
        private set

    var objectiveHealth: Int = objectiveMaxHealth
        private set

    var resolvedHeroCount: Int = 0
        private set

    val startedWave: StartedHeroWave?
        get() = waveStartController.startedWave

    val heroState: PrototypeHeroState?
        get() = heroSimulation?.heroState

    val isStartEnabled: Boolean
        get() = phase == PrototypeRunPhase.BUILDING &&
            waveStartController.isStartEnabled

    val isControlEnabled: Boolean
        get() = isStartEnabled ||
            phase == PrototypeRunPhase.VICTORY ||
            phase == PrototypeRunPhase.DEFEAT

    fun start(): Boolean {
        if (phase != PrototypeRunPhase.BUILDING ||
            !waveStartController.start()
        ) {
            return false
        }

        val wave = requireNotNull(startedWave)
        trapSystem = DeterministicTrapSystem(wave.traps)
        heroSimulation = newHeroSimulation(wave)
        phase = PrototypeRunPhase.RUNNING
        return true
    }

    fun advance(elapsedSeconds: Float) {
        require(elapsedSeconds.isFinite() && elapsedSeconds >= 0f) {
            "Prototype run elapsed time must be finite and non-negative."
        }
        if (phase != PrototypeRunPhase.RUNNING) {
            return
        }

        var remainingSeconds = elapsedSeconds.toDouble()
        while (phase == PrototypeRunPhase.RUNNING) {
            val simulation = requireNotNull(heroSimulation)
            remainingSeconds =
                simulation.advanceAndReturnUnused(remainingSeconds)

            val heroState = simulation.heroState
            if (!heroState.isDead && !heroState.hasArrived) {
                return
            }

            resolve(heroState)
            if (phase == PrototypeRunPhase.RUNNING) {
                heroSimulation = newHeroSimulation(requireNotNull(startedWave))
            }
            if (remainingSeconds <= TIME_TOLERANCE) {
                return
            }
        }
    }

    fun restart(): Boolean {
        if (phase != PrototypeRunPhase.VICTORY &&
            phase != PrototypeRunPhase.DEFEAT
        ) {
            return false
        }

        waveStartController.restart()
        heroSimulation = null
        trapSystem = null
        objectiveHealth = objectiveMaxHealth
        resolvedHeroCount = 0
        phase = PrototypeRunPhase.BUILDING
        return true
    }

    private fun resolve(heroState: PrototypeHeroState) {
        resolvedHeroCount++
        if (heroState.hasArrived) {
            objectiveHealth = max(
                0,
                objectiveHealth - upcomingWave.objectiveDamage,
            )
            if (objectiveHealth == 0) {
                phase = PrototypeRunPhase.DEFEAT
                return
            }
        }

        if (resolvedHeroCount == upcomingWave.count) {
            phase = PrototypeRunPhase.VICTORY
        }
    }

    private fun newHeroSimulation(
        wave: StartedHeroWave,
    ) = FixedStepHeroSimulation(
        startedWave = wave,
        trapSystem = requireNotNull(trapSystem),
    )

    private companion object {
        const val TIME_TOLERANCE = 1e-12
    }
}
