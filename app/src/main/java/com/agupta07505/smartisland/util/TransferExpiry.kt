/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

/** Expires download cards when the source app's notification disappears without a callback. */
object TransferExpiry {
    fun keysToExpire(
        cards: List<IslandNotification>,
        nowMs: Long,
        expiryMinutes: Int
    ): List<String> {
        if (expiryMinutes <= 0) return emptyList()
        val limitMs = expiryMinutes * 60_000L
        return cards.filter {
            it.mode == IslandMode.DownloadUpload &&
                !it.isEstimatedTransfer &&
                it.key.isNotEmpty() &&
                !DemoEvents.isDemoKey(it.key) &&
                it.lastPostedMs > 0L &&
                nowMs - it.lastPostedMs >= limitMs
        }.map { it.key }
    }
}
