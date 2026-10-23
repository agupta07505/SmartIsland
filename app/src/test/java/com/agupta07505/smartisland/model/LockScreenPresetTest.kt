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

class LockScreenPresetTest {
    private val base = SmartIslandSettings.Default.copy(showOnLockScreen = true)

    @Test
    fun everyPresetReadsBackAsItself() {
        LockScreenPreset.entries.forEach { preset ->
            assertEquals(preset, LockScreenPreset.from(preset.applyTo(base)))
            assertEquals(preset, LockScreenPreset.from(preset.applyTo(base.copy(hideOnLockScreenEvents = setOf("music")))))
        }
    }

    @Test
    fun showAllKeepsEverythingAndMessagesOnlyHidesEverythingButNotificationsAndCalls() {
        val all = LockScreenPreset.ShowAll.applyTo(base.copy(hideOnLockScreenEvents = setOf("music")))
        assertTrue(all.hideOnLockScreenEvents.isEmpty())
        assertEquals(false, all.hideAllOnLockScreen)

        val messages = LockScreenPreset.MessagesOnly.applyTo(base)
        assertEquals(false, messages.isEventHiddenOnLockScreen("notification"))
        assertEquals(false, messages.isEventHiddenOnLockScreen("call"))
        assertEquals(true, messages.isEventHiddenOnLockScreen("music"))
    }

    @Test
    fun callsOnlyHidesEverythingButCallsAndOffHidesTheIslandCompletely() {
        val calls = LockScreenPreset.CallsOnly.applyTo(base)
        assertEquals(true, calls.isEventHiddenOnLockScreen("notification"))
        assertEquals(false, calls.isEventHiddenOnLockScreen("call"))

        assertEquals(false, LockScreenPreset.Off.applyTo(base).showOnLockScreen)
        assertEquals(LockScreenPreset.Off, LockScreenPreset.from(SmartIslandSettings.Default))
    }

    @Test
    fun aCustomMixSelectsNoPreset() {
        assertNull(LockScreenPreset.from(base.copy(hideOnLockScreenEvents = setOf("music"))))
    }

    @Test
    fun applyingAPresetKeepsEveryOtherSetting() {
        val custom = base.copy(width = 140f, hideOnLockScreenPackages = setOf("com.example.a"))
        val result = LockScreenPreset.CallsOnly.applyTo(custom)
        assertEquals(140f, result.width)
        assertEquals(setOf("com.example.a"), result.hideOnLockScreenPackages)
    }
}
