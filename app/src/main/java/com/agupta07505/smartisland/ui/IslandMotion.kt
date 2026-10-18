/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.ui

import kotlin.math.min
import kotlin.math.sqrt

/**
 * How the island's size springs behave.
 *
 * The physical presets are a spring-mass-damper with unit mass: force = -k * (x - target) - d * v, with
 * stiffness `k` and damping `d`. Compose's `spring()` takes the same stiffness and a damping *ratio*, which is
 * `d / (2 * sqrt(k * mass))`, so [dampingRatio] converts one to the other and the island moves exactly as the
 * reference simulation does.
 *
 *  - [Bouncy]: k 150, d 12. Highly elastic.
 *  - [Snappy]: k 300, d 22. Fast and aggressive.
 *  - [Critical]: k 180, d 27.5. Near critical damping: expands smoothly, no overshoot.
 *  - [Loose]: k 60, d 5. Slow and floating.
 *
 * [Standard] keeps the island's original Apple-style tuning (a light spring to open, a critically damped
 * one to close) and has no k/d of its own.
 */
enum class SpringPreset(val id: String, val stiffness: Float, val damping: Float) {
    Standard("standard", 0f, 0f),
    Bouncy("bouncy", 150f, 12f),
    Snappy("snappy", 300f, 22f),
    Critical("critical", 180f, 27.5f),
    Loose("loose", 60f, 5f);

    /** True for the presets defined by [stiffness] and [damping]; [Standard] is not one. */
    val isPhysical: Boolean get() = stiffness > 0f

    /** Compose's damping ratio for this preset (1.0 is critical damping); 1.0 for [Standard]. */
    val dampingRatio: Float get() = if (isPhysical) dampingRatioOf(stiffness, damping) else 1f

    companion object {
        fun fromId(id: String?): SpringPreset = entries.firstOrNull { it.id == id } ?: Standard

        /** `d / (2 * sqrt(k))` for a unit mass. */
        fun dampingRatioOf(stiffness: Float, damping: Float): Float = damping / (2f * sqrt(stiffness))
    }
}

/**
 * The faint glow that flares while the island is moving.
 *
 * It only appears when the island's speed is above [SPEED_THRESHOLD] (dp per second), and its blur radius
 * scales with that speed: `min(20, speed * 0.08)`. Fast expansions flare brightest and the glow settles back
 * to nothing as the island comes to rest.
 */
object KineticGlow {
    const val SPEED_THRESHOLD = 5f
    const val MAX_BLUR_DP = 20f
    const val BLUR_PER_SPEED = 0.08f

    /** Blur radius in dp for an island moving at [speed] dp/s (sign ignored); 0 at or below the threshold. */
    fun blurRadius(speed: Float): Float {
        val s = kotlin.math.abs(speed)
        return if (s > SPEED_THRESHOLD) min(MAX_BLUR_DP, s * BLUR_PER_SPEED) else 0f
    }
}

/**
 * Ghost-animation prevention (Rule_Book §7.3). When the overlay comes up after a (re)start with alerts already
 * restored, the island must not spring open from nothing: it snaps straight to the static bounds of what it
 * shows. Geometry snaps while the notification listener's initial sync is still in flight (at most
 * [MAX_WAIT_MS] after the first frame) and for [SETTLE_MS] after whichever of the first frame or that sync
 * came last; after that the normal springs apply.
 */
object RestoreSnap {
    /** Longest the overlay waits, snapping, for a sync that may never come (no notification access). */
    const val MAX_WAIT_MS = 5_000L

    /** How long after the first frame (or the sync) restored alerts still land without a spring. */
    const val SETTLE_MS = 600L

    /**
     * [firstFrameAtMs] is when the overlay first composed, [syncedAtMs] when the listener finished its initial
     * sync (null while it has not). Returns true while geometry should snap.
     */
    fun shouldSnap(nowMs: Long, firstFrameAtMs: Long, syncedAtMs: Long?): Boolean =
        nowMs < snapUntil(firstFrameAtMs, syncedAtMs)

    /** The moment snapping stops, for scheduling the switch back to springs. */
    fun snapUntil(firstFrameAtMs: Long, syncedAtMs: Long?): Long =
        if (syncedAtMs == null) firstFrameAtMs + MAX_WAIT_MS
        else minOf(maxOf(firstFrameAtMs, syncedAtMs) + SETTLE_MS, firstFrameAtMs + MAX_WAIT_MS)
}
