package com.fragnetics.fightertower.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerProgressTest {

    @Test
    fun `xp below threshold does not level up`() {
        val player = PlayerProgress(level = 1, xp = 0)
        val levelsGained = player.addXp(player.xpToNextLevel - 1)
        assertEquals(0, levelsGained)
        assertEquals(1, player.level)
    }

    @Test
    fun `xp at threshold levels up and carries remainder`() {
        val player = PlayerProgress(level = 1, xp = 0)
        val threshold = player.xpToNextLevel
        val levelsGained = player.addXp(threshold + 10)
        assertEquals(1, levelsGained)
        assertEquals(2, player.level)
        assertEquals(10, player.xp)
    }

    @Test
    fun `a large xp gain can trigger multiple level ups at once`() {
        val player = PlayerProgress(level = 1, xp = 0)
        val levelsGained = player.addXp(1000)
        assertTrue(levelsGained >= 2)
        assertEquals(1 + levelsGained, player.level)
    }

    @Test
    fun `spending more gold than available fails and leaves balance unchanged`() {
        val player = PlayerProgress(gold = 10)
        assertFalse(player.spendGold(20))
        assertEquals(10, player.gold)
    }

    @Test
    fun `spending affordable gold succeeds`() {
        val player = PlayerProgress(gold = 20)
        assertTrue(player.spendGold(20))
        assertEquals(0, player.gold)
    }
}
