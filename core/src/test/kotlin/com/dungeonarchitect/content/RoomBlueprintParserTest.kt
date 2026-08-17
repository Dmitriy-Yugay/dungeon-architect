package com.dungeonarchitect.content

import com.dungeonarchitect.domain.CardinalDirection
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.RoomDoor
import com.dungeonarchitect.domain.RoomSocketType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class RoomBlueprintParserTest {
    @Test
    fun `authored prototype room parses without libGDX global state`() {
        val blueprint = RoomBlueprintParser.parse(authoredRoomJson())

        assertEquals("prototype-room", blueprint.id)
        assertEquals("Prototype Room", blueprint.displayName)
        assertEquals(9, blueprint.footprint.size)
        assertEquals(
            setOf(position(0, 1), position(2, 1)),
            blueprint.doorPositions,
        )
        assertEquals(
            setOf(
                RoomDoor(position(0, 1), CardinalDirection.WEST),
                RoomDoor(position(2, 1), CardinalDirection.EAST),
            ),
            blueprint.doors,
        )
        assertEquals(
            mapOf(position(1, 1) to RoomSocketType.FLOOR),
            blueprint.sockets,
        )
    }

    @Test
    fun `second authored room has distinct identity and route geometry`() {
        val prototypeRoom = RoomBlueprintParser.parse(authoredRoomJson())
        val longGallery = RoomBlueprintParser.parse(
            authoredRoomJson("long-gallery.json"),
        )

        assertEquals("long-gallery", longGallery.id)
        assertEquals("Long Gallery", longGallery.displayName)
        assertNotEquals(prototypeRoom.id, longGallery.id)
        assertEquals(
            setOf(
                position(0, 0),
                position(1, 0),
                position(2, 0),
                position(3, 0),
                position(0, 1),
                position(1, 1),
                position(2, 1),
                position(3, 1),
            ),
            longGallery.footprint,
        )
        assertEquals(
            setOf(position(0, 1), position(3, 1)),
            longGallery.doorPositions,
        )
        assertEquals(
            setOf(
                RoomDoor(position(0, 1), CardinalDirection.WEST),
                RoomDoor(position(3, 1), CardinalDirection.EAST),
            ),
            longGallery.doors,
        )
        assertEquals(
            mapOf(position(2, 1) to RoomSocketType.FLOOR),
            longGallery.sockets,
        )
        assertNotEquals(prototypeRoom.footprint, longGallery.footprint)
        assertNotEquals(prototypeRoom.sockets, longGallery.sockets)
    }

    @Test
    fun `authored corner room has adjacent doors and one ordinary socket`() {
        val cornerRoom = RoomBlueprintParser.parse(
            authoredRoomJson("corner-room.json"),
        )

        assertEquals("corner-room", cornerRoom.id)
        assertEquals("Corner Room", cornerRoom.displayName)
        assertEquals(
            setOf(
                position(0, 0),
                position(1, 0),
                position(0, 1),
                position(1, 1),
            ),
            cornerRoom.footprint,
        )
        assertEquals(
            setOf(
                RoomDoor(position(0, 0), CardinalDirection.WEST),
                RoomDoor(position(1, 1), CardinalDirection.NORTH),
            ),
            cornerRoom.doors,
        )
        assertEquals(
            mapOf(position(1, 0) to RoomSocketType.FLOOR),
            cornerRoom.sockets,
        )
    }

    @Test
    fun `parser reads identity positions doors and sockets from supplied JSON`() {
        val blueprint = RoomBlueprintParser.parse(roomJson())

        assertEquals("guard-hall", blueprint.id)
        assertEquals("Guard Hall", blueprint.displayName)
        assertEquals(
            setOf(position(0, 0), position(1, 0), position(0, 1), position(1, 1)),
            blueprint.footprint,
        )
        assertEquals(
            setOf(position(0, 0), position(1, 1)),
            blueprint.doorPositions,
        )
        assertEquals(
            mapOf(position(1, 0) to RoomSocketType.FLOOR),
            blueprint.sockets,
        )
    }

    @Test
    fun `parser requires every room blueprint field`() {
        val fields = requiredFields()

        fields.keys.forEach { omittedField ->
            val json = jsonObject(fields - omittedField)

            assertFailsWith<IllegalArgumentException>(omittedField) {
                RoomBlueprintParser.parse(json)
            }
        }
    }

    @Test
    fun `parser rejects blank malformed and non-object JSON`() {
        assertFailsWith<IllegalArgumentException> {
            RoomBlueprintParser.parse(" \t")
        }
        assertFails {
            RoomBlueprintParser.parse("{")
        }
        assertFailsWith<IllegalArgumentException> {
            RoomBlueprintParser.parse("[]")
        }
    }

    @Test
    fun `parser rejects malformed grid positions`() {
        listOf(
            roomJson(footprint = "[7]"),
            roomJson(footprint = """[{"column": 0}]"""),
            roomJson(
                doors = """
                    [
                      {
                        "position": {"column": "left", "row": 0},
                        "facing": "west"
                      }
                    ]
                """.trimIndent(),
            ),
            roomJson(
                sockets = """
                    [
                      {
                        "position": {"column": 1, "row": 0.5},
                        "type": "floor"
                      }
                    ]
                """.trimIndent(),
            ),
        ).forEach { json ->
            assertFailsWith<IllegalArgumentException> {
                RoomBlueprintParser.parse(json)
            }
        }
    }

    @Test
    fun `parser applies room door validation`() {
        val doorOutsideFootprint = roomJson(
            doors = """
                [
                  {
                    "position": {"column": 2, "row": 0},
                    "facing": "east"
                  }
                ]
            """.trimIndent(),
        )

        assertFailsWith<IllegalArgumentException> {
            RoomBlueprintParser.parse(doorOutsideFootprint)
        }
    }

    @Test
    fun `parser requires a known facing for every door`() {
        listOf(
            """[{"position": {"column": 0, "row": 0}}]""",
            """
                [
                  {
                    "position": {"column": 0, "row": 0},
                    "facing": "diagonal"
                  }
                ]
            """.trimIndent(),
        ).forEach { doors ->
            assertFailsWith<IllegalArgumentException> {
                RoomBlueprintParser.parse(roomJson(doors = doors))
            }
        }
    }

    @Test
    fun `parser rejects a repeated directional door`() {
        val repeatedDoor = """
            [
              {
                "position": {"column": 0, "row": 0},
                "facing": "west"
              },
              {
                "position": {"column": 0, "row": 0},
                "facing": "west"
              }
            ]
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            RoomBlueprintParser.parse(roomJson(doors = repeatedDoor))
        }
    }

    @Test
    fun `parser requires complete socket entries and known socket types`() {
        listOf(
            """[{"type": "floor"}]""",
            """[{"position": {"column": 1, "row": 0}}]""",
            """
                [
                  {
                    "position": {"column": 1, "row": 0},
                    "type": "ceiling"
                  }
                ]
            """.trimIndent(),
        ).forEach { sockets ->
            assertFailsWith<IllegalArgumentException> {
                RoomBlueprintParser.parse(roomJson(sockets = sockets))
            }
        }
    }

    @Test
    fun `parser rejects multiple sockets at one position`() {
        val sockets = """
            [
              {
                "position": {"column": 1, "row": 0},
                "type": "floor"
              },
              {
                "position": {"column": 1, "row": 0},
                "type": "floor"
              }
            ]
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            RoomBlueprintParser.parse(roomJson(sockets = sockets))
        }
    }

    private fun roomJson(
        footprint: String = """
            [
              {"column": 0, "row": 0},
              {"column": 1, "row": 0},
              {"column": 0, "row": 1},
              {"column": 1, "row": 1}
            ]
        """.trimIndent(),
        doors: String = """
            [
              {
                "position": {"column": 0, "row": 0},
                "facing": "west"
              },
              {
                "position": {"column": 1, "row": 1},
                "facing": "east"
              }
            ]
        """.trimIndent(),
        sockets: String = """
            [
              {
                "position": {"column": 1, "row": 0},
                "type": "floor"
              }
            ]
        """.trimIndent(),
    ): String = jsonObject(
        requiredFields(
            footprint = footprint,
            doors = doors,
            sockets = sockets,
        ),
    )

    private fun requiredFields(
        footprint: String = """[{"column": 0, "row": 0}]""",
        doors: String = """
            [
              {
                "position": {"column": 0, "row": 0},
                "facing": "west"
              }
            ]
        """.trimIndent(),
        sockets: String = "[]",
    ): Map<String, String> = linkedMapOf(
        "id" to "\"guard-hall\"",
        "displayName" to "\"Guard Hall\"",
        "footprint" to footprint,
        "doors" to doors,
        "sockets" to sockets,
    )

    private fun jsonObject(fields: Map<String, String>): String =
        fields.entries.joinToString(prefix = "{", postfix = "}") { (name, value) ->
            "\"$name\": $value"
        }

    private fun authoredRoomJson(
        fileName: String = "prototype-room.json",
    ): String {
        val config = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("assets/content/$fileName") }
            .firstOrNull { Files.isRegularFile(it) }
            ?: error("Could not locate authored room config '$fileName'.")

        return Files.readString(config)
    }

    private fun position(column: Int, row: Int) =
        GridPosition(column = column, row = row)
}
