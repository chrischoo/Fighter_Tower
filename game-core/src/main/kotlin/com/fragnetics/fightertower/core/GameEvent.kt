package com.fragnetics.fightertower.core

sealed class GameEvent {
    data class TowerAttacked(val towerId: Long, val towerPos: GridPos, val enemyId: Long, val damage: Double) : GameEvent()
    data class EnemyKilled(val enemyId: Long, val goldReward: Int, val xpReward: Int) : GameEvent()
    data class EnemyReachedBase(val enemyId: Long, val damage: Double) : GameEvent()
    data class LevelUp(val newLevel: Int) : GameEvent()
    data class WaveStarted(val waveNumber: Int) : GameEvent()
    data class WaveCleared(val waveNumber: Int) : GameEvent()
    data object GameOver : GameEvent()
}
