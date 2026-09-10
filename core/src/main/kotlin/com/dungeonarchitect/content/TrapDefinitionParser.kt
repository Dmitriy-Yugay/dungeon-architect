package com.dungeonarchitect.content

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.dungeonarchitect.domain.RoomSocketType
import com.dungeonarchitect.domain.TrapDefinition

object TrapDefinitionParser {
    fun parse(json: String): TrapDefinition {
        require(json.isNotBlank()) {
            "Trap definition JSON must not be blank."
        }

        val root = JsonReader().parse(json)
        require(root.isObject) {
            "Trap definition JSON must contain an object."
        }

        return TrapDefinition(
            id = root.requiredString("id"),
            displayName = root.requiredString("displayName"),
            damage = root.requiredInt("damage"),
            cooldownSeconds = root.requiredFloat("cooldownSeconds"),
            compatibleSocketTypes = root.requiredStringArray(
                "compatibleSocketTypes",
            ).mapTo(mutableSetOf(), ::parseSocketType),
            costGold = root.requiredInt("costGold"),
        )
    }

    private fun parseSocketType(value: String): RoomSocketType =
        RoomSocketType.entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: throw IllegalArgumentException(
            "Unknown room socket type '$value'.",
        )

    private fun JsonValue.requiredString(name: String): String {
        val value = required(name)
        require(value.isString) {
            "Trap definition field '$name' must be a string."
        }
        return value.asString()
    }

    private fun JsonValue.requiredStringArray(name: String): List<String> {
        val value = required(name)
        require(value.isArray) {
            "Trap definition field '$name' must be an array."
        }
        return value.mapIndexed { index, item ->
            require(item.isString) {
                "Trap definition field '$name' item $index must be a string."
            }
            item.asString()
        }
    }

    private fun JsonValue.requiredInt(name: String): Int {
        val value = required(name)
        require(value.isLong) {
            "Trap definition field '$name' must be a whole number."
        }

        val number = value.asLong()
        require(number in Int.MIN_VALUE..Int.MAX_VALUE) {
            "Trap definition field '$name' must fit in a 32-bit integer."
        }
        return number.toInt()
    }

    private fun JsonValue.requiredFloat(name: String): Float {
        val value = required(name)
        require(value.isNumber) {
            "Trap definition field '$name' must be a number."
        }
        return value.asFloat()
    }

    private fun JsonValue.required(name: String): JsonValue =
        get(name) ?: throw IllegalArgumentException(
            "Trap definition is missing required field '$name'.",
        )
}
