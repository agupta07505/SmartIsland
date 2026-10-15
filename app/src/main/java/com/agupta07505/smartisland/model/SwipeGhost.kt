/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Which recorded layer may stand in for the outgoing card of a sideways swipe (ui/IslandCardHost.kt).
 *
 * The card is recorded into two alternating layers while a swipe is under way. When the card changes, the layer drawn last
 * frame may stand in as the outgoing card only if it holds the OLD card. If the pager has already drawn the new card for
 * a frame before the switch is detected, that layer holds the NEW card, and using it would show the card twice: once as the
 * "outgoing" card and once as the incoming one. So each layer remembers the key of the card it holds.
 */
object SwipeGhost {
    /**
     * The layer to park for the card [from] that is being swiped away, or -1 when no layer holds it (then there is no ghost: a
     * plain cut is better than showing the wrong card).
     *
     * [last] is the layer drawn last frame (-1 = none), [keys] the key of the card each of the two layers holds.
     */
    fun layerToPark(last: Int, keys: Array<String?>, from: String?): Int {
        if (from == null || last !in 0..1) return -1
        return when {
            keys[last] == from -> last
            // The last frame already showed the new card: the frame before it (the other layer) still holds the old one.
            keys[1 - last] == from -> 1 - last
            else -> -1
        }
    }
}
