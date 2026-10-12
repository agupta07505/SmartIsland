/*
 * Realistic Dynamic Island Fork
 * Package: com.srcpolygot.dynamicisland
 */

package com.srcpolygot.dynamicisland.ui.overlay

object IslandGestureHandlers {

    /** Minimum drag distance (px) to trigger dismiss or floating-window. */
    const val MIN_SWIPE_DISTANCE_PX = 48f

    /** Long-press duration before Clear All becomes available. */
    const val LONG_PRESS_DURATION_MS = 300L

    /** Scale applied during quick notification switch. */
    const val SWITCH_SCALE_DOWN = 0.92f

    const val SWITCH_BOUNCE_DAMPING = 0.65f
    const val SWITCH_BOUNCE_STIFFNESS = 650f
}
