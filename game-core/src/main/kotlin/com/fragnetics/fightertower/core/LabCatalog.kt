package com.fragnetics.fightertower.core

/**
 * Prices and caps for the Laboratory's permanent, Diamond-funded upgrades, plus the pure
 * purchase functions that spend a [PermanentProgress]'s wallet against them. Each purchase
 * function returns the updated progress, or null if the level is already maxed or the wallet
 * can't afford the next level, so callers can't accidentally spend Diamonds twice.
 */
object LabCatalog {
    /** Capped below TITAN so merging towers up in a run still matters even with a maxed Lab. */
    const val MAX_STARTING_TOWER_LEVEL = 4
    const val MAX_BULLET_DAMAGE_LEVEL = 10
    const val BULLET_DAMAGE_BONUS_PER_LEVEL = 0.10

    fun towerUpgradeCost(currentLevel: Int): Int? {
        if (currentLevel >= MAX_STARTING_TOWER_LEVEL) return null
        return 20 * currentLevel
    }

    fun bulletUpgradeCost(currentLevel: Int): Int? {
        if (currentLevel >= MAX_BULLET_DAMAGE_LEVEL) return null
        return 10 * (currentLevel + 1)
    }

    fun purchaseTowerUpgrade(progress: PermanentProgress): PermanentProgress? {
        val cost = towerUpgradeCost(progress.startingTowerLevel) ?: return null
        if (progress.diamonds < cost) return null
        return progress.copy(
            diamonds = progress.diamonds - cost,
            startingTowerLevel = progress.startingTowerLevel + 1
        )
    }

    fun purchaseBulletUpgrade(progress: PermanentProgress): PermanentProgress? {
        val cost = bulletUpgradeCost(progress.bulletDamageLevel) ?: return null
        if (progress.diamonds < cost) return null
        return progress.copy(
            diamonds = progress.diamonds - cost,
            bulletDamageLevel = progress.bulletDamageLevel + 1
        )
    }
}
