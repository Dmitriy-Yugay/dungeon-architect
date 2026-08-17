package com.dungeonarchitect.simulation

import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.HeroGridPosition
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.PlacedTrap
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeterministicTrapSystemTest {
    @Test
    fun `trap damages only a hero occupying its grid cell`() {
        val system = trapSystem(
            trapPosition = GridPosition(column = 2, row = 3),
            damage = 4,
        )

        assertEquals(
            10,
            system.applyStep(
                heroPosition = HeroGridPosition(column = 1.49f, row = 3f),
                heroHealth = 10,
                stepSeconds = FIXED_STEP_SECONDS,
            ),
        )
        assertEquals(
            6,
            system.applyStep(
                heroPosition = HeroGridPosition(column = 1.5f, row = 3f),
                heroHealth = 10,
                stepSeconds = FIXED_STEP_SECONDS,
            ),
        )
    }

    @Test
    fun `trap cannot damage again until its cooldown expires`() {
        val system = trapSystem(damage = 3, cooldownSeconds = 0.25f)
        val target = HeroGridPosition(column = 0f, row = 0f)

        assertEquals(7, system.applyStep(target, 10, stepSeconds = 0.05))
        assertEquals(7, system.applyStep(target, 7, stepSeconds = 0.10))
        assertEquals(7, system.applyStep(target, 7, stepSeconds = 0.10))
        assertEquals(4, system.applyStep(target, 7, stepSeconds = 0.05))
    }

    @Test
    fun `lethal trap damage clamps health to zero`() {
        val system = trapSystem(damage = 12)
        val target = HeroGridPosition(column = 0f, row = 0f)

        assertEquals(0, system.applyStep(target, 10, FIXED_STEP_SECONDS))
        assertEquals(0, system.applyStep(target, 0, FIXED_STEP_SECONDS))
    }

    @Test
    fun `trap emits activation before actual applied damage`() {
        val events = mutableListOf<SimulationEvent>()
        val trapPosition = GridPosition(column = 2, row = 3)
        val system = trapSystem(
            trapPosition = trapPosition,
            damage = 12,
            eventSink = events::add,
        )

        assertEquals(
            0,
            system.applyStep(
                heroPosition = HeroGridPosition(column = 2f, row = 3f),
                heroHealth = 10,
                stepSeconds = FIXED_STEP_SECONDS,
                heroNumber = 3,
            ),
        )
        assertEquals(
            listOf(
                TrapActivated(
                    heroNumber = 3,
                    trapId = "spike_trap",
                    trapPosition = trapPosition,
                ),
                HeroDamaged(
                    heroNumber = 3,
                    sourceTrapId = "spike_trap",
                    damage = 10,
                    remainingHealth = 0,
                ),
            ),
            events,
        )
    }

    @Test
    fun `trap rejects an invalid hero number even without activation`() {
        val system = trapSystem(
            trapPosition = GridPosition(column = 2, row = 3),
            damage = 4,
        )

        assertFailsWith<IllegalArgumentException> {
            system.applyStep(
                heroPosition = HeroGridPosition(column = 0f, row = 0f),
                heroHealth = 10,
                stepSeconds = FIXED_STEP_SECONDS,
                heroNumber = 0,
            )
        }
    }

    private fun trapSystem(
        trapPosition: GridPosition = GridPosition(column = 0, row = 0),
        damage: Int,
        cooldownSeconds: Float = 0.25f,
        eventSink: (SimulationEvent) -> Unit = {},
    ) = DeterministicTrapSystem(
        placedTraps = listOf(
            placedTrap(
                position = trapPosition,
                damage = damage,
                cooldownSeconds = cooldownSeconds,
            ),
        ),
        eventSink = eventSink,
    )

    private fun placedTrap(
        position: GridPosition,
        damage: Int,
        cooldownSeconds: Float,
    ): PlacedTrap {
        val localSocketPosition = GridPosition(column = 0, row = 0)
        val room = PlacedRoom(
            blueprint = RoomBlueprint(
                id = "socket-room",
                displayName = "Socket Room",
                heartAnchor = GridPosition(column = 0, row = 0),
                footprint = setOf(localSocketPosition),
                doorPositions = setOf(localSocketPosition),
                sockets = mapOf(
                    localSocketPosition to RoomSocketType.FLOOR,
                ),
            ),
            origin = position,
        )

        return PlacedTrap(
            definition = TrapDefinition(
                id = "spike_trap",
                displayName = "Spike Trap",
                damage = damage,
                cooldownSeconds = cooldownSeconds,
                compatibleSocketTypes = setOf(RoomSocketType.FLOOR),
            ),
            room = room,
            localSocketPosition = localSocketPosition,
        )
    }

    private companion object {
        const val FIXED_STEP_SECONDS = 1.0 / 60.0
    }
}
