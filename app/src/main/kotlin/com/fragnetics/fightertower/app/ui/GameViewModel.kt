package com.fragnetics.fightertower.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fragnetics.fightertower.app.game.GameSession
import com.fragnetics.fightertower.app.game.GameSnapshot
import com.fragnetics.fightertower.core.TowerTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val HUD_POLL_INTERVAL_MS = 100L

/**
 * Owns the [GameSession] the GL surface renders and mutates, and republishes its state as a
 * [StateFlow] the Compose HUD can collect. The simulation itself only advances on the GL thread
 * (inside GameRenderer.onDrawFrame); this class just polls a snapshot for display.
 */
class GameViewModel : ViewModel() {
    val session = GameSession()

    private val _state = MutableStateFlow(session.snapshot())
    val state: StateFlow<GameSnapshot> = _state.asStateFlow()

    val towerCost: Int = TowerTier.BASE_TOWER_COST

    init {
        viewModelScope.launch {
            while (true) {
                _state.value = session.snapshot()
                delay(HUD_POLL_INTERVAL_MS)
            }
        }
    }

    fun buyTower() {
        session.buyTower()
    }

    fun restart() {
        session.reset()
    }
}
