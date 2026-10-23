/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TICK_PILL_SETTLE_MS = 550L

private const val TICK_PILL_WAIT_MS = 3000L

private const val TICK_PILL_KEEP_MS = 400L

/**
 * Runs [body] (one tick: hourly, day moment or weather change) on the wide pill.
 *
 * Portrait and every case where the pill is already wide ([isWide]): [body] just runs. Landscape with the pill gone (a long press hid it, the
 * widgets are held off, no card): the pill is asked to come in ([IslandInteractionState.onTickHold]), [body] runs on it, and the pill goes again
 * when the last running tick is done. The idle widgets stay off the pill meanwhile (the overlay passes the hold as "suppressed"), so only the tick shows.
 * The hold is counted, so the hour tick and a day moment that follows it share one stay of the pill.
 */
internal suspend fun IslandInteractionState.withTickPill(canHold: Boolean, isWide: () -> Boolean, body: suspend () -> Unit) {
    if (isWide() || !canHold) {
        body()
        return
    }
    tickHolds += 1
    if (tickHolds == 1) onTickHold(true)
    try {
        var waited = 0L
        while (!isWide() && waited < TICK_PILL_WAIT_MS) {
            delay(50)
            waited += 50
        }
        if (!isWide()) return
        // The pill is wide once its settings have arrived; its slide in takes a little longer.
        delay(TICK_PILL_SETTLE_MS)
        body()
    } finally {
        withContext(NonCancellable) {
            delay(TICK_PILL_KEEP_MS)
            tickHolds = (tickHolds - 1).coerceAtLeast(0)
            if (tickHolds == 0) onTickHold(false)
        }
    }
}
