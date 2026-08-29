package com.fragnetics.fightertower.app.input

import android.opengl.Matrix
import android.view.MotionEvent
import com.fragnetics.fightertower.app.game.GameSession
import com.fragnetics.fightertower.app.gl.GameRenderer
import com.fragnetics.fightertower.app.gl.WorldLayout
import com.fragnetics.fightertower.core.GridPos
import kotlin.math.abs

/**
 * Converts screen touches into merge-yard grid cells by unprojecting a ray through the
 * renderer's current view-projection matrix and intersecting it with the ground plane (y = 0),
 * then turns a press-drag-release gesture into a [GameSession.mergeOrMove] call: press on a
 * tower, drag, release over another cell to move it there or merge two same-tier towers.
 */
class TouchController(
    private val session: GameSession,
    private val renderer: GameRenderer
) {
    private var dragFrom: GridPos? = null

    fun onTouchEvent(event: MotionEvent, viewWidth: Int, viewHeight: Int): Boolean {
        if (viewWidth == 0 || viewHeight == 0) return true
        val cell = screenToGridCell(event.x, event.y, viewWidth, viewHeight)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> dragFrom = cell
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val from = dragFrom
                dragFrom = null
                if (from != null && cell != null && from != cell) {
                    session.mergeOrMove(from, cell)
                }
            }
        }
        return true
    }

    private fun screenToGridCell(screenX: Float, screenY: Float, viewWidth: Int, viewHeight: Int): GridPos? {
        val ndcX = 2f * screenX / viewWidth - 1f
        val ndcY = 1f - 2f * screenY / viewHeight

        val viewProjection = renderer.currentViewProjectionMatrix()
        val inverse = FloatArray(16)
        if (!Matrix.invertM(inverse, 0, viewProjection, 0)) return null

        val nearPoint = unproject(inverse, ndcX, ndcY, -1f)
        val farPoint = unproject(inverse, ndcX, ndcY, 1f)

        val dy = farPoint[1] - nearPoint[1]
        if (abs(dy) < 1e-6f) return null
        val t = -nearPoint[1] / dy
        if (t < 0f) return null

        val worldX = nearPoint[0] + t * (farPoint[0] - nearPoint[0])
        val worldZ = nearPoint[2] + t * (farPoint[2] - nearPoint[2])

        val (columns, rows) = session.boardDimensions()
        val colF = (worldX - WorldLayout.GRID_ORIGIN_X) / WorldLayout.CELL_SPACING
        val rowF = (worldZ - WorldLayout.GRID_ORIGIN_Z) / WorldLayout.CELL_SPACING
        val col = Math.round(colF)
        val row = Math.round(rowF)
        if (abs(colF - col) > 0.7f || abs(rowF - row) > 0.7f) return null
        if (col !in 0 until columns || row !in 0 until rows) return null
        return GridPos(col, row)
    }

    private fun unproject(inverseViewProjection: FloatArray, ndcX: Float, ndcY: Float, ndcZ: Float): FloatArray {
        val clip = floatArrayOf(ndcX, ndcY, ndcZ, 1f)
        val world = FloatArray(4)
        Matrix.multiplyMV(world, 0, inverseViewProjection, 0, clip, 0)
        val w = if (abs(world[3]) < 1e-6f) 1e-6f else world[3]
        return floatArrayOf(world[0] / w, world[1] / w, world[2] / w)
    }
}
