/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.roundToInt

/**
 * Blur of the card content during a cutout tap swap. No clock of its own, the blur is a function of the content's
 * alpha (itself a function of swap progress), so it crosses with the same windows as the fades, follows a reversal
 * for free and scales with the speed setting.
 */
object SwapBlur {
    /**
     * The swap blurs the card content as it shrinks into and grows out of the cutout. Below Android 12 the swap
     * crossfades by alpha alone
     */
    const val ENABLED = true

    /**
     * Lets [SwapBlurGate] drop the blur for the session after repeated janky swaps.
     * TODO: off for now. A few late frames in a row (cold start, burst of taps) tripped it and the swap lost its blur
     * for good, so alpha should only be the fallback for devices without RenderEffect. Bring it back once the jank
     * check tells a cold path from a device that really can't blur
     */
    const val DISABLE_ON_JANK = false

    /**
     * Blur (sigma) at full strength, in dp of the card's own pixels. Kept small: it only has to soften the text while
     * it is squeezed and fades, so the morph is not read, not turn the card into a smear
     */
    const val MAX_RADIUS_DP = 2.5f

    /** Blur needs a RenderEffect (Android 12); below it the swap crossfades by alpha alone. */
    const val MIN_SDK = 31

    /** Blur is set in whole pixels, so a handful of effect objects serve every frame (no allocation per frame). */
    const val MAX_LEVEL = 64

    const val MIN_LEVEL = 1

    private const val GAIN = 1.5f

    fun supported(sdk: Int): Boolean = sdk >= MIN_SDK

    fun strength(alpha: Float): Float {
        val t = ((1f - alpha.coerceIn(0f, 1f)) * GAIN).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun radiusPx(alpha: Float, maxPx: Float): Float = maxPx * strength(alpha)

    /** Whole-pixel level of [radiusPx], 0 when there is nothing to draw. */
    fun level(radiusPx: Float): Int {
        val l = radiusPx.roundToInt()
        return if (l < MIN_LEVEL) 0 else minOf(l, MAX_LEVEL)
    }
}

/**
 * Turns the blur off for good when it is seen to cost frames (only with [disableOnJank], see
 * [SwapBlur.DISABLE_ON_JANK]): a swap that is blurred and drops frames three swaps in
 * a row stays alpha-only for the rest of the session. A frame is late past [LATE_FRAME_NS] (a missed frame at 60 Hz;
 * a high refresh rate runs at a lower rate than its peak, so only real jank counts). Pauses between two runs of the
 * swap's spring (a flip) are not frames: [restart] forgets the last time.
 */
class SwapBlurGate(private val disableOnJank: Boolean = SwapBlur.DISABLE_ON_JANK) {
    var enabled: Boolean = true
        private set

    private var last = -1L
    private var late = 0
    private var frames = 0
    private var badSwaps = 0

    fun beginSwap() {
        last = -1L
        late = 0
        frames = 0
    }

    fun restart() {
        last = -1L
    }

    fun frame(nowNs: Long) {
        if (!enabled) return
        if (last >= 0L) {
            frames++
            if (nowNs - last > LATE_FRAME_NS) late++
        }
        last = nowNs
    }

    fun endSwap(blurred: Boolean) {
        if (!disableOnJank || !enabled || !blurred || frames < MIN_FRAMES) return
        badSwaps = if (late >= LATE_FRAMES_BAD) badSwaps + 1 else 0
        if (badSwaps >= BAD_SWAPS_TO_DISABLE) enabled = false
    }

    companion object {
        const val LATE_FRAME_NS = 25_000_000L
        const val LATE_FRAMES_BAD = 2
        const val MIN_FRAMES = 4
        const val BAD_SWAPS_TO_DISABLE = 3
    }
}
