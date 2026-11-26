/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object PillOpenDrag {
    const val RANGE_MIN_DP = 100f

    const val RANGE_MAX_DP = 220f

    const val COMMIT = 0.35f

    const val PILL_CONTENT_GONE_AT = 0.25f

    const val CARD_CONTENT_FROM = 0.2f

    const val CARD_CONTENT_FULL = 0.8f

    const val NEXT_EVENT_PEEK_AT = 0.25f

    const val RANGE_SCALE = 1.6f

    const val NEXT_EVENT_RELEASE_AT = 0.15f

    fun rangePx(pillHeightDp: Float, cardHeightDp: Float, density: Float): Float =
        ((cardHeightDp - pillHeightDp) * RANGE_SCALE).coerceIn(RANGE_MIN_DP, RANGE_MAX_DP) * density

    fun progress(dragY: Float, slopPx: Float, rangePx: Float): Float {
        if (rangePx <= 0f) return 0f
        return ((dragY - slopPx) / rangePx).coerceIn(0f, 1f)
    }

    fun closeProgress(dragUpPx: Float, slopPx: Float, rangePx: Float): Float = 1f - progress(dragUpPx, slopPx, rangePx)

    /** Lifting here collapses: past the commit point, or past the plain swipe's distance. */
    fun closeCommits(openProgress: Float, dragUpPx: Float, thresholdPx: Float): Boolean =
        (1f - openProgress) >= COMMIT || dragUpPx >= thresholdPx

    fun commits(progress: Float): Boolean = progress >= COMMIT

    fun peeksNextEvent(progress: Float): Boolean = progress >= NEXT_EVENT_PEEK_AT

    /**
     * The other event's pill sits on the cutout. During a drag it waits for [peeked] and then stays until the island is back inside [NEXT_EVENT_RELEASE_AT] or the lift. A
     * lift that [closing] the island drops it once the island is back inside [NEXT_EVENT_PEEK_AT]
     */
    fun nextEventOnCutout(
        dragActive: Boolean,
        peeked: Boolean,
        closing: Boolean,
        progress: Float,
        expandedPlan: Boolean
    ): Boolean = when {
        !dragActive -> expandedPlan
        closing -> peeked && progress > NEXT_EVENT_PEEK_AT
        else -> expandedPlan && peeked && progress > NEXT_EVENT_RELEASE_AT
    }

    fun lerp(from: Float, to: Float, progress: Float): Float = lerpFloat(from, to, progress)

    fun pillContentAlpha(progress: Float): Float = (1f - progress / PILL_CONTENT_GONE_AT).coerceIn(0f, 1f)

    fun cardContentAlpha(progress: Float): Float =
        ((progress - CARD_CONTENT_FROM) / (CARD_CONTENT_FULL - CARD_CONTENT_FROM)).coerceIn(0f, 1f)
}
