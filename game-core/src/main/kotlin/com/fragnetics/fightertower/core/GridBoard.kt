package com.fragnetics.fightertower.core

/** A tower placement spot: the [slot]-th spot along lane [lane] (see [LaneLayout]). */
data class GridPos(val lane: Int, val slot: Int)

data class Tower(
    val id: Long,
    val tier: TowerTier,
    var position: GridPos,
    var cooldownRemaining: Double = 0.0
)

/**
 * The player's merge yard: a fixed set of spots spread across [laneCount] lanes, [slotsPerLane]
 * spots each. Any tower can be dragged onto any other spot regardless of lane to relocate or
 * merge it — the lane a spot belongs to only affects its physical position (see [LaneLayout]),
 * not what it can merge with.
 */
class GridBoard(val laneCount: Int, val slotsPerLane: Int) {
    private val cells: Array<Tower?> = arrayOfNulls(laneCount * slotsPerLane)
    private var nextTowerId = 1L

    val towers: List<Tower> get() = cells.filterNotNull()

    private fun index(pos: GridPos) = pos.lane * slotsPerLane + pos.slot
    private fun inBounds(pos: GridPos) = pos.lane in 0 until laneCount && pos.slot in 0 until slotsPerLane

    fun towerAt(pos: GridPos): Tower? {
        if (!inBounds(pos)) return null
        return cells[index(pos)]
    }

    fun findEmptyCell(): GridPos? {
        for (lane in 0 until laneCount) {
            for (slot in 0 until slotsPerLane) {
                val pos = GridPos(lane, slot)
                if (towerAt(pos) == null) return pos
            }
        }
        return null
    }

    fun placeNewTower(tier: TowerTier = TowerTier.entries.first(), at: GridPos? = null): Tower? {
        val target = at ?: findEmptyCell() ?: return null
        if (towerAt(target) != null) return null
        val tower = Tower(id = nextTowerId++, tier = tier, position = target)
        cells[index(target)] = tower
        return tower
    }

    sealed class MoveResult {
        data class Moved(val tower: Tower) : MoveResult()
        data class Merged(val resultTower: Tower) : MoveResult()
        data object Invalid : MoveResult()
    }

    /** Dragging onto an empty spot relocates the tower; dragging onto a same-tier tower merges, regardless of lane. */
    fun moveOrMerge(from: GridPos, to: GridPos): MoveResult {
        if (!inBounds(from) || !inBounds(to) || from == to) return MoveResult.Invalid
        val source = towerAt(from) ?: return MoveResult.Invalid
        val destination = towerAt(to)
        return when {
            destination == null -> {
                cells[index(from)] = null
                val moved = source.copy(position = to)
                cells[index(to)] = moved
                MoveResult.Moved(moved)
            }
            destination.tier == source.tier && !source.tier.isMaxTier -> {
                val nextTier = source.tier.next()!!
                cells[index(from)] = null
                val merged = Tower(id = nextTowerId++, tier = nextTier, position = to)
                cells[index(to)] = merged
                MoveResult.Merged(merged)
            }
            else -> MoveResult.Invalid
        }
    }
}
