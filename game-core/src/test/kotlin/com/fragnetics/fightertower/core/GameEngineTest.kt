package com.fragnetics.fightertower.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameEngineTest {

    @Test
    fun `buying a tower spends gold and places it on the board`() {
        val engine = GameEngine(player = PlayerProgress(gold = 50))
        val tower = engine.buyTower()
        assertNotNull(tower)
        assertEquals(TowerTier.SCOUT, tower.tier)
        assertEquals(50 - TowerTier.BASE_TOWER_COST, engine.player.gold)
        assertEquals(1, engine.board.towers.size)
    }

    @Test
    fun `buying a tower fails and refunds nothing extra when gold is insufficient`() {
        val engine = GameEngine(player = PlayerProgress(gold = 5))
        val tower = engine.buyTower()
        assertNull(tower)
        assertEquals(5, engine.player.gold)
        assertTrue(engine.board.towers.isEmpty())
    }

    @Test
    fun `merging through the engine upgrades the tower on the board`() {
        val engine = GameEngine()
        engine.board.placeNewTower(tier = TowerTier.SCOUT, at = GridPos(0, 0))
        engine.board.placeNewTower(tier = TowerTier.SCOUT, at = GridPos(1, 0))

        val result = engine.mergeOrMove(GridPos(0, 0), GridPos(1, 0))

        assertIs<GridBoard.MoveResult.Merged>(result)
        assertEquals(TowerTier.GUNNER, engine.board.towerAt(GridPos(1, 0))?.tier)
    }

    @Test
    fun `a strong tower clears early waves, levels up the player, and holds the base`() {
        // Difficulty compounds with both wave number and player level (by design), so a single
        // tower is only expected to comfortably hold for the early game, not indefinitely.
        val engine = GameEngine(board = GridBoard(columns = 2, rows = 1))
        engine.board.placeNewTower(tier = TowerTier.TITAN, at = GridPos(0, 0))

        val allEvents = mutableListOf<GameEvent>()
        var simulatedSeconds = 0.0
        val dt = 0.1
        while (simulatedSeconds < 60.0 && !engine.gameOver) {
            allEvents += engine.update(dt)
            simulatedSeconds += dt
        }

        assertFalse(engine.gameOver, "a titan-tier tower should comfortably hold the base early on")
        assertTrue(engine.baseHealth > 0.0)
        assertTrue(engine.player.level > 1, "killing many enemies should level up the player")
        assertTrue(engine.waveNumber > 2, "the engine should progress through multiple waves")
        assertTrue(allEvents.any { it is GameEvent.EnemyKilled })
        assertTrue(allEvents.any { it is GameEvent.LevelUp })
        assertTrue(allEvents.any { it is GameEvent.WaveCleared })
    }

    @Test
    fun `leveling up mid-game makes the next wave tougher than it would otherwise have been`() {
        val engineNoKills = GameEngine(board = GridBoard(columns = 1, rows = 1))
        val waveAtLevelOne = WaveGenerator.generate(engineNoKills.waveNumber + 1, engineNoKills.player.level)

        val leveledPlayer = PlayerProgress(level = 1, xp = 0, gold = 0)
        leveledPlayer.addXp(10_000)
        val waveAtHigherLevel = WaveGenerator.generate(engineNoKills.waveNumber + 1, leveledPlayer.level)

        assertTrue(
            waveAtHigherLevel.spawns.first().blueprint.health > waveAtLevelOne.spawns.first().blueprint.health,
            "leveling up should make subsequent waves stronger"
        )
    }

    @Test
    fun `with no defenses, waves eventually overwhelm the base and end the game`() {
        val engine = GameEngine(board = GridBoard(columns = 1, rows = 1))

        val allEvents = mutableListOf<GameEvent>()
        var simulatedSeconds = 0.0
        val dt = 0.25
        while (simulatedSeconds < 1000.0 && !engine.gameOver) {
            allEvents += engine.update(dt)
            simulatedSeconds += dt
        }

        assertTrue(engine.gameOver, "an undefended base should eventually fall")
        assertEquals(0.0, engine.baseHealth)
        assertTrue(allEvents.any { it is GameEvent.EnemyReachedBase })
        assertTrue(allEvents.any { it is GameEvent.GameOver })
        assertTrue(engine.update(1.0).isEmpty(), "no further events should be produced after game over")
    }
}
