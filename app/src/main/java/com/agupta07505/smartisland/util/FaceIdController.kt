/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object FaceIdController {
    enum class Phase { Idle, Scanning }

    enum class Style { Square, Cutout, SquareTick }

    data class State(
        val phase: Phase = Phase.Idle,
        val session: Int = 0,
        val accepted: Boolean = false,
        val waitMs: Int = DEMO_WAIT_MS,
        val cancelled: Boolean = false,
        val rejected: Boolean = false,
        val fingerprint: Boolean = false,
        val direct: Boolean = false,
        val style: Style = Style.Square,
        /** False while the card waits for the system's real scan start; blinking only begins once true. */
        val anchored: Boolean = true
    ) {
        val active: Boolean get() = phase != Phase.Idle
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun start(
        accepted: Boolean = false,
        waitMs: Int = DEMO_WAIT_MS,
        style: Style = Style.Square,
        anchored: Boolean = true
    ) {
        _state.update { State(Phase.Scanning, it.session + 1, accepted, waitMs, style = style, anchored = anchored) }
    }

    fun startForUnlock(style: Style = Style.Square, waitMs: Int = REAL_WAIT_MS) =
        start(accepted = false, waitMs = waitMs, style = style)

    fun startForApp(style: Style = Style.Square, anchored: Boolean = true) =
        start(accepted = false, waitMs = APP_WAIT_MS, style = style, anchored = anchored)

    /** The system's real scan has begun: the blink clock starts now. */
    fun anchor() {
        _state.update { if (it.active && !it.anchored) it.copy(anchored = true) else it }
    }

    fun startFingerprint(style: Style = Style.Square) {
        _state.update { State(Phase.Scanning, it.session + 1, false, REAL_WAIT_MS, fingerprint = true, direct = true, style = style) }
    }

    fun styleOf(id: String): Style = when (id) {
        "cutout" -> Style.Cutout
        "square_tick" -> Style.SquareTick
        else -> Style.Square
    }

    fun cancel() {
        _state.update { if (it.active) it.copy(cancelled = true) else it }
    }

    fun accept() {
        _state.update { if (it.active) it.copy(accepted = true) else it }
    }

    fun reject() {
        _state.update {
            if (it.active && !it.accepted && !it.cancelled && !it.fingerprint && !it.rejected) {
                it.copy(rejected = true)
            } else {
                it
            }
        }
    }

    fun fingerprint() {
        _state.update { if (it.active && !it.accepted && !it.cancelled && !it.fingerprint) it.copy(fingerprint = true) else it }
    }

    fun stop() {
        _state.update { it.copy(phase = Phase.Idle, accepted = false, cancelled = false, rejected = false, fingerprint = false, direct = false) }
    }

    fun finish(session: Int) {
        _state.update {
            if (it.session == session) it.copy(phase = Phase.Idle, accepted = false, cancelled = false, rejected = false, fingerprint = false, direct = false) else it
        }
    }


    const val DEMO_WAIT_MS = 2320

    const val REAL_WAIT_MS = 2320

    const val LOG_WAIT_MS = 5000

    const val APP_WAIT_MS = 5000
}
