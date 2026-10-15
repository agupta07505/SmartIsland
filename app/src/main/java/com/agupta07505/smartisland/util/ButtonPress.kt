/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * Whether a press on a clock button can reach the app. A button mirrored from the clock's custom view is a proxy
 * broadcast that presses a kept view; the broadcast succeeds even when that view is gone, so it must be checked first.
 */
object ButtonPress {

    enum class Refusal {
        /** The button has no intent. */
        NoIntent,

        /** Proxy button whose kept view is gone. */
        ViewGone
    }

    /** Null when the press may be sent. */
    fun refusal(hasIntent: Boolean, viaView: Boolean, liveLabels: List<String>, title: String): Refusal? = when {
        !hasIntent -> Refusal.NoIntent
        viaView && !hasLive(liveLabels, title) -> Refusal.ViewGone
        else -> null
    }

    /** True when a kept view is still behind the proxy button with this label. */
    fun hasLive(liveLabels: List<String>, title: String): Boolean {
        val wanted = title.trim()
        return liveLabels.any { it.trim().equals(wanted, ignoreCase = true) }
    }
}
