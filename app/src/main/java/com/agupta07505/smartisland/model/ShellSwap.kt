/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

// Swapping the expanded event with one tapped on the cutout. The spring owns the progress value;
// this only tracks direction, flips, one queued tap and when the swap is finished.
object ShellSwap {

    const val SETTLE_EPS = 0.001f

    data class State(
        val expanded: String,
        val incoming: String? = null,
        val target: Float = 0f,
        val pending: String? = null
    ) {
        val swapping: Boolean get() = incoming != null
    }

    fun tap(s: State, key: String): State {
        val incoming = s.incoming
        if (incoming == null) {
            return if (key == s.expanded) s else s.copy(incoming = key, target = 1f)
        }
        return when (key) {
            incoming -> if (s.target == 1f) s else s.copy(target = 1f, pending = null)
            s.expanded -> if (s.target == 0f) s else s.copy(target = 0f, pending = null)
            else -> s.copy(pending = key)
        }
    }

    fun settled(s: State, progress: Float): Boolean {
        if (!s.swapping) return false
        return if (s.target == 1f) progress >= 1f - SETTLE_EPS else progress <= SETTLE_EPS
    }

    fun settle(s: State, progress: Float): State {
        if (!settled(s, progress)) return s
        val base = if (s.target == 1f) State(expanded = s.incoming!!) else State(expanded = s.expanded)
        val next = s.pending
        return if (next != null && next != base.expanded) base.copy(incoming = next, target = 1f) else base
    }

    enum class TapEffect {
        None,
        Retarget,
        /** A third event was tapped, queued until the swap settles. */
        Remember
    }

    fun effect(before: State, after: State): TapEffect = when {
        !before.swapping -> TapEffect.None
        after.target != before.target -> TapEffect.Retarget
        after.pending != before.pending -> TapEffect.Remember
        else -> TapEffect.None
    }

    fun nextAfterSettle(s: State): String? =
        settle(s, if (s.target == 1f) 1f else 0f).incoming

    /** Reverses the swap's direction; any queued tap is dropped. */
    fun turnedAround(s: State): State = s.copy(target = if (s.target == 1f) 0f else 1f, pending = null)

    sealed interface Intent {
        data object None : Intent

        data object Flip : Intent

        data class Third(val after: State) : Intent
    }

    /**
     * [tappedKey] is null when the touch landed on the island, or on a cutout with no known event;
     * both reverse the swap. A tap means "I want this event open", not "toggle", so tapping the event
     * we're already heading to does nothing and a double tap can't undo itself.
     */
    fun intentOf(s: State?, tappedKey: String?): Intent {
        if (s == null || tappedKey == null) return Intent.Flip
        val after = tap(s, tappedKey)
        return when (effect(s, after)) {
            TapEffect.Retarget -> Intent.Flip
            TapEffect.Remember -> Intent.Third(after)
            TapEffect.None -> Intent.None
        }
    }

    fun withoutPending(s: State): State = if (s.pending == null) s else s.copy(pending = null)
}

object TwoShellSwap {
    // progress is already eased by the spring, don't ease twice
    fun grow(p: Float): Float = p.coerceIn(0f, 1f)

    fun ghostOpen(p: Float): Float = 1f - grow(p)

    fun scaleAt(minScale: Float, open: Float): Float = minScale + (1f - minScale) * open.coerceIn(0f, 1f)

    fun radiusAt(cutoutRadius: Float, cardRadius: Float, open: Float): Float =
        cutoutRadius + (cardRadius - cutoutRadius) * open.coerceIn(0f, 1f)

    fun shiftAt(delta: Float, open: Float): Float = delta * (1f - open.coerceIn(0f, 1f))

    // Alpha for the shell underneath the cutout. A plain crossfade of 2 layers dips in opacity
    // midway (0.5 over 0.5 isn't 1.0), so this solves for the under-layer alpha that keeps it opaque.
    fun holdUnder(cutoutAlpha: Float, opacity: Float): Float {
        val c = cutoutAlpha.coerceIn(0f, 1f)
        if (c <= 0f) return 1f
        if (opacity >= 0.999f) return if (c >= 0.999f) 0f else 1f
        val o = opacity.coerceAtLeast(0.001f)
        return ((1f - (1f - o) / (1f - o * c)) / o).coerceIn(0f, 1f)
    }

