/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Open / close state of the landscape split (card island plus picker or picture island), derived from whether the
 * beside island is wanted and the ONE split progress (0 = card only, 1 = both).
 */
enum class SplitPhase { Closed, Opening, Open, Closing }

object SplitMotion {
    /** The progress counts as fully open from this share. */
    const val OPEN_AT = 0.999f

    /** The last share of the progress is a slow spring tail. */
    const val TAIL = 0.1f

    /**
     * Counts as gone at or below this share, the point where [geometry] reaches 0, so the hand-off to the single
     * surface has no pop.
     */
    const val GONE_AT = TAIL

    /**
     * Progress the islands' sizes are drawn from: 0 up to [TAIL], then straight to 1. The reflow ([REFLOW_AT],
     * [reflowAlpha]), shadow ([SHADOW_FROM]) and phase read the raw progress.
     */
    fun geometry(progress: Float): Float = ((progress - TAIL) / (1f - TAIL)).coerceIn(0f, 1f)

    /**
     * Raw progress that gives geometry [g] (inverse of [geometry] above [TAIL]): a finger-driven split is 1:1 with
     * the islands' sizes.
     */
    fun fromGeometry(g: Float): Float = TAIL + g.coerceIn(0f, 1f) * (1f - TAIL)

    fun phase(wanted: Boolean, progress: Float): SplitPhase = when {
        wanted -> if (progress >= OPEN_AT) SplitPhase.Open else SplitPhase.Opening
        progress > GONE_AT -> SplitPhase.Closing
        else -> SplitPhase.Closed
    }

    /**
     * Longest the content waits (ms) for the shell to confirm it is the single island before flipping to the single
     * layout anyway (the shell never confirms if the island collapsed or is hidden meanwhile).
     */
    const val HANDOVER_CAP_MS = 120L

    /**
     * The content keeps the two-island layout while closing and after, until the shell reports its single surface,
     * clip and window are back ([shellSingleReady]).
     */
    fun holdTwoIslandLayout(wasSplit: Boolean, wanted: Boolean, progress: Float, shellSingleReady: Boolean): Boolean =
        wasSplit && !wanted && (phase(wanted, progress) == SplitPhase.Closing || !shellSingleReady)

    fun twoIslandLayout(phase: SplitPhase): Boolean = phase != SplitPhase.Closed

    /**
     * The card's slot in the window is the swap's (wide enough for the single island) in every phase but
     * [SplitPhase.Open], where it is the card's natural width.
     */
    fun swapping(phase: SplitPhase): Boolean = phase != SplitPhase.Open

    /**
     * Card content is laid out at only TWO widths during the swap: single-island layout before this share of
     * progress, split layout from it on; the text re-wraps once here.
     */
    const val REFLOW_AT = 0.3f

    /** Half the width of the fade that hides the re-wrap, as a share of the progress. */
    const val REFLOW_FADE_HALF = 0.06f

    /** True from [REFLOW_AT] on: the content has its split-width layout (opening and closing use the same point). */
    fun reflowed(progress: Float): Boolean = progress >= REFLOW_AT

    /**
     * Opacity of the card's foreground (text, buttons, pictures): dips to 0 at [REFLOW_AT] to hide the re-wrap.
     * Backdrop and scrim stay full so the island is never a bare translucent slab.
     */
    fun reflowAlpha(progress: Float): Float =
        (kotlin.math.abs(progress - REFLOW_AT) / REFLOW_FADE_HALF).coerceIn(0f, 1f)

    /**
     * Performance: shadows are the costliest draw while the split moves, so they are off during the motion and come
     * in over the last part of an opening / go out over the first part of a closing.
     */
    const val SHADOW_FROM = 0.9f

    fun shadowStrength(progress: Float): Float = ((progress - SHADOW_FROM) / (1f - SHADOW_FROM)).coerceIn(0f, 1f)
}
