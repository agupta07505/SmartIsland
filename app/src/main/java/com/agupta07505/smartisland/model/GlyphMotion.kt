/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class GlyphMotionKind { None, SunSpin, RainFall, WindSway }

data class GlyphMotion(val kind: GlyphMotionKind, val periodMs: Int = 0) {
    val isRunning: Boolean get() = kind != GlyphMotionKind.None && periodMs > 0

    companion object {
        val NONE = GlyphMotion(GlyphMotionKind.None)
    }
}

object GlyphMotions {
    const val SUN_PERIOD_MS = 24_000

    const val RAIN_PERIOD_MS = 1_300

    const val WIND_CALM_KMH = 2f

    const val WIND_STRONG_KMH = 60f

    const val WIND_SLOWEST_MS = 5_000
    const val WIND_FASTEST_MS = 700

    const val WIND_SWAY_SHARE = 0.12f

    fun windPeriodMs(windKmh: Float): Int {
        if (windKmh.isNaN() || windKmh < WIND_CALM_KMH) return 0
        val t = ((windKmh - WIND_CALM_KMH) / (WIND_STRONG_KMH - WIND_CALM_KMH)).coerceIn(0f, 1f)
        return Math.round(WIND_SLOWEST_MS + (WIND_FASTEST_MS - WIND_SLOWEST_MS) * t)
    }

    fun forWidget(
        kind: IdleWidgetKind,
        look: WeatherLook?,
        isDay: Boolean,
        windKmh: Float,
        animationsOff: Boolean
    ): GlyphMotion {
        if (animationsOff) return GlyphMotion.NONE
        return when (kind) {
            IdleWidgetKind.Weather -> when {
                look == WeatherLook.Clear && isDay -> GlyphMotion(GlyphMotionKind.SunSpin, SUN_PERIOD_MS)
                look == WeatherLook.Rain -> GlyphMotion(GlyphMotionKind.RainFall, RAIN_PERIOD_MS)
                else -> GlyphMotion.NONE
            }
            IdleWidgetKind.Wind -> windPeriodMs(windKmh).let {
                if (it > 0) GlyphMotion(GlyphMotionKind.WindSway, it) else GlyphMotion.NONE
            }
            else -> GlyphMotion.NONE
        }
    }

    fun sunAngle(phase: Float): Float = (phase - Math.floor(phase.toDouble()).toFloat()) * 360f

    fun windOffsetShare(phase: Float): Float = (sin(2.0 * PI * phase) * WIND_SWAY_SHARE).toFloat()

    data class Drop(val offsetShare: Float, val alpha: Float)

    fun drop(phase: Float): Drop {
        val p = phase - Math.floor(phase.toDouble()).toFloat()
        return Drop(offsetShare = -0.4f + 0.8f * p, alpha = sin(PI * p).toFloat())
    }
}

object WeatherMorph {
    const val MORPH_MS = 650

    const val TURN_DEGREES = 60f

    const val MIN_SCALE = 0.6f

    const val LANDING_MS = 180
    const val LANDING_OVERSHOOT = 0.08f

    fun landingScale(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return 1f + LANDING_OVERSHOOT * Math.sin(Math.PI * p).toFloat()
    }

    data class Layer(val alpha: Float, val scale: Float, val rotationDegrees: Float)

    data class Frame(val outgoing: Layer, val incoming: Layer)

    private fun dayChangesGlyph(look: WeatherLook) = look == WeatherLook.Clear || look == WeatherLook.PartlyCloudy

    fun changed(fromLook: WeatherLook, fromIsDay: Boolean, toLook: WeatherLook, toIsDay: Boolean): Boolean =
        fromLook != toLook || (fromIsDay != toIsDay && dayChangesGlyph(toLook))

