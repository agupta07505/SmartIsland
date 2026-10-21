/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object NotchCollapseFade {
    const val FOREGROUND_GONE_AT = 0.5f
    const val LAYER_RELEASE_AT = 0.03f

    fun appliesTo(notchMode: Boolean, isMusic: Boolean, artworkBackdrop: Boolean): Boolean =
        notchMode && isMusic && artworkBackdrop

    fun darkness(expandedAlpha: Float): Float = (1f - expandedAlpha).coerceIn(0f, 1f)

    fun foregroundAlpha(expandedAlpha: Float): Float {
        val t = ((expandedAlpha - FOREGROUND_GONE_AT) / (1f - FOREGROUND_GONE_AT)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun layerAlpha(expandedAlpha: Float): Float =
        (expandedAlpha / LAYER_RELEASE_AT).coerceIn(0f, 1f)
}

object NotchTopEdge {
    /**
     * 0 at 0, 1 at 1, slope 1 leaving 0 and 0 arriving at 1 (t + t^2 - t^3). A smoothstep starts flat, so the top
     * stayed square for a long stretch while the shell was clearly open, then rounded in a rush. This one starts
     * rounding at once and still settles into full roundness without a kink
     */
    private fun ease(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x + x * x - x * x * x
    }

    const val COLLAPSE_EASE_BELOW = 0.4f

    /**
     * A swap shell is rounded earlier than a plain collapse. It runs between the card and the cutout (smaller than the
     * pill a plain collapse ends on) and a burst of taps keeps shells in the low range, so with the plain collapse's
     * constant the swap edge read much flatter than a normal collapse. Tuning value, raise it to square the top sooner
     */
    const val SWAP_EASE_BELOW = 0.2f

    /** Uses the slower axis to ease the notch top flat. The top edge used to flash on collapse. */
    fun collapseRoundness(progressX: Float, progressY: Float): Float {
        val slow = maxOf(progressX, progressY).coerceIn(0f, 1f)
        return ease(slow / COLLAPSE_EASE_BELOW)
    }

    /**
     * Top corner roundness of a swap shell with the open amounts [openW] and [openH] (1 = full card, 0 = on the
     * cutout): 0 = square (the cutout), 1 = round. Same curve and slower-axis rule as the plain collapse, but it
     * finishes rounding at [SWAP_EASE_BELOW] instead of [COLLAPSE_EASE_BELOW], so the top stays round longer
     */
    fun swapRoundness(openW: Float, openH: Float = openW): Float {
        val slow = maxOf(openW, openH).coerceIn(0f, 1f)
        return ease(slow / SWAP_EASE_BELOW)
    }

    fun swapFlat(openW: Float, openH: Float = openW): Float = 1f - swapRoundness(openW, openH)

    fun roundness(expandProgress: Float, swapOpen: Float?, collapsing: Boolean = false,
            progressY: Float = expandProgress): Float {
        val base = if (collapsing) collapseRoundness(expandProgress, progressY) else expandProgress.coerceIn(0f, 1f)
        return if (swapOpen == null) base else minOf(base, swapRoundness(swapOpen))
    }

    fun flat(expandProgress: Float, swapOpen: Float?, collapsing: Boolean = false,
            progressY: Float = expandProgress): Float {
        val base = if (collapsing) 1f - collapseRoundness(expandProgress,
            progressY) else EdgePillRules.shellFlat(expandProgress)
        return if (swapOpen == null) base else maxOf(base, swapFlat(swapOpen))
    }
}

enum class NotchEarContent { None, SiblingPicker, OtherEvent }

data class NotchCompanionPlan(
    val pillStays: Boolean,
    val pillCardIndex: Int,
    val earContent: NotchEarContent,
    val earCardIndex: Int
) {
    val earShown: Boolean get() = earContent != NotchEarContent.None

    companion object {
        val None = NotchCompanionPlan(false, -1, NotchEarContent.None, -1)
    }
}

fun notchCompanionPlan(cards: List<IslandNotification>, currentIndex: Int, expanded: Boolean): NotchCompanionPlan {
    if (cards.getOrNull(currentIndex) == null) return NotchCompanionPlan.None
    val otherIndex = secondaryCardIndex(cards, currentIndex, expanded = false)
    if (!expanded) {
        return if (otherIndex >= 0) {
            NotchCompanionPlan(false, -1, NotchEarContent.OtherEvent, otherIndex)
        } else NotchCompanionPlan.None
    }
    val siblingIndex = nextSameAppIndex(cards, currentIndex)
    if (siblingIndex >= 0) {
        return NotchCompanionPlan(otherIndex >= 0, otherIndex, NotchEarContent.SiblingPicker, siblingIndex)
    }
    if (otherIndex < 0) return NotchCompanionPlan.None
    // Expanded: the next event takes the pill's spot and no bubble stays beside the open card, whatever the number of
    // events
    return NotchCompanionPlan(true, otherIndex, NotchEarContent.None, -1)
}
