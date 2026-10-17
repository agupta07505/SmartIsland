/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.runtime

import com.agupta07505.smartisland.buddy.model.MascotMood
import com.agupta07505.smartisland.buddy.model.MascotReaction
import com.agupta07505.smartisland.buddy.model.MascotVisit
import com.agupta07505.smartisland.buddy.model.MascotVisitKind
import com.agupta07505.smartisland.buddy.motion.MascotMotion
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Decides when a reaction plays. Pure and single-threaded (call it from the main thread): time comes
 * from [clock] (seconds), so tests drive it with virtual time.
 *
 * A moment posted while the buddy cannot show it (something else is on the island) waits up to
 * [MascotReactionGate.PATIENCE_SEC] and plays the instant the island is at rest again, if the gate
 * admits it; otherwise it is dropped.
 */
class MascotController(private val clock: () -> Double) {
    private val _reaction = MutableStateFlow<MascotReactionEvent?>(null)
    /** The latest reaction to play. Each has a fresh [MascotReactionEvent.id]. */
    val reaction: StateFlow<MascotReactionEvent?> = _reaction.asStateFlow()

    private val _visit = MutableStateFlow<MascotVisit?>(null)
    /** The stroll under way, if any. The layer plays it and reports back with [endVisit]. */
    val visit: StateFlow<MascotVisit?> = _visit.asStateFlow()

    private val _tucked = MutableStateFlow(false)
    /** Out of sight behind the camera after a long idle (or while it waits to arrive). The pill drops its wing. */
    val tucked: StateFlow<Boolean> = _tucked.asStateFlow()

    private val gate = MascotReactionGate()
    private var nextId = 1L
    private var pending: Pending? = null

    /** Whether the buddy is resting in a closed, empty pill and can show a reaction. */
    var atRest: Boolean = false
        private set

    /** The "React to Events" setting. When off, nothing plays and nothing waits. */
    var reacts: Boolean = true
        set(value) {
            field = value
            if (!value) pending = null
        }

    /**
     * Whether the closed pill is showing something else but has room for the buddy to come out over it:
     * enabled, collapsed, visible, with an activity on it. Reactions then play as a cameo instead of waiting.
     */
    var canCameo: Boolean = false
        private set

    fun setCanCameo(value: Boolean) {
        if (canCameo == value) return
        canCameo = value
        if (value) flush()
    }

    private data class Pending(val reaction: MascotReaction, val deadlineSec: Double)

    fun now(): Double = clock()

    /** Starts a stroll if the buddy is at rest and none is under way. Returns whether it started. */
    fun startVisit(kind: MascotVisitKind, greeting: MascotMood = MascotMood.Happy): Boolean {
        if (!atRest || _visit.value != null) return false
        _visit.value = MascotVisit(nextId++, kind, greeting, clock())
        return true
    }

    /**
     * Comes out over what the pill shows, for the last [totalSec] seconds of a timer, to watch it run out.
     * Needs room on the pill (see [canCameo]); returns whether it started.
     */
    fun startCountdown(totalSec: Float): Boolean {
        if (!canCameo || _visit.value != null || totalSec <= 1f) return false
        _visit.value = MascotVisit(nextId++, MascotVisitKind.Countdown(totalSec), MascotMood.Idle, clock())
        return true
    }

    /** The timer it watched was paused or cancelled: back behind the camera. */
    fun retreat() {
        val v = _visit.value ?: return
        if (v.kind !is MascotVisitKind.Countdown) return
        _visit.value = MascotVisit(nextId++, MascotVisitKind.Retreat, MascotMood.Idle, clock())
    }

    /** Moved to the camera's other side: from where it rested, behind the camera, across, and out to its new place. */
    fun startCross(greeting: MascotMood = MascotMood.Wink): Boolean {
        if (!atRest || _visit.value != null) return false
        _visit.value = MascotVisit(nextId++, MascotVisitKind.Cross, greeting, clock())
        return true
    }

    /** The layer finished (or abandoned) visit [id]. Leaving for the idle tuck leaves it tucked. */
    fun endVisit(id: Long) {
        val v = _visit.value ?: return
        if (v.id != id) return
        _visit.value = null
        if (v.kind == HIDE_AWAY) _tucked.value = true
        if (v.kind == MascotVisitKind.Cross) _tucked.value = false
        flush()
    }

