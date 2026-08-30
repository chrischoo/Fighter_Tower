package com.fragnetics.fightertower.core

/**
 * Canonical 2D layout of the merge grid and the enemy lane, shared by gameplay (tower range
 * checks in [GameEngine] use real geometric distance to enemies, not just how far along the lane
 * they've walked) and rendering (the app module's WorldLayout mirrors these same values so a
 * tower's effective range always matches what's drawn on screen).
 */
object BoardGeometry {
    const val CELL_SPACING = 1.4
    const val GRID_ORIGIN_X = -2.1
    const val GRID_ORIGIN_Z = 2.0

    const val LANE_X = -4.2
    const val LANE_SPAWN_Z = 16.0
    const val LANE_BASE_Z = -1.0

    fun cellX(pos: GridPos): Double = GRID_ORIGIN_X + pos.col * CELL_SPACING
    fun cellZ(pos: GridPos): Double = GRID_ORIGIN_Z + pos.row * CELL_SPACING

    /** progress: 0 = spawn point (far away), 1 = reached the base. */
    fun laneZ(progress: Double): Double = LANE_SPAWN_Z + (LANE_BASE_Z - LANE_SPAWN_Z) * progress
}
