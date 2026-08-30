package com.fragnetics.fightertower.app.gl

import com.fragnetics.fightertower.core.BoardGeometry
import com.fragnetics.fightertower.core.GridPos

/**
 * Maps game-core grid cells and lane progress onto 3D world-space coordinates (as Float, for
 * OpenGL). Mirrors [BoardGeometry] so the drawn board always matches the geometry gameplay uses
 * for tower range checks. Shared by both the renderer (drawing) and the touch controller
 * (screen -> world -> grid cell).
 */
object WorldLayout {
    val CELL_SPACING: Float = BoardGeometry.CELL_SPACING.toFloat()
    val GRID_ORIGIN_X: Float = BoardGeometry.GRID_ORIGIN_X.toFloat()
    val GRID_ORIGIN_Z: Float = BoardGeometry.GRID_ORIGIN_Z.toFloat()

    val LANE_X: Float = BoardGeometry.LANE_X.toFloat()
    val LANE_SPAWN_Z: Float = BoardGeometry.LANE_SPAWN_Z.toFloat()
    val LANE_BASE_Z: Float = BoardGeometry.LANE_BASE_Z.toFloat()

    fun cellCenter(pos: GridPos): FloatArray =
        floatArrayOf(BoardGeometry.cellX(pos).toFloat(), 0f, BoardGeometry.cellZ(pos).toFloat())

    /** progress: 0 = spawn point (far away), 1 = reached the base. */
    fun laneWorldZ(progress: Double): Float = BoardGeometry.laneZ(progress).toFloat()
}