    // The ghost already shows the outgoing look, so the old card never draws on the real shell
    fun incomingShellOutgoingAlpha(): Float = 0f

    // "ready" = the outgoing card's last frame is parked and the ghost is drawing it. Until then the
    // real shell is just the normal full-size island. On "back" the 2 shells trade roles.

    /**
     * Real shell's open amount. Stays full size until the ghost is ready, so it doesn't dip toward the
     * cutout while the incoming card composes (and doesn't reverse when the ghost takes over). If no
     * card ever gets parked you get a plain crossfade on the full shell, never an empty shrunken one.
     */
    fun realOpen(ready: Boolean, back: Boolean, p: Float): Float = when {
        !ready -> 1f
        back -> ghostOpen(p)
        else -> grow(p)
    }

    /**
     * Width's open amount for a given height [open]. Width leads height: collapsing shells narrow first
     * and may undershoot the cutout width a bit (same as the plain collapse); growing ones widen first
     * but are clamped at the card width. [widthDamping] = 1 moves both axes together.
     */
    fun widthOpen(collapsing: Boolean, open: Float, widthDamping: Float = CollapseMotion.WIDTH_DAMPING): Float {
        val o = open.coerceIn(0f, 1f)
        return if (collapsing) 1f - CollapseMotion.widthTravel(1f - o, widthDamping)
        else CollapseMotion.widthTravel(o, widthDamping).coerceAtMost(1f)
    }

    fun ghostOpenFor(back: Boolean, p: Float): Float = if (back) grow(p) else ghostOpen(p)

    fun ghostAlphaFor(back: Boolean, p: Float, opacity: Float = 1f): Float =
        holdUnder(cutoutAlpha(back, p), opacity)

    fun ghostContentAlpha(back: Boolean, p: Float, from: Float = TapSwap.OUTGOING_FADE_START): Float = when {
        back -> 0f
        GHOST_CONTENT_FADE_END <= from -> if (p >= GHOST_CONTENT_FADE_END) 0f else 1f
        else -> 1f - ((p - from) / (GHOST_CONTENT_FADE_END - from)).coerceIn(0f, 1f)
    }

    const val GHOST_CONTENT_FADE_END = 0.62f

    fun realContentAlpha(ready: Boolean, back: Boolean, stillOutgoing: Boolean, p: Float): Float = when {
        !ready -> if (back || stillOutgoing) 1f else TapSwap.incomingContentAlpha(p)
        back -> TapSwap.outgoingContentAlpha(p)
        stillOutgoing -> incomingShellOutgoingAlpha()
        else -> TapSwap.incomingContentAlpha(p)
    }

    fun cutoutAlpha(back: Boolean, p: Float): Float = TapSwap.cutoutAlpha(if (back) 1f - p.coerceIn(0f, 1f) else p)
}

object TapSwap {
    // Ids are for the *spot* that was tapped, not whichever widget happens to be drawn there.
    // SECONDARY: the round bubble or picker button (never holds the pill's spot).
    // PILL_SPOT: beside the open card, taken by either the secondary or the tertiary pill.
    const val CUTOUT_SECONDARY = 1
    const val CUTOUT_PILL_SPOT = 2
    const val CUTOUT_EDGE_EAR = 3
    const val CUTOUT_EDGE_THIRD = 4
    const val CUTOUT_EDGE_PILL = 5

    const val MIN_SHELL_SCALE = 0.02f

    // Fade windows for the live card on the real shell. The gap between outgoing and incoming is
    // deliberate, backAfter relies on it.
    const val OUTGOING_FADE_START = 0.0f
    const val OUTGOING_FADE_END = 0.3f
    const val INCOMING_FADE_START = 0.4f
    const val INCOMING_FADE_END = 0.75f

    const val ACTIONS_FADE_START = 0.7f
    const val ACTIONS_FADE_END = 0.92f

    // safety net: a stuck swap would leave things hidden / eating touches
    const val MAX_SWAP_MS = 1500L

