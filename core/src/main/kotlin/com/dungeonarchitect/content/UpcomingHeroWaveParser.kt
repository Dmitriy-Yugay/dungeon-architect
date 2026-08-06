package com.dungeonarchitect.content

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.dungeonarchitect.domain.UpcomingHeroWave

object UpcomingHeroWaveParser {
    fun parse(json: String): UpcomingHeroWave {
        require(json.isNotBlank()) {
            "Upcoming hero wave JSON must not be blank."
        }

        val root = JsonReader().parse(json)
        require(root.isObject) {
            "Upcoming hero wave JSON must contain an object."
        }

        return UpcomingHeroWave(
            heroType = root.requiredString("heroType"),
            heroDisplayName = root.requiredString("heroDisplayName"),
            count = root.requiredInt("count"),
            heroHealth = root.requiredInt("heroHealth"),
            objectiveDamage = root.requiredInt("objectiveDamage"),
            movementSpeedTilesPerSecond =
                root.requiredFloat("movementSpeedTilesPerSecond"),
            traitDescription = root.requiredString("traitDescription"),
        )
    }

    private fun JsonValue.requiredString(name: String): String {
        val value = required(name)
        require(value.isString) {
            "Upcoming hero wave field '$name' must be a string."
        }
        return value.asString()
    }

    private fun JsonValue.requiredInt(name: String): Int {
        val value = required(name)
        require(value.isLong) {
            "Upcoming hero wave field '$name' must be a whole number."
        }

        val number = value.asLong()
        require(number in Int.MIN_VALUE..Int.MAX_VALUE) {
            "Upcoming hero wave field '$name' must fit in a 32-bit integer."
        }
        return number.toInt()
    }

    private fun JsonValue.requiredFloat(name: String): Float {
        val value = required(name)
        require(value.isNumber) {
            "Upcoming hero wave field '$name' must be a number."
        }
        return value.asFloat()
    }

    private fun JsonValue.required(name: String): JsonValue =
        get(name) ?: throw IllegalArgumentException(
            "Upcoming hero wave is missing required field '$name'.",
        )
}
