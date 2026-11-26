/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

// Width maths for the pulse pill: how wide it grows and how that width splits around the camera hole.
// Centre camera layout: glyph + title on the left of the hole, value on the right.
object PulseWidth {
    // Part of each screen edge kept clear for the clock / status icons. Side cameras and the edge pill
    // share this one number, centre cameras use the LEFT/RIGHT pair below.
    const val EDGE_RESERVE_DP = 72f

    // Centre camera, clock side (left in LTR). Only the clock must stay visible, the notification icons
    // next to it can be covered. Clock ends around 50dp, plus some air.
    // Hardcoded instead of measured: clock width moves with font scale, 12/24h and carrier text.
    const val EDGE_RESERVE_LEFT_DP = 60f

    // Centre camera, status icon side (right in LTR). Reserves space for the battery: ~22dp wide, 12dp from
    // the edge, ~10dp gap. Signal/wifi icons can get covered, but only as far as the content actually
    // reaches, so short values leave them alone. Hardcoded for the same reason as the left one.
    const val EDGE_RESERVE_RIGHT_DP = 44f

    data class Reserves(val leftDp: Float, val rightDp: Float)

    // The status bar mirrors in RTL (clock first, icons last), so the reserves swap too
    fun edgeReserves(rtl: Boolean): Reserves =
        if (rtl) Reserves(EDGE_RESERVE_RIGHT_DP, EDGE_RESERVE_LEFT_DP) else Reserves(EDGE_RESERVE_LEFT_DP, EDGE_RESERVE_RIGHT_DP)

    /**
     * Room a centre pill can grow into, measured from its settled centre. The 2 sides usually
     * differ: the reserves aren't equal and the bubble takes room from whichever side it sits on.
     */
    data class MaxWidth(val leftDp: Float, val rightDp: Float) {
        val widthDp: Float get() = leftDp + rightDp

        /** Widest pill that grows evenly both ways, so the tighter side is the limit. */
        val symmetricWidthDp: Float get() = 2f * minOf(leftDp, rightDp)

        companion object {
            fun symmetric(widthDp: Float): MaxWidth = MaxWidth(widthDp / 2f, widthDp / 2f)
        }
    }

    data class Fit(val widthDp: Float, val titleMaxDp: Float, val centreShiftDp: Float = 0f)

    // [shiftDp] is the hole's centre relative to the pill's (see cameraShiftDp). The 2 sides of the
    // hole then get different room, and the pill is sized for whichever side wants more.
    fun fit(
        baseDp: Float,
        cameraGapDp: Float,
        titleDp: Float,
        valueDp: Float,
        maxDp: Float,
        shiftDp: Float = 0f
    ): Fit {
        val fixedLeft = SLOT_START_PADDING_DP + SLOT_GLYPH_SIZE_DP + (if (titleDp > 0f) SLOT_PAIR_GAP_DP else 0f)
        val fixedRight = valueDp + SLOT_END_PADDING_DP
        val wantedHalf = maxOf(fixedLeft + titleDp - shiftDp, fixedRight + shiftDp)
        val width = (cameraGapDp + 2f * wantedHalf).coerceIn(baseDp, maxOf(baseDp, maxDp))
        return Fit(width, (rooms(width, cameraGapDp, shiftDp).leftDp - fixedLeft).coerceAtLeast(0f))
    }

    /**
     * Centre camera, notification: glyph + message left of the hole, title alone on the right (no value).
     * Each side takes what its content wants but never less than the settled pill, so a short title
     * doesn't end up with the room a long message needs. Sides come out unequal, hence [Fit.centreShiftDp].
     * [mediaIconDp] is [MEDIA_ICON_DP] for pictures/stickers, else 0. [maxDp] splits evenly around the
     * settled centre. Here [Fit.titleMaxDp] is the title's room on the right.
     */
    fun fitSplit(
        baseDp: Float,
        cameraGapDp: Float,
        messageDp: Float,
        titleDp: Float,
        maxDp: Float,
        shiftDp: Float = 0f,
        mediaIconDp: Float = 0f
    ): Fit = fitSplit(baseDp, cameraGapDp, messageDp, titleDp, MaxWidth.symmetric(maxDp), shiftDp, mediaIconDp)

