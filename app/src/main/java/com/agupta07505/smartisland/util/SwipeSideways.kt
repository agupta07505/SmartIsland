/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlin.math.abs
import kotlin.math.sign

/**
 * The sideways card swipe on the expanded island. While the finger drags, the WHOLE island leans sideways a short, eased
 * distance and stretches a little in that direction (like [SwipeDownPull] does for a swipe down); the card stays inside the shell.
 * On release the lean springs back while the old card leaves and the new one arrives (see ExpandedCardHost).
 *
 * Pure maths only: the gesture handler feeds [leanPx], the shell's `graphicsLayer` turns the lean into a translation and a scale.
 */
object SwipeSideways {
    /** Most the island leans (dp) for a swipe that brings another card. The shell's own gesture area moves with it. */
    const val MAX_LEAN_DP = 60f

    /** x of the finger's travel the lean starts with (it flattens towards [MAX_LEAN_DP]). */
    const val FOLLOW = 0.5f

    /** A swipe whose card stays (no neighbour, an action that keeps the card) leans this share of [MAX_LEAN_DP]. */
    const val STAY_SHARE = 0.4f

    /** Wider by 2 % at full lean, in the direction of the swipe. */
    const val STRETCH_X = 0.02f

    /** Lower by 0.8 % at full lean (the island is a little squeezed while it is stretched). */
    const val SQUEEZE_Y = 0.008f

    /** The lean (px, signed like [dragX]) for a finger travel of [dragX] px. [share] is 1 for a card change, [STAY_SHARE] otherwise. */
    fun leanPx(dragX: Float, density: Float, share: Float = 1f): Float =
        softFollow(dragX, FOLLOW, maxLeanPx(density) * share)

    fun maxLeanPx(density: Float): Float = MAX_LEAN_DP * density

    fun progress(leanPx: Float, maxLeanPx: Float): Float =
        if (maxLeanPx <= 0f) 0f else (abs(leanPx) / maxLeanPx).coerceIn(0f, 1f)

    fun scaleX(progress: Float): Float = 1f + STRETCH_X * progress
    fun scaleY(progress: Float): Float = 1f - SQUEEZE_Y * progress

    /** The shell scales around its centre, so a wider shell would grow on both sides. */
    fun edgeAnchorShiftPx(scaleX: Float, widthPx: Float, lean: Float): Float = sign(lean) * (scaleX - 1f) * widthPx / 2f

    /**
     * The island's total lean (px) while a swipe's swap runs: what the finger still holds ([dragLean]) plus what the
     * release has left of the lean the finger let go with ([releaseLean] x the part of the swap still to run, `1 -
     * swapProgress`).
     */
    fun totalLeanPx(dragLean: Float, releaseLean: Float, swapProgress: Float): Float =
        dragLean + releaseLean * (1f - swapProgress.coerceIn(0f, 1f))

    /**
     * Where the two cards stand during a swap (px, signed): the new card comes from [sign] x [widthPx] to 0 and the
     * old one leaves from 0 to -[sign] x [widthPx], one card apart the whole time, so no gap opens between them.
     */
    fun incomingX(sign: Int, widthPx: Float, swapProgress: Float): Float = sign * widthPx * (1f - swapProgress.coerceIn(0f, 1f))
    fun outgoingX(sign: Int, widthPx: Float, swapProgress: Float): Float = -sign * widthPx * swapProgress.coerceIn(0f, 1f)
}
