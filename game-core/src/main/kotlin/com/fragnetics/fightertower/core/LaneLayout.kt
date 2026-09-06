package com.fragnetics.fightertower.core

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class Vec2(val x: Double, val z: Double) {
    fun distanceTo(other: Vec2): Double = hypot(x - other.x, z - other.z)
}

/**
 * Three lanes converge on a single central base at the origin, fanning outward so enemies
 * approach from three directions at once. Tower spots line each lane; combat range is real 2D
 * distance from a spot to an enemy's current position (gameplay in [GameEngine]) and the app
 * module's WorldLayout mirrors these same values for rendering, so a tower's effective range
 * always matches what's drawn on screen. A spot near the convergence point can plausibly help
 * defend an adjacent lane too, not just its own.
 */
object LaneLayout {
    const val LANE_COUNT = 3
    const val SLOTS_PER_LANE = 6
    const val LANE_LENGTH = 14.0

    private const val SPOT_BASE_RADIUS = 3.0
    private const val SPOT_SPACING = 1.8
    private const val SPOT_SIDE_OFFSET = 1.1

    /** Degrees from the forward (+Z) axis; the lanes fan out in front of the base. */
    private val laneAngleDegrees = doubleArrayOf(-50.0, 0.0, 50.0)

    val basePosition = Vec2(0.0, 0.0)

    private fun laneDirection(lane: Int): Vec2 {
        val radians = Math.toRadians(laneAngleDegrees[lane])
        return Vec2(sin(radians), cos(radians))
    }

    private fun laneNormal(lane: Int): Vec2 {
        val dir = laneDirection(lane)
        return Vec2(dir.z, -dir.x)
    }

    fun spawnPosition(lane: Int): Vec2 {
        val dir = laneDirection(lane)
        return Vec2(dir.x * LANE_LENGTH, dir.z * LANE_LENGTH)
    }

    /** progress: 0 = spawn point (far from the base), 1 = reached the base. */
    fun positionOnLane(lane: Int, progress: Double): Vec2 {
        val remaining = 1.0 - progress.coerceIn(0.0, 1.0)
        val spawn = spawnPosition(lane)
        return Vec2(spawn.x * remaining, spawn.z * remaining)
    }

    fun spotPosition(pos: GridPos): Vec2 {
        val dir = laneDirection(pos.lane)
        val normal = laneNormal(pos.lane)
        val radius = SPOT_BASE_RADIUS + pos.slot * SPOT_SPACING
        val side = if (pos.slot % 2 == 0) 1.0 else -1.0
        return Vec2(
            dir.x * radius + normal.x * side * SPOT_SIDE_OFFSET,
            dir.z * radius + normal.z * side * SPOT_SIDE_OFFSET
        )
    }
}
