package com.fragnetics.fightertower.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fragnetics.fightertower.app.game.GameSnapshot

@Composable
fun GameHud(
    state: GameSnapshot,
    diamonds: Int,
    towerCost: Int,
    onBuyTower: () -> Unit,
    onRetry: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Lv ${state.level}", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("Wave ${state.wave}", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("Gold ${state.gold}", color = Color(0xFFFFD54F), style = MaterialTheme.typography.titleMedium)
                Text("♦ $diamonds", color = Color(0xFF4DD0E1), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (state.xp.toFloat() / state.xpToNext.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF64B5F6),
                trackColor = Color(0x33FFFFFF)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Base Health", color = Color.White, style = MaterialTheme.typography.labelMedium)
            LinearProgressIndicator(
                progress = { (state.baseHealth / state.baseMaxHealth).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFEF5350),
                trackColor = Color(0x33FFFFFF)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onBuyTower, enabled = !state.gameOver && state.gold >= towerCost) {
                Text("Buy Tower ($towerCost gold)")
            }
        }

        if (state.gameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Base Destroyed", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Reached wave ${state.wave} at level ${state.level}", color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onHome) {
                        Text("Home")
                    }
                }
            }
        }
    }
}