    fun fitSplit(
        baseDp: Float,
        cameraGapDp: Float,
        messageDp: Float,
        titleDp: Float,
        max: MaxWidth,
        shiftDp: Float = 0f,
        mediaIconDp: Float = 0f
    ): Fit {
        val half = cameraGapDp / 2f
        val fixedRight = splitRightFixedDp(titleDp > 0f)
        // icon sits before the message so a short label isn't pushed into scrolling by it;
        // an icon-only card still reserves its room
        val lead = mediaIconDp.coerceAtLeast(0f)
        val body = if (messageDp > 0f || lead > 0f) SLOT_PAIR_GAP_DP + lead + messageDp else 0f
        val wantLeft = half + SLOT_START_PADDING_DP + SLOT_GLYPH_SIZE_DP + body + SLOT_PAIR_GAP_DP
        val reach = extents(baseDp, max, shiftDp, wantLeft, half + fixedRight + titleDp)
        return Fit(reach.left + reach.right, (reach.right - half - fixedRight).coerceAtLeast(0f), reach.centreShiftDp(shiftDp))
    }

    /**
     * Centre camera, events with an external title (network, song, device, caller, file): glyph + title
     * left of the hole, value right. Sides are sized on their own, so a long title doesn't drag the
     * value away from the hole with it. Centre shifts like [fitSplit].
     */
    fun fitPerSide(
        baseDp: Float,
        cameraGapDp: Float,
        titleDp: Float,
        valueDp: Float,
        maxDp: Float,
        shiftDp: Float = 0f
    ): Fit = fitPerSide(baseDp, cameraGapDp, titleDp, valueDp, MaxWidth.symmetric(maxDp), shiftDp)

    fun fitPerSide(
        baseDp: Float,
        cameraGapDp: Float,
        titleDp: Float,
        valueDp: Float,
        max: MaxWidth,
        shiftDp: Float = 0f
    ): Fit {
        val half = cameraGapDp / 2f
        val fixedLeft = SLOT_START_PADDING_DP + SLOT_GLYPH_SIZE_DP + (if (titleDp > 0f) SLOT_PAIR_GAP_DP else 0f)
        val reach = extents(baseDp, max, shiftDp, half + fixedLeft + titleDp, half + valueDp + SLOT_END_PADDING_DP)
        return Fit(reach.left + reach.right, (reach.left - half - fixedLeft).coerceAtLeast(0f), reach.centreShiftDp(shiftDp))
    }

    private class Reach(val left: Float, val right: Float) {
        fun centreShiftDp(shiftDp: Float): Float = shiftDp + (right - left) / 2f
    }

    // Each side takes what it wants, measured from the hole's centre line, but never less than the
    // settled pill (baseDp, hole at shiftDp) and never more than the room max allows on that side.
    private fun extents(baseDp: Float, max: MaxWidth, shiftDp: Float, wantLeft: Float, wantRight: Float): Reach {
        val reachLeft = maxOf(baseDp / 2f, max.leftDp)
        val reachRight = maxOf(baseDp / 2f, max.rightDp)
        return Reach(
            maxOf(baseDp / 2f + shiftDp, minOf(wantLeft, reachLeft + shiftDp)),
            maxOf(baseDp / 2f - shiftDp, minOf(wantRight, reachRight - shiftDp))
        )
    }

    const val MEDIA_ICON_SIZE_DP = 14f
    const val MEDIA_ICON_GAP_DP = 4f
    const val MEDIA_ICON_DP = MEDIA_ICON_SIZE_DP + MEDIA_ICON_GAP_DP

    const val ARTIST_GAP_DP = SLOT_PAIR_GAP_DP

    /**
     * Width of a song's value side: artist + gap + visualiser. [leadGapDp] is the gap before the artist
     * when nothing else spaces it (centre camera, from the hole); side cameras add their own gap so pass 0.
     * Without an artist it's just [valueDp].
     */
    fun valueSideDp(valueDp: Float, artistDp: Float, leadGapDp: Float = 0f): Float =
        if (artistDp > 0f) leadGapDp + artistDp + ARTIST_GAP_DP + valueDp else valueDp

    fun artistMaxDp(valueRoomDp: Float, valueDp: Float, leadGapDp: Float = 0f): Float =
        (valueRoomDp - leadGapDp - ARTIST_GAP_DP - valueDp).coerceAtLeast(0f)

