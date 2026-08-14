package com.dungeonarchitect.content

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomDoor
import com.dungeonarchitect.domain.RoomSocketType

object RoomBlueprintParser {
    fun parse(json: String): RoomBlueprint {
        require(json.isNotBlank()) {
            "Room blueprint JSON must not be blank."
        }

        val root = JsonReader().parse(json)
        require(root.isObject) {
            "Room blueprint JSON must contain an object."
        }

        val doors = root.requiredDoors("doors")
        return RoomBlueprint(
            id = root.requiredString("id"),
            displayName = root.requiredString("displayName"),
            footprint = root.requiredPositions("footprint"),
            doors = doors,
            sockets = root.requiredSockets("sockets"),
        )
    }

    private fun JsonValue.requiredDoors(name: String): List<RoomDoor> {
        val value = required(name)
        require(value.isArray) {
            "Room blueprint field '$name' must be an array."
        }

        val doors = value.mapIndexed { index, item ->
            require(item.isObject) {
                "Room blueprint field '$name' item $index must be an object."
            }
            val owner = "Room blueprint door $index"
            RoomDoor(
                position = item.requiredPosition("position", owner),
                facing = parseDirection(item.requiredString("facing", owner)),
            )
        }
        require(doors.distinct().size == doors.size) {
            "Room blueprint doors must be distinct by position and facing."
        }
        return doors
    }

    private fun JsonValue.requiredPositions(name: String): Set<GridPosition> {
        val value = required(name)
        require(value.isArray) {
            "Room blueprint field '$name' must be an array."
        }

        return value.mapIndexed { index, item ->
            item.toPosition("Room blueprint field '$name' item $index")
        }.toSet()
    }

    private fun JsonValue.requiredSockets(
        name: String,
    ): Map<GridPosition, RoomSocketType> {
        val value = required(name)
        require(value.isArray) {
            "Room blueprint field '$name' must be an array."
        }

        val entries = value.mapIndexed { index, item ->
            require(item.isObject) {
                "Room blueprint field '$name' item $index must be an object."
            }
            item.requiredPosition("position", "Room blueprint socket $index") to
                parseSocketType(
                    item.requiredString("type", "Room blueprint socket $index"),
                )
        }
        require(entries.map { it.first }.distinct().size == entries.size) {
            "Room blueprint sockets must occupy distinct positions."
        }
        return entries.toMap()
    }

    private fun JsonValue.requiredPosition(
        name: String,
        owner: String,
    ): GridPosition = required(name, owner).toPosition("$owner field '$name'")

    private fun JsonValue.toPosition(owner: String): GridPosition {
        require(isObject) {
            "$owner must be an object."
        }
        return GridPosition(
            column = requiredInt("column", owner),
            row = requiredInt("row", owner),
        )
    }

    private fun parseSocketType(value: String): RoomSocketType =
        RoomSocketType.entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: throw IllegalArgumentException(
            "Unknown room socket type '$value'.",
        )

    private fun parseDirection(value: String): CardinalDirection =
        CardinalDirection.entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: throw IllegalArgumentException(
            "Unknown cardinal direction '$value'.",
        )

    private fun JsonValue.requiredString(
        name: String,
        owner: String = "Room blueprint",
    ): String {
        val value = required(name, owner)
        require(value.isString) {
            "$owner field '$name' must be a string."
        }
        return value.asString()
    }

    private fun JsonValue.requiredInt(name: String, owner: String): Int {
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
        owner: String = "Room blueprint",
    ): JsonValue = get(name) ?: throw IllegalArgumentException(
        "$owner is missing required field '$name'.",
    )
}
