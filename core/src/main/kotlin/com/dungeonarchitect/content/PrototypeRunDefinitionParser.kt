package com.dungeonarchitect.content

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.dungeonarchitect.domain.PrototypeRunDefinition

object PrototypeRunDefinitionParser {
    fun parse(json: String): PrototypeRunDefinition {
        require(json.isNotBlank()) {
            "Prototype run JSON must not be blank."
        }

        val root = JsonReader().parse(json)
        require(root.isObject) {
            "Prototype run JSON must contain an object."
        }

        return PrototypeRunDefinition(
            objectiveHealth = root.requiredInt("objectiveHealth"),
        )
    }

    private fun JsonValue.requiredInt(name: String): Int {
        val value = get(name) ?: throw IllegalArgumentException(
            "Prototype run is missing required field '$name'.",
        )
        require(value.isLong) {
            "Prototype run field '$name' must be a whole number."
        }

        val number = value.asLong()
        require(number in Int.MIN_VALUE..Int.MAX_VALUE) {
            "Prototype run field '$name' must fit in a 32-bit integer."
        }
        return number.toInt()
    }
}
