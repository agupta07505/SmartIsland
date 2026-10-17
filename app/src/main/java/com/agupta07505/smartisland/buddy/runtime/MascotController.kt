/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.runtime

import com.agupta07505.smartisland.buddy.model.MascotReaction
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

    private data class Pending(val reaction: MascotReaction, val deadlineSec: Double)

    fun now(): Double = clock()

    /** Something happened. Plays now if the buddy is at rest, else waits for it. */
    fun post(moment: MascotMoment) {
        val reaction = moment.reaction ?: return
        post(reaction)
    }

    fun post(reaction: MascotReaction) {
        if (!reacts) return
        pending = Pending(reaction, clock() + MascotReactionGate.PATIENCE_SEC)
        flush()
    }

    /** The Settings preview: plays [reaction] at once, ignoring the gate and the setting. */
    fun demo(reaction: MascotReaction) {
        _reaction.value = MascotReactionEvent(nextId++, reaction, clock())
    }

    fun setAtRest(value: Boolean) {
        if (atRest == value) return
        atRest = value
        if (value) flush()
    }

    private fun flush() {
        val p = pending ?: return
        val now = clock()
        if (now > p.deadlineSec || !reacts) {
            pending = null
            return
        }
        if (!atRest) return
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
