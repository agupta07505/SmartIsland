/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs
import kotlin.math.exp

/**
 * Numbers behind a card step (preview, reader, picture) that follows the finger: a vertical drag moves progress 0..1,
 * lifting past [COMMIT_PROGRESS] finishes the step, short of it goes back
 */
object StepDrag {
    /** Finger travel (dp), past the tap slop, for the step to be fully open. */
    const val RANGE_DP = 110f

    const val COMMIT_PROGRESS = 0.4f

    /**
     * After the lift the shell keeps the dragged height until its spring is this close (dp), or [HANDOVER_MAX_MS] has
     * passed
     */
    const val HANDOVER_TOLERANCE_DP = 1.5f
    const val HANDOVER_MAX_MS = 300L

    const val SETTLE_MS = 240

    const val GO_BACK_SETTLE_MS = 160

    const val MAX_PROGRESS = 1f

    fun progress(signedDragPx: Float, slopPx: Float, rangePx: Float): Float =
        if (rangePx <= 0f) 0f else ((signedDragPx - slopPx) / rangePx).coerceIn(0f, 1f)

    fun commits(progress: Float): Boolean = progress >= COMMIT_PROGRESS

    /**
     * Light smoothing of the finger-driven progress (about 75 ms) so uneven touch samples do not make the height
     * stepped
     */
    const val SMOOTH_TAU_MS = 75f

    const val SMOOTH_SNAP = 0.002f

    /**
     * 1 frame of the ease: [current] moves toward [target] by `1 - e^(-dt / tau)` of the gap. Frame-rate independent,
     * never overshoots, an idle finger lets it arrive exactly
     */
    fun smoothed(current: Float, target: Float, dtMs: Float): Float {
        if (dtMs <= 0f) return current
        val gap = target - current
        if (abs(gap) <= SMOOTH_SNAP) return target
        return current + gap * (1f - exp(-dtMs / SMOOTH_TAU_MS))
    }

    /**
     * Old and new step trade places within the first [CROSSFADE_END] of the move, alphas sum to 1 so the card never
     * goes see-through
     */
    const val CROSSFADE_END = 0.4f

    fun oldStepAlpha(progress: Float): Float = 1f - newStepAlpha(progress)

    fun newStepAlpha(progress: Float): Float = (progress / CROSSFADE_END).coerceIn(0f, 1f)

    fun heightDp(fromDp: Float, targetDp: Float, progress: Float): Float =
        fromDp + (targetDp - fromDp) * progress.coerceIn(0f, 1f)
}

/**
 * 1 buzz per step drag: at the far end, or at the lift for a drag past [StepDrag.COMMIT_PROGRESS] that never reached
 * it. Taps buzz once at the tap. Make a new one per touch
 */
class StepDragHaptic {
    private var played = false

    fun onProgress(progress: Float): Boolean {
        if (played || progress < StepDrag.MAX_PROGRESS) return false
        played = true
        return true
    }

    fun onLift(committed: Boolean): Boolean {
        val fire = committed && !played
        played = true
        return fire
    }
}
