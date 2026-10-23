/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.ui.graphics.Color
import com.agupta07505.smartisland.ui.components.BatteryVisual
import com.agupta07505.smartisland.ui.components.batteryVisualColor
import com.agupta07505.smartisland.ui.components.vivid
import com.agupta07505.smartisland.ui.components.resolveBatteryVisual
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryVisualTest {
    @Test
    fun categoryDecidesTheState() {
        assertEquals(BatteryVisual.PluggedIn, resolveBatteryVisual("battery_plugged_in", "Plugged in", 15f))
        assertEquals(BatteryVisual.PluggedOut, resolveBatteryVisual("battery_plugged_out", "Unplugged", 80f))
        assertEquals(BatteryVisual.Full, resolveBatteryVisual("battery_full", "Fully Charged", 100f))
        assertEquals(BatteryVisual.Low, resolveBatteryVisual("battery_low", "Low Battery", 15f))
        assertEquals(BatteryVisual.Saver, resolveBatteryVisual("battery_saver", "Battery Saver ON", 60f))
    }

    @Test
    fun unpluggingAtLowPercentStaysUnplugged() {
        assertEquals(BatteryVisual.PluggedOut, resolveBatteryVisual("battery_plugged_out", "Unplugged", 12f))
    }

    @Test
    fun unknownCategoryFallsBackToTitleAndPercent() {
        assertEquals(BatteryVisual.Saver, resolveBatteryVisual(null, "Battery Saver ON", 60f))
        assertEquals(BatteryVisual.Low, resolveBatteryVisual(null, "Battery", 10f))
        assertEquals(BatteryVisual.PluggedIn, resolveBatteryVisual(null, "Battery", 60f))
    }

    @Test
    fun unpluggedLowAndSaverUseTheirOwnColours() {
        val user = Color(0xFF10B981)
        // PluggedIn and Full show the user's colour, brightened (see vividKeepsHueAndRaisesBrightness).
        assertEquals(vivid(user), batteryVisualColor(BatteryVisual.PluggedIn, user))
        assertEquals(vivid(user), batteryVisualColor(BatteryVisual.Full, user))
        assertNotEquals(user, batteryVisualColor(BatteryVisual.PluggedOut, user))
        assertNotEquals(user, batteryVisualColor(BatteryVisual.Low, user))
        assertNotEquals(user, batteryVisualColor(BatteryVisual.Saver, user))
    }

    @Test
    fun vividKeepsHueAndRaisesBrightness() {
        val dull = Color(0xFF10B981)
        val bright = vivid(dull)
        assertEquals(0.96f, maxOf(bright.red, bright.green, bright.blue), 0.01f)
        // Same hue family: green still leads, blue is still above red.
        assertTrue(bright.green > bright.blue && bright.blue > bright.red)
        // Already bright stays bright; grey stays grey; alpha is kept.
        assertEquals(1f, maxOf(vivid(Color.White).red, 0f), 0.001f)
        assertEquals(vivid(Color(0xFF808080)).red, vivid(Color(0xFF808080)).blue, 0.001f)
        // Colour alpha is stored in 8 bits (0.5 becomes 128/255), so allow one step of rounding.
        assertEquals(0.5f, vivid(dull.copy(alpha = 0.5f)).alpha, 0.005f)
    }
}
