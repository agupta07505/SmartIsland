/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object BubbleIntoCutout {
    const val BASE_MS = 420
    const val HANDOFF_START = 0.55f
    private const val CONTENT_FADE_START = 0.2f
    private const val CONTENT_FADE_END = 0.5f

    const val GLIDE_X1 = 0f
    const val GLIDE_Y1 = 0f
    const val GLIDE_X2 = 0.2f
    const val GLIDE_Y2 = 1f

    const val MAX_STRETCH = 0.12f

    /**
     * Stretch along the travel for a bubble with [remaining] (0..1) of its way left. On an ease-out the speed follows
     * the distance left, so the stretch is biggest at the start and gone on the cutout
     */
    fun stretch(remaining: Float): Float = 1f + MAX_STRETCH * remaining.coerceIn(0f, 1f)

    /** Matching size across the travel, so the bubble keeps its area as it stretches. */
    fun squash(stretch: Float): Float = 1f / stretch.coerceAtLeast(0.01f)

    const val SHAPE_START = 0f

    const val PICKER_ARRIVE_START = 0.1f
    const val PICKER_ARRIVE_END = 0.6f
    private const val PICKER_ARRIVE_MIN_SCALE = 0.3f

    /**
     * How far the bubble has turned into the pill (0 = round, 1 = pill size and shape). [p] is the glide's eased
     * progress, so the widening follows the travel one to one
     */
    fun shape(p: Float): Float = ramp(p, SHAPE_START, 1f)

    fun contentAlpha(p: Float): Float = 1f - ramp(p, CONTENT_FADE_START, CONTENT_FADE_END)

    private const val ICON_GONE_AT = 0.3f

    private const val GLYPHS_START = 0.65f

    fun iconAlpha(shape: Float): Float = 1f - ramp(shape, 0f, ICON_GONE_AT)

    fun glyphAlpha(shape: Float): Float = ramp(shape, GLYPHS_START, 1f)

    fun flies(expanded: Boolean, bubbleWasOut: Boolean, cutoutTakesEvent: Boolean): Boolean =
        expanded && bubbleWasOut && cutoutTakesEvent

    fun pickerArrival(p: Float): Float {
        val t = ramp(p, PICKER_ARRIVE_START, PICKER_ARRIVE_END)
        return t * t * (3f - 2f * t)
    }

    fun pickerScale(p: Float): Float = PICKER_ARRIVE_MIN_SCALE + (1f - PICKER_ARRIVE_MIN_SCALE) * pickerArrival(p)
}
