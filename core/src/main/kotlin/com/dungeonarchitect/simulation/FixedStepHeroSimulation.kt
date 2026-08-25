package com.dungeonarchitect.simulation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.HeroGridPosition
import com.dungeonarchitect.domain.PrototypeHeroState
import com.dungeonarchitect.domain.StartedHeroWave
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

class FixedStepHeroSimulation internal constructor(
    startedWave: StartedHeroWave,
    private val trapSystem: DeterministicTrapSystem,
    private val heroNumber: Int = 1,
    private val eventSink: (SimulationEvent) -> Unit = {},
) {
    constructor(
        startedWave: StartedHeroWave,
        heroNumber: Int = 1,
        eventSink: (SimulationEvent) -> Unit = {},
    ) : this(
        startedWave = startedWave,
        trapSystem = DeterministicTrapSystem(startedWave.traps, eventSink),
        heroNumber = heroNumber,
        eventSink = eventSink,
    )

    private val routeSegments = startedWave.route
        .zipWithNext(::RouteSegment)
    private val finalPosition = startedWave.route.last().toHeroPosition()
    private val routeLength = routeSegments.sumOf(RouteSegment::length)
    private val speed = startedWave.wave.movementSpeedTilesPerSecond.toDouble()
    private val maxHealth = startedWave.wave.heroHealth
    private var simulatedDistance = 0.0
    private var accumulatedSeconds = 0.0
    private var health = startedWave.wave.heroHealth

    var heroState: PrototypeHeroState = stateAt(
        distance = 0.0,
        health = health,
    )
        private set

    init {
        require(routeSegments.all(RouteSegment::isCardinal)) {
            "A hero route must contain only non-zero cardinal segments."
        }
        eventSink(
            HeroSpawned(
                heroNumber = heroNumber,
                heroType = startedWave.wave.heroType,
                position = heroState.position,
                health = heroState.health,
            ),
        )
        recordTerminalEvent()
    }

    fun advance(elapsedSeconds: Float) {
        advanceAndReturnUnused(elapsedSeconds.toDouble())
    }

    internal fun advanceAndReturnUnused(elapsedSeconds: Double): Double {
        require(elapsedSeconds.isFinite() && elapsedSeconds >= 0.0) {
            "Hero simulation elapsed time must be finite and non-negative."
        }
        if (heroState.hasArrived || heroState.isDead) {
            return elapsedSeconds
        }
        if (elapsedSeconds == 0.0) {
            return 0.0
        }

        accumulatedSeconds += elapsedSeconds
        var completedSteps = floor(
            (accumulatedSeconds + STEP_COMPARISON_TOLERANCE) /
                FIXED_STEP_SECONDS,
        ).toLong()
        if (completedSteps > 0L) {
            accumulatedSeconds -=
                completedSteps.toDouble() * FIXED_STEP_SECONDS
            if (accumulatedSeconds < 0.0) {
                accumulatedSeconds = 0.0
            }
        }
        while (completedSteps > 0L &&
            health > 0 &&
            simulatedDistance < routeLength
        ) {
            health = trapSystem.applyStep(
                heroPosition = positionAt(simulatedDistance),
                heroHealth = health,
                stepSeconds = FIXED_STEP_SECONDS,
                heroNumber = heroNumber,
            )
            if (health > 0) {
                simulatedDistance = min(
                    routeLength,
                    simulatedDistance + FIXED_STEP_SECONDS * speed,
                )
            }
            completedSteps--
        }

        if (health == 0 || simulatedDistance >= routeLength) {
            val unusedSeconds =
                completedSteps * FIXED_STEP_SECONDS + accumulatedSeconds
            accumulatedSeconds = 0.0
            heroState = stateAt(simulatedDistance, health)
            recordTerminalEvent()
            return unusedSeconds
        }

        val interpolatedDistance = min(
            routeLength,
            simulatedDistance + accumulatedSeconds * speed,
        )
        heroState = stateAt(interpolatedDistance, health)

        if (heroState.hasArrived) {
            simulatedDistance = min(
                routeLength,
                interpolatedDistance,
            )
            accumulatedSeconds = 0.0
            recordTerminalEvent()
        }

        return 0.0
    }

    private fun recordTerminalEvent() {
        when {
            heroState.isDead -> eventSink(
                HeroDied(
                    heroNumber = heroNumber,
                    position = heroState.position,
                ),
            )
            heroState.hasArrived -> eventSink(
                HeroArrived(
                    heroNumber = heroNumber,
                    position = heroState.position,
                ),
            )
        }
    }

    private fun stateAt(
        distance: Double,
        health: Int,
    ): PrototypeHeroState {
        val hasArrived = health > 0 && distance >= routeLength
        return PrototypeHeroState(
            position = if (hasArrived) {
                finalPosition
            } else {
                positionAt(distance)
            },
            hasArrived = hasArrived,
            health = health,
            maxHealth = maxHealth,
        )
    }

    private fun positionAt(distance: Double): HeroGridPosition {
        var remainingDistance = distance
        routeSegments.forEach { segment ->
            if (remainingDistance <= segment.length) {
                return segment.positionAt(remainingDistance)
            }
            remainingDistance -= segment.length
        }
        return finalPosition
    }

    private data class RouteSegment(
        val start: GridPosition,
        val end: GridPosition,
    ) {
        private val columnDelta = end.column.toDouble() - start.column
        private val rowDelta = end.row.toDouble() - start.row

        val isCardinal: Boolean =
            (columnDelta == 0.0) xor (rowDelta == 0.0)
        val length: Double = abs(columnDelta) + abs(rowDelta)

        fun positionAt(distance: Double): HeroGridPosition {
            val progress = distance / length
            return HeroGridPosition(
                column = (start.column + columnDelta * progress).toFloat(),
                row = (start.row + rowDelta * progress).toFloat(),
            )
        }
    }

    private fun GridPosition.toHeroPosition() = HeroGridPosition(
        column = column.toFloat(),
        row = row.toFloat(),
    )

    companion object {
        const val FIXED_STEP_SECONDS: Double = 1.0 / 60.0

        private const val STEP_COMPARISON_TOLERANCE = 1e-12
    }
}
