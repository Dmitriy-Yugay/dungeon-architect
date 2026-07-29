package com.dungeonarchitect

import com.badlogic.gdx.Game
import com.dungeonarchitect.application.PrototypeScreen

class DungeonArchitectGame : Game() {
    override fun create() {
        screen = PrototypeScreen()
    }

    override fun dispose() {
        screen?.dispose()
    }
}
