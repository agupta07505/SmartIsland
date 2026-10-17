/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.model

/** One stroll of the buddy through the closed pill. */
sealed interface MascotVisitKind {
    /** From its resting place, around the pill and back. */
    data object Lap : MascotVisitKind
    /** In at one end and out at the other, over what the pill shows. */
    data object Pass : MascotVisitKind
    /** Out from behind the camera to its place. */
    data object Home : MascotVisitKind
    /** Out from behind the camera over what the pill shows, to play a reaction where it would rest, and back. */
    data class Cameo(val reaction: MascotReaction) : MascotVisitKind
    /** The last seconds of a countdown, this long. */
    data class Countdown(val totalSec: Float) : MascotVisitKind
    /** A countdown it watched was paused: back behind the camera. */
    data object Retreat : MascotVisitKind
    /** Switched off: a glad hop where it rests and away behind the camera. */
    data object Farewell : MascotVisitKind
    /** Moved to the camera's other side. */
    data object Cross : MascotVisitKind
    /** Switched on: out from behind the camera to its place, or in at a capsule's near end. */
    data object Arrive : MascotVisitKind
    /** Still where it rested as an activity took its place: it plays a reaction there and goes behind the camera. */
    data class Linger(val reaction: MascotReaction) : MascotVisitKind

    /** It ends out of sight, past the pill's end or behind the camera. */
    val endsOutOfSight: Boolean
        get() = when (this) {
            Pass, is Cameo, is Countdown, Retreat, Farewell, is Linger -> true
            Lap, Home, Cross, Arrive -> false
        }

    /** It stands over the timer's mark only, and the reading stays in view. */
    val watchesTimer: Boolean get() = this is Countdown || this == Retreat

    /** Over an activity beside a camera it takes only the wing it stands in. */
    val takesOnlyItsWing: Boolean
        get() = when (this) {
            is Countdown, Retreat, is Cameo, is Linger -> true
            else -> false
        }

    /** The reaction it comes out for, if any. */
    val visitReaction: MascotReaction?
        get() = when (this) {
            is Cameo -> reaction
            is Linger -> reaction
            else -> null
        }
}

data class MascotVisit(
    val id: Long,
    val kind: MascotVisitKind,
    /** The face it shows while it greets. */
    val greeting: MascotMood,
    /** When it began, on the controller's clock. */
    val startSec: Double
) {
    val durationSec: Float get() = com.agupta07505.smartisland.buddy.motion.MascotMotion.duration(kind)
}

/**
 * A walk sampled densely enough to play as given: where the buddy is (x, dp from the pill's left
 * edge), how high a hop lifts it (dp), how much a landing squashes it (positive flattens) and where its
 * eyes look (shares of its size). [keyTimes] are shares of [durationSec], from 0 to 1.
 */
class MascotPath(
    val keyTimes: FloatArray,
    val x: FloatArray,
    val lift: FloatArray,
    val squash: FloatArray,
    val gaze: FloatArray,
    val durationSec: Float,
    /** When its greeting face shows, in seconds from the start. */
    val greeting: ClosedFloatingPointRange<Float> = 0f..0f
) {
    val size: Int get() = keyTimes.size

    /** The pose [timeSec] into the walk, linearly interpolated between samples. */
    fun at(timeSec: Float, out: Sample = Sample()): Sample {
        if (size == 0) return out.set(0f, 0f, 0f, 0f)
        val share = if (durationSec > 0f) (timeSec / durationSec).coerceIn(0f, 1f) else 1f
        // Largest index whose key time is at most `share`.
        var lo = 0
        var hi = size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) ushr 1
            if (keyTimes[mid] <= share) lo = mid else hi = mid - 1
        }
        if (lo >= size - 1) return out.set(x[size - 1], lift[size - 1], squash[size - 1], gaze[size - 1])
        val span = keyTimes[lo + 1] - keyTimes[lo]
        val f = if (span > 0f) ((share - keyTimes[lo]) / span).coerceIn(0f, 1f) else 0f
        return out.set(
            x[lo] + (x[lo + 1] - x[lo]) * f,
            lift[lo] + (lift[lo + 1] - lift[lo]) * f,
            squash[lo] + (squash[lo + 1] - squash[lo]) * f,
            gaze[lo] + (gaze[lo + 1] - gaze[lo]) * f
        )
    }

    class Sample {
        var x = 0f; var lift = 0f; var squash = 0f; var gaze = 0f
        fun set(x: Float, lift: Float, squash: Float, gaze: Float): Sample {
            this.x = x; this.lift = lift; this.squash = squash; this.gaze = gaze
            return this
        }
    }

    companion object {
        val Empty = MascotPath(FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0), 0f)
    }
}
