package com.dungeonarchitect.content

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.dungeonarchitect.domain.PrototypeRunDefinition
import com.dungeonarchitect.domain.RunCompletionCondition
import com.dungeonarchitect.domain.RunWaveDefinition

object PrototypeRunDefinitionParser {
    fun parse(
        json: String,
        availableWaveContentPaths: Set<String>,
        availableRoomBlueprintIds: Set<String>,
    ): PrototypeRunDefinition {
        require(json.isNotBlank()) {
            "Prototype run JSON must not be blank."
        }

        val root = JsonReader().parse(json)
        require(root.isObject) {
            "Prototype run JSON must contain an object."
        }

        val waves = root.requiredWaves("waves")
        val unknownPaths = waves
            .map(RunWaveDefinition::contentPath)
            .filterNot(availableWaveContentPaths::contains)
            .distinct()
        require(unknownPaths.isEmpty()) {
            "Prototype run references unknown wave content: ${unknownPaths.joinToString()}."
        }
        val unknownRoomBlueprintIds = waves
            .flatMap(RunWaveDefinition::roomOfferBlueprintIds)
            .filterNot(availableRoomBlueprintIds::contains)
            .distinct()
        require(unknownRoomBlueprintIds.isEmpty()) {
            "Prototype run references unknown room blueprints: " +
                "${unknownRoomBlueprintIds.joinToString()}."
        }

        return PrototypeRunDefinition(
            heartHealth = root.requiredInt("heartHealth"),
            startingGold = root.requiredInt("startingGold"),
            waves = waves,
            completionCondition = parseCompletionCondition(
                root.requiredString("completionCondition"),
            ),
        )
    }

    private fun JsonValue.requiredWaves(name: String): List<RunWaveDefinition> {
        val value = required(name)
        require(value.isArray) {
            "Prototype run field '$name' must be an array."
        }
        return value.mapIndexed { index, item ->
            require(item.isObject) {
                "Prototype run field '$name' item $index must be an object."
            }
            RunWaveDefinition(
                id = item.requiredString("id", "Prototype run wave $index"),
                contentPath = item.requiredString(
                    "contentPath",
                    "Prototype run wave $index",
                ),
                rewardGold = item.requiredInt(
                    "rewardGold",
                    "Prototype run wave $index",
                ),
                roomOfferBlueprintIds = item.optionalStringList(
                    "roomOfferBlueprintIds",
                    "Prototype run wave $index",
                ),
            )
        }
    }

    private fun JsonValue.optionalStringList(
        name: String,
        owner: String,
    ): List<String> {
        val value = get(name) ?: return emptyList()
        require(value.isArray) {
            "$owner field '$name' must be an array."
        }
        return value.mapIndexed { index, item ->
            require(item.isString) {
                "$owner field '$name' item $index must be a string."
            }
            item.asString()
        }
    }

    private fun parseCompletionCondition(value: String): RunCompletionCondition =
        when (value.lowercase()) {
            "clear_all_waves" -> RunCompletionCondition.CLEAR_ALL_WAVES
            else -> throw IllegalArgumentException(
                "Unknown run completion condition '$value'.",
            )
        }

    private fun JsonValue.requiredString(
        name: String,
        owner: String = "Prototype run",
    ): String {
        val value = required(name, owner)
        require(value.isString) {
            "$owner field '$name' must be a string."
        }
        return value.asString()
    }

    private fun JsonValue.requiredInt(
        name: String,
        owner: String = "Prototype run",
    ): Int {
        val value = required(name, owner)
        require(value.isLong) {
            "$owner field '$name' must be a whole number."
        }

        val number = value.asLong()
        require(number in Int.MIN_VALUE..Int.MAX_VALUE) {
            "$owner field '$name' must fit in a 32-bit integer."
        }
        return number.toInt()
    }

    private fun JsonValue.required(
        name: String,
        owner: String = "Prototype run",
    ): JsonValue = get(name) ?: throw IllegalArgumentException(
        "$owner is missing required field '$name'.",
    )
}
