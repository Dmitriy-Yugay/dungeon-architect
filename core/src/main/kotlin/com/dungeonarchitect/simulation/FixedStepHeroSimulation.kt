package com.dungeonarchitect.simulation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.HeroGridPosition
import com.dungeonarchitect.domain.PrototypeHeroState
import com.dungeonarchitect.domain.StartedHeroWave
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

class FixedStepHeroSimulation(
    startedWave: StartedHeroWave,
) {
    private val routeSegments = startedWave.route
        .zipWithNext(::RouteSegment)
    private val finalPosition = startedWave.route.last().toHeroPosition()
    private val routeLength = routeSegments.sumOf(RouteSegment::length)
    private val speed = startedWave.wave.movementSpeedTilesPerSecond.toDouble()

    private var simulatedDistance = 0.0
    private var accumulatedSeconds = 0.0

    var heroState: PrototypeHeroState = stateAt(distance = 0.0)
        private set

    init {
        require(routeSegments.all(RouteSegment::isCardinal)) {
            "A hero route must contain only non-zero cardinal segments."
        }
    }

    fun advance(elapsedSeconds: Float) {
        require(elapsedSeconds.isFinite() && elapsedSeconds >= 0f) {
            "Hero simulation elapsed time must be finite and non-negative."
        }
        if (heroState.hasArrived || elapsedSeconds == 0f) {
            return
        }

        accumulatedSeconds += elapsedSeconds.toDouble()
        val completedSteps = floor(
            (accumulatedSeconds + STEP_COMPARISON_TOLERANCE) /
                FIXED_STEP_SECONDS,
        )
        if (completedSteps > 0.0) {
            simulatedDistance = min(
                routeLength,
                simulatedDistance +
                    completedSteps * FIXED_STEP_SECONDS * speed,
            )
            accumulatedSeconds -= completedSteps * FIXED_STEP_SECONDS
            if (accumulatedSeconds < 0.0) {
                accumulatedSeconds = 0.0
            }
        }

        val interpolatedDistance = min(
            routeLength,
            simulatedDistance + accumulatedSeconds * speed,
        )
        heroState = stateAt(interpolatedDistance)

        if (heroState.hasArrived) {
            simulatedDistance = routeLength
            accumulatedSeconds = 0.0
        }
    }

    private fun stateAt(distance: Double): PrototypeHeroState {
        val hasArrived = distance >= routeLength
        return PrototypeHeroState(
            position = if (hasArrived) {
                finalPosition
            } else {
                positionAt(distance)
            },
            hasArrived = hasArrived,
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
