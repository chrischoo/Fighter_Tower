package com.fragnetics.fightertower.core

data class GridPos(val col: Int, val row: Int)

data class Tower(
    val id: Long,
    val tier: TowerTier,
    var position: GridPos,
    var cooldownRemaining: Double = 0.0
)

/**
 * The player's merge yard. Every tower on the board, regardless of cell, can fire at any
 * enemy on the lane that is within its tier's range (the grid is inventory/merge space,
 * not a per-lane placement grid).
 */
class GridBoard(val columns: Int, val rows: Int) {
    private val cells: Array<Tower?> = arrayOfNulls(columns * rows)
    private var nextTowerId = 1L

    val towers: List<Tower> get() = cells.filterNotNull()

    private fun index(pos: GridPos) = pos.row * columns + pos.col
    private fun inBounds(pos: GridPos) = pos.col in 0 until columns && pos.row in 0 until rows

    fun towerAt(pos: GridPos): Tower? {
        if (!inBounds(pos)) return null
        return cells[index(pos)]
    }

    fun findEmptyCell(): GridPos? {
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val pos = GridPos(col, row)
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

    /** Dragging onto an empty cell relocates the tower; dragging onto a same-tier tower merges. */
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
