/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.roundToInt

enum class TorchTimeout { Normal, None, Hold }

object TorchCard {
    fun owns(mode: IslandMode?): Boolean = mode == IslandMode.Flashlight

    fun open(mode: IslandMode?, expanded: Boolean): Boolean = expanded && owns(mode)

    fun followsSystemTorch(cameraBusy: Boolean, callRinging: Boolean): Boolean = !cameraBusy && !callRinging

    fun mustStopPattern(patternRunning: Boolean, eventShown: Boolean): Boolean = patternRunning && !eventShown

    fun timeout(mode: IslandMode?, autoOpened: Boolean, touched: Boolean): TorchTimeout = when {
        !owns(mode) -> TorchTimeout.Normal
        !autoOpened -> TorchTimeout.None
        touched -> TorchTimeout.Hold
        else -> TorchTimeout.Normal
    }

    /** Keep the open card visible so the user can turn the torch back on. */
    fun endsEventWhenDark(cardOpen: Boolean): Boolean = !cardOpen

    fun eventDone(cardOpen: Boolean, torchActive: Boolean, warningShown: Boolean): Boolean =
        !cardOpen && !torchActive && !warningShown

    fun ringingCallIndex(cards: List<IslandNotification>): Int =
        cards.indexOfFirst { it.mode == IslandMode.IncomingCall && it.isCallRinging }

    fun mayPulse(torchOpen: Boolean, card: IslandNotification): Boolean =
        !torchOpen || (card.mode == IslandMode.IncomingCall && card.isCallRinging)

    fun settlesPatternOnSwap(wasOpen: Boolean, expanded: Boolean, selected: IslandMode?, torchShown: Boolean): Boolean =
        wasOpen && expanded && !owns(selected) && torchShown

    /** The island closes on the torch card while a call rings: the pattern would flash through the call. */
    fun settlesPatternOnCollapse(wasOpen: Boolean, expanded: Boolean, callRinging: Boolean, torchShown: Boolean): Boolean =
        wasOpen && !expanded && callRinging && torchShown
}

enum class TorchView { Main, Strobe }

sealed interface TorchChip {
    data object Off : TorchChip
    data object On : TorchChip
    data class OnAtLevel(val level: Int, val max: Int) : TorchChip
    data object Sos : TorchChip
    data class Strobe(val hz: Int) : TorchChip
    data object StrobePaused : TorchChip
}

enum class TorchHint { TapToTurnOn, TapToStop, DragBeam, NoBrightnessSteps, OtherApp }

sealed interface TorchOutput {
    data object Off : TorchOutput
    data class Steady(val level: Int) : TorchOutput
    data object Sos : TorchOutput
    data class Strobe(val hz: Int) : TorchOutput
}

/**
 * Everything the torch card shows and does, as one value. Each tap or drag is a function that returns the next state,
 * [output] says what the torch should then be doing.
 */
