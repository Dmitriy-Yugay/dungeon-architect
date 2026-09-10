package com.dungeonarchitect.application

import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PrototypeHeroState
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.StartedHeroWave
import com.dungeonarchitect.domain.UpcomingHeroWave
import com.dungeonarchitect.evaluation.WaveEvaluationReport
import com.dungeonarchitect.simulation.DeterministicTrapSystem
import com.dungeonarchitect.simulation.FixedStepHeroSimulation
import com.dungeonarchitect.simulation.HeartDamaged
import com.dungeonarchitect.simulation.SimulationEvent
import com.dungeonarchitect.simulation.WaveOutcome
import com.dungeonarchitect.simulation.WaveResolved
import kotlin.math.max
import kotlin.math.ceil

class PrototypeRunController(
    private val grid: DungeonGrid,
    private val upcomingWave: UpcomingHeroWave,
    runDefinition: PrototypeRunDefinition,
) {
    private val waveStartController = WaveStartController(grid, upcomingWave)
    private var heroSimulation: FixedStepHeroSimulation? = null
    private var trapSystem: DeterministicTrapSystem? = null
    private val mutableEvents = mutableListOf<SimulationEvent>()
    private var currentHeroElapsedSeconds = 0.0
    private var completedHeroElapsedSeconds = 0.0

    val heartMaxHealth: Int = runDefinition.heartHealth

    var phase: PrototypeRunPhase = PrototypeRunPhase.DEFENSE_PREPARATION
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

    var evaluationReport: WaveEvaluationReport? = null
        private set

    val isStartEnabled: Boolean
        get() = phase == PrototypeRunPhase.DEFENSE_PREPARATION &&
            waveStartController.isStartEnabled

    val isCancelEnabled: Boolean
        get() = phase == PrototypeRunPhase.DEFENSE_PREPARATION &&
            grid.placedRooms.isNotEmpty()

    val isControlEnabled: Boolean
        get() = isStartEnabled ||
            phase == PrototypeRunPhase.RUN_VICTORY ||
            phase == PrototypeRunPhase.RUN_DEFEAT

    fun start(): Boolean {
        if (phase != PrototypeRunPhase.DEFENSE_PREPARATION ||
            !waveStartController.start()
        ) {
            return false
        }

        val wave = requireNotNull(startedWave)
        currentHeroElapsedSeconds = 0.0
        completedHeroElapsedSeconds = 0.0
        evaluationReport = null
        trapSystem = DeterministicTrapSystem(wave.traps, ::recordEvent)
        heroSimulation = newHeroSimulation(wave)
        transitionTo(PrototypeRunPhase.COMBAT)
        return true
    }

    fun cancelLastPlacedRoom(): Boolean {
        if (phase != PrototypeRunPhase.DEFENSE_PREPARATION) {
            return false
        }

        return grid.cancelLastPlacedRoom()
    }

    fun placeOrRelocateHeart(room: PlacedRoom): Boolean {
        if (phase != PrototypeRunPhase.DEFENSE_PREPARATION) {
            return false
        }

        return grid.placeOrRelocateHeart(room)
    }

    fun advance(elapsedSeconds: Float) {
        require(elapsedSeconds.isFinite() && elapsedSeconds >= 0f) {
            "Prototype run elapsed time must be finite and non-negative."
        }
        if (phase != PrototypeRunPhase.COMBAT) {
            return
        }

        var remainingSeconds = elapsedSeconds.toDouble()
        while (phase == PrototypeRunPhase.COMBAT) {
            val simulation = requireNotNull(heroSimulation)
            val offeredSeconds = remainingSeconds
            remainingSeconds =
                simulation.advanceAndReturnUnused(remainingSeconds)
            currentHeroElapsedSeconds += offeredSeconds - remainingSeconds

            val heroState = simulation.heroState
            if (!heroState.isDead && !heroState.hasArrived) {
                return
            }

            resolve(heroState)
            if (phase == PrototypeRunPhase.COMBAT) {
                heroSimulation = newHeroSimulation(requireNotNull(startedWave))
            }
            if (remainingSeconds <= TIME_TOLERANCE) {
                return
            }
        }
    }

    fun restart(): Boolean {
        if (phase != PrototypeRunPhase.RUN_VICTORY &&
            phase != PrototypeRunPhase.RUN_DEFEAT
        ) {
            return false
        }

        waveStartController.restart()
        heroSimulation = null
        trapSystem = null
        heartHealth = heartMaxHealth
        resolvedHeroCount = 0
        currentHeroElapsedSeconds = 0.0
        completedHeroElapsedSeconds = 0.0
        evaluationReport = null
        mutableEvents.clear()
        resetToDefensePreparation()
        return true
    }

    private fun resolve(heroState: PrototypeHeroState) {
        recordCompletedHeroElapsedTime()
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
                resolveWave(
                    waveOutcome = WaveOutcome.DEFEAT,
                    runOutcome = PrototypeRunPhase.RUN_DEFEAT,
                )
                return
            }
        }

        if (resolvedHeroCount == upcomingWave.count) {
            resolveWave(
                waveOutcome = WaveOutcome.VICTORY,
                runOutcome = PrototypeRunPhase.RUN_VICTORY,
            )
        }
    }

    private fun recordCompletedHeroElapsedTime() {
        val completedSteps = ceil(
            currentHeroElapsedSeconds /
                FixedStepHeroSimulation.FIXED_STEP_SECONDS -
                COMPLETED_STEP_TOLERANCE,
        ).coerceAtLeast(0.0)
        completedHeroElapsedSeconds +=
            completedSteps * FixedStepHeroSimulation.FIXED_STEP_SECONDS
        currentHeroElapsedSeconds = 0.0
    }

    private fun recordEvaluationReport() {
        evaluationReport = WaveEvaluationReport.fromEvents(
            events = mutableEvents,
            elapsedSimulationSeconds = completedHeroElapsedSeconds,
        )
    }

    private fun resolveWave(
        waveOutcome: WaveOutcome,
        runOutcome: PrototypeRunPhase,
    ) {
        transitionTo(PrototypeRunPhase.WAVE_REPORT)
        recordEvent(
            WaveResolved(
                outcome = waveOutcome,
                heartHealth = heartHealth,
            ),
        )
        recordEvaluationReport()
        transitionTo(runOutcome)
    }

    private fun transitionTo(next: PrototypeRunPhase) {
        check(phase.canTransitionTo(next)) {
            "Illegal prototype run phase transition: $phase -> $next."
        }
        phase = next
    }

    private fun resetToDefensePreparation() {
        phase = PrototypeRunPhase.DEFENSE_PREPARATION
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
        const val COMPLETED_STEP_TOLERANCE = 1e-4
    }
}
