package com.dungeonarchitect.domain

class BuildState(
    availableRoomBlueprints: List<RoomBlueprint>,
    selectedRoomBlueprint: RoomBlueprint,
    val selectedTrapDefinition: TrapDefinition,
    selectedRoomOrientation: RoomOrientation = RoomOrientation.UNROTATED,
) {
    val availableRoomBlueprints: List<RoomBlueprint> =
        availableRoomBlueprints.toList()

    var selectedRoomBlueprint: RoomBlueprint = selectedRoomBlueprint
        private set

    var selectedRoomOrientation: RoomOrientation = selectedRoomOrientation
        private set

    var isHeartPlacementModeActive: Boolean = false
        private set

    init {
        require(this.availableRoomBlueprints.isNotEmpty()) {
            "Build state must contain at least one available room blueprint."
        }
        require(
            this.availableRoomBlueprints.distinctBy(RoomBlueprint::id).size ==
                this.availableRoomBlueprints.size,
        ) {
            "Available room blueprints must have distinct IDs."
        }

        this.selectedRoomBlueprint = this.availableRoomBlueprints
            .singleOrNull { it.id == selectedRoomBlueprint.id }
            ?: throw IllegalArgumentException(
                "The selected room blueprint must be available to build.",
            )
    }

    fun selectRoomBlueprint(id: String): Boolean {
        val blueprint = availableRoomBlueprints.firstOrNull { it.id == id }
            ?: return false

        selectedRoomBlueprint = blueprint
        return true
    }

    fun rotateSelectedRoomClockwise() {
        selectedRoomOrientation = selectedRoomOrientation.rotateClockwise()
    }

    fun rotateSelectedRoomCounterClockwise() {
        selectedRoomOrientation = selectedRoomOrientation.rotateCounterClockwise()
    }

    fun toggleHeartPlacementMode() {
        isHeartPlacementModeActive = !isHeartPlacementModeActive
    }

    fun deactivateHeartPlacementMode() {
        isHeartPlacementModeActive = false
    }
}
