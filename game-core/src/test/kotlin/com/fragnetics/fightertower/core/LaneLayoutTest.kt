package com.fragnetics.fightertower.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LaneLayoutTest {

    @Test
    fun `three lanes fan out from a shared base at the origin`() {
        assertEquals(3, LaneLayout.LANE_COUNT)
        assertEquals(Vec2(0.0, 0.0), LaneLayout.basePosition)
    }

    @Test
    fun `spawn points on different lanes are far apart, but the lanes converge near the base`() {
        val spawn0 = LaneLayout.spawnPosition(0)
        val spawn1 = LaneLayout.spawnPosition(1)
        val spawn2 = LaneLayout.spawnPosition(2)

        val spawnSeparation = spawn0.distanceTo(spawn1)

        val nearBase0 = LaneLayout.positionOnLane(0, progress = 0.98)
        val nearBase1 = LaneLayout.positionOnLane(1, progress = 0.98)
        val nearBaseSeparation = nearBase0.distanceTo(nearBase1)

        assertTrue(
            nearBaseSeparation < spawnSeparation,
            "lanes should converge near the base, not stay as far apart as their spawn points"
        )
        assertTrue(spawn2.distanceTo(LaneLayout.basePosition) > 10.0, "spawns should be far from the base")
    }

    @Test
    fun `a spot near the base is within a typical tower's range of an adjacent lane`() {
        // An innermost spot on lane 0, and an enemy close to the base on lane 1 (a different
        // lane): a fully-merged tower here should be able to help defend lane 1 too, which is
        // the whole point of letting towers merge across spots/lanes freely.
        val innerSpotLane0 = LaneLayout.spotPosition(GridPos(lane = 0, slot = 0))
        val enemyNearBaseOnLane1 = LaneLayout.positionOnLane(lane = 1, progress = 0.95)

        val distance = innerSpotLane0.distanceTo(enemyNearBaseOnLane1)

        assertTrue(
            distance <= TowerTier.JUGGERNAUT.range,
            "expected an inner spot to reach a near-base enemy on another lane " +
                "(distance=$distance, range=${TowerTier.JUGGERNAUT.range})"
        )
    }

    @Test
    fun `an outer spot cannot reach an enemy still far away on a different lane`() {
        val outerSpotLane0 = LaneLayout.spotPosition(GridPos(lane = 0, slot = LaneLayout.SLOTS_PER_LANE - 1))
        val enemyAtSpawnOnLane2 = LaneLayout.positionOnLane(lane = 2, progress = 0.0)

        val distance = outerSpotLane0.distanceTo(enemyAtSpawnOnLane2)

        assertTrue(
            distance > TowerTier.TITAN.range,
            "a distant enemy on another lane should be out of range even for the strongest tower"
        )
    }

    @Test
    fun `positionOnLane interpolates from the spawn point down to the base`() {
        val lane = 1
        val atSpawn = LaneLayout.positionOnLane(lane, progress = 0.0)
        val atBase = LaneLayout.positionOnLane(lane, progress = 1.0)

        assertEquals(LaneLayout.spawnPosition(lane), atSpawn)
        assertEquals(LaneLayout.basePosition, atBase)
    }

    @Test
    fun `all spots stay within the lane length, leaving room for enemies to spawn beyond them`() {
        for (lane in 0 until LaneLayout.LANE_COUNT) {
            for (slot in 0 until LaneLayout.SLOTS_PER_LANE) {
                val spot = LaneLayout.spotPosition(GridPos(lane, slot))
                val distanceFromBase = spot.distanceTo(LaneLayout.basePosition)
                assertTrue(
                    distanceFromBase < LaneLayout.LANE_LENGTH,
                    "spot (lane=$lane, slot=$slot) at distance $distanceFromBase should be inside the lane length"
                )
            }
        }
    }
}
