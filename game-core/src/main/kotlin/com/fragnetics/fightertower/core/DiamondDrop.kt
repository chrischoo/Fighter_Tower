package com.fragnetics.fightertower.core

/**
 * A pile of Diamonds left behind by a defeated enemy, sitting at the lane position where the
 * enemy died. Unlike Gold, Diamonds are not credited automatically on kill: the player must
 * collect the drop (see [GameEngine.collectDiamond]) before [timeRemaining] runs out and the
 * drop vanishes uncollected.
 */
data class DiamondDrop(
    val id: Long,
    val progress: Double,
    val amount: Int,
    var timeRemaining: Double = LIFETIME_SECONDS
) {
    companion object {
        const val LIFETIME_SECONDS = 10.0
    }
}
