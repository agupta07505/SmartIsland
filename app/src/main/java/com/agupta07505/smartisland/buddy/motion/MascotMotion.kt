/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.motion

import com.agupta07505.smartisland.buddy.model.MascotPath
import com.agupta07505.smartisland.buddy.model.MascotReaction
import com.agupta07505.smartisland.buddy.model.MascotTrack
import com.agupta07505.smartisland.buddy.model.MascotVisitKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** The buddy's choreography: where it walks for each kind of visit, sampled at 60 frames a second. */
object MascotMotion {
    const val LAP_SEC = 3.6f
    const val PASS_SEC = 3.4f
    const val HOME_SEC = 0.62f
    /** A cameo lands where it would rest this long after it starts, and its reaction plays from there. */
    const val CAMEO_ARRIVAL_SEC = 0.5f
    /** It is back behind the camera as its exit ends, so the visit ends there. */
    const val CAMEO_EXIT_SEC = 0.32f
    const val FAREWELL_SEC = 0.78f
    const val CROSS_SEC = 1.24f
    /** How long before a countdown runs out the buddy comes to watch it. */
    const val COUNTDOWN_LEAD_SEC = 5f
    /** It stays past the end for a beat, under the notice that takes over. */
    const val COUNTDOWN_LINGER_SEC = 0.1f

    /** How long a cameo stays where it landed: its reaction and a beat after it. */
    fun cameoHold(reaction: MascotReaction): Float = reaction.lengthSec + 0.3f

    fun duration(kind: MascotVisitKind): Float = when (kind) {
        MascotVisitKind.Lap -> LAP_SEC
        MascotVisitKind.Pass -> PASS_SEC
        MascotVisitKind.Home, MascotVisitKind.Arrive -> HOME_SEC
        is MascotVisitKind.Cameo -> CAMEO_ARRIVAL_SEC + cameoHold(kind.reaction) + CAMEO_EXIT_SEC
        is MascotVisitKind.Linger -> cameoHold(kind.reaction) + CAMEO_EXIT_SEC
        is MascotVisitKind.Countdown -> kind.totalSec + COUNTDOWN_LINGER_SEC + CAMEO_EXIT_SEC
        MascotVisitKind.Retreat -> CAMEO_EXIT_SEC
        MascotVisitKind.Farewell -> FAREWELL_SEC
        MascotVisitKind.Cross -> CROSS_SEC
    }

    /**
     * A stroll of [kind]. Resting right of the camera, it walks the left side's stroll seen in a mirror.
     */
    fun path(kind: MascotVisitKind, track: MascotTrack): MascotPath {
        // A countdown is watched from the camera's left, over the timer's mark, whichever side it rests on.
        when (kind) {
            is MascotVisitKind.Countdown -> return countdown(track.leftSide, kind.totalSec)
            MascotVisitKind.Retreat -> return retreat(track.leftSide)
            else -> Unit
        }
        if (!track.mirrored) {
            return when (kind) {
                MascotVisitKind.Lap -> lap(track)
                MascotVisitKind.Pass -> pass(track)
                MascotVisitKind.Home -> home(track)
                is MascotVisitKind.Cameo -> cameo(track, cameoHold(kind.reaction))
                is MascotVisitKind.Linger -> linger(track, cameoHold(kind.reaction))
                MascotVisitKind.Farewell -> farewell(track)
                MascotVisitKind.Cross -> cross(track)
                MascotVisitKind.Arrive -> arrive(track)
                is MascotVisitKind.Countdown, MascotVisitKind.Retreat -> MascotPath.Empty
            }
        }
        val left = track.copy(mirrored = false, rest = track.width - track.rest)
        val p = path(kind, left)
        return MascotPath(
            p.keyTimes,
            FloatArray(p.size) { track.width - p.x[it] },
            p.lift,
            p.squash,
            FloatArray(p.size) { -p.gaze[it] },
            p.durationSec,
            p.greeting
        )
    }

    private sealed interface Step {
        data class Stay(val sec: Float, val bounces: Int = 0) : Step
        data class Hop(val to: Float, val sec: Float, val height: Float) : Step
        data class Walk(val to: Float, val sec: Float) : Step
        data class Jump(val to: Float) : Step
    }

