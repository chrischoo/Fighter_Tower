package com.fragnetics.fightertower.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class LabCatalogTest {

    @Test
    fun `tower upgrade succeeds when affordable and raises the starting tier`() {
        val progress = PermanentProgress(diamonds = 100, startingTowerLevel = 1)
        val cost = LabCatalog.towerUpgradeCost(1)
        assertNotNull(cost)

        val upgraded = LabCatalog.purchaseTowerUpgrade(progress)
        assertNotNull(upgraded)
        assertEquals(2, upgraded.startingTowerLevel)
        assertEquals(100 - cost, upgraded.diamonds)
    }

    @Test
    fun `tower upgrade fails when diamonds are insufficient`() {
        val progress = PermanentProgress(diamonds = 0, startingTowerLevel = 1)
        assertNull(LabCatalog.purchaseTowerUpgrade(progress))
    }

    @Test
    fun `tower upgrade fails once the max level is reached`() {
        val progress = PermanentProgress(diamonds = 10_000, startingTowerLevel = LabCatalog.MAX_STARTING_TOWER_LEVEL)
        assertNull(LabCatalog.towerUpgradeCost(progress.startingTowerLevel))
        assertNull(LabCatalog.purchaseTowerUpgrade(progress))
    }

    @Test
    fun `bullet upgrade succeeds when affordable and raises the damage multiplier`() {
        val progress = PermanentProgress(diamonds = 100, bulletDamageLevel = 0)
        val before = progress.bulletDamageMultiplier

        val upgraded = LabCatalog.purchaseBulletUpgrade(progress)
        assertNotNull(upgraded)
        assertEquals(1, upgraded.bulletDamageLevel)
        assertEquals(before + LabCatalog.BULLET_DAMAGE_BONUS_PER_LEVEL, upgraded.bulletDamageMultiplier)
    }

    @Test
    fun `bullet upgrade fails once the max level is reached`() {
        val progress = PermanentProgress(diamonds = 10_000, bulletDamageLevel = LabCatalog.MAX_BULLET_DAMAGE_LEVEL)
        assertNull(LabCatalog.bulletUpgradeCost(progress.bulletDamageLevel))
        assertNull(LabCatalog.purchaseBulletUpgrade(progress))
    }
}
