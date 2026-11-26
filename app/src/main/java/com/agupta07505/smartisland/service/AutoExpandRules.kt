/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import com.agupta07505.smartisland.model.IslandMode

/**
 * One card opens the island once. The same card again within [debounceMs] does not open it twice, and a different
 * card in the same instant ([burstMs]) waits as a pulse instead of fighting the first for the island. A different
 * card a moment later is a new event and opens it
 */
internal class AutoExpandSlot(
    private val clock: () -> Long,
    private val debounceMs: Long,
    private val burstMs: Long
) {
    @Volatile private var lastTimeMs = 0L
    @Volatile private var lastKey: String? = null

    fun claim(key: String): Boolean {
        val now = clock()
        val since = now - lastTimeMs
        val window = if (key == lastKey) debounceMs else burstMs
        if (since < window) return false
        lastTimeMs = now
        lastKey = key
        return true
    }

    fun reset() {
        lastTimeMs = 0L
        lastKey = null
    }
}

internal object AutoExpandDecision {
    /**
     * Whether a posted card opens the island. [mayAutoExpand] and [claimSlot] are read only when the earlier
     * checks pass, in this order, since claiming the slot is what uses it up
     */
    fun shouldAutoExpand(
        isMiuiRom: Boolean,
        isInitialSync: Boolean,
        mode: IslandMode,
        repostOfShownPost: Boolean,
        timerJustFinished: Boolean,
        shouldIslandOnly: Boolean,
        autoExpandOnNotification: Boolean,
        mayAutoExpand: () -> Boolean,
        claimSlot: () -> Boolean
    ): Boolean =
        if (isMiuiRom) {
            when {
                isInitialSync -> false
                mode == IslandMode.Timer || mode == IslandMode.Stopwatch -> true
                shouldIslandOnly && autoExpandOnNotification -> claimSlot()
                else -> false
            }
        } else {
            // a timer that has just run out is a re-announcement, not a repost of the same post
            !isInitialSync && (!repostOfShownPost || timerJustFinished) && shouldIslandOnly &&
                mayAutoExpand() && claimSlot()
        }
}
