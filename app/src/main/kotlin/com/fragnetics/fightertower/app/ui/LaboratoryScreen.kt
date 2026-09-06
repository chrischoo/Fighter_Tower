package com.fragnetics.fightertower.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fragnetics.fightertower.core.LabCatalog
import com.fragnetics.fightertower.core.PermanentProgress

/**
 * The Laboratory: spend the persistent Diamond wallet on permanent upgrades that apply to every
 * future run (a higher starting tower tier, and a bullet damage bonus for every tower). Reached
 * via the Home button on the game-over screen; "Start Game" begins a new run with whatever
 * upgrades are currently bought.
 */
@Composable
fun LaboratoryScreen(
    progress: PermanentProgress,
    onBuyTowerUpgrade: () -> Unit,
    onBuyBulletUpgrade: () -> Unit,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF12141C))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Laboratory", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text("♦ ${progress.diamonds}", color = Color(0xFF4DD0E1), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))

        UpgradeCard(
            title = "Starting Tower Level",
            description = "New towers you buy start at this tier instead of Tier 1.",
            level = progress.startingTowerLevel,
            cost = LabCatalog.towerUpgradeCost(progress.startingTowerLevel),
            diamonds = progress.diamonds,
            onBuy = onBuyTowerUpgrade
        )
        Spacer(modifier = Modifier.height(16.dp))
        UpgradeCard(
            title = "Bullet Damage Level",
            description = "Every tower deals +${(LabCatalog.BULLET_DAMAGE_BONUS_PER_LEVEL * 100).toInt()}% damage per level.",
            level = progress.bulletDamageLevel,
            cost = LabCatalog.bulletUpgradeCost(progress.bulletDamageLevel),
            diamonds = progress.diamonds,
            onBuy = onBuyBulletUpgrade
        )

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onStartGame, modifier = Modifier.fillMaxWidth()) {
            Text("Start Game")
        }
    }
}

@Composable
private fun UpgradeCard(
    title: String,
    description: String,
    level: Int,
    cost: Int?,
    diamonds: Int,
    onBuy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text("Level $level", color = Color(0xFFB0BEC5), style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(description, color = Color(0xFFB0BEC5), style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onBuy, enabled = cost != null && diamonds >= cost) {
            Text(if (cost != null) "Upgrade (♦ $cost)" else "Maxed Out")
        }
    }
}
