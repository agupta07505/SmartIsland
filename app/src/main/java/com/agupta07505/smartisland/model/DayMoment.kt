/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Moments the Day moments widget marks in the pill's left slot at local time: sunrise 06:00, afternoon 12:00, sunset
 * 17:30, midnight 23:00. Pure rules; drawing is in ui/DayMoment.kt.
 */
enum class DayMoment(val id: String, val hour: Int, val minute: Int, val towardsDown: Boolean) {
    Sunrise("sunrise", 6, 0, false),
    Afternoon("afternoon", 12, 0, false),
    Sunset("sunset", 17, 30, true),
    Midnight("midnight", 23, 0, true)
}

enum class MomentMotion {
    /** Sun rises from behind the horizon line; the old glyph sinks away. */
    RiseFromBottom,
    /** New glyph grows out of the old one in place. */
    GrowFromPrevious,
    /** Old sun sinks past the bottom edge; the horizon glyph settles in. */
    SinkIntoBottom
}

enum class MomentGlyph { Horizon, Sun, Moon }

/** Glyph pose at a tick progress: [offsetShare] of its height (down = positive), [scale], [alpha]. */
data class GlyphPose(val offsetShare: Float, val scale: Float, val alpha: Float, val lift: Float = 1f, val stretch: Float = 1f)

object DayMoments {
    /** How far a sinking glyph goes below the slot's bottom edge (share of its height), hidden by the clip. */
    const val HIDDEN_BELOW_SHARE = 1.1f
    const val GROW_FROM_SCALE = 0.45f
    const val HORIZON_IN_SHARE = 0.35f

    fun glyph(moment: DayMoment): MomentGlyph = when (moment) {
        DayMoment.Sunrise, DayMoment.Sunset -> MomentGlyph.Horizon
        DayMoment.Afternoon -> MomentGlyph.Sun
        DayMoment.Midnight -> MomentGlyph.Moon
    }

    fun motion(moment: DayMoment): MomentMotion = when (moment) {
        DayMoment.Sunrise -> MomentMotion.RiseFromBottom
        DayMoment.Afternoon, DayMoment.Midnight -> MomentMotion.GrowFromPrevious
        DayMoment.Sunset -> MomentMotion.SinkIntoBottom
    }

    /** Incoming glyph at tick [progress] (0..1; the heavy tick's wind-up below 0 counts as 0). */
    fun incomingPose(moment: DayMoment, progress: Float): GlyphPose {
        val p = progress.coerceIn(0f, 1f)
        return when (motion(moment)) {
            // The horizon line stays put; only the sun rises behind it.
            MomentMotion.RiseFromBottom -> GlyphPose(0f, 1f, (p / HORIZON_IN_SHARE).coerceIn(0f, 1f), lift = p)
            MomentMotion.GrowFromPrevious -> GlyphPose(0f, GROW_FROM_SCALE + (1f - GROW_FROM_SCALE) * p, p)
            MomentMotion.SinkIntoBottom -> GlyphPose(0f, 1f, (p / HORIZON_IN_SHARE).coerceIn(0f, 1f), lift = 1f)
        }
    }

    fun outgoingPose(moment: DayMoment, progress: Float): GlyphPose {
        val p = progress.coerceIn(0f, 1f)
        return when (motion(moment)) {
            MomentMotion.RiseFromBottom -> GlyphPose(HIDDEN_BELOW_SHARE * p, 1f, 1f - p)
            // The old glyph swells into the new one so the two read as one shape.
            MomentMotion.GrowFromPrevious -> GlyphPose(0f, 1f + (1f - GROW_FROM_SCALE) * 0.25f * p, 1f - p)
            MomentMotion.SinkIntoBottom -> GlyphPose(HIDDEN_BELOW_SHARE * p, 1f, 1f - p)
        }
    }

    fun previous(moment: DayMoment): DayMoment {
        val all = DayMoment.values()
        return all[Math.floorMod(moment.ordinal - 1, all.size)]
    }

    /** A moment on a full hour waits this long (ms) for the hour tick to finish first. */
    const val AFTER_HOUR_TICK_MS = 7000L

    /** Same timing as the hour chime: hold, heavy tick, hold, short fades (ms). */
    const val HOLD_BEFORE_MS = HourChime.HOLD_BEFORE_MS
    const val TICK_MS = HourChime.TICK_MS
    const val HOLD_AFTER_MS = HourChime.HOLD_AFTER_MS
    const val FADE_IN_MS = HourChime.FADE_IN_MS
    const val FADE_OUT_MS = HourChime.FADE_OUT_MS

    /** When [moment] happens [dayOffset] days after the day of [nowMs] in [zone]. */
    fun atMs(moment: DayMoment, nowMs: Long, dayOffset: Int = 0, zone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = nowMs
        cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        cal.set(Calendar.HOUR_OF_DAY, moment.hour)
        cal.set(Calendar.MINUTE, moment.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** First moment strictly after [nowMs] and when it happens (tomorrow's once today's four have passed). */
    fun next(nowMs: Long, zone: TimeZone = TimeZone.getDefault()): Pair<DayMoment, Long> =
        DayMoment.values()
            .flatMap { m -> listOf(0, 1).map { d -> m to atMs(m, nowMs, d, zone) } }
            .filter { it.second > nowMs }
            .minByOrNull { it.second }!!
}
