/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/** Whether a press on a clock button can reach the app. */
object ButtonPress {

    enum class Refusal {
        NoIntent,

        ViewGone
    }

    /** Null when the press may be sent. */
    fun refusal(hasIntent: Boolean, viaView: Boolean, liveLabels: List<String>, title: String): Refusal? = when {
        !hasIntent -> Refusal.NoIntent
        viaView && !hasLive(liveLabels, title) -> Refusal.ViewGone
        else -> null
    }

    fun hasLive(liveLabels: List<String>, title: String): Boolean {
        val wanted = title.trim()
        return liveLabels.any { it.trim().equals(wanted, ignoreCase = true) }
    }
}
