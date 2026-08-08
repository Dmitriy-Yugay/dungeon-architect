package com.dungeonarchitect.presentation

import com.dungeonarchitect.domain.BuildState

data class RoomChoiceView(
    val id: String,
    val displayName: String,
    val isSelected: Boolean,
)

class RoomChoicesView private constructor(
    val choices: List<RoomChoiceView>,
) {
    companion object {
        fun from(buildState: BuildState) = RoomChoicesView(
            choices = buildState.availableRoomBlueprints.map { blueprint ->
                RoomChoiceView(
                    id = blueprint.id,
                    displayName = blueprint.displayName,
                    isSelected = blueprint === buildState.selectedRoomBlueprint,
                )
            },
        )
    }
}
