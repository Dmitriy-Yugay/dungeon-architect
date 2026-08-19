package com.dungeonarchitect.application

import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PrototypeHeroState
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.StartedHeroWave
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.simulation.DeterministicTrapSystem
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import com.dungeonarchitect.simulation.HeartDamaged
import com.dungeonarchitect.simulation.SimulationEvent
import com.dungeonarchitect.simulation.WaveOutcome
import com.dungeonarchitect.simulation.WaveResolved
import kotlin.math.max

class PrototypeRunController(
    private val grid: DungeonGrid,
    private val upcomingWave: UpcomingHeroWave,
    runDefinition: PrototypeRunDefinition,
) {
    private val waveStartController = WaveStartController(grid, upcomingWave)
    private var heroSimulation: FixedStepHeroSimulation? = null
    private var trapSystem: DeterministicTrapSystem? = null
    private val mutableEvents = mutableListOf<SimulationEvent>()

    val heartMaxHealth: Int = runDefinition.heartHealth

    var phase: PrototypeRunPhase = PrototypeRunPhase.BUILDING
        private set

    var heartHealth: Int = heartMaxHealth
        private set

    var resolvedHeroCount: Int = 0
        private set

    val startedWave: StartedHeroWave?
        get() = waveStartController.startedWave

    val heroState: PrototypeHeroState?
        get() = heroSimulation?.heroState

    val events: List<SimulationEvent>
        get() = mutableEvents.toList()

    val isStartEnabled: Boolean
        get() = phase == PrototypeRunPhase.BUILDING &&
            waveStartController.isStartEnabled

    val isCancelEnabled: Boolean
        get() = phase == PrototypeRunPhase.BUILDING &&
            grid.placedRooms.isNotEmpty()

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
        trapSystem = DeterministicTrapSystem(wave.traps, ::recordEvent)
        heroSimulation = newHeroSimulation(wave)
        phase = PrototypeRunPhase.RUNNING
        return true
    }

    fun cancelLastPlacedRoom(): Boolean {
        if (phase != PrototypeRunPhase.BUILDING) {
            return false
        }

        return grid.cancelLastPlacedRoom()
    }

    fun placeOrRelocateHeart(room: PlacedRoom): Boolean {
        if (phase != PrototypeRunPhase.BUILDING) {
            return false
        }

        return grid.placeOrRelocateHeart(room)
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
        heartHealth = heartMaxHealth
        resolvedHeroCount = 0
        mutableEvents.clear()
        phase = PrototypeRunPhase.BUILDING
        return true
    }

    private fun resolve(heroState: PrototypeHeroState) {
        val heroNumber = resolvedHeroCount + 1
        resolvedHeroCount++
        if (heroState.hasArrived) {
            val healthBeforeDamage = heartHealth
            heartHealth = max(
                0,
                heartHealth - upcomingWave.heartDamage,
            )
            recordEvent(
                HeartDamaged(
                    heroNumber = heroNumber,
                    damage = healthBeforeDamage - heartHealth,
                    remainingHealth = heartHealth,
                ),
            )
            if (heartHealth == 0) {
                phase = PrototypeRunPhase.DEFEAT
                recordEvent(
                    WaveResolved(
                        outcome = WaveOutcome.DEFEAT,
                        heartHealth = heartHealth,
                    ),
                )
                return
            }
        }

        if (resolvedHeroCount == upcomingWave.count) {
            phase = PrototypeRunPhase.VICTORY
            recordEvent(
                WaveResolved(
                    outcome = WaveOutcome.VICTORY,
                    heartHealth = heartHealth,
                ),
            )
        }
    }

    private fun newHeroSimulation(
        wave: StartedHeroWave,
    ) = FixedStepHeroSimulation(
        startedWave = wave,
        trapSystem = requireNotNull(trapSystem),
        heroNumber = resolvedHeroCount + 1,
        eventSink = ::recordEvent,
    )

    private fun recordEvent(event: SimulationEvent) {
        mutableEvents += event
    }

    private companion object {
        const val TIME_TOLERANCE = 1e-12
    }
}
