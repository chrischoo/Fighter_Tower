package com.fragnetics.fightertower.core

import kotlin.math.pow

/**
 * Merging two towers of the same tier produces the next tier: heavier, harder-hitting,
 * longer-ranged, faster-firing. TITAN is the ceiling — it cannot be merged further.
 */
enum class TowerTier(val level: Int, val displayName: String) {
    SCOUT(1, "Scout Turret"),
    GUNNER(2, "Gunner Post"),
    CANNON(3, "Cannon Battery"),
    HEAVY_CANNON(4, "Heavy Cannon"),
    SIEGE_TOWER(5, "Siege Tower"),
    RAILGUN(6, "Railgun Spire"),
    JUGGERNAUT(7, "Juggernaut"),
    TITAN(8, "Titan Colossus");

    val isMaxTier: Boolean get() = this == TITAN

    fun next(): TowerTier? = entries.getOrNull(ordinal + 1)

    val damage: Double get() = BASE_DAMAGE * DAMAGE_GROWTH.pow(level - 1)
    val range: Double get() = BASE_RANGE + (level - 1) * RANGE_GROWTH
    val attackCooldownSeconds: Double
        get() = (BASE_COOLDOWN * COOLDOWN_SHRINK.pow(level - 1)).coerceAtLeast(MIN_COOLDOWN)

    companion object {
        private const val BASE_DAMAGE = 5.0
        private const val DAMAGE_GROWTH = 1.8
        private const val BASE_RANGE = 6.0
        private const val RANGE_GROWTH = 0.5
        private const val BASE_COOLDOWN = 1.0
        private const val COOLDOWN_SHRINK = 0.92
        private const val MIN_COOLDOWN = 0.25

        const val BASE_TOWER_COST = 20
    }
}
