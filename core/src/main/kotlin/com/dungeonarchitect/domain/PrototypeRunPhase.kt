package com.dungeonarchitect.domain

enum class PrototypeRunPhase {
    INTELLIGENCE,
    ROOM_DRAFT,
    DEFENSE_PREPARATION,
    COMBAT,
    WAVE_REPORT,
    RUN_VICTORY,
    RUN_DEFEAT,
    ;

    val allowedTransitions: Set<PrototypeRunPhase>
        get() = LEGAL_TRANSITIONS.getValue(this)

    fun canTransitionTo(next: PrototypeRunPhase): Boolean =
        next in allowedTransitions

    private companion object {
        val LEGAL_TRANSITIONS = mapOf(
            INTELLIGENCE to setOf(ROOM_DRAFT),
            ROOM_DRAFT to setOf(DEFENSE_PREPARATION),
            DEFENSE_PREPARATION to setOf(COMBAT),
            COMBAT to setOf(WAVE_REPORT),
            WAVE_REPORT to setOf(
                INTELLIGENCE,
                RUN_VICTORY,
                RUN_DEFEAT,
            ),
            RUN_VICTORY to emptySet(),
            RUN_DEFEAT to emptySet(),
        )
    }
}
