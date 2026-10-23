/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Locale

/**
 * Texts of the collapsed pill's two glyph slots for the idle widgets that put text in them. Pure rules; drawing is
 * IdleWidgetSlot in ui/IdleWidgets.kt.
 */
object IdleSlots {
    /**
     * Glyph size choices (percent) in Layout > Idle widgets; stored values are kept within
     * [MIN_GLYPH_SCALE]..[MAX_GLYPH_SCALE].
     */
    val GLYPH_SCALES = listOf(85, 100, 115, 130)
    const val DEFAULT_GLYPH_SCALE = 100
    const val MIN_GLYPH_SCALE = 70
    const val MAX_GLYPH_SCALE = 150

    fun normalizeGlyphScale(percent: Int): Int = percent.coerceIn(MIN_GLYPH_SCALE, MAX_GLYPH_SCALE)

    /** Date widget: two-digit day ("05") on the left, standard month abbreviation ("Feb") on the right. */
    const val DAY_PATTERN = "dd"
    const val MONTH_PATTERN = "MMM"

    fun alarmShowsHour(tick: Int): Boolean = Math.floorMod(tick, 2) == 1

    /** Alarm hour as the system clock writes it: "07" in 24 h, "7" in 12 h ("12" at noon and midnight). */
    fun alarmHourText(hour24: Int, is24: Boolean): String {
        val h = Math.floorMod(hour24, 24)
        if (is24) return String.format(Locale.US, "%02d", h)
        val h12 = h % 12
        return (if (h12 == 0) 12 else h12).toString()
    }

    fun alarmMinuteText(minute: Int): String = String.format(Locale.US, "%02d", Math.floorMod(minute, 60))

    fun shortWatts(watts: Double): String {
        val tenth = Math.round(watts * 10.0) / 10.0
        return if (tenth >= 10.0) "${Math.round(watts)}W" else String.format(Locale.US, "%.1fW", tenth)
    }

    fun shortBatteryTemp(tenthsC: Int, fahrenheit: Boolean): String = IdleWidgets.displayTemp(tenthsC / 10f, fahrenheit)

    /** Battery shows wattage and temperature only while charging with a readable current; otherwise icon and percent. */
    fun batteryShowsPower(charging: Boolean, watts: Double?): Boolean = charging && watts != null

    /** Range (seconds) of the self-ticking glyph timer slider; stored values are kept within it. */
    const val MIN_TICK_SECONDS = 1.5f
    const val MAX_TICK_SECONDS = 10f
    const val DEFAULT_TICK_SECONDS = 2.5f

    fun normalizeTickSeconds(seconds: Float): Float =
        (if (seconds.isNaN()) DEFAULT_TICK_SECONDS else seconds).coerceIn(MIN_TICK_SECONDS, MAX_TICK_SECONDS)

    fun tickPeriodMs(seconds: Float): Long = (Math.round(normalizeTickSeconds(seconds) * 2f) * 500L)

    /** Idle glyphs tick left to right (out on the right, back in from the left) every this many ms. */
    const val SIDE_TICK_PERIOD_MS = 2500L

    /** The automatic widget switch takes this many ticks: old glyphs step out right, next ones step in from the left. */
    const val SWITCH_TICK_STEPS = 3

    /** Net speed below this (bytes/s) is too quiet to count as one side exceeding the other. */
    const val SPEED_DOMINANCE_MIN_BPS = 1_000L

    /** Faster side of the net speed widget; that slot ticks to its arrow instead of the sideways tick. */
    enum class SpeedLead { None, Upload, Download }

    fun speedLead(downBytesPerSecond: Long, upBytesPerSecond: Long): SpeedLead = when {
        maxOf(downBytesPerSecond, upBytesPerSecond) < SPEED_DOMINANCE_MIN_BPS -> SpeedLead.None
        upBytesPerSecond > downBytesPerSecond -> SpeedLead.Upload
        downBytesPerSecond > upBytesPerSecond -> SpeedLead.Download
        else -> SpeedLead.None
    }

    fun speedLeadOwnsSlot(lead: SpeedLead, right: Boolean): Boolean =
        (lead == SpeedLead.Upload && !right) || (lead == SpeedLead.Download && right)

