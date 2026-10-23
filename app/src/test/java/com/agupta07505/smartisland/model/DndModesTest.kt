/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DndModesTest {
    @Test
    fun filtersOtherThanAllMeanDoNotDisturbIsOn() {
        assertFalse(DndModes.filterActive(DndModes.FILTER_ALL))
        assertFalse(DndModes.filterActive(DndModes.FILTER_UNKNOWN))
        assertTrue(DndModes.filterActive(DndModes.FILTER_PRIORITY))
        assertTrue(DndModes.filterActive(DndModes.FILTER_NONE))
        assertTrue(DndModes.filterActive(DndModes.FILTER_ALARMS))
    }

    @Test
    fun fromFilterNamesThePlainModes() {
        assertEquals(ModeStatus.Off, DndModes.fromFilter(DndModes.FILTER_ALL))
        assertEquals(ModeStatus.Off, DndModes.fromFilter(DndModes.FILTER_UNKNOWN))
        assertEquals("Do Not Disturb", DndModes.fromFilter(DndModes.FILTER_PRIORITY).name)
        assertEquals(ModeKind.TotalSilence, DndModes.fromFilter(DndModes.FILTER_NONE).kind)
        assertEquals(ModeKind.AlarmsOnly, DndModes.fromFilter(DndModes.FILTER_ALARMS).kind)
        assertTrue(DndModes.fromFilter(DndModes.FILTER_PRIORITY).active)
    }

    @Test
    fun kindsAreToldFromNames() {
        assertEquals(ModeKind.Bedtime, DndModes.kindFromName("Bedtime mode"))
        assertEquals(ModeKind.Bedtime, DndModes.kindFromName("Sleeping"))
        assertEquals(ModeKind.Driving, DndModes.kindFromName("Driving"))
        assertEquals(ModeKind.Driving, DndModes.kindFromName("Drive mode"))
        assertEquals(ModeKind.Theater, DndModes.kindFromName("Theatre"))
        assertEquals(ModeKind.Gaming, DndModes.kindFromName("Game Mode"))
        assertEquals(ModeKind.Work, DndModes.kindFromName("Work"))
        assertEquals(ModeKind.Dnd, DndModes.kindFromName("Do not disturb"))
        assertEquals(ModeKind.Custom, DndModes.kindFromName("Yoga"))
        assertEquals(ModeKind.Custom, DndModes.kindFromName(""))
    }

    @Test
    fun drivingWinsOverBedtimeWinsOverThePlainMode() {
        fun status(kind: ModeKind) = ModeStatus(true, kind, DndModes.defaultName(kind), fromRule = true)
        assertEquals(ModeKind.Driving, DndModes.pick(listOf(status(ModeKind.Bedtime), status(ModeKind.Driving)))?.kind)
        assertEquals(ModeKind.Bedtime, DndModes.pick(listOf(status(ModeKind.Dnd), status(ModeKind.Bedtime)))?.kind)
        assertEquals(ModeKind.Custom, DndModes.pick(listOf(status(ModeKind.Dnd), status(ModeKind.Custom)))?.kind)
        assertNull(DndModes.pick(emptyList()))
    }

    @Test
    fun aBlankNameFallsBackToTheKindsName() {
        assertEquals("Bedtime", DndModes.withName(ModeKind.Bedtime, null))
        assertEquals("Bedtime", DndModes.withName(ModeKind.Bedtime, "  "))
        assertEquals("Sleep time", DndModes.withName(ModeKind.Bedtime, " Sleep time "))
    }

    @Test
    fun detailLineSaysWhatTheModeLetsThrough() {
        assertEquals("Mode is on", DndModes.detail(ModeStatus(true, ModeKind.Bedtime, "Bedtime", fromRule = true)))
        assertEquals("Only alarms can interrupt", DndModes.detail(DndModes.fromFilter(DndModes.FILTER_ALARMS)))
        assertEquals("Nothing can interrupt", DndModes.detail(DndModes.fromFilter(DndModes.FILTER_NONE)))
        assertEquals("Only priority interruptions", DndModes.detail(DndModes.fromFilter(DndModes.FILTER_PRIORITY)))
    }

    @Test
    fun everyKindHasADistinctRankAndAName() {
        assertEquals(ModeKind.values().size, ModeKind.values().map { DndModes.rank(it) }.toSet().size)
        ModeKind.values().forEach { assertTrue(DndModes.defaultName(it).isNotBlank()) }
    }
}
