/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.CollapsedPulse
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.QuickCardHold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Runs [QuickCardHold] for the view model: [merge] puts the held copies back into the posted cards, and [changes]
 * ticks when a bubble wait is over. Called on the main thread, [nowMs] is the clock the model's times come from
 */
internal class QuickCardHolder(
    private val scope: CoroutineScope,
    private val nowMs: () -> Long
) {
    private var state = QuickCardHold.State()
    private var timer: Job? = null
    private var scheduledFor: Long? = null
    private val _changes = MutableStateFlow(0)
    val changes: StateFlow<Int> = _changes

    /** Ids a tap on the pulsing pill opened the island on, held until [releaseKept]. */
    private var kept: Set<String> = emptySet()

    fun keepWhileOpen(id: String) {
        kept = kept + id
    }

    /** The island closed: the kept copies may go, the card's own time decides from here. */
    fun releaseKept() {
        if (kept.isEmpty()) return
        kept = emptySet()
        _changes.value++
    }

    /** The cards the monitors posted plus the quick ones the pulse still needs, [pulse] as it was before this change. */
    fun merge(raw: List<IslandNotification>, pulse: CollapsedPulse.State): List<IslandNotification> {
        state = QuickCardHold.update(state, raw, pulse, nowMs(), kept)
        schedule(QuickCardHold.nextDeadlineMs(state))
        val held = QuickCardHold.cards(state)
        return if (held.isEmpty()) raw else raw + held
    }

    private fun schedule(due: Long?) {
        if (due == scheduledFor) return
        timer?.cancel()
        scheduledFor = due
        timer = due?.let {
            scope.launch {
                delay((it - nowMs()).coerceAtLeast(0L))
                scheduledFor = null
                _changes.value++
            }
        }
    }
}
