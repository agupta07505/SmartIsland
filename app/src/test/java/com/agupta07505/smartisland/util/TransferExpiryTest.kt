/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferExpiryTest {
    private fun card(
        key: String,
        mode: IslandMode = IslandMode.DownloadUpload,
        postedMs: Long,
        estimated: Boolean = false
    ) = IslandNotification(
        key = key, packageName = "p", appName = "a", title = "t", text = "x", timeMillis = 0L,
        mode = mode, lastPostedMs = postedMs, isEstimatedTransfer = estimated
    )

    private val now = 10_000_000L
    private val min = 60_000L

    @Test fun oldTransferExpires() {
        assertEquals(listOf("a"), TransferExpiry.keysToExpire(listOf(card("a", postedMs = now - 20 * min)), now, 15))
    }

    @Test fun freshTransferStays() {
        assertEquals(emptyList<String>(), TransferExpiry.keysToExpire(listOf(card("a", postedMs = now - 5 * min)), now, 15))
    }

    @Test fun zeroMeansNever() {
        assertEquals(emptyList<String>(), TransferExpiry.keysToExpire(listOf(card("a", postedMs = 1L)), now, 0))
    }

    @Test fun otherModesEstimatedAndUnknownTimeAreSkipped() {
        val cards = listOf(
            card("n", mode = IslandMode.Notification, postedMs = 1L),
            card("e", postedMs = 1L, estimated = true),
            card("u", postedMs = 0L)
        )
        assertEquals(emptyList<String>(), TransferExpiry.keysToExpire(cards, now, 15))
    }
}
