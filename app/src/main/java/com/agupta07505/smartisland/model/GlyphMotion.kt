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

/** The small looping motion an idle widget glyph can carry. */
enum class GlyphMotionKind { None, SunSpin, RainFall, WindSway }

/** One motion and how long a full loop takes. [None] has no period. */
data class GlyphMotion(val kind: GlyphMotionKind, val periodMs: Int = 0) {
    val isRunning: Boolean get() = kind != GlyphMotionKind.None && periodMs > 0

    companion object {
        val NONE = GlyphMotion(GlyphMotionKind.None)
    }
}

/** Which motion a glyph gets and how fast. Pure, so it can be tested without Compose. */
object GlyphMotions {
    /** A full turn of the sun takes this long: slow enough to read as ambient, not as a spinner. */
    const val SUN_PERIOD_MS = 24_000

    /** One raindrop falls once in this time (a second drop follows half a loop later). */
    const val RAIN_PERIOD_MS = 1_300

    /** Below this wind speed (km/h) the wind glyph stays still. */
    const val WIND_CALM_KMH = 2f

    /** From this speed (km/h) on the wind glyph moves at its fastest. */
    const val WIND_STRONG_KMH = 60f

    /** One sway loop at the calm end and at the strong end of the range, in ms. */
    const val WIND_SLOWEST_MS = 5_000
    const val WIND_FASTEST_MS = 700

    /** How far the wind glyph sways sideways, as a share of its size, each way. */
    const val WIND_SWAY_SHARE = 0.12f

    /**
     * Wind sway loop length for a wind speed in km/h: longer when calm, shorter when strong, in between in proportion.
     * 0 means "do not move" (unknown, negative or calm wind).
     */
    fun windPeriodMs(windKmh: Float): Int {
        if (windKmh.isNaN() || windKmh < WIND_CALM_KMH) return 0
        val t = ((windKmh - WIND_CALM_KMH) / (WIND_STRONG_KMH - WIND_CALM_KMH)).coerceIn(0f, 1f)
        return Math.round(WIND_SLOWEST_MS + (WIND_FASTEST_MS - WIND_SLOWEST_MS) * t)
    }

    /**
     * The motion for one idle widget glyph.
     * [look] and [isDay] describe the weather (null when it is not loaded); [windKmh] is negative when unknown;
     * [animationsOff] is the system's "remove animations" and always wins.
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

    /** Degrees the sun is turned at [phase] (0..1 of a loop). */
    fun sunAngle(phase: Float): Float = (phase - Math.floor(phase.toDouble()).toFloat()) * 360f

    /** Sideways offset of the wind glyph as a share of its size at [phase] (0..1 of a loop). */
    fun windOffsetShare(phase: Float): Float = (sin(2.0 * PI * phase) * WIND_SWAY_SHARE).toFloat()

    /** A raindrop's place in its fall: vertical offset (share of the glyph size, -0.4 top to +0.4 bottom) and opacity. */
    data class Drop(val offsetShare: Float, val alpha: Float)

    /** [phase] is 0..1 through one fall. The drop fades in at the top and out at the bottom. */
    fun drop(phase: Float): Drop {
        val p = phase - Math.floor(phase.toDouble()).toFloat()
        return Drop(offsetShare = -0.4f + 0.8f * p, alpha = sin(PI * p).toFloat())
    }
}

/**
 * The morph between two weather glyphs: the old glyph turns and shrinks away while the new one turns in and grows to
 * full size, cross-fading so the two shapes blend. Pure, so the timing and the "did the look change" rule can be tested.
 */
object WeatherMorph {
    /** The whole morph takes this long. The ambient motions start only after it. */
    const val MORPH_MS = 650

    /** How far each glyph turns, in degrees: the old one leaves this far, the new one arrives from the same way round. */
    const val TURN_DEGREES = 60f

    /** The smallest size, as a share of full size, a glyph shrinks to (the old one at the end, the new one at the start). */
    const val MIN_SCALE = 0.6f

    /** One glyph's look at one moment of the morph. */
    data class Layer(val alpha: Float, val scale: Float, val rotationDegrees: Float)

    /** Both glyphs at one moment of the morph. */
    data class Frame(val outgoing: Layer, val incoming: Layer)

    /** The sun and moon variants only differ for these looks; for any other look day and night draw the same glyph. */
    private fun dayChangesGlyph(look: WeatherLook) = look == WeatherLook.Clear || look == WeatherLook.PartlyCloudy

    /**
     * True when the glyph drawn for the weather really changes: another look, or day turning to night (or back) on a look that
     * has a separate night glyph. A refresh with the same look, and a temperature-only change, never morph.
     */
    fun changed(fromLook: WeatherLook, fromIsDay: Boolean, toLook: WeatherLook, toIsDay: Boolean): Boolean =
        fromLook != toLook || (fromIsDay != toIsDay && dayChangesGlyph(toLook))

    /** Smooth start and end, so the turn does not begin or stop abruptly. */
    private fun ease(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return p * p * (3f - 2f * p)
    }

    /**
     * The two glyphs at [progress] (0 = old glyph fully shown, 1 = new glyph fully shown). The alphas always add up to 1, so
     * the glyph never dips to nothing in the middle.
     */
    fun frame(progress: Float): Frame {
        val e = ease(progress)
        return Frame(
            outgoing = Layer(alpha = 1f - e, scale = 1f - (1f - MIN_SCALE) * e, rotationDegrees = TURN_DEGREES * e),
            incoming = Layer(alpha = e, scale = MIN_SCALE + (1f - MIN_SCALE) * e, rotationDegrees = -TURN_DEGREES * (1f - e))
        )
    }
}

