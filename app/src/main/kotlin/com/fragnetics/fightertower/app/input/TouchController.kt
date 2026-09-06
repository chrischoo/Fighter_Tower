package com.fragnetics.fightertower.app.input

import android.opengl.Matrix
import android.view.MotionEvent
import com.fragnetics.fightertower.app.game.GameSession
import com.fragnetics.fightertower.app.gl.GameRenderer
import com.fragnetics.fightertower.app.gl.WorldLayout
import com.fragnetics.fightertower.core.GridPos
import kotlin.math.abs

private const val SPOT_HIT_RADIUS = 1.0f

/**
 * Converts screen touches into tower spots by unprojecting a ray through the renderer's current
 * view-projection matrix, intersecting it with the ground plane (y = 0), and finding the nearest
 * spot to that point (spots sit at irregular positions around the three lanes, not on a
 * rectangular grid, so this is a nearest-neighbor search rather than simple rounding).
 *
 * A tap that lands on a dropped diamond pile (on any lane) collects it immediately (see
 * [GameSession.collectDiamond]) instead of starting a drag. Otherwise, a press-drag-release
 * gesture moves or merges towers: press on a tower, drag, release over another spot — on any
 * lane — to move it there or merge two same-tier towers.
 */
class TouchController(
    private val session: GameSession,
    private val renderer: GameRenderer
) {
    private var dragFrom: GridPos? = null

    fun onTouchEvent(event: MotionEvent, viewWidth: Int, viewHeight: Int): Boolean {
        if (viewWidth == 0 || viewHeight == 0) return true
        val worldPoint = screenToGroundWorld(event.x, event.y, viewWidth, viewHeight)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val diamondId = worldPoint?.let { nearestDiamondId(it) }
                dragFrom = if (diamondId != null) {
                    session.collectDiamond(diamondId)
                    null
                } else {
                    worldPoint?.let { nearestSpot(it) }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val from = dragFrom
                dragFrom = null
                val to = worldPoint?.let { nearestSpot(it) }
                if (from != null && to != null && from != to) {
                    session.mergeOrMove(from, to)
                }
            }
        }
        return true
    }

    private fun nearestDiamondId(worldPoint: FloatArray): Long? {
        var bestId: Long? = null
        var bestDistSq = DIAMOND_HIT_RADIUS * DIAMOND_HIT_RADIUS
        for (diamond in session.snapshotDiamonds()) {
            val diamondWorld = WorldLayout.diamondWorld(diamond)
            val dx = worldPoint[0] - diamondWorld[0]
            val dz = worldPoint[2] - diamondWorld[2]
            val distSq = dx * dx + dz * dz
            if (distSq <= bestDistSq) {
                bestDistSq = distSq
                bestId = diamond.id
            }
        }
        return bestId
    }

    private fun nearestSpot(worldPoint: FloatArray): GridPos? {
        val (laneCount, slotsPerLane) = session.boardDimensions()
        var best: GridPos? = null
        var bestDistanceSq = Float.MAX_VALUE
        for ((pos, spot) in WorldLayout.allSpots(laneCount, slotsPerLane)) {
            val dx = worldPoint[0] - spot.x.toFloat()
            val dz = worldPoint[2] - spot.z.toFloat()
            val distanceSq = dx * dx + dz * dz
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq
                best = pos
            }
        }
        return if (bestDistanceSq <= SPOT_HIT_RADIUS * SPOT_HIT_RADIUS) best else null
    }

    private fun screenToGroundWorld(screenX: Float, screenY: Float, viewWidth: Int, viewHeight: Int): FloatArray? {
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
        return floatArrayOf(worldX, 0f, worldZ)
    }

    private fun unproject(inverseViewProjection: FloatArray, ndcX: Float, ndcY: Float, ndcZ: Float): FloatArray {
        val clip = floatArrayOf(ndcX, ndcY, ndcZ, 1f)
        val world = FloatArray(4)
        Matrix.multiplyMV(world, 0, inverseViewProjection, 0, clip, 0)
        val w = if (abs(world[3]) < 1e-6f) 1e-6f else world[3]
        return floatArrayOf(world[0] / w, world[1] / w, world[2] / w)
    }

    private companion object {
        const val DIAMOND_HIT_RADIUS = 0.9f
    }
}
