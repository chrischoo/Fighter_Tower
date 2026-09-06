package com.fragnetics.fightertower.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.fragnetics.fightertower.app.gl.GameGLSurfaceView
import com.fragnetics.fightertower.app.ui.GameHud
import com.fragnetics.fightertower.app.ui.GameViewModel
import com.fragnetics.fightertower.app.ui.LaboratoryScreen
import com.fragnetics.fightertower.app.ui.Screen

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsState()
            val screen by viewModel.screen.collectAsState()
            val permanentProgress by viewModel.permanentProgress.collectAsState()

            when (screen) {
                Screen.GAME -> Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { context -> GameGLSurfaceView(context, viewModel.session) },
                        modifier = Modifier.fillMaxSize()
                    )
                    GameHud(
                        state = state,
                        diamonds = permanentProgress.diamonds,
                        towerCost = viewModel.towerCost,
                        onBuyTower = viewModel::buyTower,
                        onRetry = viewModel::retry,
                        onHome = viewModel::goToLaboratory
                    )
                }

                Screen.LABORATORY -> LaboratoryScreen(
                    progress = permanentProgress,
                    onBuyTowerUpgrade = viewModel::purchaseTowerUpgrade,
                    onBuyBulletUpgrade = viewModel::purchaseBulletUpgrade,
                    onStartGame = viewModel::startNewRun
                )
            }
        }
    }
}
