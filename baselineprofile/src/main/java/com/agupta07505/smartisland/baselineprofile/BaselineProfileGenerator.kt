/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Plays the paths that stutter the first time they run: the home screen, the Demo tab, sample cards
 * opened and cleared in portrait, then the landscape lab (rotate, expand a card with siblings, open
 * and close the picker). Run with a phone or emulator connected:
 *
 *     ./gradlew :app:generateBaselineProfile
 *
 * The phone must be unlocked and the screen on. The test grants the overlay, accessibility and
 * notification-listener access itself (adb shell commands), so the island shows during the run.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE,
        includeInStartupProfile = true
    ) {
        grantAccess()
        pressHome()
        startActivityAndWait()

        // Portrait: sample cards opened and cleared.
        openTab("Demo")
        repeat(4) { showNextSample() }
        device.waitForIdle()
        SystemClock.sleep(1_500)
        device.findObject(By.textStartsWith("Clear all demo cards"))?.click()
        device.waitForIdle()

        // Landscape: the lab plays rotation, idle and pill layouts, with the sibling picker.
        try {
            device.setOrientationLandscape()
            device.waitForIdle()
            device.wait(Until.findObject(By.text("Play")), 5_000)?.click()
            device.wait(Until.hasObject(By.textStartsWith("Done.")), LAB_TIMEOUT_MS)
        } finally {
            device.findObject(By.text("Stop"))?.click()
            device.setOrientationNatural()
            device.waitForIdle()
        }

        // Settings screens are the other first-open cost on the app side.
        openTab("Settings")
        device.waitForIdle()
    }

    private fun openTab(label: String) {
        device.wait(Until.findObject(By.text(label)), 5_000)?.click()
        device.waitForIdle()
    }

    /** Taps the first "Show" button on the Demo tab, scrolling down one screen for the next one. */
    private fun showNextSample() {
        val show = device.findObject(By.text("Show"))
        if (show != null) {
            show.click()
            device.waitForIdle()
            SystemClock.sleep(600)
        }
    }

    private fun grantAccess() {
        val service = "$PACKAGE/$PACKAGE.service.SmartIslandOverlayService"
        val listener = "$PACKAGE/$PACKAGE.service.SmartIslandNotificationListenerService"
        device.executeShellCommand("appops set $PACKAGE SYSTEM_ALERT_WINDOW allow")
        device.executeShellCommand("cmd notification allow_listener $listener")
        val current = device.executeShellCommand("settings get secure enabled_accessibility_services").trim()
        if (!current.contains(service)) {
            val joined = if (current.isEmpty() || current == "null") service else "$current:$service"
            device.executeShellCommand("settings put secure enabled_accessibility_services $joined")
        }
        device.executeShellCommand("settings put secure accessibility_enabled 1")
    }

    private companion object {
        const val PACKAGE = "com.agupta07505.smartisland"
        const val LAB_TIMEOUT_MS = 6 * 60_000L
    }
}
