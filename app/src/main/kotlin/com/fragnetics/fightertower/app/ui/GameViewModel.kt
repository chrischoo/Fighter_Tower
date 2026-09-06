package com.fragnetics.fightertower.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fragnetics.fightertower.app.game.GameSession
import com.fragnetics.fightertower.app.game.GameSnapshot
import com.fragnetics.fightertower.app.game.PermanentProgressStore
import com.fragnetics.fightertower.core.LabCatalog
import com.fragnetics.fightertower.core.PermanentProgress
import com.fragnetics.fightertower.core.TowerTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val HUD_POLL_INTERVAL_MS = 100L

enum class Screen { GAME, LABORATORY }

/**
 * Owns the [GameSession] the GL surface renders and mutates, and republishes its state as a
 * [StateFlow] the Compose HUD can collect. The simulation itself only advances on the GL thread
 * (inside GameRenderer.onDrawFrame); this class just polls a snapshot for display.
 *
 * Also owns the persistent Diamond wallet and Laboratory upgrades (see [PermanentProgressStore]):
 * these survive across games, unlike the per-run [GameSnapshot] state.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val progressStore = PermanentProgressStore(application)

    private val _permanentProgress = MutableStateFlow(progressStore.load())
    val permanentProgress: StateFlow<PermanentProgress> = _permanentProgress.asStateFlow()

    val session = GameSession(
        permanentProgress = _permanentProgress.value,
        onDiamondCollected = ::creditDiamonds
    )

    private val _state = MutableStateFlow(session.snapshot())
    val state: StateFlow<GameSnapshot> = _state.asStateFlow()

    private val _screen = MutableStateFlow(Screen.GAME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    val towerCost: Int = TowerTier.BASE_TOWER_COST

    init {
        viewModelScope.launch {
            while (true) {
                _state.value = session.snapshot()
                delay(HUD_POLL_INTERVAL_MS)
            }
        }
    }

    private fun creditDiamonds(amount: Int) {
        updateProgress(_permanentProgress.value.copy(diamonds = _permanentProgress.value.diamonds + amount))
    }

    private fun updateProgress(progress: PermanentProgress) {
        _permanentProgress.value = progress
        progressStore.save(progress)
    }

    fun buyTower() {
        session.buyTower()
    }

    /** Retry: replay immediately with the same Laboratory upgrades, no trip to the Laboratory. */
    fun retry() {
        session.updatePermanentProgress(_permanentProgress.value)
        session.reset()
    }

    fun goToLaboratory() {
        _screen.value = Screen.LABORATORY
    }

    fun purchaseTowerUpgrade() {
        val upgraded = LabCatalog.purchaseTowerUpgrade(_permanentProgress.value) ?: return
        updateProgress(upgraded)
    }

    fun purchaseBulletUpgrade() {
        val upgraded = LabCatalog.purchaseBulletUpgrade(_permanentProgress.value) ?: return
        updateProgress(upgraded)
    }

    /** Leaves the Laboratory and starts a fresh run with whatever upgrades were just bought. */
    fun startNewRun() {
        session.updatePermanentProgress(_permanentProgress.value)
        session.reset()
        _screen.value = Screen.GAME
    }
}
