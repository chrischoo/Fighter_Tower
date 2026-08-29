package com.fragnetics.fightertower.core

import kotlin.test.Test
import kotlin.test.assertTrue

class WaveGeneratorTest {

    @Test
    fun `later waves are tougher than earlier waves at the same player level`() {
        val wave1 = WaveGenerator.generate(waveNumber = 1, playerLevel = 1)
        val wave5 = WaveGenerator.generate(waveNumber = 5, playerLevel = 1)

        val hp1 = wave1.spawns.first().blueprint.health
        val hp5 = wave5.spawns.first().blueprint.health
        assertTrue(hp5 > hp1, "wave 5 enemies ($hp5 hp) should be tougher than wave 1 ($hp1 hp)")
        assertTrue(wave5.spawns.size >= wave1.spawns.size)
    }

    @Test
    fun `leveling up makes enemies stronger at the same wave number`() {
        val lowLevel = WaveGenerator.generate(waveNumber = 3, playerLevel = 1)
        val highLevel = WaveGenerator.generate(waveNumber = 3, playerLevel = 5)

        val hpLow = lowLevel.spawns.first().blueprint.health
        val hpHigh = highLevel.spawns.first().blueprint.health
        val dmgLow = lowLevel.spawns.first().blueprint.damageToBase
        val dmgHigh = highLevel.spawns.first().blueprint.damageToBase

        assertTrue(hpHigh > hpLow, "higher player level should raise enemy health at the same wave")
        assertTrue(dmgHigh > dmgLow, "higher player level should raise enemy damage at the same wave")
    }

    @Test
    fun `boss waves include a stronger elite spawn`() {
        val wave5 = WaveGenerator.generate(waveNumber = 5, playerLevel = 1)
        val wave4 = WaveGenerator.generate(waveNumber = 4, playerLevel = 1)

        assertTrue(wave5.spawns.any { it.blueprint.name.contains("Elite") })
        assertTrue(wave4.spawns.none { it.blueprint.name.contains("Elite") })
    }
}
