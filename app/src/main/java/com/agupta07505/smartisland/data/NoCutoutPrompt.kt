/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

object NoCutoutPrompt {
    /** The Positions camera card opens for any row key that starts with this. */
    const val CARD_KEY_PREFIX = "layout.cutout"

    /** Row the prompt's button jumps to: the style selector in that card. */
    const val ROW_KEY = "layout.cutout_style"

    /**
     * Asked once, in portrait (detection only reads the hole there), when nothing was detected and the person has not
     * said where the camera is, by a style or by a manual area
     */
    fun shouldShow(
        hasHardwareCutout: Boolean,
        style: String,
        manualArea: Boolean,
        dismissed: Boolean,
        isPortrait: Boolean
    ): Boolean = isPortrait && !hasHardwareCutout && !dismissed && !manualArea &&
        style == SmartIslandSettings.CUTOUT_STYLE_AUTO
}