    /** After a long idle: a yawn, then away behind the camera. */
    fun requestTuck(): Boolean = startVisit(HIDE_AWAY, MascotMood.Sleepy)

    /** Out of sight at once, so a buddy about to arrive is not seen resting first. */
    fun hideForArrival() {
        _tucked.value = true
    }

    /** Comes back out from behind the camera. */
    fun untuck(greeting: MascotMood = MascotMood.Happy) {
        if (!_tucked.value) return
        _tucked.value = false
        startVisit(MascotVisitKind.Arrive, greeting)
    }

    /** Drops anything under way, as when the buddy is switched off. */
    fun reset() {
        pending = null
        _visit.value = null
        _tucked.value = false
    }

    /** Something happened. Plays now if the buddy is at rest, else waits for it. */
    fun post(moment: MascotMoment) {
        when (moment) {
            MascotMoment.CountdownStart -> if (reacts) startCountdown(MascotMotion.COUNTDOWN_LEAD_SEC)
            MascotMoment.CountdownCancel -> retreat()
            else -> moment.reaction?.let { post(it) }
        }
    }

    fun post(reaction: MascotReaction) {
        if (!reacts) return
        pending = Pending(reaction, clock() + MascotReactionGate.PATIENCE_SEC)
        // Something to react to brings it back out first, and the reaction plays when it lands.
        if (_tucked.value && atRest) untuck()
        flush()
    }

    /** The Settings preview: plays [reaction] at once, ignoring the gate and the setting. */
    fun demo(reaction: MascotReaction) {
        _reaction.value = MascotReactionEvent(nextId++, reaction, clock())
    }

    fun setAtRest(value: Boolean) {
        if (atRest == value) return
        atRest = value
        if (value) {
            // A reaction that waited for the pill to empty brings a tucked buddy back out.
            if (pending != null && _tucked.value) untuck()
            flush()
        }
    }

    private fun flush() {
        val p = pending ?: return
        val now = clock()
        if (now > p.deadlineSec || !reacts) {
            pending = null
            return
        }
        if (_visit.value != null) return
        if (!atRest && canCameo) {
            // Busy pill with room: out from behind the camera over what it shows, react where it would rest.
            pending = null
            if (!gate.admits(p.reaction, now)) return
            _visit.value = MascotVisit(nextId++, MascotVisitKind.Cameo(p.reaction), MascotMood.Idle, now)
            return
        }
        if (!atRest || _tucked.value) return
        pending = null
        if (!gate.admits(p.reaction, now)) return
        _reaction.value = MascotReactionEvent(nextId++, p.reaction, now)
    }

    /** Drops anything waiting (for example when the buddy is switched off). */
    fun clear() {
        pending = null
    }
}

/**
 * The visit schedule and the greetings it brings. Pure, so tests can pin the numbers.
 */
object MascotVisitScheduler {
    /** How long after being put away it stays idle before tucking itself behind the camera. */
    const val HIDE_DELAY_SEC = 30f
    /** The faces it greets with, picked at random. */
    val greetings = listOf(MascotMood.Happy, MascotMood.Wink, MascotMood.Love)

    /** Seconds until the next visit for [frequency], or null when visits are off. */
    fun nextDelaySec(frequency: com.agupta07505.smartisland.buddy.model.MascotVisitFrequency, random: kotlin.random.Random = kotlin.random.Random): Float? {
        val range = frequency.delaySec ?: return null
        return range.start + random.nextFloat() * (range.endInclusive - range.start)
    }

    fun greeting(random: kotlin.random.Random = kotlin.random.Random): MascotMood = greetings[random.nextInt(greetings.size)]
}

/** The idle tuck: a yawn where it rests, then away behind the camera. */
val HIDE_AWAY: MascotVisitKind = MascotVisitKind.Linger(MascotReaction.Yawn)

/**
 * One-line entry point for receivers and services, so they need no controller reference. Moments
 * posted here reach the live controller on the main thread.
 */
object MascotEvents {
    private val _moments = MutableSharedFlow<MascotMoment>(extraBufferCapacity = 16)
    val moments: SharedFlow<MascotMoment> = _moments.asSharedFlow()

    fun post(moment: MascotMoment) {
        _moments.tryEmit(moment)
    }
}
