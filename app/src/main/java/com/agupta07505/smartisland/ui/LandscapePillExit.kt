/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IdleCloseSequence
import com.agupta07505.smartisland.model.PILL_SPRING_DAMPING
import com.agupta07505.smartisland.model.PILL_SPRING_STIFFNESS
import com.agupta07505.smartisland.model.PillExitDirection
import com.agupta07505.smartisland.model.PillPhase
import com.agupta07505.smartisland.model.PillTransition
import com.agupta07505.smartisland.model.pillExitAlpha
import com.agupta07505.smartisland.model.pillExitDirection
import com.agupta07505.smartisland.model.pillExitOffsetDp
import com.agupta07505.smartisland.model.pillExitTravelDp
import com.agupta07505.smartisland.model.pillSlideProgress
import com.agupta07505.smartisland.model.pillTransition
import com.agupta07505.smartisland.util.AnimationSpeed
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * One state machine for the landscape cutout leaving AND coming back (rules: model/PillPresence.kt, maths of the
 * slide: model/LandscapePillExit.kt).
 */
internal class PillExitState {
    // Plain fields: written after composition, read when composing.
    var hadCollapsedContent = false
    var hadNoContent = false
    var lastSettings: SmartIslandSettings? = null
    var lastCenterXDp = 0f
    var lastTopDp = 0f
    var lastWidthDp = 0f
    var lastHeightDp = 0f

    /** The orientation the record above was made in: a pill seen in the other orientation is no help for this one. */
    var recordedLandscape: Boolean? = null
    var job: Job? = null

    var phase by mutableStateOf(PillPhase.None)
    private var frozen by mutableStateOf<SmartIslandSettings?>(null)
    private var direction = PillExitDirection.Up
    private var travelDp = 0f

    /** 1 = pill in place, 0 = gone. Mirror of [presenceAnim] that the draw code reads. */
    var presence by mutableFloatStateOf(1f)

    private val presenceAnim = Animatable(1f)

    /** Stiffness (user speed applied, system scale not) the next exit runs on, or null for the normal pill spring. */
    var exitStiffnessUser: Float? = null

    /** The next run starts from [presence] at rest (a new exit or entry), instead of continuing the running motion. */
    var restartPresence = true

    /** Set while composing: the island's size and position jump instead of gliding. */
    var snapGeometry = false

    /** Set while composing: the settings the island keeps rendering with during the exit, else null. */
    var frozenNow: SmartIslandSettings? = null

    /** Set while composing on the very frame a card arrives on the idle circle: draw the pill as already away. */
    var enterStarting = false

    /** Set while composing: the pill is leaving (or about to), so the secondary cutout keeps its merge instead of snapping apart. */
    var leavingNow = false

    /**
     * The separate idle dot exists (landscape, not notch mode), so it can stand in for the island's own idle cutout
     * while the island takes the idle settings. Portrait has no dot and keeps its own hand-over.
     */
    var dotCoversHandover = false

    /** True once the island shell has really reached the idle settings (size and opacity), read from the overlay. */
    var shellSettled: () -> Boolean = { true }

    val hasRecord: Boolean get() = lastSettings != null && lastWidthDp > 0f && lastHeightDp > 0f

    fun prepare(screenWidthDp: Float) {
        direction = pillExitDirection(lastCenterXDp, screenWidthDp)
        travelDp = pillExitTravelDp(direction, lastCenterXDp, lastWidthDp, lastHeightDp, lastTopDp, screenWidthDp)
    }

    fun frozenSettings(): SmartIslandSettings? = frozen

    fun freeze(settings: SmartIslandSettings?) {
        frozen = settings
    }

    fun reset() {
        job?.cancel()
        frozen = null
        phase = PillPhase.None
        presence = 1f
        restartPresence = true
        exitStiffnessUser = null
    }

    private fun effPresence(): Float = if (enterStarting) 0f else if (phase == PillPhase.None || phase == PillPhase.Settling) 1f else presence

    fun slideXDp(): Float = pillExitOffsetDp(direction, travelDp, pillSlideProgress(effPresence())).dx

    fun slideYDp(): Float = pillExitOffsetDp(direction, travelDp, pillSlideProgress(effPresence())).dy

    fun pillAlpha(): Float = pillExitAlpha(pillSlideProgress(effPresence()))

    /** Opacity of the island shell itself. */
    fun shellAlpha(): Float = if (phase == PillPhase.Settling && dotCoversHandover) 0f else pillAlpha()

    /** Opacity of the idle circle: on or off, never animated. */
    fun dotAlpha(base: Float): Float =
        if (phase == PillPhase.Leaving || (phase == PillPhase.Settling && dotCoversHandover)) 1f else base

    fun start(block: suspend () -> Unit, scope: kotlinx.coroutines.CoroutineScope) {
        job?.cancel()
        job = scope.launch { block() }
    }

    private fun presenceSpec(speed: AnimationSpeed) =
        spring<Float>(
            dampingRatio = PILL_SPRING_DAMPING,
            stiffness = exitStiffnessUser?.let { speed.systemStiffness(it) } ?: speed.stiffness(PILL_SPRING_STIFFNESS)
        )

    private suspend fun beginPresenceRun() {
        if (restartPresence) {
            presenceAnim.snapTo(presence)
            restartPresence = false
        }
    }

