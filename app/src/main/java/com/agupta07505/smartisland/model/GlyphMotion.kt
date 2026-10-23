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

/** One motion and the time a full loop takes; [None] has no period. */
data class GlyphMotion(val kind: GlyphMotionKind, val periodMs: Int = 0) {
    val isRunning: Boolean get() = kind != GlyphMotionKind.None && periodMs > 0

    companion object {
        val NONE = GlyphMotion(GlyphMotionKind.None)
    }
}

/** Which motion a glyph gets and how fast. Pure, so it is testable without Compose. */
object GlyphMotions {
    /** Full turn of the sun: slow enough to read as ambient, not as a spinner. */
    const val SUN_PERIOD_MS = 24_000

    /** One raindrop's fall time (a second drop follows half a loop later). */
    const val RAIN_PERIOD_MS = 1_300

    /** Below this wind speed (km/h) the wind glyph stays still. */
    const val WIND_CALM_KMH = 2f

    /** From this speed (km/h) the wind glyph moves at its fastest. */
    const val WIND_STRONG_KMH = 60f

    /** Sway loop length (ms) at the calm end of the range. */
    const val WIND_SLOWEST_MS = 5_000
    const val WIND_FASTEST_MS = 700

    /** Sideways sway each way, as a share of the glyph size. */
    const val WIND_SWAY_SHARE = 0.12f

    /**
     * Sway loop length for [windKmh]: longer when calm, shorter when strong. 0 = do not move (unknown, negative or
     * calm).
     */
    fun windPeriodMs(windKmh: Float): Int {
        if (windKmh.isNaN() || windKmh < WIND_CALM_KMH) return 0
        val t = ((windKmh - WIND_CALM_KMH) / (WIND_STRONG_KMH - WIND_CALM_KMH)).coerceIn(0f, 1f)
        return Math.round(WIND_SLOWEST_MS + (WIND_FASTEST_MS - WIND_SLOWEST_MS) * t)
    }

    /**
     * Motion for one idle widget glyph. [look] / [isDay] describe the weather (null if not loaded); [windKmh] is
     * negative when unknown; [animationsOff] always wins.
     */
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

    /** A raindrop's place in its fall: vertical offset (share of glyph size, -0.4 top to +0.4 bottom) and opacity. */
    data class Drop(val offsetShare: Float, val alpha: Float)

    fun drop(phase: Float): Drop {
        val p = phase - Math.floor(phase.toDouble()).toFloat()
        return Drop(offsetShare = -0.4f + 0.8f * p, alpha = sin(PI * p).toFloat())
    }
}

/**
 * Morph between two weather glyphs: the old one turns and shrinks away while the new one turns in and grows, cross-
 * fading. Pure, so timing and the "did the look change" rule are testable.
 */
object WeatherMorph {
    /** Whole morph length; the ambient motions start after it. */
    const val MORPH_MS = 650

    /** Turn in degrees: the old glyph leaves this far, the new one arrives from the same way round. */
    const val TURN_DEGREES = 60f

    /** Smallest size (share of full) a glyph shrinks to. */
    const val MIN_SCALE = 0.6f

    /** After the morph the new glyph swells by [LANDING_OVERSHOOT] and settles back over [LANDING_MS]. */
    const val LANDING_MS = 180
    const val LANDING_OVERSHOOT = 0.08f

    /** Landing size factor at [progress] (0 = just arrived, 1 = settled): 1 at both ends, the overshoot between. */
    fun landingScale(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return 1f + LANDING_OVERSHOOT * Math.sin(Math.PI * p).toFloat()
    }

    data class Layer(val alpha: Float, val scale: Float, val rotationDegrees: Float)

    data class Frame(val outgoing: Layer, val incoming: Layer)

    private fun dayChangesGlyph(look: WeatherLook) = look == WeatherLook.Clear || look == WeatherLook.PartlyCloudy