    /**
     * Whether a slot takes the sideways tick on the 2.5 s beat. Alarm slots, the clock, weather slots mid-tick and a
     * net slot carrying an arrow skip it.
     */
    fun takesSideTick(kind: IdleWidgetKind, right: Boolean, ownTickRunning: Boolean, lead: SpeedLead): Boolean = when {
        ownTickRunning -> false
        kind == IdleWidgetKind.Clock || kind == IdleWidgetKind.DayMoments -> false
        // The clock widget moves only when its hour or minute changes.
        kind == IdleWidgetKind.LiveClock -> false
        // Notifications and modes put their glyph in the left slot; the right slot (count) stays static while the glyph
        // ticks.
        (kind == IdleWidgetKind.Notifications || kind == IdleWidgetKind.Modes) && right -> false
        kind == IdleWidgetKind.NextAlarm && !right -> false
        kind == IdleWidgetKind.NetSpeed && speedLeadOwnsSlot(lead, right) -> false
        else -> true
    }

    /** Share of a heavy tick spent on the slow, suspended pull-back, and how far back it goes (share of the travel). */
    const val HEAVY_WINDUP_SHARE = 0.4f
    const val HEAVY_WINDUP_DEPTH = 0.12f

    /**
     * Heavy-tick easing: a slow suspended pull-back (to -[HEAVY_WINDUP_DEPTH]) over [HEAVY_WINDUP_SHARE], then a snap
     * to 1. Goes below 0 on purpose; clamp where it drives an alpha.
     */
    fun heavyTickProgress(fraction: Float): Float {
        val f = fraction.coerceIn(0f, 1f)
        if (f >= 1f) return 1f
        if (f < HEAVY_WINDUP_SHARE) {
            val u = f / HEAVY_WINDUP_SHARE
            return -HEAVY_WINDUP_DEPTH * (u * u * (3f - 2f * u))
        }
        val u = (f - HEAVY_WINDUP_SHARE) / (1f - HEAVY_WINDUP_SHARE)
        val fast = 1f - (1f - u) * (1f - u) * (1f - u)
        return -HEAVY_WINDUP_DEPTH + (1f + HEAVY_WINDUP_DEPTH) * fast
    }

    private const val HOP_SHARE = 0.45f
    private const val LAST_HOP_SHARE = 0.85f

    /** How much longer the last hop of the automatic switch is than the others (1 = equal). */
    const val LAST_HOP_WEIGHT = 1.6f

    /** Total switch time relative to [steps] equal hops, so the first hops keep their speed when the last is longer. */
    fun switchTimeScale(steps: Int = SWITCH_TICK_STEPS): Float {
        val n = steps.coerceAtLeast(1)
        return ((n - 1) + LAST_HOP_WEIGHT) / n
    }

    fun tickProgress(fraction: Float, steps: Int = SWITCH_TICK_STEPS): Float {
        val f = fraction.coerceIn(0f, 1f)
        if (f >= 1f) return 1f
        val n = steps.coerceAtLeast(1)
        val pos = f * ((n - 1) + LAST_HOP_WEIGHT)
        val step = pos.toInt().coerceAtMost(n - 1)
        val last = step == n - 1
        val local = (pos - step) / (if (last) LAST_HOP_WEIGHT else 1f)
        val hop = (local / (if (last) LAST_HOP_SHARE else HOP_SHARE)).coerceIn(0f, 1f)
        val left = 1f - hop
        val eased = if (last) 1f - left * left * left else 1f - left * left
        return (step + eased) / n
    }

    /** The right slot starts this long after the left, so the two read as one mechanical flip. */
    const val STAGGER_MS = 75

    /** Time the tick takes to land after its snap. */
    const val SETTLE_MS = 90
    const val SETTLE_OVERSHOOT = 0.03f

    /**
     * Heavy tick plus settle: over [tickMs] it is [heavyTickProgress] reaching a little past 1, then over [settleMs]
     * it eases back to 1, like a clock hand landing. [fraction] covers both parts.
     */
    fun settledTickProgress(fraction: Float, tickMs: Int, settleMs: Int = SETTLE_MS): Float {
        val f = fraction.coerceIn(0f, 1f)
        if (f >= 1f) return 1f
        val share = tickMs.toFloat() / (tickMs + settleMs)
        if (f < share) return heavyTickProgress(f / share) * (1f + SETTLE_OVERSHOOT)
        val s = (f - share) / (1f - share)
        val ease = s * s * (3f - 2f * s)
        return 1f + SETTLE_OVERSHOOT * (1f - ease)
    }

    /** Stretch along the travel at the fastest part of the snap: scale added, alpha taken off. */
    const val STRETCH_SCALE = 0.08f
    const val STRETCH_FADE = 0.12f

    /**
     * Whether the 2.5 s beat plays now: with "beat only when a value changes" ([onlyOnChange]) it needs a [changed]
     * value since the last beat; otherwise always.
     */
    fun beatDue(onlyOnChange: Boolean, changed: Boolean): Boolean = !onlyOnChange || changed