/** The attention state an idle widget glyph can be in. */
enum class GlyphAttention { None, SpeedPulse, AlarmGlow }

/**
 * Attention states: a fast connection makes the speed arrow breathe, an alarm that is about to ring makes the alarm
 * glyph glow. Both are soft, slow breathing, never a flash. Pure, so the thresholds, the hysteresis and the timing can be
 * tested without Compose.
 */
object AttentionStates {
    /** The speed arrow starts pulsing from this speed: 1 MB/s (decimal, the same MB as the speed text). */
    const val FAST_SPEED_START_BPS = 1_000_000L

    /** Once pulsing, the arrow only stops when the speed falls below this, so it does not flicker around 1 MB/s. */
    const val FAST_SPEED_STOP_BPS = 800_000L

    /** The alarm glyph glows when the next alarm is less than this far away: 10 minutes. */
    const val ALARM_SOON_MS = 10 * 60_000L

    /** The longest the glow waits before looking at the clock again, so a changed system clock is noticed. */
    const val ALARM_RECHECK_MAX_MS = 60_000L

    /** One breath (in and out) takes this long: slow, like the privacy glyph. */
    const val SPEED_PULSE_MS = 2_400
    const val ALARM_GLOW_MS = 2_800

    /** The speed arrow grows by this share of its size at the top of a breath. */
    const val SPEED_PULSE_GROWTH = 0.12f

    /** The alarm halo's opacity at the bottom and at the top of a breath. */
    const val GLOW_MIN_ALPHA = 0.25f
    const val GLOW_MAX_ALPHA = 0.75f

    /** The alarm halo's size, as a share of the glyph size, at the bottom and at the top of a breath. */
    const val GLOW_MIN_SCALE = 0.9f
    const val GLOW_MAX_SCALE = 1.15f

    /**
     * Whether the speed counts as fast. [wasFast] is the previous answer: below [FAST_SPEED_START_BPS] a pulse that is not
     * running stays off, and a running one carries on until the speed falls below [FAST_SPEED_STOP_BPS].
     */
    fun isFastSpeed(bytesPerSecond: Long, wasFast: Boolean): Boolean =
        bytesPerSecond >= if (wasFast) FAST_SPEED_STOP_BPS else FAST_SPEED_START_BPS

    /** True while the alarm is still ahead and less than [ALARM_SOON_MS] away. 0 or a past time means no alarm. */
    fun isAlarmSoon(nextAlarmMs: Long, nowMs: Long): Boolean =
        nextAlarmMs > nowMs && nextAlarmMs - nowMs < ALARM_SOON_MS

    /**
     * How long to wait before asking [isAlarmSoon] again: until the glow window opens, or, while it is open, until the alarm
     * rings. Null when there is nothing left to wait for (no alarm, or it has already rung).
     */
    fun alarmRecheckDelayMs(nextAlarmMs: Long, nowMs: Long): Long? {
        if (nextAlarmMs <= nowMs) return null
        val left = nextAlarmMs - nowMs
        val wait = if (left < ALARM_SOON_MS) left else left - ALARM_SOON_MS + 1
        return wait.coerceIn(1L, ALARM_RECHECK_MAX_MS)
    }

    /** Which attention state a glyph is in. [animationsOff] is the system's "remove animations" and always wins. */
    fun forWidget(kind: IdleWidgetKind, fastSpeed: Boolean, alarmSoon: Boolean, animationsOff: Boolean): GlyphAttention {
        if (animationsOff) return GlyphAttention.None
        return when {
            kind == IdleWidgetKind.NetSpeed && fastSpeed -> GlyphAttention.SpeedPulse
            kind == IdleWidgetKind.NextAlarm && alarmSoon -> GlyphAttention.AlarmGlow
            else -> GlyphAttention.None
        }
    }

    /** The loop length of an attention state, 0 for none. */
    fun periodMs(attention: GlyphAttention): Int = when (attention) {
        GlyphAttention.SpeedPulse -> SPEED_PULSE_MS
        GlyphAttention.AlarmGlow -> ALARM_GLOW_MS
        GlyphAttention.None -> 0
    }

    /** A smooth breath: 0 at [phase] 0, 1 half a loop later, back to 0 at the end, with no sharp turn at either end. */
    fun breath(phase: Float): Float {
        val p = phase - Math.floor(phase.toDouble()).toFloat()
        return (0.5 - 0.5 * cos(2.0 * PI * p)).toFloat()
    }

    /** The speed arrow's scale at a [breath] value (1 at rest). */
    fun pulseScale(breath: Float): Float = 1f + SPEED_PULSE_GROWTH * breath.coerceIn(0f, 1f)

    /** The alarm halo's opacity at a [breath] value. */
    fun glowAlpha(breath: Float): Float = GLOW_MIN_ALPHA + (GLOW_MAX_ALPHA - GLOW_MIN_ALPHA) * breath.coerceIn(0f, 1f)

    /** The alarm halo's size, as a share of the glyph size, at a [breath] value. */
    fun glowScale(breath: Float): Float = GLOW_MIN_SCALE + (GLOW_MAX_SCALE - GLOW_MIN_SCALE) * breath.coerceIn(0f, 1f)
}
