package com.fragnetics.fightertower.core

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
}