    suspend fun runExit(speed: AnimationSpeed) {
        beginPresenceRun()
        presenceAnim.animateTo(0f, presenceSpec(speed)) { presence = value }
        // Gone: the island takes the idle settings with a jump and the dot hands over at the same spot.
        presenceAnim.snapTo(1f)
        frozen = null
        presence = 1f
        phase = PillPhase.Settling
        withFrameNanos { }
        withFrameNanos { }
        // Hold the hand-over (shell hidden, dot drawn) until the shell is really at the idle size and opacity, else
        // the pill-sized shell shows at the idle spot and shrinks and fades into the circle.
        if (dotCoversHandover && !com.agupta07505.smartisland.util.SystemAnimScale.animationsOff()) {
            var startNanos = -1L
            while (true) {
                val now = withFrameNanos { it }
                if (startNanos < 0L) startNanos = now
                if (shellSettled() || (now - startNanos) / 1_000_000L >= SETTLE_CAP_MS) break
            }
        }
        phase = PillPhase.None
        exitStiffnessUser = null
    }

    suspend fun runEnter(speed: AnimationSpeed) {
        beginPresenceRun()
        presenceAnim.animateTo(1f, presenceSpec(speed)) { presence = value }
        presenceAnim.snapTo(1f)
        presence = 1f
        phase = PillPhase.None
        exitStiffnessUser = null
    }
}

private const val SETTLE_CAP_MS = 700L

/**
 * Creates the state machine and runs it. Call before the island's layout so the snap flag and the frozen settings are
 * known when its geometry targets change.
 */
@Composable
internal fun rememberLandscapePillExit(
    hasContent: Boolean,
    expanded: Boolean,
    isLandscape: Boolean,
    /** Step 2 of the idle-circle close: the pill leaves on the sequence's own exit spring. */
    idleLeaving: Boolean,
    notchMode: Boolean,
    screenWidthDp: Float,
    animSpeed: AnimationSpeed,
    /** The collapsed pill is really on screen (not auto-hidden). */
    pillOnScreen: Boolean = true
): PillExitState {
    val state = remember { PillExitState() }
    val scope = rememberCoroutineScope()
    // Landscape only (portrait closes with its normal fade, there is no pill slide), and until a run that began there is over.
    val eligible = !notchMode && (isLandscape || state.phase != PillPhase.None)
    val exitStiffness = if (idleLeaving) IdleCloseSequence.plan(animSpeed).exitStiffness else null
    val transition = pillTransition(
        phase = state.phase,
        eligible = eligible,
        expanded = expanded,
        hasContent = hasContent,
        hadPill = state.hadCollapsedContent,
        hadNoContent = state.hadNoContent,
        hasRecord = state.hasRecord && state.recordedLandscape == isLandscape
    )
    val startingExit = transition == PillTransition.StartExit
    val startingEnter = transition == PillTransition.StartEnter
    if (startingExit || startingEnter) state.prepare(screenWidthDp)
    state.enterStarting = startingEnter
    state.leavingNow = startingExit || state.phase == PillPhase.Leaving
    state.frozenNow = if (startingExit) state.lastSettings else state.frozenSettings()
    state.snapGeometry = startingExit || startingEnter || state.phase != PillPhase.None
    SideEffect {
        when (transition) {
            PillTransition.StartExit -> {
                state.exitStiffnessUser = exitStiffness
                state.freeze(state.lastSettings)
                state.presence = 1f
                state.restartPresence = true
                state.phase = PillPhase.Leaving
                state.start({ state.runExit(animSpeed) }, scope)
            }
            PillTransition.StartEnter -> {
                state.freeze(null)
                state.presence = 0f
                state.restartPresence = true
                state.phase = PillPhase.Entering
                state.start({ state.runEnter(animSpeed) }, scope)
            }
            PillTransition.ReverseToPill -> {
                // A card (or expanding) arrived mid-exit: turn around from the current spot.
                state.freeze(null)
                state.phase = PillPhase.Entering
                state.start({ state.runEnter(animSpeed) }, scope)
            }
            PillTransition.ReverseToIdle -> {
                // The card went mid-entry: leave again from the current spot.
                state.freeze(state.lastSettings)
                state.phase = PillPhase.Leaving
                state.start({ state.runExit(animSpeed) }, scope)
            }
            PillTransition.Cancel -> state.reset()
            PillTransition.None -> Unit
        }
        // Not tied to [eligible]: the composition before a portrait tap is not eligible yet, but the pill was there.
        state.hadCollapsedContent = !notchMode && !expanded && hasContent && pillOnScreen
        state.hadNoContent = !notchMode && !expanded && !hasContent
    }
    return state
}

internal fun PillExitState.record(
    shown: Boolean,
    isLandscape: Boolean,
    settings: SmartIslandSettings,
    centerXDp: Float,
    topDp: Float,
    widthDp: Float,
    heightDp: Float
) {
    if (shown && widthDp > 0f && heightDp > 0f) {
        lastSettings = settings
        recordedLandscape = isLandscape
        lastCenterXDp = centerXDp
        lastTopDp = topDp
        lastWidthDp = widthDp
        lastHeightDp = heightDp
    }
}