    /** From its resting place to the other side of the camera, a hello, out at the far end and back in at the near one. */
    fun lap(track: MascotTrack): MascotPath {
        val size = track.size
        val offstageRight = track.width + size
        val offstageLeft = -size
        val steps = ArrayList<Step>()
        steps += Step.Stay(0.18f)
        val hidden = track.hidden
        if (hidden != null) {
            steps += Step.Hop(track.behindCamera(hidden, 2f), 0.34f, track.hop(0.24f))
            steps += Step.Walk(hidden.endInclusive - size / 2f - 2f * track.scale, 0.50f)
            steps += Step.Hop(track.farSpot, 0.34f, track.hop(0.24f))
        } else {
            steps += Step.Hop(track.farSpot, 0.60f, track.hop(0.3f))
            steps += Step.Stay(0.58f)
        }
        steps += Step.Stay(0.94f, bounces = 2)
        steps += Step.Walk(offstageRight, 0.40f)
        steps += Step.Jump(offstageLeft)
        steps += Step.Hop(track.rest, 0.52f, track.hop(0.26f))
        val path = sample(steps, track.rest, track, LAP_SEC)
        val greetStart = if (hidden == null) 0.78f else 1.30f
        return path.withGreeting(greetStart..(greetStart + 1.0f))
    }

    /** In at the near end, a hello on that side, behind the camera, out at the far end. */
    fun pass(track: MascotTrack): MascotPath {
        val size = track.size
        val steps = ArrayList<Step>()
        steps += Step.Stay(0.20f)
        steps += Step.Hop(track.rest, 0.42f, track.hop(0.26f))
        steps += Step.Stay(0.88f, bounces = 2)
        val hidden = track.hidden
        if (hidden != null) {
            steps += Step.Hop(track.behindCamera(hidden, 2f), 0.34f, track.hop(0.24f))
            steps += Step.Walk(hidden.endInclusive - size / 2f - 2f * track.scale, 0.50f)
            steps += Step.Hop(track.farSpot, 0.34f, track.hop(0.24f))
            steps += Step.Stay(0.22f)
        } else {
            steps += Step.Hop(track.farSpot, 0.60f, track.hop(0.3f))
            steps += Step.Stay(0.80f)
        }
        steps += Step.Walk(track.width + size, 0.40f)
        return sample(steps, -size, track, PASS_SEC).withGreeting(0.55f..1.55f)
    }

    /** Out from behind the camera with a little hop to its place. A capsule has no camera, so it lands where it rests. */
    fun home(track: MascotTrack): MascotPath {
        val hidden = track.hidden
            ?: return sample(
                listOf(Step.Stay(0.08f), Step.Hop(track.rest, 0.36f, track.hop(0.2f))),
                track.rest, track, HOME_SEC
            ).withGreeting(0f..0.4f)
        return sample(
            listOf(Step.Stay(0.06f), Step.Hop(track.rest, 0.42f, track.hop(0.2f))),
            track.behindCamera(hidden, 2f), track, HOME_SEC
        ).withGreeting(0f..0.44f)
    }

    /** Out from behind the camera to where it would rest, [hold] there for its reaction, and back behind the camera. */
    fun cameo(track: MascotTrack, hold: Float): MascotPath {
        val size = track.size
        val duration = CAMEO_ARRIVAL_SEC + hold + CAMEO_EXIT_SEC
        val hidden = track.hidden
            ?: return sample(
                listOf(
                    Step.Stay(0.06f), Step.Hop(track.rest, CAMEO_ARRIVAL_SEC - 0.06f, track.hop(0.3f)),
                    Step.Stay(hold), Step.Walk(track.width + size, CAMEO_EXIT_SEC)
                ),
                -size, track, duration
            )
        // Just out of sight, so it shows the moment it moves and is gone as it lands.
        val behind = track.behindCamera(hidden, 1f)
        return sample(
            listOf(
                Step.Stay(0.06f), Step.Hop(track.rest, CAMEO_ARRIVAL_SEC - 0.06f, track.hop(0.2f)),
                Step.Stay(hold), Step.Hop(behind, CAMEO_EXIT_SEC, track.hop(0.16f))
            ),
            behind, track, duration
        )
    }

