/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.SmartIslandSettings
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleScheduleTest {
    private val all = listOf(IdleWidgetKind.Weather, IdleWidgetKind.Date, IdleWidgetKind.Battery, IdleWidgetKind.NetSpeed)

    private fun prefs(
        work: Boolean = false,
        night: Boolean = false,
        charging: Boolean = false,
        workHidden: String = "",
        nightHidden: String = "",
        chargingHidden: String = "",
        weekdays: Boolean = true
    ) = IdleSchedulePrefs(
        workEnabled = work, workHidden = workHidden, workWeekdaysOnly = weekdays,
        nightEnabled = night, nightHidden = nightHidden,
        chargingEnabled = charging, chargingHidden = chargingHidden
    )

    @Test fun windowWrapsMidnightAndEqualEndsAreEmpty() {
        assertTrue(IdleSchedule.inWindow(22, 6, 23))
        assertTrue(IdleSchedule.inWindow(22, 6, 5))
        assertFalse(IdleSchedule.inWindow(22, 6, 6))
        assertTrue(IdleSchedule.inWindow(9, 17, 9))
        assertFalse(IdleSchedule.inWindow(9, 17, 17))
        assertFalse(IdleSchedule.inWindow(8, 8, 8))
    }

    @Test fun nothingSwitchedOnIsAlwaysDay() {
        assertEquals(IdleSchedule.Context.Day, IdleSchedule.contextFor(prefs(), charging = true, hour = 23, dayOfWeek = Calendar.MONDAY))
    }

    @Test fun chargingBeatsNightBeatsWork() {
        val p = prefs(work = true, night = true, charging = true)
        assertEquals(IdleSchedule.Context.Charging, IdleSchedule.contextFor(p, true, 23, Calendar.MONDAY))
        assertEquals(IdleSchedule.Context.Night, IdleSchedule.contextFor(p, false, 23, Calendar.MONDAY))
        assertEquals(IdleSchedule.Context.Work, IdleSchedule.contextFor(p, false, 10, Calendar.MONDAY))
        assertEquals(IdleSchedule.Context.Day, IdleSchedule.contextFor(p, false, 19, Calendar.MONDAY))
    }

    @Test fun aSwitchedOffRuleNeverApplies() {
        val p = prefs(work = true, night = false, charging = false)
        assertEquals(IdleSchedule.Context.Work, IdleSchedule.contextFor(p, true, 10, Calendar.TUESDAY))
        assertEquals(IdleSchedule.Context.Day, IdleSchedule.contextFor(p, false, 23, Calendar.TUESDAY))
    }

    @Test fun workHoursCanBeWeekdaysOnly() {
        val weekdays = prefs(work = true, weekdays = true)
        assertEquals(IdleSchedule.Context.Day, IdleSchedule.contextFor(weekdays, false, 10, Calendar.SATURDAY))
        assertEquals(IdleSchedule.Context.Day, IdleSchedule.contextFor(weekdays, false, 10, Calendar.SUNDAY))
        assertEquals(IdleSchedule.Context.Work, IdleSchedule.contextFor(weekdays, false, 10, Calendar.FRIDAY))
        val everyDay = prefs(work = true, weekdays = false)
        assertEquals(IdleSchedule.Context.Work, IdleSchedule.contextFor(everyDay, false, 10, Calendar.SUNDAY))
    }

    @Test fun filterDropsOnlyTheHiddenOfTheContextInForce() {
        val p = prefs(night = true, nightHidden = "weather,speed", work = true, workHidden = "date")
        assertEquals(listOf(IdleWidgetKind.Date, IdleWidgetKind.Battery), IdleSchedule.filter(all, p, IdleSchedule.Context.Night))
        assertEquals(
            listOf(IdleWidgetKind.Weather, IdleWidgetKind.Battery, IdleWidgetKind.NetSpeed),
            IdleSchedule.filter(all, p, IdleSchedule.Context.Work)
        )
        assertEquals(all, IdleSchedule.filter(all, p, IdleSchedule.Context.Day))
        assertEquals(all, IdleSchedule.filter(all, p, IdleSchedule.Context.Charging))
    }

    @Test fun everythingHiddenLeavesNothing() {
        val p = prefs(night = true, nightHidden = all.joinToString(",") { it.id })
        assertTrue(IdleSchedule.filter(all, p, IdleSchedule.Context.Night).isEmpty())
    }

    @Test fun allowsFollowsTheHiddenList() {
        val p = prefs(charging = true, chargingHidden = "clock")
        assertFalse(IdleSchedule.allows(p, IdleSchedule.Context.Charging, IdleWidgetKind.Clock))
        assertTrue(IdleSchedule.allows(p, IdleSchedule.Context.Charging, IdleWidgetKind.Weather))
        assertTrue(IdleSchedule.allows(p, IdleSchedule.Context.Day, IdleWidgetKind.Clock))
    }

    @Test fun toggleHiddenAddsRemovesAndIgnoresUnknown() {
        val once = IdleSchedule.toggleHidden("bogus", IdleWidgetKind.Weather)
        assertEquals("weather", once)
        assertEquals("weather,date", IdleSchedule.toggleHidden(once, IdleWidgetKind.Date))
        assertEquals("date", IdleSchedule.toggleHidden("weather,date", IdleWidgetKind.Weather))
        assertEquals("", IdleSchedule.toggleHidden("weather", IdleWidgetKind.Weather))
    }

    @Test fun normalizeHiddenKeepsKnownDistinctIds() {
        assertEquals("weather,date", IdleSchedule.normalizeHidden(" weather,bogus,date,weather"))
        assertEquals("", IdleSchedule.normalizeHidden(""))
    }

    @Test fun pinnedWidgetLeadsOnlyWhenItIsInTheList() {
        assertEquals(
            listOf(IdleWidgetKind.Battery, IdleWidgetKind.Weather, IdleWidgetKind.Date, IdleWidgetKind.NetSpeed),
            IdleSchedule.ordered(all, "battery")
        )
        assertEquals(all, IdleSchedule.ordered(all, "weather"))
        assertEquals(all, IdleSchedule.ordered(all, "alarm"))
        assertEquals(all, IdleSchedule.ordered(all, ""))
        assertEquals(all, IdleSchedule.ordered(all, "bogus"))
    }

    @Test fun togglePinnedPinsAndUnpins() {
        assertEquals("date", IdleSchedule.togglePinned("", IdleWidgetKind.Date))
        assertEquals("battery", IdleSchedule.togglePinned("date", IdleWidgetKind.Battery))
        assertEquals("", IdleSchedule.togglePinned("date", IdleWidgetKind.Date))
        assertEquals("", IdleSchedule.normalizePinned("bogus"))
    }

    @Test fun moveToFrontKeepsTheRest() {
        assertEquals("battery,weather,date", IdleSchedule.moveToFront("weather,date,battery", IdleWidgetKind.Battery))
        assertEquals("weather,date", IdleSchedule.moveToFront("weather,date", IdleWidgetKind.Battery))
    }

    @Test fun moveTakesThePlaceOfTheTarget() {
        assertEquals("date,battery,weather", IdleSchedule.move("weather,date,battery", IdleWidgetKind.Weather, IdleWidgetKind.Battery))
        assertEquals("battery,weather,date", IdleSchedule.move("weather,date,battery", IdleWidgetKind.Battery, IdleWidgetKind.Weather))
        assertEquals("weather,date", IdleSchedule.move("weather,date", IdleWidgetKind.Weather, IdleWidgetKind.Weather))
        assertEquals("weather,date", IdleSchedule.move("weather,date", IdleWidgetKind.Battery, IdleWidgetKind.Weather))
    }

    @Test fun needsClockOnlyForWorkAndNight() {
        assertFalse(prefs(charging = true).needsClock)
        assertTrue(prefs(work = true).needsClock)
        assertTrue(prefs(night = true).needsClock)
    }

    @Test fun settingsBackupRoundTrips() {
        val s = SmartIslandSettings(
            idleScheduleWorkEnabled = true,
            idleScheduleWorkHidden = "weather,date",
            idleScheduleWorkStartHour = 8,
            idleScheduleWorkEndHour = 18,
            idleScheduleWorkWeekdaysOnly = false,
            idleScheduleNightEnabled = true,
            idleScheduleNightHidden = "speed",
            idleScheduleNightStartHour = 23,
            idleScheduleNightEndHour = 7,
            idleScheduleChargingEnabled = true,
            idleScheduleChargingHidden = "clock",
            idleWidgetPinnedFirst = "battery"
        )
        val back = SmartIslandSettings.fromJson(s.toJson(appVersion = "7.0.0"))
        assertEquals(s.idleSchedulePrefs(), back.idleSchedulePrefs())
    }
}
