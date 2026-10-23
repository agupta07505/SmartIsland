/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Card and sibling picker trade places by size on ONE progress (0 = card, 1 = picker): a close that starts mid-open
 * reverses in place, so both directions mirror each other.
 */
object PickerSwap {
    /** Base length of the swap while the picker opens. */
    const val OPEN_MS = 280

    /** Base length while it closes: a little quicker, the picker is being put away. */
    const val CLOSE_MS = 240

    /** Landscape's two islands run their ONE progress on a spring (no bounce). */
    const val SPLIT_OPEN_STIFFNESS = 400f
    const val SPLIT_CLOSE_STIFFNESS = 520f

    /** The card is fully gone (alpha 0) once the progress reaches this share. */
    const val CARD_GONE_AT = 0.6f

    /** The picker starts to show past this share (a short overlap, never two solid layers). */
    const val PICKER_IN_FROM = 0.3f

    /** The card shrinks to this scale at progress 1; the picker starts from the same scale at 0. */
    const val END_SCALE = 0.92f

    fun cardAlpha(progress: Float): Float = 1f - (progress / CARD_GONE_AT).coerceIn(0f, 1f)

    fun pickerAlpha(progress: Float): Float = ((progress - PICKER_IN_FROM) / (1f - PICKER_IN_FROM)).coerceIn(0f, 1f)

    fun cardScale(progress: Float): Float = 1f - (1f - END_SCALE) * progress.coerceIn(0f, 1f)

    fun pickerScale(progress: Float): Float = END_SCALE + (1f - END_SCALE) * progress.coerceIn(0f, 1f)

    fun cardLayerShown(progress: Float, pickerOpen: Boolean): Boolean = !pickerOpen || progress < 0.999f

    fun pickerLayerShown(progress: Float, pickerOpen: Boolean): Boolean = pickerOpen || progress > 0.001f

    fun lengthMs(opening: Boolean): Int = if (opening) OPEN_MS else CLOSE_MS
}
