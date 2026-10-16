/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.ui.graphics.Color

/**
 * Colors for content drawn ON the island. The island surface is always black (it blends with the
 * camera cutout), so these are fixed dark-appearance values, not theme colors. All text colors are
 * at least 4.5:1 on black; status colors always ship with an icon or text label.
 */
object IslandPalette {
    val label = Color(0xFFFFFFFF)
    /** Secondary text: 8.4:1 on black. */
    val secondaryLabel = Color(0xFFA1A1A6)
    /** Resting fill for buttons and tracks (white at 16%). */
    val fill = Color(0x29FFFFFF)
    /** Pressed / emphasized fill (white at 24%). */
    val fillStrong = Color(0x3DFFFFFF)
    val success = Color(0xFF30D158)
    val warning = Color(0xFFFF9F0A)
    val danger = Color(0xFFFF453A)
}
