/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

/**
 * A VoIP app (WhatsApp, Telegram ...) often posts the ringing and ongoing-call notifications under different keys.
 */
object CallCardRules {

    /** A ringing call replaces nothing (a second incoming call may arrive while one is running). */
    fun supersededKeys(
        existing: List<IslandNotification>,
        newKey: String,
        packageName: String,
        newIsRinging: Boolean
    ): List<String> =
        if (newIsRinging) {
            emptyList()
        } else {
            existing
                .filter { it.mode == IslandMode.IncomingCall && it.packageName == packageName && it.key != newKey }
                .map { it.key }
        }
}
