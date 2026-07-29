package com.dungeonarchitect

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20

class DungeonArchitectGame : ApplicationAdapter() {
    override fun create() {
        Gdx.gl.glClearColor(
            BACKGROUND_RED,
            BACKGROUND_GREEN,
            BACKGROUND_BLUE,
            BACKGROUND_ALPHA,
        )
    }

    override fun render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }

    private companion object {
        const val BACKGROUND_RED = 0.04f
        const val BACKGROUND_GREEN = 0.05f
        const val BACKGROUND_BLUE = 0.07f
        const val BACKGROUND_ALPHA = 1f
    }
}
