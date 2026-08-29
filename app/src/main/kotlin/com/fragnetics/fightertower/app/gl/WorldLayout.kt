package com.fragnetics.fightertower.app.gl

import com.fragnetics.fightertower.core.GridPos

/**
 * Maps game-core grid cells and lane progress onto 3D world-space coordinates. Kept separate
 * from GameEngine so the visual layout can change without touching gameplay rules, and shared by
 * both the renderer (drawing) and the touch controller (screen -> world -> grid cell).
 */
object WorldLayout {
    const val CELL_SPACING = 1.6f
    const val GRID_ORIGIN_X = -0.5f
    const val GRID_ORIGIN_Z = 2.0f

    const val LANE_X = -5.5f
    const val LANE_SPAWN_Z = 16f
    const val LANE_BASE_Z = -1f

    fun cellCenter(pos: GridPos): FloatArray =
        floatArrayOf(GRID_ORIGIN_X + pos.col * CELL_SPACING, 0f, GRID_ORIGIN_Z + pos.row * CELL_SPACING)

    /** progress: 0 = spawn point (far away), 1 = reached the base. */
    fun laneWorldZ(progress: Double): Float =
        LANE_SPAWN_Z + (LANE_BASE_Z - LANE_SPAWN_Z) * progress.toFloat()
}
