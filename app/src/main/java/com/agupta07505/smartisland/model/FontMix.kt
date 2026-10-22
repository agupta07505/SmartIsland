/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * "Mix fonts": island text uses up to 3 fonts, like a system family's Light, Regular and Medium cuts, the requested
 * weight picks the font
 */
enum class FontSlot(
    val representativeWeight: Int,
    val weights: List<Int>
) {
    Light(400, listOf(100, 200, 300, 400)),
    Regular(500, listOf(500, 600)),
    Medium(700, listOf(700, 800, 900))
}

object FontMix {
    fun effectiveId(slotId: String, islandFont: String): String = slotId.ifEmpty { islandFont }

    fun idOf(slot: FontSlot, light: String, regular: String, medium: String): String = when (slot) {
        FontSlot.Light -> light
        FontSlot.Regular -> regular
        FontSlot.Medium -> medium
    }

    /** Active only when enabled and at least one slot has its own font. */
    fun isActive(enabled: Boolean, light: String, regular: String, medium: String): Boolean =
        enabled && (light.isNotEmpty() || regular.isNotEmpty() || medium.isNotEmpty())
}
