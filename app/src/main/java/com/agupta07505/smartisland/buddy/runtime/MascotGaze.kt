/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.runtime

/** Where the buddy's eyes go for a touch, and how long they stay. */
object MascotGaze {
    /** The eyes move to the touch in this long, stay [HOLD_MS], and come back in [BACK_MS]. */
    const val MOVE_MS = 160
    const val HOLD_MS = 900
    const val BACK_MS = 340

    /**
     * Gaze for a pointer at ([px], [py]) with the buddy's centre at ([cx], [cy]), all in the same unit:
     * toward it, more the farther it is, within its face. Returns shares of [size] (x, y), y down.
     */
    fun pointerGaze(cx: Float, cy: Float, px: Float, py: Float, size: Float): Pair<Float, Float> {
        if (size <= 0f) return 0f to 0f
        val dx = (px - cx) / (size * 3f)
        val dy = (py - cy) / (size * 2f)
        return dx.coerceIn(-1f, 1f) * 0.08f to dy.coerceIn(-1f, 1f) * 0.05f
    }
}

/** A touch the buddy should look at, in dp from the pill's top-left corner. [id] is unique per touch. */
data class MascotAttention(val id: Long, val xDp: Float, val yDp: Float)
