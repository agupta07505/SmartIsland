/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign

// Shared spring constants for swipe feel. Stretch, lean and rubber-band numbers live next to the
// maths that uses them instead: SwipeDownPull below, RUBBER_BAND_* in ExpandedSwipe.kt.
object SwipeFeel {
    // Springs that drop a swipe's temporary state (arm swell, dead-zone shrink, lock cue).
    // Critically damped so they stop dead instead of wobbling.
    const val RETURN_DAMPING_RATIO = 1f

    const val RETURN_STIFFNESS = 400f

    // For a drag coming back to rest (shell, content, lean, pull, card). About a third of StiffnessMedium.
    // NOTE: that's a higher number than RETURN_STIFFNESS, so look at the damping ratio where it's used
    // before assuming this one is the softer of the two.
    const val EASE_BACK_STIFFNESS = 550f

    // Base for the cutout tap swap, the speed setting scales it. Tuned against the reference recording
    // (~0.19 s to 95% at Normal); the plain collapse spring was too slow and made the swap feel heavy.
    const val TAP_SWAP_STIFFNESS = 620f
    const val TAP_SWAP_DAMPING_RATIO = 1f

    const val PILL_LEAN_Y_MAX_DP = 6f

    const val SWITCH_SQUASH_SCALE = 0.96f
    const val SWITCH_RETURN_DAMPING_RATIO = 0.8f
    const val SWITCH_RETURN_STIFFNESS = 600f
}

object SwipeSideways {
    // The shell's touch area moves with the lean, so this also bounds how far it shifts
    const val MAX_LEAN_DP = 60f

    const val FOLLOW = 0.5f

    const val STAY_SHARE = 0.4f

    // Stretch along the swipe, squeeze a touch vertically so it looks like volume is kept
    const val STRETCH_X = 0.02f
    const val SQUEEZE_Y = 0.008f

    /** Signed lean in px for a drag of [dragX]. [share] is 1 for a card change, [STAY_SHARE] otherwise. */
    fun leanPx(dragX: Float, density: Float, share: Float = 1f): Float =
        softFollow(dragX, FOLLOW, maxLeanPx(density) * share)

    fun maxLeanPx(density: Float): Float = MAX_LEAN_DP * density

    fun progress(leanPx: Float, maxLeanPx: Float): Float =
        if (maxLeanPx <= 0f) 0f else (abs(leanPx) / maxLeanPx).coerceIn(0f, 1f)

    fun scaleX(progress: Float): Float = 1f + STRETCH_X * progress
    fun scaleY(progress: Float): Float = 1f - SQUEEZE_Y * progress

    // Scaling is around the centre, so without this the shell would grow on both sides.
    // Shifting by half the extra width pins the edge opposite the swipe.
    fun edgeAnchorShiftPx(scaleX: Float, widthPx: Float, lean: Float): Float = sign(lean) * (scaleX - 1f) * widthPx / 2f

    fun totalLeanPx(dragLean: Float, releaseLean: Float, swapProgress: Float): Float =
        dragLean + releaseLean * (1f - swapProgress.coerceIn(0f, 1f))

    // The two cards move in lockstep, exactly 1 card width apart, so no gap opens mid-swap.
    // Incoming slides from sign*width to 0, outgoing from 0 to -sign*width.
    fun incomingX(sign: Int, widthPx: Float, swapProgress: Float): Float =
            sign * widthPx * (1f - swapProgress.coerceIn(0f, 1f))
    fun outgoingX(sign: Int, widthPx: Float, swapProgress: Float): Float =
            -sign * widthPx * swapProgress.coerceIn(0f, 1f)
}

object SwipeGhost {
    fun layerToPark(last: Int, keys: Array<String?>, from: String?): Int {
        if (from == null || last !in 0..1) return -1
        return when {
            keys[last] == from -> last
            // last frame already switched to the new card, so the old one is on the other layer
            keys[1 - last] == from -> 1 - last
            else -> -1
        }
    }
}

// Swipe down isn't configurable: it expands the collapsed pill and does nothing on the expanded island
// (cards with expandable text or an image still open as usual).
val FIXED_PILL_SWIPE_DOWN_ACTION: SwipeAction get() = SwipeAction.Expand
val FIXED_EXPANDED_SWIPE_DOWN_ACTION: SwipeAction get() = SwipeAction.None

