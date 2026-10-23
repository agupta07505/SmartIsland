/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundAppTest {

    private val ignored: (String) -> Boolean = { it == "com.agupta07505.smartisland" || it == "com.android.systemui" || it == "com.keyboard" }

    private fun window(pkg: String?, app: Boolean = true, active: Boolean = false, focused: Boolean = false) =
        AppWindow(pkg, app, active, focused)

    @Test
    fun theFocusedApplicationWindowIsTheForegroundApp() {
        val windows = listOf(
            window("com.google.android.youtube", active = false, focused = false),
            window("com.some.dialog", active = true, focused = true)
        )
        assertEquals("com.some.dialog", ForegroundApp.pick(windows, ignored))
    }

    @Test
    fun aFloatingWindowThatIsNotFocusedDoesNotReplaceTheApp() {
        val windows = listOf(
            window("com.google.android.youtube", active = true, focused = true),
            window("com.some.floating", active = false, focused = false)
        )
        assertEquals("com.google.android.youtube", ForegroundApp.pick(windows, ignored))
    }

    @Test
    fun activeIsTheFallbackWhenNothingIsFocused() {
        val windows = listOf(window("com.a", active = false), window("com.b", active = true))
        assertEquals("com.b", ForegroundApp.pick(windows, ignored))
    }

    @Test
    fun thisAppSystemUiAndKeyboardsNeverCount() {
        val windows = listOf(
            window("com.agupta07505.smartisland", focused = true),
            window("com.android.systemui", focused = true),
            window("com.keyboard", focused = true)
        )
        assertNull(ForegroundApp.pick(windows, ignored))
    }

    @Test
    fun nonApplicationWindowsAndWindowsWithoutAPackageAreSkipped() {
        val windows = listOf(window("com.a", app = false, focused = true), window(null, focused = true), window("", focused = true))
        assertNull(ForegroundApp.pick(windows, ignored))
        assertNull(ForegroundApp.pick(emptyList(), ignored))
    }

    @Test
    fun liveCardsOfTheForegroundAppAreHidden() {
        val live = listOf(
            IslandMode.Music, IslandMode.Navigation, IslandMode.Timer, IslandMode.Stopwatch, IslandMode.ScreenRecording,
            IslandMode.AudioRecording, IslandMode.Hotspot, IslandMode.LiveActivity, IslandMode.DownloadUpload
        )
        for (mode in live) assertTrue("$mode", ForegroundApp.hidesCard(mode, "com.app", "com.app"))
    }

    @Test
    fun callsNotificationsAndSystemEventsAreNeverHidden() {
        val kept = listOf(
            IslandMode.IncomingCall, IslandMode.Notification, IslandMode.Battery, IslandMode.Bluetooth,
            IslandMode.Flashlight, IslandMode.Empty
        )
        for (mode in kept) assertFalse("$mode", ForegroundApp.hidesCard(mode, "com.app", "com.app"))
    }

    @Test
    fun anotherAppsCardsAndANullForegroundHideNothing() {
        assertFalse(ForegroundApp.hidesCard(IslandMode.Music, "com.app", "com.other"))
        assertFalse(ForegroundApp.hidesCard(IslandMode.Music, "com.app", null))
        assertFalse(ForegroundApp.hidesCard(IslandMode.Music, "com.app", ""))
    }
}
