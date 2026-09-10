package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.BuildState
import com.dungeonarchitect.domain.GridPosition
import com.dungeonarchitect.domain.PrototypeRunPhase
import com.dungeonarchitect.domain.RoomDoor
import com.dungeonarchitect.domain.RoomSocketType

data class RoomChoiceThumbnail(
    val footprint: Set<GridPosition>,
    val doors: Set<RoomDoor>,
    val sockets: Map<GridPosition, RoomSocketType>,
    val heartAnchor: GridPosition?,
) {
    companion object {
        val EMPTY = RoomChoiceThumbnail(
            footprint = emptySet(),
            doors = emptySet(),
            sockets = emptyMap(),
            heartAnchor = null,
        )
    }
}

data class RoomChoiceView(
    val id: String,
    val displayName: String,
    val isSelected: Boolean,
    val thumbnail: RoomChoiceThumbnail = RoomChoiceThumbnail.EMPTY,
)

class RoomChoicesView private constructor(
    val choices: List<RoomChoiceView>,
) {
    companion object {
        fun from(
            buildState: BuildState,
            blueprintIds: List<String>? = null,
        ): RoomChoicesView {
            val blueprintsById = buildState.availableRoomBlueprints
                .associateBy { it.id }
            val visibleBlueprints = blueprintIds?.map { id ->
                requireNotNull(blueprintsById[id]) {
                    "Unknown room choice blueprint '$id'."
                }
            } ?: buildState.availableRoomBlueprints
            return RoomChoicesView(
                choices = visibleBlueprints.map { blueprint ->
                    val geometry = blueprint.geometry(
                        buildState.selectedRoomOrientation,
                    )
                    RoomChoiceView(
                        id = blueprint.id,
                        displayName = blueprint.displayName,
                        isSelected = blueprint === buildState.selectedRoomBlueprint,
                        thumbnail = RoomChoiceThumbnail(
                            footprint = geometry.footprint,
                            doors = geometry.doors,
                            sockets = geometry.sockets,
                            heartAnchor = geometry.heartAnchor,
                        ),
                    )
                },
            )
        }

        fun forPhase(
            buildState: BuildState,
            phase: PrototypeRunPhase,
            offeredBlueprintIds: List<String>,
        ): RoomChoicesView = when {
            phase == PrototypeRunPhase.ROOM_DRAFT ->
                from(buildState, offeredBlueprintIds)
            phase == PrototypeRunPhase.DEFENSE_PREPARATION &&
                offeredBlueprintIds.isEmpty() -> from(buildState)
            else -> from(buildState, emptyList())
        }
    }
}