    /**
     * Side camera: room for the artist in a pill [widthDp] wide. The title yields first, so the artist
     * gets whatever is left after glyph, gaps, end padding and visualiser, and the title gets none.
     * Same maths [fitSide] sizes the pill with. [gapDp] is the side camera's own gap, or the settled
     * pill's for a centred landscape classic pill ([fitClassic]).
     */
    fun sideArtistMaxDp(
        widthDp: Float,
        glyphFromCameraDp: Float,
        hasTitle: Boolean,
        valueDp: Float,
        gapDp: Float = SideCamera.PAIR_GAP_DP
    ): Float {
        val fixed = glyphFromCameraDp + SLOT_GLYPH_SIZE_DP + (if (hasTitle) SLOT_PAIR_GAP_DP else 0f)
        return artistMaxDp(widthDp - fixed - gapDp - SLOT_END_PADDING_DP, valueDp)
    }

    fun splitRightFixedDp(hasTitle: Boolean): Float =
        if (hasTitle) SLOT_PAIR_GAP_DP + SLOT_END_PADDING_DP else 0f

    fun splitMessageMaxDp(leftRoomDp: Float, mediaIconDp: Float = 0f): Float =
        (leftRoomDp - SLOT_START_PADDING_DP - SLOT_GLYPH_SIZE_DP - SLOT_PAIR_GAP_DP - mediaIconDp.coerceAtLeast(0f) -
            SLOT_PAIR_GAP_DP).coerceAtLeast(0f)

    /** Hole centre relative to pill centre. The pill grows about its centre so this stays valid while growing. */
    fun cameraShiftDp(cameraCentreInPillDp: Float?, pillWidthDp: Float): Float =
        if (cameraCentreInPillDp == null) 0f else cameraCentreInPillDp - pillWidthDp / 2f

    data class Rooms(val leftDp: Float, val rightDp: Float)

    fun rooms(widthDp: Float, cameraGapDp: Float, shiftDp: Float): Rooms = Rooms(
        (widthDp / 2f + shiftDp - cameraGapDp / 2f).coerceAtLeast(0f),
        (widthDp / 2f - shiftDp - cameraGapDp / 2f).coerceAtLeast(0f)
    )

    fun revealAlpha(roomDp: Float, needDp: Float): Float = (1f + (roomDp - needDp) / REVEAL_DP).coerceIn(0f, 1f)

    private const val REVEAL_DP = 8f

    private const val FOLLOW_FROM_PROGRESS = 0.95f

    fun followsFitChange(progress: Float): Boolean = progress >= FOLLOW_FROM_PROGRESS

    /**
     * Landscape classic, no hole: glyph + title from the start edge, value at the end edge, laid out
     * like the settled pill ([gapDp] comes from [LandscapeClassicPulse.centreGapDp]). Unlike [fit], the
     * sides can differ in width. Short content keeps the settled width. Grows about its centre.
     */
    fun fitClassic(
        baseDp: Float,
        gapDp: Float,
        titleDp: Float,
        valueDp: Float,
        maxDp: Float,
        messageDp: Float = 0f,
        mediaIconDp: Float = 0f
    ): Fit {
        val body = sideBodyDp(titleDp, messageDp, mediaIconDp)
        val fixedLeft = SLOT_START_PADDING_DP + SLOT_GLYPH_SIZE_DP + (if (body > 0f) SLOT_PAIR_GAP_DP else 0f)
        val tail = (if (valueDp > 0f) gapDp.coerceAtLeast(0f) + valueDp else 0f) + SLOT_END_PADDING_DP
        val width = (fixedLeft + body + tail).coerceIn(baseDp, maxOf(baseDp, maxDp))
        return Fit(width, sideTextRooms(width - fixedLeft - tail, titleDp, messageBlockDp(messageDp, mediaIconDp)).titleDp)
    }