    /** Where it rests, [hold] for its reaction, and then behind the camera as a cameo leaves, or out at a capsule's far end. */
    fun linger(track: MascotTrack, hold: Float): MascotPath {
        val duration = hold + CAMEO_EXIT_SEC
        val hidden = track.hidden
            ?: return sample(
                listOf(Step.Stay(hold), Step.Walk(track.width + track.size, CAMEO_EXIT_SEC)),
                track.rest, track, duration
            )
        return sample(
            listOf(Step.Stay(hold), Step.Hop(track.behindCamera(hidden, 1f), CAMEO_EXIT_SEC, track.hop(0.16f))),
            track.rest, track, duration
        )
    }

    /**
     * The last [total] seconds of a countdown: out from behind the camera over the timer's mark, eyes on
     * the reading, a small hop with every second that goes, and back behind the camera just after it runs out.
     */
    fun countdown(track: MascotTrack, total: Float): MascotPath {
        val size = track.size
        val duration = total + COUNTDOWN_LINGER_SEC + CAMEO_EXIT_SEC
        val hidden = track.hidden
            ?: return sample(listOf(Step.Stay(duration)), -size, track, duration)
        val behind = track.behindCamera(hidden, 1f)
        val steps = ArrayList<Step>()
        steps += Step.Stay(0.06f)
        steps += Step.Hop(track.rest, CAMEO_ARRIVAL_SEC - 0.06f, track.hop(0.2f))
        var time = CAMEO_ARRIVAL_SEC
        // The reading changes on every whole second left; it hops just as it does.
        var remaining = kotlin.math.ceil(total) - 1f
        val ticks = ArrayList<Float>()
        while (remaining >= 1f) {
            ticks += total - remaining
            remaining -= 1f
        }
        for (tick in ticks.filter { it - 0.15f > time + 0.1f }) {
            steps += Step.Stay(tick - 0.15f - time)
            steps += Step.Hop(track.rest, 0.3f, track.hop(0.1f))
            time = tick + 0.15f
        }
        steps += Step.Stay(max(0f, total + COUNTDOWN_LINGER_SEC - time))
        steps += Step.Hop(behind, CAMEO_EXIT_SEC, track.hop(0.16f))
        val path = sample(steps, behind, track, duration)
        // Its eyes stay on the reading while it watches.
        val gaze = FloatArray(path.size) { i ->
            val t = path.keyTimes[i] * duration
            if (t >= CAMEO_ARRIVAL_SEC && t <= total + COUNTDOWN_LINGER_SEC) 0.07f else path.gaze[i]
        }
        return MascotPath(path.keyTimes, path.x, path.lift, path.squash, gaze, path.durationSec, path.greeting)
    }

    /** Switched on. A capsule has no camera to come out from, so it hops in at the near end. */
    fun arrive(track: MascotTrack): MascotPath {
        if (track.hidden != null) return home(track)
        return sample(
            listOf(Step.Stay(0.06f), Step.Hop(track.rest, 0.46f, track.hop(0.3f))),
            -track.size, track, HOME_SEC
        ).withGreeting(0f..0.52f)
    }

    /** To this side of the camera from the other: into the camera's far edge, across behind it, and out with a wink. */
    fun cross(track: MascotTrack): MascotPath {
        val hidden = track.hidden ?: return home(track)
        val size = track.size
        return sample(
            listOf(
                Step.Stay(0.06f),
                Step.Hop(hidden.endInclusive - size / 2f - 2f * track.scale, 0.34f, track.hop(0.24f)),
                Step.Walk(track.behindCamera(hidden, 2f), 0.38f),
                Step.Hop(track.rest, 0.36f, track.hop(0.24f)),
                Step.Stay(0.1f)
            ),
            track.farSpot, track, CROSS_SEC
        ).withGreeting(0.72f..CROSS_SEC)
    }

    /** Switched off: a glad little hop where it rests, then away behind the camera. A capsule walks out past the far end. */
    fun farewell(track: MascotTrack): MascotPath {
        val size = track.size
        val hidden = track.hidden
            ?: return sample(
                listOf(
                    Step.Stay(0.06f), Step.Hop(track.rest, 0.3f, track.hop(0.22f)), Step.Stay(0.02f),
                    Step.Walk(track.width + size, 0.4f)
                ),
                track.rest, track, FAREWELL_SEC
            ).withGreeting(0f..0.44f)
        return sample(
            listOf(
                Step.Stay(0.06f), Step.Hop(track.rest, 0.3f, track.hop(0.22f)), Step.Stay(0.08f),
                Step.Hop(track.behindCamera(hidden, 1f), CAMEO_EXIT_SEC, track.hop(0.16f))
            ),
            track.rest, track, FAREWELL_SEC
        ).withGreeting(0f..0.48f)
    }

