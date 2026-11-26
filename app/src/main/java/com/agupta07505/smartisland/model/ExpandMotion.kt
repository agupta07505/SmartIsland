/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

object ExpandMotion {
    const val PILL_OUT_END_PROGRESS = 0.25f
    const val CARD_IN_START_PROGRESS = 0.1f
    const val CARD_IN_END_PROGRESS = 0.9f

    /** Secondary bubble's glide into the cutout lasts the island's whole expand time, so both finish together. */
    const val GLIDE_END_FRACTION = 1f

    const val EXPAND_DONE_PROGRESS = 0.995f

    const val MIN_GLIDE_MS = 120

    fun expandDoneMs(stiffness: Float, damping: Float): Int =
        springTimeToProgressMs(stiffness, damping, EXPAND_DONE_PROGRESS).toInt()

    /**
     * Length (ms) of the bubble's glide: it lands when the island has run [GLIDE_END_FRACTION] of its expand, so both
     * come to rest together. [BubbleIntoCutout.BASE_MS] when the spring is unknown
     */
    fun glideMs(stiffness: Float, damping: Float): Int =
        if (stiffness <= 0f) BubbleIntoCutout.BASE_MS
        else (expandDoneMs(stiffness, damping) * GLIDE_END_FRACTION).toInt().coerceAtLeast(MIN_GLIDE_MS)

    const val MIN_FADE_MS = 50

    private fun at(progress: Float, stiffness: Float, damping: Float): Int =
        springTimeToProgressMs(stiffness, damping, progress).toInt()

    fun pillOutMs(stiffness: Float, damping: Float): Int =
        at(PILL_OUT_END_PROGRESS, stiffness, damping).coerceAtLeast(MIN_FADE_MS)

    fun cardInDelayMs(stiffness: Float, damping: Float): Int =
        at(CARD_IN_START_PROGRESS, stiffness, damping)

    fun cardInMs(stiffness: Float, damping: Float): Int =
        (at(CARD_IN_END_PROGRESS, stiffness, damping) - cardInDelayMs(stiffness, damping)).coerceAtLeast(MIN_FADE_MS)

    private fun downTo(fromProgress: Float, toProgress: Float, stiffness: Float, damping: Float): Int {
        val from = fromProgress.coerceIn(0f, 1f)
        if (from <= toProgress) return 0
        return at(1f - toProgress / from, stiffness, damping)
    }

    fun cardOutDelayFromMs(fromProgress: Float, stiffness: Float, damping: Float): Int =
        downTo(fromProgress, CARD_IN_END_PROGRESS, stiffness, damping)

    fun cardOutMsFrom(fromProgress: Float, stiffness: Float, damping: Float): Int =
        (downTo(fromProgress, CARD_IN_START_PROGRESS, stiffness, damping) - cardOutDelayFromMs(fromProgress, stiffness,
            damping))
            .coerceAtLeast(MIN_FADE_MS)

    fun pillInDelayFromMs(fromProgress: Float, stiffness: Float, damping: Float): Int =
        downTo(fromProgress, PILL_OUT_END_PROGRESS, stiffness, damping)

    fun pillInMs(stiffness: Float, damping: Float): Int = pillOutMs(stiffness, damping)
}

object ExpandTravel {
    const val MAX_SLOWDOWN = 1.6f

    fun slowdown(edgeDxDp: Float, pillCentreYDp: Float, cardCentreYDp: Float, screenHeightDp: Float): Float {
        val travel = hypot(edgeDxDp, cardCentreYDp - pillCentreYDp)
        val mirrored = hypot(edgeDxDp, cardCentreYDp - (screenHeightDp - pillCentreYDp))
        val nearest = min(travel, mirrored)
        if (nearest <= 0f) return 1f
        return (travel / nearest).coerceIn(1f, MAX_SLOWDOWN)
    }

    fun stiffness(base: Float, slowdown: Float): Float = base / (slowdown * slowdown)
}

object ExpandedAnchor {
    const val EDGE_MARGIN_DP = 8f

