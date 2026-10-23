/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** Which recorded layer may stand in for the outgoing card of a sideways swipe (ui/IslandCardHost.kt). */
object SwipeGhost {
    /**
     * Layer to park for the card [from] being swiped away, or -1 when no layer holds it (a plain cut beats showing
     * the wrong card).
     */
    fun layerToPark(last: Int, keys: Array<String?>, from: String?): Int {
        if (from == null || last !in 0..1) return -1
        return when {
            keys[last] == from -> last
            // The last frame already showed the new card: the other layer still holds the old one.
            keys[1 - last] == from -> 1 - last
            else -> -1
        }
    }
}
