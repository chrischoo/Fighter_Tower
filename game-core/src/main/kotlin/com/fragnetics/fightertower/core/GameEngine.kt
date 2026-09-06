package com.fragnetics.fightertower.core

/**
 * Ties the board, waves, combat, and player progression into a single deterministic tick.
 * Owns no rendering/Android concerns so it can be driven and unit-tested headlessly.
 */
class GameEngine(
    val board: GridBoard = GridBoard(laneCount = LaneLayout.LANE_COUNT, slotsPerLane = LaneLayout.SLOTS_PER_LANE),
    val player: PlayerProgress = PlayerProgress(),
    val permanentProgress: PermanentProgress = PermanentProgress()
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

    private val diamondsById = LinkedHashMap<Long, DiamondDrop>()
    private var nextDiamondId = 1L

    private val eventsBuffer = mutableListOf<GameEvent>()

    init {
        startNextWave()
    }

    val activeEnemies: List<Enemy> get() = enemiesById.values.toList()
    val activeDiamonds: List<DiamondDrop> get() = diamondsById.values.toList()

    fun buyTower(): Tower? {
        if (gameOver) return null
        val cost = TowerTier.BASE_TOWER_COST
        if (!player.spendGold(cost)) return null
        val tower = board.placeNewTower(tier = permanentProgress.startingTowerTier)
        if (tower == null) {
            player.addGold(cost)
            return null
        }
        return tower
    }

    /** Collects a dropped diamond pile by id, returning the amount gained, or null if it's
     * already been collected or expired. */
    fun collectDiamond(id: Long): Int? {
        val drop = diamondsById.remove(id) ?: return null
        return drop.amount
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
        advanceDiamonds(deltaSeconds)
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
                    laneIndex = spawn.laneIndex,
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

            val target = nearestEnemyInRange(LaneLayout.spotPosition(tower.position), tower.tier.range, candidates)
                ?: continue

            val damage = tower.tier.damage * permanentProgress.bulletDamageMultiplier
            target.health = (target.health - damage).coerceAtLeast(0.0)
            tower.cooldownRemaining = tower.tier.attackCooldownSeconds
            eventsBuffer += GameEvent.TowerAttacked(tower.id, tower.position, target.id, damage)

            if (target.isDead) {
                enemiesById.remove(target.id)
                player.addGold(target.goldReward)
                val levelsGained = player.addXp(target.xpReward)
                eventsBuffer += GameEvent.EnemyKilled(target.id, target.goldReward, target.xpReward)
                repeat(levelsGained) {
                    eventsBuffer += GameEvent.LevelUp(player.level)
                }
                val diamondAmount = 1 + target.xpReward / 20
                val drop = DiamondDrop(
                    id = nextDiamondId++,
                    laneIndex = target.laneIndex,
                    progress = target.progress,
                    amount = diamondAmount
                )
                diamondsById[drop.id] = drop
                eventsBuffer += GameEvent.DiamondDropped(drop.id, drop.amount)
            }
        }
    }

    private fun advanceDiamonds(dt: Double) {
        val expired = mutableListOf<Long>()
        for (drop in diamondsById.values) {
            drop.timeRemaining -= dt
            if (drop.timeRemaining <= 0.0) expired += drop.id
        }
        for (id in expired) {
            diamondsById.remove(id)
            eventsBuffer += GameEvent.DiamondExpired(id)
        }
    }

    private fun nearestEnemyInRange(towerPos: Vec2, range: Double, enemies: Collection<Enemy>): Enemy? {
        var best: Enemy? = null
        var bestDistance = Double.MAX_VALUE
        for (enemy in enemies) {
            if (enemy.isDead) continue
            val distance = towerPos.distanceTo(enemy.position())
            if (distance <= range && distance < bestDistance) {
                best = enemy
                bestDistance = distance
            }
        }
        return best
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
