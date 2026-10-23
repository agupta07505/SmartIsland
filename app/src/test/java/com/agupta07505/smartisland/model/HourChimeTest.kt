/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HourChimeTest {
    private val ist = TimeZone.getTimeZone("Asia/Kolkata")

    private fun at(h: Int, m: Int, s: Int = 0, ms: Int = 0, zone: TimeZone = ist): Long =
        Calendar.getInstance(zone).also { it.clear(); it.set(2026, Calendar.OCTOBER, 11, h, m, s); it.set(Calendar.MILLISECOND, ms) }.timeInMillis

    @Test
    fun nextFullHourIsTheTopOfTheNextHour() {
        assertEquals(at(13, 0), HourChime.nextFullHourMs(at(12, 0, 0, 1), ist))
        assertEquals(at(13, 0), HourChime.nextFullHourMs(at(12, 59, 59, 999), ist))
        assertEquals(at(13, 0), HourChime.nextFullHourMs(at(12, 30), ist))
    }

    @Test
    fun exactlyOnTheHourWaitsForTheNextOne() {
        assertEquals(at(14, 0), HourChime.nextFullHourMs(at(13, 0), ist))
    }

    @Test
    fun midnightRollsOver() {
        val next = HourChime.nextFullHourMs(at(23, 15), ist)
        assertEquals(0, HourChime.hourOf(next, ist))
        assertTrue(next > at(23, 15))
    }

    @Test
    fun hourOfReadsTheLocalHour() {
        assertEquals(13, HourChime.hourOf(at(13, 0), ist))
        assertEquals(1, HourChime.hourOf(at(1, 0), ist))
    }

    @Test
    fun halfHourZonesStillLandOnTheFullHour() {
        val nepal = TimeZone.getTimeZone("Asia/Kathmandu")
        val next = HourChime.nextFullHourMs(at(9, 20, zone = nepal), nepal)
        assertEquals(at(10, 0, zone = nepal), next)
    }

    @Test
    fun previousHourWrapsAtMidnight() {
        assertEquals(23, HourChime.previousHour(0))
        assertEquals(0, HourChime.previousHour(1))
        assertEquals(12, HourChime.previousHour(13))
    }

    @Test
    fun hourLabels24h() {
        assertEquals("01", HourChime.hourLabel(1, true))
        assertEquals("13", HourChime.hourLabel(13, true))
        assertEquals("00", HourChime.hourLabel(0, true))
    }

    @Test
    fun hourLabels12h() {
        assertEquals("1", HourChime.hourLabel(1, false))
        assertEquals("1", HourChime.hourLabel(13, false))
        assertEquals("12", HourChime.hourLabel(0, false))
        assertEquals("12", HourChime.hourLabel(12, false))
    }

    @Test
    fun chimeLabelsPairTheOldAndNewHour() {
        val a = hourChimeLabels(13, true)
        assertEquals("12", a.oldHour); assertEquals("13", a.newHour)
        val b = hourChimeLabels(1, true)
        assertEquals("00", b.oldHour); assertEquals("01", b.newHour)
        val c = hourChimeLabels(0, false)
        assertEquals("11", c.oldHour); assertEquals("12", c.newHour)
        val d = hourChimeLabels(13, false)
        assertEquals("12", d.oldHour); assertEquals("1", d.newHour)
    }
}
