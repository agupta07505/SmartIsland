/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object PulseBubble {
    const val SWELL_SCALE = 1.22f

    const val SWELL_MS = 220

    /**
     * Index the bubble's card is picked from. While [pulsingId] pulses it takes the cutout, so the bubble holds the
     * best card besides it (the one it displaced); without a pulse, or when its card is gone, it is [currentIndex]
     */
    fun basisIndex(cards: List<IslandNotification>, pulsingId: String?, currentIndex: Int): Int {
        if (pulsingId == null) return currentIndex
        val index = cards.indexOfFirst { CollapsedPulse.entryOf(it).id == pulsingId }
        return if (index >= 0) index else currentIndex
    }

    /** The bubble merges into the cutout only once nothing pulses or waits, the idle timer starts from then. */
    fun mayMerge(expanded: Boolean, hidden: Boolean, bubblePresent: Boolean, pulseBusy: Boolean): Boolean =
        !expanded && !hidden && bubblePresent && !pulseBusy

    fun scale(swell: Float): Float = 1f + (SWELL_SCALE - 1f) * swell

    fun nearestIsGlyph(bubbleOnLeft: Boolean, cameraSide: CameraSide): Boolean =
        bubbleOnLeft == (cameraSide != CameraSide.Right)

    fun nearestIsGlyphOnStrip(glyphTopDp: Float, valueTopDp: Float, valueLenDp: Float, bubbleBelow: Boolean): Boolean =
        valueLenDp <= 0f || (glyphTopDp > valueTopDp) == bubbleBelow

    fun spanDp(circleDp: Float, gapDp: Float): Float = circleDp + gapDp

    /**
     * Sideways shift (dp, left negative) of a bubble while the wide pulse has grown the pill by [extraDp]. A centre
     * pill grows both ways, so the bubble side gains half of it; a side camera's pill grows only away from the
     * camera, and the bubble sits on that side ([SideCamera.circleLeft]), so it follows the whole growth; a bubble on
     * the fixed side (the notch's ear is not placed away from the hole) does not move. A split pulse may also move the
     * centre pill's centre by [centreShiftDp] (right positive), which carries both edges
     */
    fun shiftDp(extraDp: Float, cameraSide: CameraSide, bubbleOnLeft: Boolean, centreShiftDp: Float = 0f): Float {
        if (onFixedSide(cameraSide, bubbleOnLeft)) return 0f
        val share = if (cameraSide == CameraSide.Centre) 0.5f else 1f
        val centre = if (cameraSide == CameraSide.Centre) centreShiftDp else 0f
        return (if (bubbleOnLeft) -1f else 1f) * extraDp.coerceAtLeast(0f) * share + centre
    }

    /** Whether the bubble is on the edge a side camera's pill keeps fixed, so the pill grows away from it. */
    fun onFixedSide(cameraSide: CameraSide, bubbleOnLeft: Boolean): Boolean =
        (cameraSide == CameraSide.Left && bubbleOnLeft) || (cameraSide == CameraSide.Right && !bubbleOnLeft)

    fun swells(
        pulsingId: String?,
        previousPulsingId: String?,
        heldKey: String?,
        previousHeldKey: String?,
        morphedFromId: String? = null
    ): Boolean {
        val tookOver = morphedFromId != null && previousPulsingId == morphedFromId
        val newPulse = pulsingId != previousPulsingId && !tookOver
        return pulsingId != null && (newPulse || heldKey != previousHeldKey) && heldKey != null
    }
}
