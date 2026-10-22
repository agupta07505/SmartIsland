/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

data class IdleStrips(val leftDp: Float, val bandDp: Float, val rightDp: Float)

const val IDLE_STRIP_MIN_WEIGHT = 0.001f

fun idleStripsFor(pillWidthDp: Float, band: CameraBand): IdleStrips {
    if (pillWidthDp <= 0f) return IdleStrips(0f, 0f, 0f)
    val width = band.widthDp.coerceIn(0f, pillWidthDp)
    val left = (pillWidthDp / 2f + band.shiftDp - width / 2f).coerceIn(0f, pillWidthDp - width)
    return IdleStrips(left, width, pillWidthDp - left - width)
}

fun IdleStrips.weights(): Triple<Float, Float, Float> = Triple(
    leftDp.coerceAtLeast(IDLE_STRIP_MIN_WEIGHT),
    bandDp.coerceAtLeast(IDLE_STRIP_MIN_WEIGHT),
    rightDp.coerceAtLeast(IDLE_STRIP_MIN_WEIGHT)
)

/**
 * Hides idle widgets by the selected settings and current state: after dismissal, while the pill is hidden, or while
 * notifications remain
 */
object IdleWidgetOff {
    fun suppressed(
        closedByTap: Boolean,
        pillHidden: Boolean,
        offAfterTapClose: Boolean,
        offAfterLongPress: Boolean,
        hasStoredNotifications: Boolean = false,
        offWhenStored: Boolean = false
    ): Boolean = (closedByTap && offAfterTapClose) || (pillHidden && offAfterLongPress)
        || (hasStoredNotifications && offWhenStored)

    fun normalizeCycleSeconds(seconds: Int): Int =
        seconds.coerceIn(IdleWidgets.MIN_CYCLE_SECONDS, IdleWidgets.MAX_CYCLE_SECONDS)

    fun allIds(): String = IdleWidgets.serialize(IdleWidgetKind.values().toList())
}
