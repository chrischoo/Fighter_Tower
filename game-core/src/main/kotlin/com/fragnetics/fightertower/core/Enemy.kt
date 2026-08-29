package com.fragnetics.fightertower.core

object Lane {
    /** Arbitrary world-unit length of the path from spawn to base; tower range is measured in the same units. */
    const val LENGTH = 20.0
}

data class Enemy(
    val id: Long,
    val name: String,
    val maxHealth: Double,
    var health: Double,
    val speed: Double,
    val damageToBase: Double,
    val goldReward: Int,
    val xpReward: Int,
    var progress: Double = 0.0
) {
    val isDead: Boolean get() = health <= 0.0

    /** Remaining distance to the base, in lane units; compared against tower range. */
    val distanceRemaining: Double get() = (1.0 - progress).coerceAtLeast(0.0) * Lane.LENGTH
}
