package com.fragnetics.fightertower.app.game

import com.fragnetics.fightertower.core.Enemy
import com.fragnetics.fightertower.core.GameEngine
import com.fragnetics.fightertower.core.GameEvent
import com.fragnetics.fightertower.core.GridBoard
import com.fragnetics.fightertower.core.GridPos
import com.fragnetics.fightertower.core.Tower

data class GameSnapshot(
    val level: Int,
    val xp: Int,
    val xpToNext: Int,
    val gold: Int,
    val wave: Int,
    val baseHealth: Double,
    val baseMaxHealth: Double,
    val gameOver: Boolean
)

/**
 * Thread-safe façade over [GameEngine]: the GL render loop ticks it on the GL thread every
 * frame, while touch input and the Compose HUD read and mutate it from the main thread.
 */
class GameSession {
    private val lock = Any()
    private var engine = GameEngine()

    fun update(deltaSeconds: Double): List<GameEvent> = synchronized(lock) { engine.update(deltaSeconds) }

    fun buyTower(): Tower? = synchronized(lock) { engine.buyTower() }

    fun mergeOrMove(from: GridPos, to: GridPos): GridBoard.MoveResult =
        synchronized(lock) { engine.mergeOrMove(from, to) }

    fun snapshotTowers(): List<Tower> = synchronized(lock) { engine.board.towers.map { it.copy() } }

    fun snapshotEnemies(): List<Enemy> = synchronized(lock) { engine.activeEnemies.map { it.copy() } }

    fun boardDimensions(): Pair<Int, Int> = synchronized(lock) { engine.board.columns to engine.board.rows }

    fun snapshot(): GameSnapshot = synchronized(lock) {
        GameSnapshot(
            level = engine.player.level,
            xp = engine.player.xp,
            xpToNext = engine.player.xpToNextLevel,
            gold = engine.player.gold,
            wave = engine.waveNumber,
            baseHealth = engine.baseHealth,
            baseMaxHealth = engine.baseMaxHealth,
            gameOver = engine.gameOver
        )
    }

    fun reset() {
        synchronized(lock) { engine = GameEngine() }
    }
}
