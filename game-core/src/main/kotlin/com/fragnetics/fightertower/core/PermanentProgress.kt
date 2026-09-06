package com.fragnetics.fightertower.core

/**
 * Progress that survives across separate games: the Diamond wallet, and the permanent Laboratory
 * upgrades bought with it (see [LabCatalog]). Unlike [PlayerProgress] (gold/xp/level, which reset
 * every game), a [GameEngine] only reads this once at construction time; the app layer is
 * responsible for persisting it to disk and reloading it between sessions.
 */
data class PermanentProgress(
    val diamonds: Int = 0,
    val startingTowerLevel: Int = 1,
    val bulletDamageLevel: Int = 0
) {
    /** The tier a freshly bought tower starts at, upgraded via the Laboratory's tower upgrade. */
    val startingTowerTier: TowerTier
        get() = TowerTier.entries[(startingTowerLevel - 1).coerceIn(0, TowerTier.entries.lastIndex)]

    /** Multiplies every tower's base damage; raised via the Laboratory's bullet upgrade. */
    val bulletDamageMultiplier: Double
        get() = 1.0 + bulletDamageLevel * LabCatalog.BULLET_DAMAGE_BONUS_PER_LEVEL
}