data class TorchCardState(
    val maxLevel: Int = 1,
    val level: Int = 1,
    val view: TorchView = TorchView.Main,
    val on: Boolean = false,
    val pattern: TorchPattern? = null,
    val hz: Int = DEFAULT_HZ,
    val acknowledged: Boolean = false,
    val gateShown: Boolean = false,
    val external: ExternalTorchPattern? = null
) {
    val supportsBrightness: Boolean get() = maxLevel > 1

    val active: Boolean get() = on || pattern != null || external != null

    val beamLevel: Int get() = if (pattern != null || external != null || view == TorchView.Strobe) maxLevel else level

    val canAdjust: Boolean get() = supportsBrightness && on && pattern == null && external == null && view == TorchView.Main

    val extendedRange: Boolean get() = hz > RELIABLE_HZ

    val chip: TorchChip
        get() = when {
            pattern == TorchPattern.Sos -> TorchChip.Sos
            pattern == TorchPattern.Strobe -> TorchChip.Strobe(hz)
            external == ExternalTorchPattern.Sos -> TorchChip.Sos
            external is ExternalTorchPattern.Strobe -> TorchChip.Strobe(external.hz)
            on -> if (supportsBrightness) TorchChip.OnAtLevel(level, maxLevel) else TorchChip.On
            view == TorchView.Strobe -> TorchChip.StrobePaused
            else -> TorchChip.Off
        }

    val hint: TorchHint
        get() = when {
            pattern == TorchPattern.Sos -> TorchHint.TapToStop
            external != null -> TorchHint.OtherApp
            on -> if (supportsBrightness) TorchHint.DragBeam else TorchHint.NoBrightnessSteps
            else -> TorchHint.TapToTurnOn
        }

    val output: TorchOutput
        get() = when {
            pattern == TorchPattern.Sos -> TorchOutput.Sos
            pattern == TorchPattern.Strobe -> TorchOutput.Strobe(hz)
            on -> TorchOutput.Steady(level)
            else -> TorchOutput.Off
        }

    fun powerTap(): TorchCardState = when {
        view == TorchView.Strobe -> if (pattern != null) stopped() else requestStrobe()
        pattern != null -> stopped()
        else -> copy(on = !on)
    }

    fun sosTap(): TorchCardState =
        if (pattern == TorchPattern.Sos) stopped() else copy(view = TorchView.Main, on = false, pattern = TorchPattern.Sos, external = null)

    fun strobeTap(): TorchCardState = stopped().copy(view = TorchView.Strobe, external = null).requestStrobe()

    fun back(): TorchCardState = stopped().copy(view = TorchView.Main, gateShown = false)

    fun gateCancel(): TorchCardState = copy(gateShown = false, view = TorchView.Main)

    fun gateAccept(): TorchCardState = copy(acknowledged = true, gateShown = false, pattern = TorchPattern.Strobe)

    fun withHz(value: Int): TorchCardState = copy(hz = value.coerceIn(MIN_HZ, MAX_HZ))

    fun withExternal(value: ExternalTorchPattern?): TorchCardState = copy(external = value)

    fun withLevel(value: Int): TorchCardState = copy(level = value.coerceIn(1, maxLevel))

    fun systemTorch(enabled: Boolean): TorchCardState = if (pattern != null) this else copy(on = enabled)

    /** A pattern stopped without being asked to (the camera was taken). */
    fun patternEnded(): TorchCardState = if (pattern != null) stopped() else this

    fun leftForAnotherCard(): TorchCardState = when {
        pattern != null -> copy(pattern = null, on = true, view = TorchView.Main, gateShown = false)
        gateShown -> gateCancel()
        else -> this
    }

    private fun stopped(): TorchCardState = copy(pattern = null, on = false)

    private fun requestStrobe(): TorchCardState =
        if (acknowledged) copy(pattern = TorchPattern.Strobe) else copy(gateShown = true)

    companion object {
        const val MIN_HZ = TorchTiming.MIN_STROBE_HZ
        const val MAX_HZ = TorchTiming.MAX_STROBE_HZ
        const val RELIABLE_HZ = 20
        const val DEFAULT_HZ = 3

        fun initial(maxLevel: Int, level: Int, on: Boolean, acknowledged: Boolean): TorchCardState {
            val max = maxLevel.coerceAtLeast(1)
            return TorchCardState(maxLevel = max, level = level.coerceIn(1, max), on = on, acknowledged = acknowledged)
        }

        fun cycleMs(hz: Int): Int = Math.round(1000f / hz.coerceIn(MIN_HZ, MAX_HZ))
    }
}

object TorchBeam {
    private const val MIN_REACH = 0.3f
    private const val MIN_ALPHA = 0.35f
    private const val NO_STEPS_ALPHA = 0.9f

    fun reach(level: Int, max: Int): Float =
        if (max <= 1) 1f else MIN_REACH + (1f - MIN_REACH) * level.coerceIn(1, max) / max

    fun alpha(level: Int, max: Int): Float =
        if (max <= 1) NO_STEPS_ALPHA else MIN_ALPHA + (1f - MIN_ALPHA) * level.coerceIn(1, max) / max

    /** A tick [yFromTop] down the beam is drawn only when it is below the brightness icon that ends [iconBottom] down. */
    fun tickClearsIcon(yFromTop: Float, iconBottom: Float): Boolean = yFromTop >= iconBottom

    /** The step a finger [heightFromBottom] up a beam [beamHeight] tall asks for, 1 on phones without steps. */
    fun levelAt(heightFromBottom: Float, beamHeight: Float, max: Int): Int {
        if (max <= 1 || beamHeight <= 0f) return 1
        val fraction = (heightFromBottom / beamHeight - MIN_REACH) / (1f - MIN_REACH)
        return (fraction * max).roundToInt().coerceIn(1, max)
    }
}

object TorchCardSize {
    const val WIDTH_FRACTION = 0.72f
    const val HEIGHT_FRACTION = 0.34f
    const val MIN_HEIGHT_DP = 320f
    const val MAX_HEIGHT_DP = 360f

    /** Floor on a short screen (landscape), where the usual minimum would not fit. */
    const val SHORT_SCREEN_MIN_HEIGHT_DP = 260f
    const val SHORT_SCREEN_DP = 500f

    const val ESTIMATE_HEIGHT_DP = 345f

    fun heightDp(screenHeightDp: Float): Float {
        val min = if (screenHeightDp < SHORT_SCREEN_DP) SHORT_SCREEN_MIN_HEIGHT_DP else MIN_HEIGHT_DP
        return (screenHeightDp * HEIGHT_FRACTION).coerceIn(min, MAX_HEIGHT_DP)
    }
}

object TorchStrobeSlider {
    private const val SPAN = TorchCardState.MAX_HZ - TorchCardState.MIN_HZ

    fun fractionOf(hz: Int): Float =
        (hz.coerceIn(TorchCardState.MIN_HZ, TorchCardState.MAX_HZ) - TorchCardState.MIN_HZ).toFloat() / SPAN

    fun hzAt(fraction: Float): Int =
        TorchCardState.MIN_HZ + (fraction.coerceIn(0f, 1f) * SPAN).roundToInt()

    val reliableFraction: Float get() = fractionOf(TorchCardState.RELIABLE_HZ)
}
