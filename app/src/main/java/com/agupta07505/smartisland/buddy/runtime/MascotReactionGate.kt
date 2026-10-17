/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.runtime

import com.agupta07505.smartisland.buddy.model.MascotReaction

/**
 * Keeps reactions short and rare. None comes within [SPACING_SEC] of the last, the same one does not
 * come back within [REPEAT_SEC], and one the buddy cannot show waits for it at most [PATIENCE_SEC].
 * Time is passed in so tests are deterministic.
 */
class MascotReactionGate {
    private var lastReaction: MascotReaction? = null
    private var lastTimeSec = 0.0
    private var hasLast = false

    /** Whether [reaction] may play at [nowSec], remembering it when it may. */
    fun admits(reaction: MascotReaction, nowSec: Double): Boolean {
        if (hasLast) {
            val since = nowSec - lastTimeSec
            if (since < SPACING_SEC) return false
            if (lastReaction == reaction && since < REPEAT_SEC) return false
        }
        lastReaction = reaction
        lastTimeSec = nowSec
        hasLast = true
        return true
    }

    fun reset() {
        hasLast = false
        lastReaction = null
    }

    companion object {
        const val SPACING_SEC = 2.5
        const val REPEAT_SEC = 30.0
        const val PATIENCE_SEC = 8.0
    }
}

/** A reaction the controller published. [id] is unique so a collector plays each one once. */
data class MascotReactionEvent(val id: Long, val reaction: MascotReaction, val startSec: Double)
