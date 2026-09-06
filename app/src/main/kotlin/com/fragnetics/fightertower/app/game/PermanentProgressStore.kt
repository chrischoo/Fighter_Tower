package com.fragnetics.fightertower.app.game

import android.content.Context
import com.fragnetics.fightertower.core.PermanentProgress

/**
 * Persists [PermanentProgress] (the Diamond wallet and Laboratory upgrade levels) across app
 * launches. Backed by SharedPreferences since it's just a handful of ints.
 */
class PermanentProgressStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): PermanentProgress = PermanentProgress(
        diamonds = prefs.getInt(KEY_DIAMONDS, 0),
        startingTowerLevel = prefs.getInt(KEY_STARTING_TOWER_LEVEL, 1),
        bulletDamageLevel = prefs.getInt(KEY_BULLET_DAMAGE_LEVEL, 0)
    )

    fun save(progress: PermanentProgress) {
        prefs.edit()
            .putInt(KEY_DIAMONDS, progress.diamonds)
            .putInt(KEY_STARTING_TOWER_LEVEL, progress.startingTowerLevel)
            .putInt(KEY_BULLET_DAMAGE_LEVEL, progress.bulletDamageLevel)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "fighter_tower_progress"
        const val KEY_DIAMONDS = "diamonds"
        const val KEY_STARTING_TOWER_LEVEL = "starting_tower_level"
        const val KEY_BULLET_DAMAGE_LEVEL = "bullet_damage_level"
    }
}
