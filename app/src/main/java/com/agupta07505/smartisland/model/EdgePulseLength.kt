/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

// Pulse sizing for the edge pill. It's a strip down the screen edge, so a pulse grows its length and
// the text runs along it, top to bottom. Middle camera layout: glyph + title above the hole, value
// below. Each side is sized on its own, so unlike the portrait PulseWidth.fit the pill needn't stay
// centred on the hole.
object EdgePulseLength {
    const val TITLE_HOLE_GAP_DP = 10f
    const val GLYPH_HOLE_GAP_DP = 6f // used when there's no title
    const val VALUE_HOLE_GAP_DP = 4f

    private const val TITLE_CLAMP_TAIL_DP = 12f
    private const val VALUE_CLAMP_TAIL_DP = 6f

    /**
     * [upperDp]/[lowerDp] run from the hole's centre to each pill end. [titleMaxDp]/[valueMaxDp] are the
     * room each text has along the strip; draw each at its own length up to that.
     */
    data class Fit(
        val upperDp: Float,
        val lowerDp: Float,
        val titleMaxDp: Float,
        val valueMaxDp: Float,
        val messageMaxDp: Float = 0f
    ) {
        val lengthDp: Float get() = upperDp + lowerDp
    }

    /**
     * [baseDp] is the settled pill length, [holeLengthDp] the hole along the edge (0 if unknown),
     * [centreYDp] the hole centre from the top of the screen. A side never ends shorter than the settled
     * pill does ([settledUpperDp]/[settledLowerDp], from the hole's centre; half of [baseDp] if centred).
     * Each end stops EDGE_RESERVE short of the screen end (status bar + gesture area). The bubble hangs
     * past its end and rides out with it, so [bubbleSpanDp] comes off that side too.
     */
    fun fit(
        baseDp: Float,
        holeLengthDp: Float,
        titleDp: Float,
        valueDp: Float,
        centreYDp: Float,
        screenHeightDp: Float,
        bubbleSpanDp: Float = 0f,
        bubbleBelow: Boolean = true,
        settledUpperDp: Float = baseDp / 2f,
        settledLowerDp: Float = baseDp / 2f
    ): Fit {
        val half = holeLengthDp.coerceAtLeast(0f) / 2f
        val title = titleDp.coerceAtLeast(0f)
        val value = valueDp.coerceAtLeast(0f)
        val glyph = EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP
        val upperNeed = half + SLOT_START_PADDING_DP + glyph +
            (if (title > 0f) SLOT_PAIR_GAP_DP + title + TITLE_HOLE_GAP_DP else GLYPH_HOLE_GAP_DP)
        val lowerNeed = half + SLOT_END_PADDING_DP + value + VALUE_HOLE_GAP_DP
        val (upper, lower) = sides(
            upperNeed, lowerNeed, settledUpperDp, settledLowerDp, centreYDp, screenHeightDp, bubbleSpanDp, bubbleBelow
        )
        return Fit(
            upperDp = upper,
            lowerDp = lower,
            titleMaxDp = (upper - half - SLOT_START_PADDING_DP - glyph - TITLE_CLAMP_TAIL_DP).coerceAtLeast(0f),
            valueMaxDp = valueRoomDp(lower - half - SLOT_END_PADDING_DP, value)
        )
    }

    private data class Sides(val upper: Float, val lower: Float)

    private fun sides(
        upperNeed: Float,
        lowerNeed: Float,
        settledUpperDp: Float,
        settledLowerDp: Float,
        centreYDp: Float,
        screenHeightDp: Float,
        bubbleSpanDp: Float,
        bubbleBelow: Boolean
    ): Sides {
        val upperCap = (centreYDp - PulseWidth.EDGE_RESERVE_DP - if (bubbleBelow) 0f else bubbleSpanDp)
            .coerceAtLeast(0f)
        val lowerCap = (screenHeightDp - PulseWidth.EDGE_RESERVE_DP - centreYDp - if (bubbleBelow) bubbleSpanDp else 0f)
            .coerceAtLeast(0f)
        return Sides(
            upper = minOf(upperCap, maxOf(settledUpperDp.coerceAtLeast(0f), upperNeed)),
            lower = minOf(lowerCap, maxOf(settledLowerDp.coerceAtLeast(0f), lowerNeed))
        )
    }