    fun centreOffsetDp(
        cameraCentreXDp: Float?,
        cardWidthDp: Float,
        screenWidthDp: Float,
        edgeMarginDp: Float = EDGE_MARGIN_DP
    ): Float {
        if (cameraCentreXDp == null || abs(cameraCentreXDp) <= CAMERA_CENTRED_TOLERANCE_DP) return 0f
        val limit = ((screenWidthDp - cardWidthDp) / 2f - edgeMarginDp).coerceAtLeast(0f)
        return cameraCentreXDp.coerceIn(-limit, limit)
    }
}

object ExpandOrigin {
    const val SETTLED_PROGRESS = 0.999f

    fun settled(progress: Float): Boolean = progress >= SETTLED_PROGRESS

    fun edgeStart(progress: Float, size: Float, pillEdge: Float, cardEdge: Float, onLeft: Boolean): Float {
        val edge = pillEdge + (cardEdge - pillEdge) * progress.coerceAtLeast(0f)
        return if (onLeft) edge else edge - size
    }

    fun anchorInCard(pillCentre: Float, cardStart: Float, cardSize: Float): Float {
        if (cardSize <= 0f) return 0.5f
        return ((pillCentre - cardStart) / cardSize).coerceIn(0f, 1f)
    }

    fun anchorAtEnd(pillCentre: Float, cardStart: Float, cardSize: Float): Float =
        pillCentre.coerceIn(cardStart, cardStart + maxOf(cardSize, 0f))

    fun start(
        progress: Float,
        size: Float,
        pillCentre: Float,
        cardStart: Float,
        cardSize: Float
    ): Float = startAnchored(progress, size, pillCentre, 0.5f, pillCentre, cardStart, cardSize)

    fun startAnchored(
        progress: Float,
        size: Float,
        pillAnchor: Float,
        pillAnchorFraction: Float,
        pillCentre: Float,
        cardStart: Float,
        cardSize: Float
    ): Float {
        val endAnchor = anchorAtEnd(pillCentre, cardStart, cardSize)
        val anchor = lerpFloat(pillAnchor, endAnchor, progress)
        val inIsland = lerpFloat(pillAnchorFraction, anchorInCard(pillCentre, cardStart, cardSize), progress)
        return anchor - inIsland * size
    }
}

object BubbleOrigin {
    const val STALE_MS = 600L

    const val CONTENT_FADE_END = 0.35f

    fun startsFromBubble(expanded: Boolean, onBubble: Boolean, selectsBubbleEvent: Boolean): Boolean =
        !expanded && onBubble && selectsBubbleEvent

    fun active(flag: Boolean, progress: Float): Boolean = flag && !ExpandOrigin.settled(progress)

    fun edge(bubble: Float, expanded: Float, normal: Float, progress: Float, weight: Float = 1f): Float {
        val p = progress.coerceAtLeast(0f)
        val fromBubble = lerpFloat(bubble, expanded, p)
        return lerpFloat(normal, fromBubble, weight.coerceIn(0f, 1f))
    }

    fun radius(
        bubbleCorner: Float,
        expandedRadius: Float,
        normalRadius: Float,
        progress: Float,
        weight: Float = 1f
    ): Float = edge(bubbleCorner, expandedRadius, normalRadius, progress, weight)

    fun opens(flag: Boolean, expanded: Boolean): Boolean = flag && expanded

    fun blends(flag: Boolean, expanded: Boolean, edgeLayout: Boolean, fingerLed: Boolean): Boolean =
        opens(flag, expanded) && !edgeLayout && !fingerLed

    fun contentAlpha(progress: Float): Float = 1f - ramp(progress, 0f, CONTENT_FADE_END)

    fun keeps(
        flag: Boolean,
        expanded: Boolean,
        progress: Float,
        orientationFlipped: Boolean,
        ageMs: Long
    ): Boolean {
        if (!flag || orientationFlipped) return false
        if (ExpandOrigin.settled(progress)) return false
        if (!expanded && progress > 0.001f) return false
        if (!expanded && progress <= 0.001f && ageMs > STALE_MS) return false
        return true
    }
}
