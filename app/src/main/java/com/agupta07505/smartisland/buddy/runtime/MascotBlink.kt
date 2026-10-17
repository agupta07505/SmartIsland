/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.runtime

import kotlin.random.Random

/**
 * When and how the buddy blinks at rest. Pure so it can be tested; the UI plays what it returns.
 * A blink is one short animation every few seconds and nothing in between.
 */
object MascotBlink {
    /** Seconds between blinks, twice as long in Battery Saver. */
    fun interval(lowPower: Boolean): ClosedFloatingPointRange<Float> = if (lowPower) 5.2f..12.8f else 2.6f..6.4f

    /** Share of blinks that are a quick double blink. */
    const val DOUBLE_BLINK_CHANCE = 0.18

    /** Eye height while shut, as a share of open. */
    const val SHUT_SCALE = 0.1f

    /** After this many blinks at rest its eyes grow heavy: about three minutes by day, under one late at night. */
    fun blinksBeforeSleep(hour: Int): Int = if (hour >= 22 || hour < 6) 12 else 36

    fun nextDelayMs(lowPower: Boolean, random: Random = Random): Long {
        val range = interval(lowPower)
        return ((range.start + random.nextFloat() * (range.endInclusive - range.start)) * 1000f).toLong()
    }

    fun isDouble(random: Random = Random): Boolean = random.nextDouble() < DOUBLE_BLINK_CHANCE

    /** One step of a blink: hold the previous value, then ease to [scale] over [durationMs]. */
    data class Step(val scale: Float, val durationMs: Int)

    /** A single blink lasts 0.17s: closes in 0.45 of it, opens in the rest. */
    val single: List<Step> = listOf(Step(SHUT_SCALE, 76), Step(1f, 94))

    /** A double blink lasts 0.44s, with the keyframes of the reference. */
    val double: List<Step> = listOf(
        Step(SHUT_SCALE, 88), Step(1f, 88), Step(1f, 79), Step(SHUT_SCALE, 88), Step(1f, 97)
    )

    val singleDurationMs: Int get() = single.sumOf { it.durationMs }
    val doubleDurationMs: Int get() = double.sumOf { it.durationMs }
}
