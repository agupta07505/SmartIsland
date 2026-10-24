/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object HotspotText {
    /** Null when the card says nothing about devices, so a card that only says the hotspot is on stays unknown. */
    fun parseDeviceCountOrNull(title: String?, text: String?): Int? {
        val fullText = "${title.orEmpty()} ${text.orEmpty()}"
        val lower = fullText.lowercase()

        if (lower.contains("no device") || lower.contains("0 device") || lower.contains("no connected")
            || lower.contains("0 connected")) {
            return 0
        }

        val pattern1 = Regex("""\b(\d+)\s*(?:device|connected|client)s?\b""", RegexOption.IGNORE_CASE)
        pattern1.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        val pattern2 = Regex("""\b(?:devices?|connected|clients?)\s*[:=]?\s*(\d+)\b""", RegexOption.IGNORE_CASE)
        pattern2.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        val pattern3 = Regex("""\b(\d+)\s+connected\s+devices?\b""", RegexOption.IGNORE_CASE)
        pattern3.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        return null
    }
}