    // A value the side was sized for keeps its full length (the fit only reserved VALUE_HOLE_GAP after
    // it). Only when the screen cut the side short do we use the demo's wider clamp tail.
    private fun valueRoomDp(sideRoomDp: Float, valueDp: Float): Float =
        if (valueDp <= sideRoomDp - VALUE_HOLE_GAP_DP) valueDp
        else (sideRoomDp - VALUE_CLAMP_TAIL_DP).coerceAtLeast(0f)

    fun splitLowerFixedDp(hasTitle: Boolean): Float = PulseWidth.splitRightFixedDp(hasTitle)

    private fun splitMessageRoomDp(upperFromHoleCentreDp: Float, half: Float, iconDp: Float = 0f): Float =
        (upperFromHoleCentreDp - half - SLOT_START_PADDING_DP - EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP -
            SLOT_PAIR_GAP_DP - iconDp - SLOT_PAIR_GAP_DP).coerceAtLeast(0f)

    /**
     * Strip version of [PulseWidth.fitSplit]: glyph + message above the hole, title alone below, no value.
     * Sides are sized independently like [fit]. [Fit.valueMaxDp] is 0; message room is [Fit.messageMaxDp].
     * Notch or not, [centreYDp] is what decides.
     */
    fun fitSplit(
        baseDp: Float,
        holeLengthDp: Float,
        messageDp: Float,
        titleDp: Float,
        centreYDp: Float,
        screenHeightDp: Float,
        bubbleSpanDp: Float = 0f,
        bubbleBelow: Boolean = true,
        settledUpperDp: Float = baseDp / 2f,
        settledLowerDp: Float = baseDp / 2f,
        mediaIconDp: Float = 0f
    ): Fit {
        val half = holeLengthDp.coerceAtLeast(0f) / 2f
        val icon = mediaIconDp.coerceAtLeast(0f)
        val message = messageDp.coerceAtLeast(0f)
        val title = titleDp.coerceAtLeast(0f)
        val lowerFixed = splitLowerFixedDp(title > 0f)
        val body = if (message > 0f || icon > 0f) SLOT_PAIR_GAP_DP + icon + message else 0f
        val upperNeed = half + SLOT_START_PADDING_DP + EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP + body + SLOT_PAIR_GAP_DP
        val lowerNeed = half + lowerFixed + title
        val (upper, lower) = sides(
            upperNeed, lowerNeed, settledUpperDp, settledLowerDp, centreYDp, screenHeightDp, bubbleSpanDp, bubbleBelow
        )
        return Fit(
            upperDp = upper,
            lowerDp = lower,
            titleMaxDp = (lower - half - lowerFixed).coerceAtLeast(0f),
            valueMaxDp = 0f,
            messageMaxDp = splitMessageRoomDp(upper, half, icon)
        )
    }

    data class SplitSlots(val messageTopDp: Float, val messageLenDp: Float, val glyphTopDp: Float,
        val titleTopDp: Float, val titleLenDp: Float,
        val iconTopDp: Float = 0f)

    /**
     * Slots for the pill as it is *right now* (it's still growing while the spring runs), same idea as
     * the portrait pulse. [messageDp]/[titleDp] are wanted lengths; each is cut to its room and scrolls.
     */
    fun splitSlots(
        lengthDp: Float,
        holeFromTopDp: Float,
        holeLengthDp: Float,
        messageDp: Float,
        titleDp: Float,
        mediaIconDp: Float = 0f
    ): SplitSlots {
        val icon = mediaIconDp.coerceAtLeast(0f)
        val half = holeLengthDp.coerceAtLeast(0f) / 2f
        val afterGlyph = SLOT_START_PADDING_DP + EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP + SLOT_PAIR_GAP_DP
        val titleTop = holeFromTopDp + half + SLOT_PAIR_GAP_DP
        val titleMax = (lengthDp - SLOT_END_PADDING_DP - titleTop).coerceAtLeast(0f)
        return SplitSlots(
            messageTopDp = afterGlyph + icon,
            messageLenDp = messageDp.coerceIn(0f, splitMessageRoomDp(holeFromTopDp, half, icon)),
            glyphTopDp = SLOT_START_PADDING_DP,
            titleTopDp = titleTop,
            titleLenDp = titleDp.coerceIn(0f, titleMax),
            iconTopDp = afterGlyph
        )
    }

