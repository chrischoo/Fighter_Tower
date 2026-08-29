package com.fragnetics.fightertower.core

data class PlayerProgress(
    var level: Int = 1,
    var xp: Int = 0,
    var gold: Int = 50
) {
    val xpToNextLevel: Int get() = xpRequiredForLevel(level)

    fun addGold(amount: Int) {
        gold += amount
    }

    fun spendGold(amount: Int): Boolean {
        if (gold < amount) return false
        gold -= amount
        return true
    }

    /** Applies xp and returns how many levels were gained (0 if none), rolling over past multiple thresholds. */
    fun addXp(amount: Int): Int {
        xp += amount
        var levelsGained = 0
        while (xp >= xpToNextLevel) {
            xp -= xpToNextLevel
            level += 1
            levelsGained += 1
        }
        return levelsGained
    }

    companion object {
        fun xpRequiredForLevel(level: Int): Int = 50 + (level - 1) * 25
    }
}
