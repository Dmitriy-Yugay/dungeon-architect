package com.dungeonarchitect.application

import com.dungeonarchitect.domain.DungeonGrid
import com.dungeonarchitect.domain.PlacedRoom
import com.dungeonarchitect.domain.RoomBlueprint
import com.dungeonarchitect.domain.RoomOrientation
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthoredRoomOfferCompatibilityTest {
    @Test
    fun `every authored intermission offer fits its deterministic run state`() {
        val buildState = PrototypeScreen.loadBuildState(::readInternalText)
        val runDefinition = PrototypeScreen.loadRunDefinition(::readInternalText)
        val blueprintsById = buildState.availableRoomBlueprints
            .associateBy(RoomBlueprint::id)
        val grid = PrototypeScreen.prototypeGrid()
        val gallery = blueprintsById.getValue("long-gallery")
        repeat(2) {
            assertTrue(grid.place(assertNotNull(legalPlacement(grid, gallery))))
        }

        runDefinition.waves.drop(1).forEachIndexed { index, wave ->
            assertEquals(3, wave.roomOfferBlueprintIds.size, wave.id)
            val legalRooms = wave.roomOfferBlueprintIds.map { blueprintId ->
                val blueprint = blueprintsById.getValue(blueprintId)
                assertNotNull(
                    legalPlacement(grid, blueprint),
                    "${wave.id} offer '$blueprintId' has no legal attachment.",
                )
            }

            if (index < runDefinition.waves.lastIndex - 1) {
                val prototypeRoom = legalRooms.single { room ->
                    room.blueprint.id == "prototype-room"
                }
                assertTrue(grid.place(prototypeRoom))
            }
        }
    }

    private fun legalPlacement(
        grid: DungeonGrid,
        blueprint: RoomBlueprint,
    ): PlacedRoom? = grid.roomAttachmentTargets.firstNotNullOfOrNull { target ->
        RoomOrientation.entries.firstNotNullOfOrNull { orientation ->
            grid.targetedPlacementPreview(
                blueprint = blueprint,
                target = target,
                orientation = orientation,
            )?.takeIf { it.isValid }?.room
        }
    }

    private fun readInternalText(path: String): String = Files.readString(
        assetRoot().resolve(path),
    )

    private fun assetRoot(): Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("assets") }
            .firstOrNull(Files::isDirectory)
            ?: error("Could not locate assets directory.")
}
