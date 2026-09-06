package com.fragnetics.fightertower.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GridBoardTest {

    @Test
    fun `placing a tower occupies an empty cell`() {
        val board = GridBoard(laneCount = 2, slotsPerLane = 2)
        val tower = board.placeNewTower()
        assertTrue(tower != null)
        assertEquals(1, board.towers.size)
        assertEquals(tower, board.towerAt(tower!!.position))
    }

    @Test
    fun `placing fails when board is full`() {
        val board = GridBoard(laneCount = 1, slotsPerLane = 1)
        assertTrue(board.placeNewTower() != null)
        assertNull(board.placeNewTower())
    }

    @Test
    fun `dragging onto an empty cell relocates the tower`() {
        val board = GridBoard(laneCount = 2, slotsPerLane = 1)
        val tower = board.placeNewTower(at = GridPos(0, 0))!!
        val result = board.moveOrMerge(GridPos(0, 0), GridPos(1, 0))
        assertIs<GridBoard.MoveResult.Moved>(result)
        assertNull(board.towerAt(GridPos(0, 0)))
        assertEquals(tower.tier, board.towerAt(GridPos(1, 0))?.tier)
    }

    @Test
    fun `dragging same-tier towers together merges into the next tier`() {
        val board = GridBoard(laneCount = 2, slotsPerLane = 1)
        board.placeNewTower(tier = TowerTier.SCOUT, at = GridPos(0, 0))
        board.placeNewTower(tier = TowerTier.SCOUT, at = GridPos(1, 0))

        val result = board.moveOrMerge(GridPos(0, 0), GridPos(1, 0))

        assertIs<GridBoard.MoveResult.Merged>(result)
        assertEquals(TowerTier.GUNNER, result.resultTower.tier)
        assertNull(board.towerAt(GridPos(0, 0)))
        assertEquals(TowerTier.GUNNER, board.towerAt(GridPos(1, 0))?.tier)
        assertEquals(1, board.towers.size)
    }

    @Test
    fun `dragging different-tier towers together is invalid and leaves both in place`() {
        val board = GridBoard(laneCount = 2, slotsPerLane = 1)
        board.placeNewTower(tier = TowerTier.SCOUT, at = GridPos(0, 0))
        board.placeNewTower(tier = TowerTier.GUNNER, at = GridPos(1, 0))

        val result = board.moveOrMerge(GridPos(0, 0), GridPos(1, 0))

        assertIs<GridBoard.MoveResult.Invalid>(result)
        assertEquals(TowerTier.SCOUT, board.towerAt(GridPos(0, 0))?.tier)
        assertEquals(TowerTier.GUNNER, board.towerAt(GridPos(1, 0))?.tier)
    }

    @Test
    fun `max tier towers cannot be merged further`() {
        val board = GridBoard(laneCount = 2, slotsPerLane = 1)
        board.placeNewTower(tier = TowerTier.TITAN, at = GridPos(0, 0))
        board.placeNewTower(tier = TowerTier.TITAN, at = GridPos(1, 0))

        val result = board.moveOrMerge(GridPos(0, 0), GridPos(1, 0))

        assertIs<GridBoard.MoveResult.Invalid>(result)
    }

    @Test
    fun `moving out of bounds or onto self is invalid`() {
        val board = GridBoard(laneCount = 2, slotsPerLane = 2)
        val tower = board.placeNewTower(at = GridPos(0, 0))!!
        assertIs<GridBoard.MoveResult.Invalid>(board.moveOrMerge(GridPos(0, 0), GridPos(0, 0)))
        assertIs<GridBoard.MoveResult.Invalid>(board.moveOrMerge(GridPos(0, 0), GridPos(5, 5)))
        assertEquals(tower, board.towerAt(GridPos(0, 0)))
    }
}