    /** Both glyphs at [progress] (0 = old fully shown, 1 = new). Alphas sum to 1, so the glyph never dips to nothing. */
    fun frame(progress: Float): Frame {
        val e = smoothstep(progress)
        return Frame(
            outgoing = Layer(alpha = 1f - e, scale = 1f - (1f - MIN_SCALE) * e, rotationDegrees = TURN_DEGREES * e),
            incoming = Layer(alpha = e, scale = MIN_SCALE + (1f - MIN_SCALE) * e,
                rotationDegrees = -TURN_DEGREES * (1f - e))
        )
    }
}

enum class GlyphAttention { None, SpeedPulse, AlarmGlow }

object AttentionStates {
    const val FAST_SPEED_START_BPS = 1_000_000L

    // Use a lower stop threshold to prevent flicker near 1 MB/s.
    const val FAST_SPEED_STOP_BPS = 800_000L

    const val ALARM_SOON_MS = 10 * 60_000L

    /** Longest the glow waits before checking the clock again, so a changed system clock is noticed. */
    const val ALARM_RECHECK_MAX_MS = 60_000L

    const val SPEED_PULSE_MS = 2_400
    const val ALARM_GLOW_MS = 2_800

    const val SPEED_PULSE_GROWTH = 0.12f

    const val GLOW_MIN_ALPHA = 0.25f
    const val GLOW_MAX_ALPHA = 0.75f

    const val GLOW_MIN_SCALE = 0.9f
    const val GLOW_MAX_SCALE = 1.15f

    /**
     * Whether the speed counts as fast, with hysteresis: starts at [FAST_SPEED_START_BPS], stops below
     * [FAST_SPEED_STOP_BPS]
     */
    fun isFastSpeed(bytesPerSecond: Long, wasFast: Boolean): Boolean =
        bytesPerSecond >= if (wasFast) FAST_SPEED_STOP_BPS else FAST_SPEED_START_BPS

    fun isAlarmSoon(nextAlarmMs: Long, nowMs: Long): Boolean =
        nextAlarmMs > nowMs && nextAlarmMs - nowMs < ALARM_SOON_MS

    fun alarmRecheckDelayMs(nextAlarmMs: Long, nowMs: Long): Long? {
        if (nextAlarmMs <= nowMs) return null
        val left = nextAlarmMs - nowMs
        val wait = if (left < ALARM_SOON_MS) left else left - ALARM_SOON_MS + 1
        return wait.coerceIn(1L, ALARM_RECHECK_MAX_MS)
    }

    fun forWidget(kind: IdleWidgetKind, fastSpeed: Boolean, alarmSoon: Boolean,
            animationsOff: Boolean): GlyphAttention {
        if (animationsOff) return GlyphAttention.None
        return when {
            kind == IdleWidgetKind.NetSpeed && fastSpeed -> GlyphAttention.SpeedPulse
            kind == IdleWidgetKind.NextAlarm && alarmSoon -> GlyphAttention.AlarmGlow
            else -> GlyphAttention.None
        }
    }

    fun periodMs(attention: GlyphAttention): Int = when (attention) {
        GlyphAttention.SpeedPulse -> SPEED_PULSE_MS
        GlyphAttention.AlarmGlow -> ALARM_GLOW_MS
        GlyphAttention.None -> 0
    }

    fun breath(phase: Float): Float {
        val p = phase - Math.floor(phase.toDouble()).toFloat()
        return (0.5 - 0.5 * cos(2.0 * PI * p)).toFloat()
    }

    fun pulseScale(breath: Float): Float = 1f + SPEED_PULSE_GROWTH * breath.coerceIn(0f, 1f)

    fun glowAlpha(breath: Float): Float = GLOW_MIN_ALPHA + (GLOW_MAX_ALPHA - GLOW_MIN_ALPHA) * breath.coerceIn(0f, 1f)

    fun glowScale(breath: Float): Float = GLOW_MIN_SCALE + (GLOW_MAX_SCALE - GLOW_MIN_SCALE) * breath.coerceIn(0f, 1f)
}
