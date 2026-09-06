package com.fragnetics.fightertower.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiamondDropTest {

    private fun killOneEnemy(engine: GameEngine) {
        engine.board.placeNewTower(tier = TowerTier.TITAN, at = GridPos(0, 0))
        var simulatedSeconds = 0.0
        while (simulatedSeconds < 30.0 && engine.activeDiamonds.isEmpty()) {
            engine.update(0.1)
            simulatedSeconds += 0.1
        }
    }

    @Test
    fun `killing an enemy drops a diamond pile that can be collected`() {
        val engine = GameEngine(board = GridBoard(columns = 1, rows = 1))
        killOneEnemy(engine)

        val drop = engine.activeDiamonds.firstOrNull()
        assertNotNull(drop, "a diamond drop should appear after a kill")
        assertTrue(drop.amount > 0)

        val collected = engine.collectDiamond(drop.id)
        assertEquals(drop.amount, collected)
        assertTrue(engine.activeDiamonds.isEmpty())
    }

    @Test
    fun `collecting an unknown or already-collected drop returns null`() {
        val engine = GameEngine(board = GridBoard(columns = 1, rows = 1))
        assertNull(engine.collectDiamond(999L))

        killOneEnemy(engine)
        val drop = engine.activeDiamonds.first()
        engine.collectDiamond(drop.id)
        assertNull(engine.collectDiamond(drop.id))
    }

    @Test
    fun `an uncollected diamond drop expires after its lifetime`() {
        // A lone TITAN tower keeps clearing waves indefinitely, so later kills keep dropping
        // fresh diamonds during the wait below; track this specific drop by id rather than
        // asserting the whole list is empty.
        val engine = GameEngine(board = GridBoard(columns = 1, rows = 1))
        killOneEnemy(engine)
        val firstDropId = engine.activeDiamonds.first().id

        var simulatedSeconds = 0.0
        while (simulatedSeconds < DiamondDrop.LIFETIME_SECONDS + 1.0) {
            engine.update(0.1)
            simulatedSeconds += 0.1
        }

        assertTrue(
            engine.activeDiamonds.none { it.id == firstDropId },
            "the drop should have expired and been removed"
        )
        assertNull(engine.collectDiamond(firstDropId))
    }

    @Test
    fun `starting tower tier follows permanent progress, and bullet damage multiplier scales attacks`() {
        val upgraded = PermanentProgress(startingTowerLevel = 3, bulletDamageLevel = 2)
        val engine = GameEngine(player = PlayerProgress(gold = 100), permanentProgress = upgraded)

        val tower = engine.buyTower()
        assertNotNull(tower)
        assertEquals(TowerTier.CANNON, tower.tier)
        assertEquals(1.0 + 2 * LabCatalog.BULLET_DAMAGE_BONUS_PER_LEVEL, upgraded.bulletDamageMultiplier)
    }
}
