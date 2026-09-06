package com.fragnetics.fightertower.core

data class Enemy(
    val id: Long,
    val name: String,
    val laneIndex: Int,
    val maxHealth: Double,
    var health: Double,
    val speed: Double,
    val damageToBase: Double,
    val goldReward: Int,
    val xpReward: Int,
    var progress: Double = 0.0
) {
    val isDead: Boolean get() = health <= 0.0

    /** Current world-space position, derived from which lane it's on and how far along it is. */
    fun position(): Vec2 = LaneLayout.positionOnLane(laneIndex, progress)
}