    /**
     * Side camera: everything sits on the roomy side and the pill grows away from the camera.
     * [glyphFromCameraDp] = [SideCamera.glyphStartFromCameraDp].
     */
    fun fitSide(
        baseDp: Float,
        glyphFromCameraDp: Float,
        titleDp: Float,
        valueDp: Float,
        maxDp: Float,
        messageDp: Float = 0f,
        mediaIconDp: Float = 0f
    ): Fit {
        val body = sideBodyDp(titleDp, messageDp, mediaIconDp)
        val fixed = glyphFromCameraDp + SLOT_GLYPH_SIZE_DP + (if (body > 0f) SLOT_PAIR_GAP_DP else 0f)
        val tail = (if (valueDp > 0f) SideCamera.PAIR_GAP_DP + valueDp else 0f) + SLOT_END_PADDING_DP
        val width = (fixed + body + tail).coerceIn(baseDp, maxOf(baseDp, maxDp))
        return Fit(width, sideTextRooms(width - fixed - tail, titleDp, messageBlockDp(messageDp, mediaIconDp)).titleDp)
    }

    fun sideBodyDp(titleDp: Float, messageDp: Float, mediaIconDp: Float = 0f): Float {
        val message = messageBlockDp(messageDp, mediaIconDp)
        return titleDp + (if (titleDp > 0f && message > 0f) SLOT_PAIR_GAP_DP else 0f) + message
    }

    fun messageBlockDp(messageDp: Float, mediaIconDp: Float = 0f): Float =
        if (messageDp > 0f || mediaIconDp > 0f) mediaIconDp.coerceAtLeast(0f) + messageDp else 0f

    data class TextRooms(val titleDp: Float, val messageDp: Float)

    /**
     * Splits the room after the glyph between title and message. If both fit, each keeps its own length.
     * If not, the title only gets more than [TITLE_ROOM_SHARE] of the room when the message is short;
     * otherwise the message takes the rest and scrolls. No message: the title gets everything.
     */
    fun sideTextRooms(roomDp: Float, titleDp: Float, messageBlockDp: Float): TextRooms {
        val room = roomDp.coerceAtLeast(0f)
        if (messageBlockDp <= 0f) return TextRooms(room, 0f)
        if (titleDp <= 0f) return TextRooms(0f, room)
        val gap = SLOT_PAIR_GAP_DP
        if (titleDp + gap + messageBlockDp <= room) return TextRooms(titleDp, room - titleDp - gap)
        val title = minOf(titleDp, maxOf(room * TITLE_ROOM_SHARE, room - gap - messageBlockDp)).coerceAtLeast(0f)
        return TextRooms(title, (room - gap - title).coerceAtLeast(0f))
    }

    private const val TITLE_ROOM_SHARE = 0.4f

    /**
     * Widest a pulse can get. Side camera: the camera-side edge stays put and the pill grows toward
     * the bubble, up to the status bar reserve. Centre camera grows both ways, see [centredMaxWidth].
     * The bubble rides out with the growing edge, so its span has to stay free as well, unless it sits
     * on the fixed edge ([PulseBubble.onFixedSide]).
     */
    fun sideMaxWidthDp(
        side: CameraSide,
        screenWidthDp: Float,
        pillLeftDp: Float,
        settledWidthDp: Float,
        bubbleSpanDp: Float = 0f,
        bubbleOnLeft: Boolean = false
    ): Float {
        val span = if (PulseBubble.onFixedSide(side, bubbleOnLeft)) 0f else bubbleSpanDp
        return when (side) {
            CameraSide.Left -> screenWidthDp - EDGE_RESERVE_DP - pillLeftDp - span
            CameraSide.Right -> pillLeftDp + settledWidthDp - EDGE_RESERVE_DP - span
            CameraSide.Centre -> centredMaxWidth(
                screenWidthDp, pillLeftDp + settledWidthDp / 2f, bubbleSpanDp, bubbleOnLeft
            ).symmetricWidthDp
        }.coerceAtLeast(0f)
    }

    fun maxWidth(
        side: CameraSide,
        screenWidthDp: Float,
        pillLeftDp: Float,
        settledWidthDp: Float,
        bubbleSpanDp: Float = 0f,
        bubbleOnLeft: Boolean = false,
        rtl: Boolean = false
    ): MaxWidth =
        if (side == CameraSide.Centre) {
            centredMaxWidth(screenWidthDp, pillLeftDp + settledWidthDp / 2f, bubbleSpanDp, bubbleOnLeft, rtl)
        } else {
            MaxWidth.symmetric(sideMaxWidthDp(side, screenWidthDp, pillLeftDp, settledWidthDp, bubbleSpanDp, bubbleOnLeft))
        }

