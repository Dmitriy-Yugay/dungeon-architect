package com.dungeonarchitect.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class PrototypeRunPhaseTest {
    @Test
    fun `legal transition table covers every phase and phase pair`() {
        val expectedTransitions = mapOf(
            PrototypeRunPhase.INTELLIGENCE to setOf(
                PrototypeRunPhase.ROOM_DRAFT,
            ),
            PrototypeRunPhase.ROOM_DRAFT to setOf(
                PrototypeRunPhase.DEFENSE_PREPARATION,
            ),
            PrototypeRunPhase.DEFENSE_PREPARATION to setOf(
                PrototypeRunPhase.COMBAT,
            ),
            PrototypeRunPhase.COMBAT to setOf(
                PrototypeRunPhase.WAVE_REPORT,
            ),
            PrototypeRunPhase.WAVE_REPORT to setOf(
                PrototypeRunPhase.INTELLIGENCE,
                PrototypeRunPhase.RUN_VICTORY,
                PrototypeRunPhase.RUN_DEFEAT,
            ),
            PrototypeRunPhase.RUN_VICTORY to emptySet(),
            PrototypeRunPhase.RUN_DEFEAT to emptySet(),
        )

        assertEquals(
            PrototypeRunPhase.entries.toSet(),
            expectedTransitions.keys,
        )
        PrototypeRunPhase.entries.forEach { current ->
            val expectedNextPhases = expectedTransitions.getValue(current)
            assertEquals(
                expectedNextPhases,
                current.allowedTransitions,
                current.name,
            )
            PrototypeRunPhase.entries.forEach { candidate ->
                assertEquals(
                    candidate in expectedNextPhases,
                    current.canTransitionTo(candidate),
                    "$current -> $candidate",
                )
            }
        }
    }
}