    // Real cutout is drawn again from here and the ghost dissolves into it. Deliberately before the
    // incoming card is fully open so you see the old island land as the cutout pill.
    const val CUTOUT_BACK_PROGRESS = 0.7f

    fun secondaryCutoutId(holdsPillSpot: Boolean): Int = if (holdsPillSpot) CUTOUT_PILL_SPOT else CUTOUT_SECONDARY

    fun hidesWithSwap(swapRunning: Boolean, tappedId: Int, widgetId: Int): Boolean = swapRunning && tappedId == widgetId

    /** Tap swaps events only when nothing else is mid-motion; otherwise the card just changes in place. */
    fun eligible(
        expanded: Boolean,
        fromKey: String?,
        fromIndex: Int,
        toIndex: Int,
        cardCount: Int,
        cardSwapRunning: Boolean,
        cardDragPx: Float,
        swapRunning: Boolean
    ): Boolean =
        expanded && fromKey != null && toIndex != fromIndex && toIndex in 0 until cardCount &&
            !cardSwapRunning && cardDragPx == 0f && !swapRunning

    fun heldByFlight(expanded: Boolean, flightRunning: Boolean, fromIndex: Int, toIndex: Int, cardCount: Int): Boolean =
        expanded && flightRunning && toIndex != fromIndex && toIndex in 0 until cardCount

    fun heldByPicker(expanded: Boolean, pickerMoving: Boolean, fromIndex: Int, toIndex: Int, cardCount: Int): Boolean =
        expanded && pickerMoving && toIndex != fromIndex && toIndex in 0 until cardCount

    fun replayIndex(heldKey: String?, keys: List<String>, openIndex: Int): Int? {
        val index = keys.indexOf(heldKey ?: return null)
        return if (index < 0 || index == openIndex) null else index
    }

    fun canFlip(active: Boolean, settling: Boolean, p: Float): Boolean =
        active && !settling && p < 0.999f

    // Only flips inside the window where neither card is visible, so the selection change can't be seen
    fun backAfter(back: Boolean, reverse: Boolean, p: Float): Boolean = when {
        !back && reverse && p <= INCOMING_FADE_START && p >= OUTGOING_FADE_END -> true
        back && !reverse && p >= OUTGOING_FADE_END -> false
        else -> back
    }

    const val OLD_CARD_COMPOSE_FRAMES = 2

    enum class SelectStep { None, PickOld, PickNew }

    /**
     * Selection changes besides the one [backAfter] does in the gap. If the swap is reversed while the
     * incoming card is still invisible, select the outgoing event right away so it composes during
     * the trip rather than after landing (the ghost keeps showing the old look meanwhile). Reverse
     * again and the incoming one is picked back.
     */
    fun selectionStep(back: Boolean, reverse: Boolean, oldPicked: Boolean, p: Float): SelectStep = when {
        !back && !reverse && oldPicked -> SelectStep.PickNew
        !back && reverse && !oldPicked && p < INCOMING_FADE_START -> SelectStep.PickOld
        else -> SelectStep.None
    }

    fun composeFramesLeft(framesSincePick: Int): Int =
        (OLD_CARD_COMPOSE_FRAMES - framesSincePick).coerceIn(0, OLD_CARD_COMPOSE_FRAMES)

    fun minScale(cutoutPx: Float, shellPx: Float): Float =
        if (shellPx <= 0f) 1f else (cutoutPx / shellPx).coerceIn(MIN_SHELL_SCALE, 1f)

    fun restExtent(cutoutPx: Float, shellPx: Float): Float = shellPx * minScale(cutoutPx, shellPx)

    fun outgoingContentAlpha(p: Float): Float = 1f - ramp(p, OUTGOING_FADE_START, OUTGOING_FADE_END)

    fun incomingContentAlpha(p: Float): Float = ramp(p, INCOMING_FADE_START, INCOMING_FADE_END)

    fun actionsAlpha(p: Float): Float = ramp(p, ACTIONS_FADE_START, ACTIONS_FADE_END)

    /** Hidden until [CUTOUT_BACK_PROGRESS], then fades in */
    fun cutoutAlpha(p: Float): Float = if (p < CUTOUT_BACK_PROGRESS) 0f else ramp(p, CUTOUT_BACK_PROGRESS, 1f)
}
