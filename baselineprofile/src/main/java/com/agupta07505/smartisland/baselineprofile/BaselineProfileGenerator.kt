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

/** Generates a baseline profile for the main screen, demo flows and landscape lab. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE,
        // Two passes at most, one unchanged pass is enough. On the 2-core CI emulator the first pass
        // takes 13 to 20 minutes and each later one about 7. Runs with 6 and with 10 passes both
        // hit the step limit (the 10 pass run lost its runner after 54 minutes), and the boot and
        // build before the test already use 11 to 16 minutes of the step.
        maxIterations = 2,
        stableIterations = 1,
        includeInStartupProfile = true
    ) {
        grantAccess()
        pressHome()
        startActivityAndWait()

        // portrait: sample cards opened and cleared
        openTab("Demo")
        repeat(4) { showNextSample() }
        device.waitForIdle()
        SystemClock.sleep(1_500)
        device.findObject(By.textStartsWith("Clear all demo cards"))?.click()
        device.waitForIdle()

        // landscape: the lab plays rotation, idle and pill layouts, with the sibling picker
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

        // Settings screens are the other first-open cost on the app side
        openTab("Settings")
        device.waitForIdle()
    }

    private fun openTab(label: String) {
        device.wait(Until.findObject(By.text(label)), 5_000)?.click()
        device.waitForIdle()
    }

    /** Taps the first "Show" button on the Demo tab, scrolling down one screen for the next one */
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
