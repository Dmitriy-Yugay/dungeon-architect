package com.dungeonarchitect.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.dungeonarchitect.DungeonArchitectGame

fun main() {
    Lwjgl3Application(DungeonArchitectGame(), desktopConfiguration())
}

private fun desktopConfiguration() = Lwjgl3ApplicationConfiguration().apply {
    setTitle(WINDOW_TITLE)
    setWindowedMode(WINDOW_WIDTH, WINDOW_HEIGHT)
    useVsync(true)
    setForegroundFPS(FOREGROUND_FPS)
}

private const val WINDOW_TITLE = "Dungeon Architect"
private const val WINDOW_WIDTH = 1280
private const val WINDOW_HEIGHT = 720
private const val FOREGROUND_FPS = 60
