/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Where the landscape split (the card island plus the picker or picture island) is in its open / close
 * motion. One name for the wanted / holding / progress combinations, instead of ad-hoc 0.999 / 0.001 checks.
 *
 * The phase is derived from two things the content already has: whether the beside island is wanted, and the ONE split
 * progress (0 = card only, 1 = card and beside island). Nothing here animates; it only names the state.
 */
enum class SplitPhase { Closed, Opening, Open, Closing }

object SplitMotion {
    /** The progress counts as fully open from this share. */
    const val OPEN_AT = 0.999f

    /**
     * The last share of the progress is a slow spring tail (a few pixels of width for about 4 frames). The islands'
     * GEOMETRY (picker width, card width, gap, the shell's split width) uses [geometry], which is 0 from this share down, so the two
     * islands are exactly the single island's size when the split counts as gone and no thin full-height bar is left at the close's end.
     */
    const val TAIL = 0.1f

    /** The progress counts as gone at or below this share: the same point where [geometry] reaches 0, so the hand-off to the single surface has no pop. */
    const val GONE_AT = TAIL

    /**
     * The progress the islands' sizes are drawn from: 0 up to [TAIL], then a straight run to 1 at the full progress. The reflow
     * ([REFLOW_AT], [reflowAlpha]), the shadow ([SHADOW_FROM]) and the phase keep reading the raw progress.
     */
    fun geometry(progress: Float): Float = ((progress - TAIL) / (1f - TAIL)).coerceIn(0f, 1f)

    /** The raw progress that gives geometry [g] (the inverse of [geometry] above [TAIL]): a finger-driven split is 1:1 with the islands' sizes. */
    fun fromGeometry(g: Float): Float = TAIL + g.coerceIn(0f, 1f) * (1f - TAIL)

    fun phase(wanted: Boolean, progress: Float): SplitPhase = when {
        wanted -> if (progress >= OPEN_AT) SplitPhase.Open else SplitPhase.Opening
        progress > GONE_AT -> SplitPhase.Closing
        else -> SplitPhase.Closed
    }

    /**
     * Longest the content waits (ms) for the shell to confirm it has taken over as the single island before it flips to the
     * single layout anyway (the shell never confirms when the island collapsed, or is not shown, in the meantime).
     */
    const val HANDOVER_CAP_MS = 120L

    /**
     * The content keeps the two-island layout while it closes AND after it has closed, until the shell reports that its single surface,
     * clip and window are back ([shellSingleReady]). At geometry 0 the two layouts are the same rectangle, so the extra frames cost
     * nothing visible. Flipping first (the old order) drew the single-layout card, which has no surface of its own, in the still
     * wide, unclipped two-island shell for a frame or more: the square, see-through flash at the end of a close.
     */
    fun holdTwoIslandLayout(wasSplit: Boolean, wanted: Boolean, progress: Float, shellSingleReady: Boolean): Boolean =
        wasSplit && !wanted && (phase(wanted, progress) == SplitPhase.Closing || !shellSingleReady)

    /** The two-island layout is composed in every phase but [SplitPhase.Closed]. */
    fun twoIslandLayout(phase: SplitPhase): Boolean = phase != SplitPhase.Closed

    /**
     * The card's slot in the window is the swap's (wide enough for the single island's width) in every phase but
     * [SplitPhase.Open], where it is the card's natural width.
     */
    fun swapping(phase: SplitPhase): Boolean = phase != SplitPhase.Open

    /**
     * The card island's content is laid out at only TWO widths during the swap instead of the island's width
     * in every frame. Before this share of the progress it keeps the single island's layout (the island clips it as it narrows);
     * from it on it has the split layout (its final width). The text re-wraps once, at this point.
     */
    const val REFLOW_AT = 0.3f

    /** Half the width of the fade that hides the re-wrap, as a share of the progress (the fade is `2 * this` wide in all). */
    const val REFLOW_FADE_HALF = 0.06f

    /** True from [REFLOW_AT] on: the content has its split-width layout. Both the opening and the closing use the same point. */
    fun reflowed(progress: Float): Boolean = progress >= REFLOW_AT

    /**
     * Opacity of the card island's FOREGROUND (text, buttons, pictures): 1 away from the re-wrap, dipping to 0 at [REFLOW_AT] and back,
     * so the lines never visibly jump (the old layout fades out, the new one fades in). Drawn from the raw progress, no recomposition.
     * Only the foreground dips; the card's backdrop and scrim stay at full strength, so the island is never a bare
     * translucent slab in the middle of the close.
     */
    fun reflowAlpha(progress: Float): Float =
        (kotlin.math.abs(progress - REFLOW_AT) / REFLOW_FADE_HALF).coerceIn(0f, 1f)

    /**
     * Performance: the two islands' shadows are the most expensive thing drawn while the split moves (every frame the islands'
     * sizes change, so each shadow is rendered again). They are off while the islands move and come in over the last part of the
     * opening (and go out over the first part of a closing); the shadow is the same at rest.
     */
    const val SHADOW_FROM = 0.9f

    fun shadowStrength(progress: Float): Float = ((progress - SHADOW_FROM) / (1f - SHADOW_FROM)).coerceIn(0f, 1f)
}
