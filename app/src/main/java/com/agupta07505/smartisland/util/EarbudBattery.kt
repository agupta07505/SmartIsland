/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * Per-bud battery levels for the Bluetooth Medium card (Rule_Book §4.6). Android only hands per-bud levels to
 * privileged apps (`BluetoothDevice` metadata), so they are read from the alert's text when its source spells them
 * out, e.g. "L 80% • R 75% • Case 40%". With fewer than two parts the card keeps its single gauge.
 */
data class EarbudBattery(val left: Int?, val right: Int?, val case: Int?) {
    val hasSplitLevels: Boolean get() = listOfNotNull(left, right, case).size >= 2

    companion object {
        private val LEFT = Regex("""(?i)\b(?:l|left)\s*[:=]?\s*(\d{1,3})\s*%""")
        private val RIGHT = Regex("""(?i)\b(?:r|right)\s*[:=]?\s*(\d{1,3})\s*%""")
        private val CASE = Regex("""(?i)\b(?:c|case)\s*[:=]?\s*(\d{1,3})\s*%""")

        fun parse(text: String?): EarbudBattery? {
            if (text.isNullOrBlank()) return null
            fun level(pattern: Regex) = pattern.find(text)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 0..100 }
            return EarbudBattery(level(LEFT), level(RIGHT), level(CASE)).takeIf { it.hasSplitLevels }
        }
    }
}
