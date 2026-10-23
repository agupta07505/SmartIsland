/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

object TimerFreeze {
    private const val SHADE_TOLERANCE_MS = 2_000L

    fun remainingMs(
        nowMs: Long,
        rememberedFrozenMs: Long?,
        runningTargetMs: Long?,
        keptFrozenMs: Long?,
        textRemainingMs: Long?,
        shadeShownMs: Long? = null,
        shadeExactMs: Long? = null,
        shownMs: Long? = null,
        previousShownMs: Long? = null
    ): Long = when {
        shadeExactMs != null && shadeExactMs > 0L -> shadeExactMs
        // Covers a resume and pause the island never saw.
        shadeShownMs != null && shadeShownMs > 0L &&
            (rememberedFrozenMs == null || kotlin.math.abs(shadeShownMs - rememberedFrozenMs) > SHADE_TOLERANCE_MS) ->
            shadeShownMs
        // Time added in the app while paused.
        rememberedFrozenMs != null && shownMs != null && previousShownMs != null &&
            shownMs - previousShownMs > SHADE_TOLERANCE_MS ->
            rememberedFrozenMs + (shownMs - previousShownMs)
        rememberedFrozenMs != null -> rememberedFrozenMs
        runningTargetMs != null && runningTargetMs > nowMs -> runningTargetMs - nowMs
        keptFrozenMs != null -> keptFrozenMs
        textRemainingMs != null && textRemainingMs > 0L -> textRemainingMs
        else -> 0L
    }
}
