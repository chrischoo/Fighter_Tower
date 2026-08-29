package com.fragnetics.fightertower.app.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.fragnetics.fightertower.app.game.GameSession
import com.fragnetics.fightertower.app.input.TouchController

class GameGLSurfaceView(context: Context, session: GameSession) : GLSurfaceView(context) {
    private val renderer = GameRenderer(session)
    private val touchController = TouchController(session, renderer)

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return touchController.onTouchEvent(event, width, height)
    }
}
