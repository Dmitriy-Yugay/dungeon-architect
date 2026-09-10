package com.dungeonarchitect.domain

data class PrototypeRunDefinition(
    val heartHealth: Int,
    val startingResources: Int,
    val waves: List<RunWaveDefinition>,
    val completionCondition: RunCompletionCondition,
) {
    init {
        require(heartHealth > 0) {
            "The prototype heart's health must be positive."
        }
        require(startingResources >= 0) {
            "A run's starting resources must not be negative."
        }
        require(waves.isNotEmpty()) {
            "A run must contain at least one wave."
        }
        require(waves.map(RunWaveDefinition::id).distinct().size == waves.size) {
            "A run's wave IDs must be distinct."
        }
        require(waves.first().roomOfferBlueprintIds.isEmpty()) {
            "The opening wave must not have a preceding room offer."
        }
        require(waves.drop(1).all { it.roomOfferBlueprintIds.size == ROOM_OFFER_SIZE }) {
            "Every intermission must offer exactly $ROOM_OFFER_SIZE rooms."
        }
    }

    companion object {
        const val ROOM_OFFER_SIZE = 3

        fun singleWave(
            heartHealth: Int,
            contentPath: String,
            startingResources: Int = 0,
            rewardResources: Int = 0,
        ) = PrototypeRunDefinition(
            heartHealth = heartHealth,
            startingResources = startingResources,
            waves = listOf(
                RunWaveDefinition(
                    id = "single-wave",
                    contentPath = contentPath,
                    rewardResources = rewardResources,
                ),
            ),
            completionCondition = RunCompletionCondition.CLEAR_ALL_WAVES,
        )
    }
}

data class RunWaveDefinition(
    val id: String,
    val contentPath: String,
    val rewardResources: Int,
    val roomOfferBlueprintIds: List<String> = emptyList(),
) {
    init {
        require(id.isNotBlank()) {
            "A run wave must have a non-blank ID."
        }
        require(contentPath.isNotBlank()) {
            "A run wave must reference a non-blank content path."
        }
        require(rewardResources >= 0) {
            "A run wave's resource reward must not be negative."
        }
        require(roomOfferBlueprintIds.none(String::isBlank)) {
            "A room offer must not contain a blank blueprint ID."
        }
        require(roomOfferBlueprintIds.distinct().size == roomOfferBlueprintIds.size) {
            "A room offer's blueprint IDs must be distinct."
        }
    }
}

enum class RunCompletionCondition {
    CLEAR_ALL_WAVES,
}