    /**
     * True when the drawn glyph really changes: another look, or day/night on a look with a separate night glyph.
     * Same look or a temperature-only change never morphs.
     */
    fun changed(fromLook: WeatherLook, fromIsDay: Boolean, toLook: WeatherLook, toIsDay: Boolean): Boolean =
        fromLook != toLook || (fromIsDay != toIsDay && dayChangesGlyph(toLook))

    private fun ease(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return p * p * (3f - 2f * p)
    }

    /** Both glyphs at [progress] (0 = old fully shown, 1 = new). Alphas sum to 1, so the glyph never dips to nothing. */
    fun frame(progress: Float): Frame {
        val e = ease(progress)
        return Frame(
            outgoing = Layer(alpha = 1f - e, scale = 1f - (1f - MIN_SCALE) * e, rotationDegrees = TURN_DEGREES * e),
            incoming = Layer(alpha = e, scale = MIN_SCALE + (1f - MIN_SCALE) * e, rotationDegrees = -TURN_DEGREES * (1f - e))
        )
    }
}

enum class GlyphAttention { None, SpeedPulse, AlarmGlow }

/**
 * Attention states: a fast connection makes the speed arrow breathe, an alarm about to ring makes the alarm glyph
 * glow.
 */
object AttentionStates {
    /** The speed arrow starts pulsing from 1 MB/s (decimal, same MB as the speed text). */
    const val FAST_SPEED_START_BPS = 1_000_000L

    /** Once pulsing, the arrow stops only below this speed, so it does not flicker around 1 MB/s. */
    const val FAST_SPEED_STOP_BPS = 800_000L

    /** The alarm glyph glows when the next alarm is closer than this (10 minutes). */
    const val ALARM_SOON_MS = 10 * 60_000L

    /** Longest the glow waits before checking the clock again, so a changed system clock is noticed. */
    const val ALARM_RECHECK_MAX_MS = 60_000L

    /** One breath (in and out): slow, like the privacy glyph. */
    const val SPEED_PULSE_MS = 2_400
    const val ALARM_GLOW_MS = 2_800

    /** Speed arrow growth (share of its size) at the top of a breath. */
    const val SPEED_PULSE_GROWTH = 0.12f

    /** Alarm halo opacity at the bottom and top of a breath. */
    const val GLOW_MIN_ALPHA = 0.25f
    const val GLOW_MAX_ALPHA = 0.75f

    /** Alarm halo size (share of the glyph) at the bottom and top of a breath. */
    const val GLOW_MIN_SCALE = 0.9f
    const val GLOW_MAX_SCALE = 1.15f

    /**
     * Whether the speed counts as fast. [wasFast] is the previous answer: a pulse that is off starts at
     * [FAST_SPEED_START_BPS], a running one stops below [FAST_SPEED_STOP_BPS].
     */
    fun isFastSpeed(bytesPerSecond: Long, wasFast: Boolean): Boolean =
        bytesPerSecond >= if (wasFast) FAST_SPEED_STOP_BPS else FAST_SPEED_START_BPS

    /** True while the alarm is ahead and closer than [ALARM_SOON_MS]; 0 or a past time means no alarm. */
    fun isAlarmSoon(nextAlarmMs: Long, nowMs: Long): Boolean =
        nextAlarmMs > nowMs && nextAlarmMs - nowMs < ALARM_SOON_MS

    /**
     * Wait before asking [isAlarmSoon] again: until the glow window opens, or while open, until the alarm rings. Null
     * when nothing is left to wait for.
     */
    fun alarmRecheckDelayMs(nextAlarmMs: Long, nowMs: Long): Long? {
        if (nextAlarmMs <= nowMs) return null
        val left = nextAlarmMs - nowMs
        val wait = if (left < ALARM_SOON_MS) left else left - ALARM_SOON_MS + 1
        return wait.coerceIn(1L, ALARM_RECHECK_MAX_MS)
    }

    /** Attention state of a glyph; [animationsOff] always wins. */
    fun forWidget(kind: IdleWidgetKind, fastSpeed: Boolean, alarmSoon: Boolean, animationsOff: Boolean): GlyphAttention {
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
