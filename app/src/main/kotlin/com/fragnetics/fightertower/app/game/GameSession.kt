package com.fragnetics.fightertower.app.game

import com.fragnetics.fightertower.core.DiamondDrop
import com.fragnetics.fightertower.core.Enemy
import com.fragnetics.fightertower.core.GameEngine
import com.fragnetics.fightertower.core.GameEvent
import com.fragnetics.fightertower.core.GridBoard
import com.fragnetics.fightertower.core.GridPos
import com.fragnetics.fightertower.core.PermanentProgress
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
 *
 * Also bridges the engine's per-run Diamond drops to the app's persistent Diamond wallet: every
 * time a drop is collected via [collectDiamond], [onDiamondCollected] fires so the caller can
 * credit the wallet and save it, since Diamonds (unlike Gold) survive across games.
 */
class GameSession(
    private var permanentProgress: PermanentProgress = PermanentProgress(),
    private val onDiamondCollected: (Int) -> Unit = {}
) {
    private val lock = Any()
    private var engine = GameEngine(permanentProgress = permanentProgress)

    fun update(deltaSeconds: Double): List<GameEvent> = synchronized(lock) { engine.update(deltaSeconds) }

    fun buyTower(): Tower? = synchronized(lock) { engine.buyTower() }

    fun mergeOrMove(from: GridPos, to: GridPos): GridBoard.MoveResult =
        synchronized(lock) { engine.mergeOrMove(from, to) }

    fun snapshotTowers(): List<Tower> = synchronized(lock) { engine.board.towers.map { it.copy() } }

    fun snapshotEnemies(): List<Enemy> = synchronized(lock) { engine.activeEnemies.map { it.copy() } }

    fun snapshotDiamonds(): List<DiamondDrop> = synchronized(lock) { engine.activeDiamonds.map { it.copy() } }

    /** Called from touch input when the player taps a dropped diamond pile. */
    fun collectDiamond(id: Long): Boolean {
        val amount = synchronized(lock) { engine.collectDiamond(id) } ?: return false
        onDiamondCollected(amount)
        return true
    }

    fun boardDimensions(): Pair<Int, Int> = synchronized(lock) { engine.board.laneCount to engine.board.slotsPerLane }

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

    /** Updates the Laboratory upgrades a freshly started run should use; takes effect on [reset]. */
    fun updatePermanentProgress(progress: PermanentProgress) {
        synchronized(lock) { permanentProgress = progress }
    }

    fun reset() {
        synchronized(lock) { engine = GameEngine(permanentProgress = permanentProgress) }
    }
}
