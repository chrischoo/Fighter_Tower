package com.fragnetics.fightertower.core

/**
 * Ties the board, waves, combat, and player progression into a single deterministic tick.
 * Owns no rendering/Android concerns so it can be driven and unit-tested headlessly.
 */
class GameEngine(
    val board: GridBoard = GridBoard(columns = 4, rows = 3),
    val player: PlayerProgress = PlayerProgress()
) {
    val baseMaxHealth: Double = 100.0
    var baseHealth: Double = baseMaxHealth
        private set
    var gameOver: Boolean = false
        private set
    var currentWave: Wave? = null
        private set
    var waveNumber: Int = 0
        private set

    private var waveClock = 0.0
    private var pendingSpawns: MutableList<EnemySpawn> = mutableListOf()
    private var interWaveTimer = 0.0
    private var waveClearedAnnounced = false
    private val interWaveDelay = 3.0

    private val enemiesById = LinkedHashMap<Long, Enemy>()
    private var nextEnemyId = 1L

    private val eventsBuffer = mutableListOf<GameEvent>()

    init {
        startNextWave()
    }

    val activeEnemies: List<Enemy> get() = enemiesById.values.toList()

    fun buyTower(): Tower? {
        if (gameOver) return null
        val cost = TowerTier.BASE_TOWER_COST
        if (!player.spendGold(cost)) return null
        val tower = board.placeNewTower()
        if (tower == null) {
            player.addGold(cost)
            return null
        }
        return tower
    }

    fun mergeOrMove(from: GridPos, to: GridPos): GridBoard.MoveResult {
        if (gameOver) return GridBoard.MoveResult.Invalid
        return board.moveOrMerge(from, to)
    }

    /** Advances the simulation by [deltaSeconds] and returns the events that occurred, in order. */
    fun update(deltaSeconds: Double): List<GameEvent> {
        eventsBuffer.clear()
        if (gameOver) return emptyList()

        spawnPendingEnemies(deltaSeconds)
        advanceEnemies(deltaSeconds)
        resolveTowerAttacks(deltaSeconds)
        checkWaveCompletion(deltaSeconds)

        return eventsBuffer.toList()
    }

    private fun spawnPendingEnemies(dt: Double) {
        waveClock += dt
        val iterator = pendingSpawns.iterator()
        while (iterator.hasNext()) {
            val spawn = iterator.next()
            if (waveClock >= spawn.delaySeconds) {
                val enemy = Enemy(
                    id = nextEnemyId++,
                    name = spawn.blueprint.name,
                    maxHealth = spawn.blueprint.health,
                    health = spawn.blueprint.health,
                    speed = spawn.blueprint.speed,
                    damageToBase = spawn.blueprint.damageToBase,
                    goldReward = spawn.blueprint.goldReward,
                    xpReward = spawn.blueprint.xpReward
                )
                enemiesById[enemy.id] = enemy
                iterator.remove()
            }
        }
    }

    private fun advanceEnemies(dt: Double) {
        val reachedBase = mutableListOf<Enemy>()
        for (enemy in enemiesById.values) {
            if (enemy.isDead) continue
            enemy.progress += enemy.speed * dt
            if (enemy.progress >= 1.0) reachedBase += enemy
        }
        for (enemy in reachedBase) {
            baseHealth = (baseHealth - enemy.damageToBase).coerceAtLeast(0.0)
            eventsBuffer += GameEvent.EnemyReachedBase(enemy.id, enemy.damageToBase)
            enemiesById.remove(enemy.id)
            if (baseHealth <= 0.0 && !gameOver) {
                gameOver = true
                eventsBuffer += GameEvent.GameOver
            }
        }
    }

    private fun resolveTowerAttacks(dt: Double) {
        if (gameOver) return
        val candidates = enemiesById.values
        for (tower in board.towers) {
            tower.cooldownRemaining = (tower.cooldownRemaining - dt).coerceAtLeast(0.0)
            if (tower.cooldownRemaining > 0.0) continue

            val target = candidates
                .filter { !it.isDead && it.distanceRemaining <= tower.tier.range }
                .minByOrNull { it.distanceRemaining }
                ?: continue

            target.health = (target.health - tower.tier.damage).coerceAtLeast(0.0)
            tower.cooldownRemaining = tower.tier.attackCooldownSeconds
            eventsBuffer += GameEvent.TowerAttacked(tower.id, tower.position, target.id, tower.tier.damage)

            if (target.isDead) {
                enemiesById.remove(target.id)
                player.addGold(target.goldReward)
                val levelsGained = player.addXp(target.xpReward)
                eventsBuffer += GameEvent.EnemyKilled(target.id, target.goldReward, target.xpReward)
                repeat(levelsGained) {
                    eventsBuffer += GameEvent.LevelUp(player.level)
                }
            }
        }
    }

    private fun checkWaveCompletion(dt: Double) {
        if (pendingSpawns.isNotEmpty() || enemiesById.isNotEmpty()) return

        if (!waveClearedAnnounced) {
            eventsBuffer += GameEvent.WaveCleared(waveNumber)
            waveClearedAnnounced = true
        }
        interWaveTimer += dt
        if (interWaveTimer >= interWaveDelay) {
            startNextWave()
        }
    }

    private fun startNextWave() {
        waveNumber += 1
        waveClock = 0.0
        interWaveTimer = 0.0
        waveClearedAnnounced = false
        val wave = WaveGenerator.generate(waveNumber, player.level)
        currentWave = wave
        pendingSpawns = wave.spawns.toMutableList()
        eventsBuffer += GameEvent.WaveStarted(waveNumber)
    }
}