    /** Glyph fades in once there's room above the hole, so it never slides over the hole. */
    fun splitGlyphAlpha(holeFromTopDp: Float, holeLengthDp: Float): Float {
        val room = holeFromTopDp - holeLengthDp.coerceAtLeast(0f) / 2f
        return PulseWidth.revealAlpha(room, SLOT_START_PADDING_DP + EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP)
    }

    data class Slots(val glyphTopDp: Float, val titleTopDp: Float, val titleLenDp: Float, val valueTopDp: Float,
        val valueLenDp: Float,
        val artistTopDp: Float = 0f, val artistLenDp: Float = 0f)

    /**
     * Every item hugs whichever pill end it's nearest to (as in the demo's `render`), so growth moves them.
     * Middle camera: glyph + title at the top, value at the bottom. Camera near 1 end: glyph sits
     * [glyphFromCameraDp] from that end, then title, value at the far end. Lower camera is the same
     * flipped so text still reads top to bottom. A song's artist goes just before the value.
     */
    fun slots(spot: EdgeCameraSpot, lengthDp: Float, glyphFromCameraDp: Float, titleLenDp: Float,
        valueLenDp: Float, artistLenDp: Float = 0f): Slots {
        val glyph = EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP
        val from = if (spot == EdgeCameraSpot.Middle) SLOT_START_PADDING_DP else glyphFromCameraDp
        val titleFrom = from + glyph + SLOT_PAIR_GAP_DP
        return if (spot == EdgeCameraSpot.Lower) {
            Slots(lengthDp - from - glyph, lengthDp - titleFrom - titleLenDp, titleLenDp, SLOT_END_PADDING_DP,
                valueLenDp, SLOT_END_PADDING_DP + valueLenDp + PulseWidth.ARTIST_GAP_DP, artistLenDp)
        } else {
            val valueTop = lengthDp - SLOT_END_PADDING_DP - valueLenDp
            Slots(from, titleFrom, titleLenDp, valueTop, valueLenDp,
                valueTop - PulseWidth.ARTIST_GAP_DP - artistLenDp, artistLenDp)
        }
    }

    fun sideMaxDp(spot: EdgeCameraSpot, settledTopDp: Float, settledLengthDp: Float, screenHeightDp: Float,
        bubbleSpanDp: Float): Float =
        (if (spot == EdgeCameraSpot.Lower) settledTopDp + settledLengthDp - PulseWidth.EDGE_RESERVE_DP - bubbleSpanDp
        else screenHeightDp - PulseWidth.EDGE_RESERVE_DP - settledTopDp - bubbleSpanDp).coerceAtLeast(0f)

    fun sideValueMaxDp(lengthDp: Float, glyphFromCameraDp: Float, titleLenDp: Float): Float {
        val glyph = EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP
        val title = if (titleLenDp > 0f) SLOT_PAIR_GAP_DP + titleLenDp else 0f
        return (lengthDp - glyphFromCameraDp - glyph - title - SideCamera.PAIR_GAP_DP - SLOT_END_PADDING_DP)
            .coerceAtLeast(0f)
    }

    data class Texts(val titleLenDp: Float, val valueLenDp: Float, val artistLenDp: Float = 0f)

    fun middleTexts(
        lengthDp: Float,
        holeFromTopDp: Float,
        holeLengthDp: Float,
        titleDp: Float,
        valueDp: Float,
        artistDp: Float = 0f
    ): Texts {
        val half = holeLengthDp.coerceAtLeast(0f) / 2f
        val titleMax = holeFromTopDp - half - SLOT_START_PADDING_DP - EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP -
            TITLE_CLAMP_TAIL_DP
        val value = valueDp.coerceIn(0f, valueRoomDp(lengthDp - holeFromTopDp - half - SLOT_END_PADDING_DP, valueDp))
        val artistRoom = lengthDp - holeFromTopDp - half - VALUE_HOLE_GAP_DP - SLOT_END_PADDING_DP - value -
            PulseWidth.ARTIST_GAP_DP
        val artist = if (artistDp > 0f) artistDp.coerceIn(0f, artistRoom.coerceAtLeast(0f)) else 0f
        return Texts(titleDp.coerceIn(0f, titleMax.coerceAtLeast(0f)), value, artist)
    }

