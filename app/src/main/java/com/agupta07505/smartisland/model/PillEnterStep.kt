/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs

/**
 * What the collapsed pill's "incoming card slide" ([com.agupta07505.smartisland.ui.IslandInteractionState.pillEnter]) has to do when the
 * shown card (or idle widget) changes, or when the pill's content layer comes back on screen.
 *
 * The slide offset lives in the shared interaction state, not in the content layer. A slide that is cancelled half way (a second card
 * arrives while it runs, or the island expands and the layer leaves the composition) leaves the offset where it was, so the
 * whole pill content (app icon, dots, widgets) would stay shifted to one side, partly outside the pill. Every run that is NOT a
 * fresh swipe slide therefore has to bring a left-over offset back to 0.
 */
enum class PillEnterStep {
    /** A swipe asked for this card: start the slide from the side opposite to the swipe. */
    Start,

    /** No swipe is waiting, but an earlier slide was cut off: ease the left-over offset back to 0. */
    SettleStale,

    /** Nothing waiting and the offset is already 0. */
    Nothing
}

/** Offsets below this many px are rest (animation tails), not a stuck slide. */
const val PILL_ENTER_REST_PX = 0.5f

fun pillEnterStep(pendingEnterSign: Int, enterOffsetPx: Float): PillEnterStep = when {
    pendingEnterSign != 0 -> PillEnterStep.Start
    abs(enterOffsetPx) > PILL_ENTER_REST_PX -> PillEnterStep.SettleStale
    else -> PillEnterStep.Nothing
}
