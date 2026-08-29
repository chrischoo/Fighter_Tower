package com.fragnetics.fightertower.core

import kotlin.math.pow

data class EnemyBlueprint(
    val name: String,
    val health: Double,
    val speed: Double,
    val damageToBase: Double,
    val goldReward: Int,
    val xpReward: Int
)

data class EnemySpawn(val delaySeconds: Double, val blueprint: EnemyBlueprint)

data class Wave(val number: Int, val spawns: List<EnemySpawn>)

/**
 * Enemies scale with both the wave number (natural difficulty ramp) and the player's level
 * (leveling up makes every subsequent wave tougher, per the game's core hook).
 */
object WaveGenerator {
    private const val BASE_HEALTH = 20.0
    private const val HEALTH_GROWTH_PER_WAVE = 1.15
    private const val HEALTH_GROWTH_PER_LEVEL = 1.25
    private const val BASE_SPEED = 0.05
    private const val SPEED_GROWTH_PER_WAVE = 1.01
    private const val BASE_DAMAGE = 5.0
    private const val DAMAGE_GROWTH_PER_WAVE = 1.08
    private const val DAMAGE_GROWTH_PER_LEVEL = 1.15
    private const val ENEMIES_BASE_COUNT = 3
    private const val ENEMIES_PER_WAVE = 1
    private const val MAX_ENEMIES_PER_WAVE = 20
    private const val SPAWN_INTERVAL = 1.2
    private const val BOSS_WAVE_INTERVAL = 5

    fun generate(waveNumber: Int, playerLevel: Int): Wave {
        val enemyCount = (ENEMIES_BASE_COUNT + (waveNumber - 1) * ENEMIES_PER_WAVE)
            .coerceAtMost(MAX_ENEMIES_PER_WAVE)
        val health = BASE_HEALTH *
            HEALTH_GROWTH_PER_WAVE.pow(waveNumber - 1) *
            HEALTH_GROWTH_PER_LEVEL.pow(playerLevel - 1)
        val speed = BASE_SPEED * SPEED_GROWTH_PER_WAVE.pow(waveNumber - 1)
        val damage = BASE_DAMAGE *
            DAMAGE_GROWTH_PER_WAVE.pow(waveNumber - 1) *
            DAMAGE_GROWTH_PER_LEVEL.pow(playerLevel - 1)

        val blueprint = EnemyBlueprint(
            name = "Raider",
            health = health,
            speed = speed,
            damageToBase = damage,
            goldReward = 5 + waveNumber,
            xpReward = 8 + waveNumber * 2
        )

        val spawns = mutableListOf<EnemySpawn>()
        for (i in 0 until enemyCount) {
            spawns += EnemySpawn(delaySeconds = i * SPAWN_INTERVAL, blueprint = blueprint)
        }
        if (waveNumber % BOSS_WAVE_INTERVAL == 0) {
            val boss = blueprint.copy(
                name = "Elite Brute",
                health = health * 6.0,
                damageToBase = damage * 2.5,
                goldReward = blueprint.goldReward * 5,
                xpReward = blueprint.xpReward * 4
            )
            spawns += EnemySpawn(delaySeconds = enemyCount * SPAWN_INTERVAL + 1.0, blueprint = boss)
        }
        return Wave(waveNumber, spawns)
    }
}
