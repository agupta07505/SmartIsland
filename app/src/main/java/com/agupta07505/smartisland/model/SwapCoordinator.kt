/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.ceil
import kotlin.math.sqrt

// The UI observes busy and back through Compose state. Direction is read directly during the swap.
class SwapCoordinator(private val onBackChange: (Boolean) -> Unit = {}) {
    enum class Phase { Idle, Preparing, Running, Settling, Aborting }

    /** Layout flags that must not change while a swap is running. */
    data class Flags(val tapSwapAllowed: Boolean, val twoIslandsDrawn: Boolean)

    var phase: Phase = Phase.Idle
        private set

    var reverse: Boolean = false
        private set

    var back: Boolean = false
        private set

    private var latched = Flags(tapSwapAllowed = false, twoIslandsDrawn = false)

    val busy: Boolean get() = phase != Phase.Idle

    val refusesFlip: Boolean get() = phase == Phase.Settling || phase == Phase.Aborting

    fun flags(live: Flags, busy: Boolean): Flags {
        if (!busy) latched = live
        return latched
    }

    fun begin(): Boolean {
        if (busy) return false
        phase = Phase.Preparing
        return true
    }

    fun turnAround() = setReverse(!reverse)

    fun setReverse(value: Boolean) {
        if (busy) reverse = value
    }

    fun setBack(value: Boolean) {
        if (!busy || back == value) return
        back = value
        onBackChange(value)
    }

    fun run() {
        if (phase == Phase.Preparing) phase = Phase.Running
    }

    fun settle() {
        if (phase == Phase.Preparing || phase == Phase.Running) phase = Phase.Settling
    }

    fun abort() {
        if (busy) phase = Phase.Aborting
    }

    fun end() {
        phase = Phase.Idle
        reverse = false
        if (back) {
            back = false
            onBackChange(false)
        }
    }
}

object SwapPickerPolicy {
    data class Result(val open: Boolean, val anchorKey: String?)

    /**
     * The swap changes the selected card midway, which shouldn't close the picker. After it settles
     * the picker stays only if the new card has siblings, and is re-anchored to that card.
     */
    fun resolve(
        open: Boolean,
        anchorKey: String?,
        shownKey: String?,
        hasSiblings: Boolean,
        swapBusy: Boolean,
        settledNow: Boolean
    ): Result {
        if (!open) return Result(false, null)
        if (swapBusy) return Result(true, anchorKey ?: shownKey)
        val anchor = if (settledNow || anchorKey == null) shownKey else anchorKey
        return Result(anchor == shownKey && hasSiblings, anchor)
    }

    /** A picker requested mid-swap waits until it settles, so it never opens on a card that's still changing. */
    fun opensAfterSwap(pending: Boolean, settledNow: Boolean, expanded: Boolean, hasSiblings: Boolean): Boolean =
        pending && settledNow && expanded && hasSiblings
}

// Stopping a swap mid-flight: let it glide to the nearest rest instead of snapping
object SwapAbort {
    const val GRACE_MS = 600L

    fun settlesBack(progress: Float): Boolean = progress < 0.5f
}

// One accepted tap per touch during a swap. The island's gesture flips on touch-down, and the
// cutout's click from that same touch mustn't flip it a second time. Tied to the touch itself, not a timer.
class SwapTouchGuard {
    private var armed = false

    fun down(flipped: Boolean) {
        armed = flipped
    }

    fun claimClick(): Boolean {
        val same = armed
        armed = false
        return same
    }

    fun reset() {
        armed = false
    }

    companion object {
        // Taps aimed at the cutout land between swaps, when the shell is already full size, and would open the
        // event's app. So the card ignores touches briefly after a swap ends.
        const val POST_SWAP_LOCK_MS = 250L

        fun cardLocked(swapActive: Boolean, nowMs: Long, lockUntilMs: Long): Boolean =
            swapActive || nowMs < lockUntilMs
    }
}

// Speeds the swap spring up on rapid taps so the shell still covers most of the card between them.
// A lone tap uses the normal collapse timing. The boost is applied when a tap retargets the spring;
// Compose keeps position and velocity across a new spec, so only the acceleration changes.
class SwapBurst {
    private var lastTapMs = NONE
    private var gapMs = NONE.toFloat()

