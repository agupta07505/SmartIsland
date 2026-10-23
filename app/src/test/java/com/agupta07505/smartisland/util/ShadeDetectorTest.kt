/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShadeDetectorTest {
    private val height = 2400

    private fun window(
        pkg: String? = ShadeDetector.SYSTEM_UI,
        title: String? = null,
        system: Boolean = true,
        active: Boolean = false,
        focused: Boolean = false,
        top: Int = 0,
        bottom: Int = height
    ) = ShadeWindow(pkg, title, system, active, focused, top, bottom)

    private fun open(vararg w: ShadeWindow, locked: Boolean = false, titles: ShadeTitles = ShadeTitles.ENGLISH) =
        ShadeDetector.isShadeOpen(w.toList(), height, locked, titles)

    @Test
    fun noWindowsMeansClosed() {
        assertFalse(open())
    }

    @Test
    fun unlockedFullHeightActiveSystemUiWindowIsTheShade() {
        assertTrue(open(window(title = "Notification shade.", active = true)))
        assertTrue(open(window(focused = true)))
    }

    @Test
    fun headsUpMakesTheWindowTallButNotActive() {
        assertFalse(open(window(title = "Notification shade.")))
    }

    @Test
    fun statusBarAndNavigationBarWindowsAreTooShort() {
        assertFalse(open(window(title = "Status bar", active = true, bottom = 120)))
        assertFalse(open(window(title = "Navigation bar", active = true, top = 2250)))
    }

    @Test
    fun otherAppsAndNonSystemWindowsNeverCount() {
        assertFalse(open(window(pkg = "com.example.app", active = true)))
        assertFalse(open(window(system = false, active = true)))
        assertFalse(open(window(pkg = null, active = true)))
    }

    @Test
    fun lockScreenAloneIsNotTheShade() {
        assertFalse(open(window(title = "Lock screen.", active = true), locked = true))
        assertFalse(open(window(title = "Lock screen.", active = true), locked = false))
    }

    @Test
    fun lockedNeedsTheShadeOrQuickSettingsTitle() {
        assertTrue(open(window(title = "Notification shade.", active = true), locked = true))
        assertTrue(open(window(title = "Quick settings.", active = true), locked = true))
        assertFalse(open(window(title = null, active = true), locked = true))
        assertFalse(open(window(title = "Something else", active = true), locked = true))
    }

    @Test
    fun titlesFollowThePhonesLanguage() {
        val german = ShadeTitles(shade = listOf("Benachrichtigungsleiste"), lockScreen = listOf("Sperrbildschirm"))
        assertTrue(open(window(title = "Benachrichtigungsleiste.", active = true), locked = true, titles = german))
        assertFalse(open(window(title = "Sperrbildschirm.", active = true), locked = true, titles = german))
    }

    @Test
    fun unknownDisplayHeightNeverReportsTheShade() {
        assertFalse(ShadeDetector.isShadeOpen(listOf(window(active = true)), 0, false))
    }
}