    /**
     * Pill growing from its own centre (centre camera, notch mode). Each side has its own reserve
     * (clock / status icons), swapped in RTL. The bubble sits outside the growing edge, so its span
     * comes out of that side's room.
     */
    fun centredMaxWidth(
        screenWidthDp: Float,
        centreDp: Float,
        bubbleSpanDp: Float,
        bubbleOnLeft: Boolean,
        rtl: Boolean = false
    ): MaxWidth {
        val reserve = edgeReserves(rtl)
        return MaxWidth(
            leftDp = (centreDp - reserve.leftDp - if (bubbleOnLeft) bubbleSpanDp else 0f).coerceAtLeast(0f),
            rightDp = (screenWidthDp - centreDp - reserve.rightDp - if (bubbleOnLeft) 0f else bubbleSpanDp)
                .coerceAtLeast(0f)
        )
    }

    // How far the progress line is inset from each side. The bottom corners curve in, so a line a few dp
    // above the bottom edge must start past where the curve reaches that height. Notch pills have flat
    // tops and corners up to full height, normal pills are fully round.
    fun lineInsetDp(heightDp: Float, cornerDp: Float, notch: Boolean, bottomDp: Float, minDp: Float): Float {
        val radius = cornerDp.coerceIn(0f, if (notch) heightDp else heightDp / 2f)
        val curve = if (radius <= bottomDp) 0f else {
            val dy = radius - bottomDp
            radius - kotlin.math.sqrt((radius * radius - dy * dy).coerceAtLeast(0f))
        }
        return maxOf(minDp, heightDp * 0.4f, curve + LINE_CURVE_MARGIN_DP)
    }

    private const val LINE_CURVE_MARGIN_DP = 4f

    fun equivalentPulse(extraDp: Float, baseDp: Float, fraction: Float, progress: Float): Float {
        if (baseDp <= 0f || fraction <= 0f) return 0f
        return extraDp.coerceAtLeast(0f) * progress.coerceAtLeast(0f) / (baseDp * fraction)
    }
}

// Short strings for the value slot. Lives here so the pulse can size itself for exactly what the slot draws.
object PulseValueText {
    private val DISTANCE = Regex("""(\d+(?:\.\d+)?)\s*(?:m|km|ft|mi|miles?|meters?)\b""", RegexOption.IGNORE_CASE)
    private val MINUTES = Regex("""(\d+)\s*(?:mins?|minutes?|min|m)\b""", RegexOption.IGNORE_CASE)

    const val DISTANCE_FALLBACK = "In 200 m"

    fun distance(title: String, text: String): String = DISTANCE.find("$title $text")?.value ?: DISTANCE_FALLBACK

    fun eta(title: String, text: String): String {
        val joined = "$title $text"
        MINUTES.find(joined)?.let { return "${it.groupValues[1]} min" }
        return if (joined.lowercase().contains("arrived")) "Arrived" else "Active"
    }

    fun hotspot(count: Int?): String = when (count) {
        // if no count from device, it dislays on
        null -> "On"
        1 -> "1 device"
        else -> "$count devices"
    }
}

// Text that overflows the pulse scrolls once, start to end, then sits. Big overflows scroll faster so one
// long string doesn't hog the pulse; the pulse stays up for as long as the pass takes.
object PulseScroll {
    const val BASE_DP_PER_S = 30f
    const val FAST_DP_PER_S = 48f

    const val FAST_FROM_DP = 60f
    const val FAST_FULL_DP = 300f

    const val START_DELAY_MS = 700L

    const val END_PAUSE_MS = 1_000L

    fun velocityDpPerSec(overflowDp: Float): Float {
        val t = ((overflowDp - FAST_FROM_DP) / (FAST_FULL_DP - FAST_FROM_DP)).coerceIn(0f, 1f)
        return BASE_DP_PER_S + (FAST_DP_PER_S - BASE_DP_PER_S) * t
    }

    fun scrollMs(overflowDp: Float): Long =
        if (overflowDp <= 0f) 0L else (overflowDp / velocityDpPerSec(overflowDp) * 1000f).toLong()

    fun remainingMs(overflowDp: Float): Long =
        if (overflowDp <= 0f) 0L else scrollMs(overflowDp) + END_PAUSE_MS
}
