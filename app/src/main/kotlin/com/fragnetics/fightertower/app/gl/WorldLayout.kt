package com.fragnetics.fightertower.app.gl

import com.fragnetics.fightertower.core.DiamondDrop
import com.fragnetics.fightertower.core.Enemy
import com.fragnetics.fightertower.core.GridPos
import com.fragnetics.fightertower.core.LaneLayout
import com.fragnetics.fightertower.core.Vec2

/**
 * Maps game-core's logical lane/spot positions onto OpenGL world-space coordinates. All X/Z
 * values come straight from [LaneLayout] — the same positions GameEngine uses for combat range
 * checks — so what's drawn always matches what's actually in range; this file only adds the
 * render-specific Y (height) and hands touch input the same lookup for hit-testing.
 */
object WorldLayout {

    fun towerWorld(pos: GridPos): FloatArray = worldFrom(LaneLayout.spotPosition(pos), y = 0f)

    fun enemyWorld(enemy: Enemy): FloatArray = worldFrom(enemy.position(), y = 0.3f)

    fun diamondWorld(diamond: DiamondDrop): FloatArray = worldFrom(diamond.position(), y = 0f)

    fun spawnWorld(lane: Int): FloatArray = worldFrom(LaneLayout.spawnPosition(lane), y = 0f)

    fun baseWorld(): FloatArray = worldFrom(LaneLayout.basePosition, y = 0f)

    fun allSpots(laneCount: Int, slotsPerLane: Int): List<Pair<GridPos, Vec2>> =
        (0 until laneCount).flatMap { lane ->
            (0 until slotsPerLane).map { slot ->
                val pos = GridPos(lane, slot)
                pos to LaneLayout.spotPosition(pos)
            }
        }

    private fun worldFrom(v: Vec2, y: Float): FloatArray = floatArrayOf(v.x.toFloat(), y, v.z.toFloat())
}