    fun tap(nowMs: Long) {
        gapMs = if (lastTapMs == NONE || nowMs - lastTapMs >= DECAY_MS) NONE.toFloat() else {
            val gap = (nowMs - lastTapMs).toFloat()
            if (gapMs < 0f) gap else gapMs * (1f - GAP_SMOOTHING) + gap * GAP_SMOOTHING
        }
        lastTapMs = nowMs
    }

    fun boostAt(nowMs: Long): Float {
        if (lastTapMs == NONE || gapMs < 0f || nowMs - lastTapMs >= DECAY_MS) return 1f
        return boostForGap(gapMs)
    }

    companion object {
        private const val NONE = -1L

        // Original tuning was 620f / 300f, measured against the reference recording.
        const val MAX_BOOST = SwipeFeel.TAP_SWAP_STIFFNESS / CollapseMotion.STIFFNESS

        const val SLOW_GAP_MS = 270f
        const val FAST_GAP_MS = 130f

        const val DECAY_MS = 300L

        const val GAP_SMOOTHING = 0.5f

        fun boostForGap(gapMs: Float): Float {
            val t = ((SLOW_GAP_MS - gapMs) / (SLOW_GAP_MS - FAST_GAP_MS)).coerceIn(0f, 1f)
            return 1f + (MAX_BOOST - 1f) * t * t * (3f - 2f * t)
        }
    }
}

// Everything in a swap is driven by spring progress, so the only real milliseconds are the safety
// limits (watchdog, abort grace). Those are counted in spring settle times rather than fixed ms,
// since settle time scales with 1/sqrt(stiffness): Relaxed speed plus a slowed system animator
// scale can outlast any fixed limit.
object SwapClock {
    private const val SETTLE_X = 4.7439f

    const val WATCHDOG_SETTLES = 5.45f

    const val GRACE_SETTLES = 2f

    const val MIN_FRAMES = 8
    private const val FRAME_MS = 1000f / 60f

    // boosting past this would settle in under MIN_FRAMES frames
    val MAX_BOOSTED_STIFFNESS: Float = (SETTLE_X / (MIN_FRAMES * FRAME_MS / 1000f)).let { it * it }

    // Never goes below base, so a spring already stiffer than the cap (animations off) is left alone
    fun boosted(base: Float, boost: Float): Float = maxOf(base, minOf(base * boost, MAX_BOOSTED_STIFFNESS))

    fun settleMs(stiffness: Float): Long =
        ceil(SETTLE_X / sqrt(stiffness.coerceAtLeast(MIN_STIFFNESS)) * 1000f).toLong()

    /** Max swap runtime before it's dropped; never under [TapSwap.MAX_SWAP_MS]. */
    fun watchdogMs(stiffness: Float): Long =
        maxOf(TapSwap.MAX_SWAP_MS, (settleMs(stiffness) * WATCHDOG_SETTLES).toLong())

    fun graceMs(stiffness: Float): Long =
        maxOf(SwapAbort.GRACE_MS, (settleMs(stiffness) * GRACE_SETTLES).toLong())

    private const val MIN_STIFFNESS = 0.01f
}

// Card and sibling picker trade places on a single progress value (0 = card, 1 = picker).
// Closing mid-open just reverses in place, so both directions mirror each other.
object PickerSwap {
    const val OPEN_MS = 280

    const val CLOSE_MS = 240

    const val SPLIT_OPEN_STIFFNESS = 400f
    const val SPLIT_CLOSE_STIFFNESS = 520f

    const val CARD_GONE_AT = 0.6f
    const val PICKER_IN_FROM = 0.3f

    const val END_SCALE = 0.92f

    fun cardAlpha(progress: Float): Float = 1f - (progress / CARD_GONE_AT).coerceIn(0f, 1f)

    fun pickerAlpha(progress: Float): Float = ((progress - PICKER_IN_FROM) / (1f - PICKER_IN_FROM)).coerceIn(0f, 1f)

    fun cardScale(progress: Float): Float = 1f - (1f - END_SCALE) * progress.coerceIn(0f, 1f)

    fun pickerScale(progress: Float): Float = END_SCALE + (1f - END_SCALE) * progress.coerceIn(0f, 1f)

    fun cardLayerShown(progress: Float, pickerOpen: Boolean): Boolean = !pickerOpen || progress < 0.999f

    fun pickerLayerShown(progress: Float, pickerOpen: Boolean): Boolean = pickerOpen || progress > 0.001f

    fun lengthMs(opening: Boolean): Int = if (opening) OPEN_MS else CLOSE_MS
}