    /** Back behind the camera from where it watched a countdown. */
    fun retreat(track: MascotTrack): MascotPath {
        val hidden = track.hidden
            ?: return sample(listOf(Step.Stay(CAMEO_EXIT_SEC)), -track.size, track, CAMEO_EXIT_SEC)
        return sample(
            listOf(Step.Hop(track.behindCamera(hidden, 1f), CAMEO_EXIT_SEC, track.hop(0.16f))),
            track.rest, track, CAMEO_EXIT_SEC
        )
    }

    private fun MascotPath.withGreeting(range: ClosedFloatingPointRange<Float>) =
        MascotPath(keyTimes, x, lift, squash, gaze, durationSec, range)

    private const val RATE = 60.0

    private fun sample(steps: List<Step>, start: Float, track: MascotTrack, duration: Float): MascotPath {
        val keyTimes = ArrayList<Float>()
        val xs = ArrayList<Float>()
        val lifts = ArrayList<Float>()
        val squashes = ArrayList<Float>()
        val gazes = ArrayList<Float>()
        var time = 0f
        var position = start
        // A landing's squash eases out over the start of the next step.
        var landingSquash = 0f
        var landingTime = 0f
        fun add(x: Float, lift: Float, squash: Float, gaze: Float) {
            val recovery = landingSquash * max(0f, 1f - (time - landingTime) / 0.14f)
            keyTimes += minOf(1f, time / duration)
            xs += x
            lifts += lift
            squashes += if (squash != 0f) squash else recovery
            gazes += gaze
        }
        add(position, 0f, 0f, 0f)
        for (step in steps) {
            when (step) {
                is Step.Jump -> {
                    // Off stage at both ends, and in no time: two frames at the same moment, so no frame
                    // ever draws it on the way across.
                    position = step.to
                    add(position, 0f, 0f, 0.05f)
                }
                is Step.Stay -> {
                    val frames = max(1, (step.sec * RATE).roundToInt())
                    for (frame in 1..frames) {
                        val share = frame.toFloat() / frames
                        time += step.sec / frames
                        val wave = if (step.bounces > 0) abs(sin(share * PI.toFloat() * step.bounces)) else 0f
                        add(position, wave * track.hop(0.16f), 0f, 0f)
                    }
                }
                is Step.Hop -> {
                    val from = position
                    val frames = max(2, (step.sec * RATE).roundToInt())
                    val direction = if (step.to >= from) 1f else -1f
                    for (frame in 1..frames) {
                        val share = frame.toFloat() / frames
                        time += step.sec / frames
                        // Eased along the ground, a parabola above it.
                        val eased = share * share * (3f - 2f * share)
                        val squash = when {
                            share < 0.2f -> -0.12f * (1f - share / 0.2f)
                            share > 0.88f -> 0.14f * (share - 0.88f) / 0.12f
                            else -> 0f
                        }
                        add(from + (step.to - from) * eased, step.height * 4f * share * (1f - share), squash, 0.05f * direction)
                    }
                    position = step.to
                    landingSquash = 0.14f
                    landingTime = time
                }
                is Step.Walk -> {
                    val from = position
                    val frames = max(2, (step.sec * RATE).roundToInt())
                    val direction = if (step.to >= from) 1f else -1f
                    for (frame in 1..frames) {
                        val share = frame.toFloat() / frames
                        time += step.sec / frames
                        // Little steps: a small bob on each.
                        val bob = abs(sin(share * PI.toFloat() * 3f)) * track.hop(0.06f)
                        add(from + (step.to - from) * share, bob, 0f, 0.05f * direction)
                    }
                    position = step.to
                }
            }
        }
        // The last step lands, and it stays for whatever time is left.
        if (time < duration) {
            time = duration
            add(position, 0f, 0f, 0f)
        } else if (keyTimes.isNotEmpty()) {
            keyTimes[keyTimes.lastIndex] = 1f
        }
        return MascotPath(
            keyTimes.toFloatArray(), xs.toFloatArray(), lifts.toFloatArray(),
            squashes.toFloatArray(), gazes.toFloatArray(), duration
        )
    }
}
