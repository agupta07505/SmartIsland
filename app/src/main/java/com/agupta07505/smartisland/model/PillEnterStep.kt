/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs

/**
 * What the pill's incoming-card slide ([IslandInteractionState.pillEnter]) does when the shown card or widget changes
 * or the content layer returns.
 */
enum class PillEnterStep {
    /** A swipe asked for this card: start the slide from the side opposite the swipe. */
    Start,

    /** No swipe is waiting but an earlier slide was cut off: ease the left-over offset back to 0. */
    SettleStale,

    Nothing
}

/** Offsets below this many px are rest (animation tails), not a stuck slide. */
const val PILL_ENTER_REST_PX = 0.5f

fun pillEnterStep(pendingEnterSign: Int, enterOffsetPx: Float): PillEnterStep = when {
    pendingEnterSign != 0 -> PillEnterStep.Start
    abs(enterOffsetPx) > PILL_ENTER_REST_PX -> PillEnterStep.SettleStale
    else -> PillEnterStep.Nothing
}
