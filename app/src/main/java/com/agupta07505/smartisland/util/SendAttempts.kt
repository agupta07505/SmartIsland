/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * Runs the ways of delivering one PendingIntent in order and reports whether any went through. No Android types.
 *
 * A clock app replaces its button intents when it re-posts, so a stale one throws on send and the caller must see that.
 */
object SendAttempts {
    /** True as soon as one attempt returns without throwing; later attempts are not run. */
    fun anySucceeds(attempts: List<() -> Unit>): Boolean {
        for (attempt in attempts) {
            val delivered = try {
                attempt()
                true
            } catch (_: Exception) {
                false
            }
            if (delivered) return true
        }
        return false
    }
}