enum class SwipeAction(override val id: String) : StableId {
    DismissCurrent("DismissCurrent"),
    DismissAll("DismissAll"),
    Collapse("Collapse"),
    OpenApp("OpenApp"),
    FloatingWindow("FloatingWindow"),
    NotificationShade("NotificationShade"),
    NextPrevious("NextPrevious"),
    NextNotification("NextNotification"),
    PreviousNotification("PreviousNotification"),
    NextEvent("NextEvent"),
    PreviousEvent("PreviousEvent"),
    NextSibling("NextSibling"),
    PreviousSibling("PreviousSibling"),
    NextTrack("NextTrack"),
    PreviousTrack("PreviousTrack"),
    PlayPause("PlayPause"),
    Expand("Expand"),
    TogglePill("TogglePill"),
    None("None");

    companion object {
        fun fromId(id: String?, default: SwipeAction = None): SwipeAction = enumById(id, default, ignoreCase = true)
    }
}

object SwipeDownPull {
    const val OVERSHOOT = 0.15f

    // The pill is tiny so it needs a bigger stretch to read; the expanded card is large,
    // a fraction of a percent is already visible there.
    const val PILL_STRETCH_X = 0.05f
    const val PILL_STRETCH_Y = 0.12f
    const val EXPANDED_STRETCH_X = 0.006f
    const val EXPANDED_STRETCH_Y = 0.015f

    const val EXPANDED_DOWN_MAX_DP = 6f

    fun expandedDownOffsetPx(dragY: Float, density: Float): Float {
        if (dragY <= 0f) return 0f
        val maxPx = EXPANDED_DOWN_MAX_DP * density
        return maxPx * (1f - exp(-dragY / (4f * maxPx)))
    }

    fun progress(dragY: Float, slopPx: Float, thresholdPx: Float): Float {
        if (dragY <= slopPx) return 0f
        val span = (thresholdPx - slopPx).coerceAtLeast(1f)
        val p = (dragY - slopPx) / span
        return if (p <= 1f) p else 1f + OVERSHOOT * (1f - exp(-2f * (p - 1f)))
    }

    const val HANDLE_SQUEEZE_X = 0.045f
    const val HANDLE_STRETCH_Y = 0.10f
    fun handleScaleX(progress: Float): Float = 1f - HANDLE_SQUEEZE_X * progress
    fun handleScaleY(progress: Float): Float = 1f + HANDLE_STRETCH_Y * progress

    fun pillScaleX(progress: Float): Float = 1f + PILL_STRETCH_X * progress
    fun pillScaleY(progress: Float): Float = 1f + PILL_STRETCH_Y * progress
    fun expandedScaleX(progress: Float): Float = 1f + EXPANDED_STRETCH_X * progress
    fun expandedScaleY(progress: Float): Float = 1f + EXPANDED_STRETCH_Y * progress

    // Same centre-scaling issue as SwipeSideways.edgeAnchorShiftPx: half the added height
    // keeps the top edge put so the stretch only goes down.
    fun topAnchorShiftPx(scaleY: Float, heightPx: Float): Float = (scaleY - 1f) * heightPx / 2f

    fun deadShrinkWeight(progress: Float): Float = (1f - progress).coerceIn(0f, 1f)
}

enum class PillSwipeDirection { Left, Right, Up, Down }

enum class GestureSensitivity(override val id: String, val distanceScale: Float, val dominance: Float) : StableId {
    LOW("low", 1.45f, 1.6f),
    NORMAL("normal", 1f, 1f),
    HIGH("high", 0.7f, 1f);

    fun scaled(dp: Float): Float = dp * distanceScale

    fun isHorizontal(dx: Float, dy: Float): Boolean = abs(dx) > abs(dy) * dominance

    fun isVertical(dx: Float, dy: Float): Boolean = abs(dy) >= abs(dx) * dominance

    fun pillSwipe(dx: Float, dy: Float, threshold: Float, horizontalThreshold: Float = threshold): PillSwipeDirection? =
            when {
        abs(dx) >= horizontalThreshold && isHorizontal(dx, dy) ->
            if (dx < 0f) PillSwipeDirection.Left else PillSwipeDirection.Right
        abs(dy) >= threshold && isVertical(dx, dy) ->
            if (dy < 0f) PillSwipeDirection.Up else PillSwipeDirection.Down
        else -> null
    }

    companion object {
        const val ID_LOW = "low"
        const val ID_NORMAL = "normal"
        const val ID_HIGH = "high"

        val VALID_IDS: Set<String> = entries.map { it.id }.toSet()

        fun fromId(id: String?): GestureSensitivity = enumById(id, NORMAL)
    }
}
