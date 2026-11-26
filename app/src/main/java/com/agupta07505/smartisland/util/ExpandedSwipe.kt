/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.model.nextAppGroupIndex
import com.agupta07505.smartisland.model.nextSiblingIndex

enum class SwipeCardOutcome { Changes, Stays, Unknown }

fun predictCardChange(action: SwipeAction, cards: List<IslandNotification>,
        index: Int): SwipeCardOutcome = when (action) {
    SwipeAction.NextPrevious, SwipeAction.NextNotification ->
        if (nextAppGroupIndex(cards, index, true) >= 0) SwipeCardOutcome.Changes else SwipeCardOutcome.Stays
    SwipeAction.PreviousNotification ->
        if (nextAppGroupIndex(cards, index, false) >= 0) SwipeCardOutcome.Changes else SwipeCardOutcome.Stays
    SwipeAction.NextSibling ->
        if (nextSiblingIndex(cards, index, true) >= 0) SwipeCardOutcome.Changes else SwipeCardOutcome.Stays
    SwipeAction.PreviousSibling ->
        if (nextSiblingIndex(cards, index, false) >= 0) SwipeCardOutcome.Changes else SwipeCardOutcome.Stays
    SwipeAction.NextEvent, SwipeAction.PreviousEvent -> SwipeCardOutcome.Unknown
    else -> SwipeCardOutcome.Stays
}

/**
 * Likely neighbouring page for the height probe: 1 next, -1 previous, 2 a card change with no side (siblings, events)
 */
fun swipeNeighbourHint(action: SwipeAction): Int = when (action) {
    SwipeAction.NextPrevious, SwipeAction.NextNotification -> 1
    SwipeAction.PreviousNotification -> -1
    else -> 2
}

/** Page offsets the probe measures, likely target first (idle order is previous, then next). */
fun neighbourProbeOrder(hint: Int): List<Int> = if (hint == 1) listOf(1, -1) else listOf(-1, 1)

fun swipeIsNoOp(action: SwipeAction, cards: List<IslandNotification>, index: Int): Boolean =
    (action == SwipeAction.NextSibling || action == SwipeAction.PreviousSibling) &&
        predictCardChange(action, cards, index) == SwipeCardOutcome.Stays

/** Follow factor of a card that stays (rubber band), never more than [RUBBER_BAND_MAX_DP]. */
fun expandedCardRubberBand(drag: Float, density: Float): Float =
    (drag * RUBBER_BAND_FOLLOW).coerceIn(-RUBBER_BAND_MAX_DP * density, RUBBER_BAND_MAX_DP * density)

const val RUBBER_BAND_FOLLOW = 0.2f
const val RUBBER_BAND_MAX_DP = 8f
/** Wait of a swipe for a card to arrive, next / previous event may import from the shade first. */
const val CARD_ENTER_WAIT_MS = 300L
const val CARD_ENTER_WAIT_EVENT_MS = 600L