    /**
     * True while the pill cannot be seen: screen off, or the widget island open over it. (A card or held-off widget
     * removes the widget altogether, which also stops the beat.)
     */
    fun beatPaused(screenOn: Boolean, islandOpen: Boolean): Boolean = !screenOn || islandOpen

    /**
     * Whether idle ticks move at all. Same rule as the weather icon ([WeatherDisplay.iconAnimates]): Battery Saver or
     * "remove animations" turns ticks into a plain cross-fade.
     */
    fun ticksAnimate(saverOn: Boolean, systemAnimationsOff: Boolean): Boolean =
        WeatherDisplay.iconAnimates(enabled = true, pauseOnSaver = true, saverOn = saverOn, systemAnimationsOff = systemAnimationsOff)

    fun beatRuns(paused: Boolean, animates: Boolean): Boolean = !paused && animates

    /** Two halves of the sideways beat (out, then back in). */
    const val BEAT_HALF_MS = 220
    const val BEAT_WINDUP_MS = 260

    /** One whole beat of the right slot: pull-back, both halves, the settle, and its lag behind the left. */
    const val BEAT_MS = BEAT_WINDUP_MS + 2 * BEAT_HALF_MS + SETTLE_MS + STAGGER_MS

    fun switchSpanMs(): Long = Math.round(900 * switchTimeScale()).toLong()

    /**
     * Whether a beat may start without colliding with a widget switch: not while one is playing, and not when the
     * next automatic switch is due before the beat ends (0 = no automatic switching).
     */
    fun beatClearOfSwitch(sinceSwitchMs: Long, sinceCycleStartMs: Long, cycleMs: Long): Boolean {
        if (sinceSwitchMs < switchSpanMs()) return false
        if (cycleMs > 0L && cycleMs - sinceCycleStartMs <= BEAT_MS) return false
        return true
    }

    /** Plain cross-fade length (ms) for takeover ticks (hour, day moment, weather change) when ticks may not move. */
    const val PLAIN_FADE_MS = 260

    /** Total time of a takeover tick: heavy tick, settle and the right slot's lag. */
    fun takeoverSpanMs(tickMs: Int, settleMs: Int = SETTLE_MS, lagMs: Int = STAGGER_MS): Int = tickMs + settleMs + lagMs

    private fun takeoverMs(clock: Float, tickMs: Int, lagged: Boolean, settleMs: Int, lagMs: Int): Float =
        clock.coerceIn(0f, 1f) * takeoverSpanMs(tickMs, settleMs, lagMs) - (if (lagged) lagMs else 0)

    /**
     * Progress (0..1, slightly past 1 while settling) of one takeover-tick slot at [clock]; the right slot ([lagged])
     * starts [lagMs] later.
     */
    fun takeoverProgress(clock: Float, tickMs: Int, lagged: Boolean, settleMs: Int = SETTLE_MS, lagMs: Int = STAGGER_MS): Float {
        val total = tickMs + settleMs
        return settledTickProgress(takeoverMs(clock, tickMs, lagged, settleMs, lagMs) / total, tickMs, settleMs)
    }

    /** Stretch (0..1) of one slot of a takeover tick at [clock]: [stretchIntensity] of that slot's own tick. */
    fun takeoverStretch(clock: Float, tickMs: Int, lagged: Boolean, settleMs: Int = SETTLE_MS, lagMs: Int = STAGGER_MS): Float {
        val total = tickMs + settleMs
        return stretchIntensity(takeoverMs(clock, tickMs, lagged, settleMs, lagMs) / total, tickMs, settleMs)
    }

    private const val STRETCH_RAMP = 0.12f

    /**
     * 0..1 glyph speed at [fraction] of a settled tick: 0 in wind-up and settle, rising with the snap, falling as it
     * slows, so the stretch follows the speed.
     */
    fun stretchIntensity(fraction: Float, tickMs: Int, settleMs: Int = SETTLE_MS): Float {
        val f = fraction.coerceIn(0f, 1f)
        val share = tickMs.toFloat() / (tickMs + settleMs)
        if (f >= share) return 0f
        val u = f / share
        if (u <= HEAVY_WINDUP_SHARE) return 0f
        val w = (u - HEAVY_WINDUP_SHARE) / (1f - HEAVY_WINDUP_SHARE)
        val ramp = (w / STRETCH_RAMP).coerceIn(0f, 1f)
        return (1f - w) * (1f - w) * ramp
    }
}
