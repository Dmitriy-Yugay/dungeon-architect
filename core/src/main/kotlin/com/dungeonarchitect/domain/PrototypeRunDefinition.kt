package com.dungeonarchitect.domain

data class PrototypeRunDefinition(
    val heartHealth: Int,
    val startingGold: Int,
    val waves: List<RunWaveDefinition>,
    val completionCondition: RunCompletionCondition,
) {
    init {
        require(heartHealth > 0) {
            "The prototype heart's health must be positive."
        }
        require(startingGold >= 0) {
            "A run's starting Gold must not be negative."
        }
        require(waves.isNotEmpty()) {
            "A run must contain at least one wave."
        }
        require(waves.map(RunWaveDefinition::id).distinct().size == waves.size) {
            "A run's wave IDs must be distinct."
        }
        require(
            startingGold.toLong() + waves.sumOf { it.rewardGold.toLong() } <=
                Int.MAX_VALUE,
        ) {
            "A run's maximum authored Gold must fit in a 32-bit integer."
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
            startingGold: Int = 0,
            rewardGold: Int = 0,
        ) = PrototypeRunDefinition(
            heartHealth = heartHealth,
            startingGold = startingGold,
            waves = listOf(
                RunWaveDefinition(
                    id = "single-wave",
                    contentPath = contentPath,
                    rewardGold = rewardGold,
                ),
            ),
            completionCondition = RunCompletionCondition.CLEAR_ALL_WAVES,
        )
    }
}

data class RunWaveDefinition(
    val id: String,
    val contentPath: String,
    val rewardGold: Int,
    val roomOfferBlueprintIds: List<String> = emptyList(),
) {
    init {
        require(id.isNotBlank()) {
            "A run wave must have a non-blank ID."
        }
        require(contentPath.isNotBlank()) {
            "A run wave must reference a non-blank content path."
        }
        require(rewardGold >= 0) {
            "A run wave's Gold reward must not be negative."
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
