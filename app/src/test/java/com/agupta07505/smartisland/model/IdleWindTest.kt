/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.SmartIslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleWindTest {
    @Test
    fun autoFollowsTheRegionAndAnExplicitUnitWins() {
        assertEquals(IdleWidgets.WIND_MPH, IdleWidgets.resolveWindUnit(IdleWidgets.UNIT_AUTO, "US"))
        assertEquals(IdleWidgets.WIND_MPH, IdleWidgets.resolveWindUnit(IdleWidgets.UNIT_AUTO, "gb"))
        assertEquals(IdleWidgets.WIND_KMH, IdleWidgets.resolveWindUnit(IdleWidgets.UNIT_AUTO, "DE"))
        assertEquals(IdleWidgets.WIND_KMH, IdleWidgets.resolveWindUnit(IdleWidgets.UNIT_AUTO, ""))
        assertEquals(IdleWidgets.WIND_KN, IdleWidgets.resolveWindUnit(IdleWidgets.WIND_KN, "US"))
        assertEquals(IdleWidgets.WIND_MS, IdleWidgets.resolveWindUnit(IdleWidgets.WIND_MS, "GB"))
    }

    @Test
    fun speedsConvertAndRound() {
        assertEquals("36", IdleWidgets.displayWind(36f, IdleWidgets.WIND_KMH))
        assertEquals("10", IdleWidgets.displayWind(36f, IdleWidgets.WIND_MS))
        assertEquals("62", IdleWidgets.displayWind(100f, IdleWidgets.WIND_MPH))
        assertEquals("54", IdleWidgets.displayWind(100f, IdleWidgets.WIND_KN))
        assertEquals("--", IdleWidgets.displayWind(-1f, IdleWidgets.WIND_KMH))
    }

    @Test
    fun theIslandTextCarriesTheUnit() {
        assertEquals("12 km/h", IdleWidgets.windIslandText(12f, IdleWidgets.WIND_KMH))
        assertEquals("3.3 m/s", IdleWidgets.windIslandText(12f, IdleWidgets.WIND_MS))
        assertEquals("--", IdleWidgets.windIslandText(-1f, IdleWidgets.WIND_MPH))
    }

    @Test
    fun compassPointsRoundToTheNearestEighth() {
        assertEquals("N", IdleWidgets.compass(0))
        assertEquals("NE", IdleWidgets.compass(44))
        assertEquals("E", IdleWidgets.compass(90))
        assertEquals("SW", IdleWidgets.compass(225))
        assertEquals("N", IdleWidgets.compass(338))
        assertNull(IdleWidgets.compass(-1))
    }

    @Test
    fun windUnitSurvivesABackupAndABadOneFallsBackToAuto() {
        val original = SmartIslandSettings.Default.copy(idleWidgetWindUnit = IdleWidgets.WIND_KN, idleWidgetTempUnit = IdleWidgets.UNIT_FAHRENHEIT)
        assertEquals(original, SmartIslandSettings.fromJson(original.toJson(appVersion = "7.0.0")))
        val bad = original.copy(idleWidgetWindUnit = "furlongs")
        assertEquals(IdleWidgets.UNIT_AUTO, SmartIslandSettings.fromJson(bad.toJson(appVersion = "7.0.0")).idleWidgetWindUnit)
        assertTrue(IdleWidgets.UNIT_AUTO in IdleWidgets.VALID_WIND_UNITS)
    }
}
