package com.fragnetics.fightertower.app.gl

import com.fragnetics.fightertower.core.TowerTier

/**
 * Which body pieces and weapon model represent each tier. Body pieces stack bottom-up (bottom,
 * then middle, then top) so higher tiers read as taller, more built-up towers; the weapon index
 * cycles through the four bundled weapon models so silhouettes vary tier to tier even within the
 * same body height class.
 */
data class TowerVisual(val bodySegments: Int, val weaponIndex: Int)

object TowerVisuals {
    private val byTier = mapOf(
        TowerTier.SCOUT to TowerVisual(bodySegments = 1, weaponIndex = 0),
        TowerTier.GUNNER to TowerVisual(bodySegments = 1, weaponIndex = 1),
        TowerTier.CANNON to TowerVisual(bodySegments = 1, weaponIndex = 2),
        TowerTier.HEAVY_CANNON to TowerVisual(bodySegments = 2, weaponIndex = 3),
        TowerTier.SIEGE_TOWER to TowerVisual(bodySegments = 2, weaponIndex = 0),
        TowerTier.RAILGUN to TowerVisual(bodySegments = 2, weaponIndex = 1),
        TowerTier.JUGGERNAUT to TowerVisual(bodySegments = 3, weaponIndex = 2),
        TowerTier.TITAN to TowerVisual(bodySegments = 3, weaponIndex = 3),
    )

    fun forTier(tier: TowerTier): TowerVisual = byTier.getValue(tier)
}