    fun sideTexts(lengthDp: Float, glyphFromCameraDp: Float, titleDp: Float, valueDp: Float, artistDp: Float = 0f): Texts {
        val room = sideValueMaxDp(lengthDp, glyphFromCameraDp, 0f)
        val value = valueDp.coerceIn(0f, room)
        val artist = if (artistDp > 0f) {
            artistDp.coerceIn(0f, (room - value - PulseWidth.ARTIST_GAP_DP).coerceAtLeast(0f))
        } else 0f
        val group = value + if (artist > 0f) PulseWidth.ARTIST_GAP_DP + artist else 0f
        val titleMax = (room - group - SLOT_PAIR_GAP_DP).coerceAtLeast(0f)
        return Texts(titleDp.coerceIn(0f, titleMax), value, artist)
    }

    fun sideSpan(spot: EdgeCameraSpot, settledTopDp: Float, settledLengthDp: Float, fitLengthDp: Float, maxDp: Float,
        progress: Float): Span {
        val length = (settledLengthDp + (fitLengthDp - settledLengthDp) * progress)
            .coerceIn(0f, maxOf(settledLengthDp, maxDp))
        val top = if (spot == EdgeCameraSpot.Lower) settledTopDp + settledLengthDp - length else settledTopDp
        return Span(top, length)
    }

    fun topDp(centreYDp: Float, fit: Fit): Float = centreYDp - fit.upperDp

    data class Span(val topDp: Float, val lengthDp: Float)

    fun span(settledTopDp: Float, settledLengthDp: Float, fit: Fit, centreYDp: Float, progress: Float,
        screenHeightDp: Float): Span {
        val fitTop = topDp(centreYDp, fit)
        val top = (settledTopDp + (fitTop - settledTopDp) * progress)
            .coerceAtLeast(minOf(settledTopDp, PulseWidth.EDGE_RESERVE_DP))
        val settledBottom = settledTopDp + settledLengthDp
        val bottom = (settledBottom + (fitTop + fit.lengthDp - settledBottom) * progress)
            .coerceAtMost(maxOf(settledBottom, screenHeightDp - PulseWidth.EDGE_RESERVE_DP))
        return Span(top, (bottom - top).coerceAtLeast(0f))
    }

    // Progress line inset from each end: the demo's max(14, length * .2), but it also has to clear the
    // rounded end. The line sits lineGapDp from the inner long side, where the corners curve in (radius
    // capped at half the width). Camera-side corners in the flat notch look are square, so they don't count.
    fun lineInsetDp(lengthDp: Float, widthDp: Float, cornerDp: Float, lineGapDp: Float): Float {
        val radius = cornerDp.coerceIn(0f, widthDp / 2f)
        val dy = radius - lineGapDp
        val curve = if (dy <= 0f) 0f else radius - kotlin.math.sqrt((radius * radius - dy * dy).coerceAtLeast(0f))
        return maxOf(PROGRESS_MIN_INSET_DP, lengthDp * PROGRESS_INSET_SHARE, curve + PROGRESS_CURVE_MARGIN_DP)
    }

    private const val PROGRESS_CURVE_MARGIN_DP = 4f

    private const val PROGRESS_MIN_INSET_DP = 14f
    private const val PROGRESS_INSET_SHARE = 0.2f
}

// Who draws the edge pill body during a pulse. The grown pill does from start of growth until it's back
// at settled length, except once a card is open: the shell is the island then, even if the pulse is
// still springing back.
object EdgePulseOwnership {
    fun ownsSurface(growing: Boolean, cardOpen: Boolean): Boolean = growing && !cardOpen
}
