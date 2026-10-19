/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.data.SmartIslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PillLayoutTest {
    private val base = SmartIslandSettings.Default

    @Test
    fun widthIsUnchangedWhenBuddyIsOff() {
        assertEquals(112f, PillLayout.effectiveWidthDp(base, holeWidthDp = 40f, notificationCount = 0), 0f)
    }

    @Test
    fun widthIsUnchangedWhileSomethingElseIsOnTheIsland() {
        val s = base.copy(buddyEnabled = true)
        assertEquals(112f, PillLayout.effectiveWidthDp(s, holeWidthDp = 40f, notificationCount = 1), 0f)
    }

    @Test
    fun defaultPillHasRoomForTheBuddyOnAPunchHole() {
        // Needs 24 + 12 = 36dp per side; a 112dp pill around a 30dp hole has 41dp.
        val s = base.copy(buddyEnabled = true)
        assertEquals(112f, PillLayout.effectiveWidthDp(s, holeWidthDp = 30f, notificationCount = 0), 0f)
    }

    @Test
    fun narrowPillWidensBothSidesByExactlyTheShortfall() {
        val s = base.copy(buddyEnabled = true, width = 76f)
        // Free wing (76 - 30) / 2 = 23dp, needs 36dp: shortfall 13dp per side.
        assertEquals(76f + 26f, PillLayout.effectiveWidthDp(s, holeWidthDp = 30f, notificationCount = 0), 0.001f)
    }

    @Test
    fun noHardwareCutoutNeedsNoWidening() {
        val s = base.copy(buddyEnabled = true, width = 76f)
        assertEquals(76f, PillLayout.effectiveWidthDp(s, holeWidthDp = null, notificationCount = 0), 0f)
    }

    @Test
    fun buddySizeFollowsPillHeightWithinBounds() {
        assertEquals(24f, PillLayout.buddySizeDp(34f), 0f)
        assertEquals(18f, PillLayout.buddySizeDp(20f), 0f)
        assertEquals(24f, PillLayout.buddySizeDp(60f), 0f)
        assertEquals(22f, PillLayout.buddySizeDp(28f), 0f)
    }

    @Test
    fun hideWhenIdleBehavesAsBeforeWithoutTheBuddy() {
        val s = base.copy(hideWhenIdle = true, enableAppShortcuts = false)
        assertTrue(PillLayout.windowIdleHidden(s, 0))
        assertTrue(PillLayout.pillIdleHiding(s, 0))
        assertFalse(PillLayout.windowIdleHidden(s, 1))
        assertFalse(PillLayout.pillIdleHiding(s, 1))
        // Shortcuts keep the window visible, exactly as the service did before.
        val withShortcuts = s.copy(enableAppShortcuts = true)
        assertFalse(PillLayout.windowIdleHidden(withShortcuts, 0))
        assertTrue(PillLayout.pillIdleHiding(withShortcuts, 0))
    }

    @Test
    fun aRestingBuddyOverridesHideWhenIdle() {
        val s = base.copy(hideWhenIdle = true, buddyEnabled = true, enableAppShortcuts = false)
        assertFalse(PillLayout.windowIdleHidden(s, 0))
        assertFalse(PillLayout.pillIdleHiding(s, 0))
    }

    // ---- Cutout enclosure and the dynamic width budget ----

    @Test
    fun theIdlePillAlwaysEnclosesTheCameraHole() {
        val s = base.copy(width = 76f)
        // A 90dp-wide hole needs 90 + 2 * 4dp.
        assertEquals(98f, PillLayout.effectiveWidthDp(s, holeWidthDp = 90f, notificationCount = 0), 0.001f)
    }

    @Test
    fun withoutSecureSettingsTheAlertWidthStaysClearOfTheStatusIcons() {
        // 411dp screen, centred pill: min(205.5 - 72, 205.5 - 88) * 2 = 235dp.
        assertEquals(235f, PillLayout.maxAlertPillWidthDp(411f, 0f, iconsManaged = false), 0.001f)
        // Offset towards the system icons shrinks the room on that side.
        assertEquals(215f, PillLayout.maxAlertPillWidthDp(411f, 10f, iconsManaged = false), 0.001f)
        // A wide screen is still capped.
        assertEquals(280f, PillLayout.maxAlertPillWidthDp(900f, 0f, iconsManaged = false), 0.001f)
        // A custom cap is honoured.
        assertEquals(200f, PillLayout.maxAlertPillWidthDp(900f, 0f, iconsManaged = false, capDp = 200f), 0.001f)
    }

    @Test
    fun withSecureSettingsTheAlertPillUsesTheFullCap() {
        assertEquals(PillLayout.MAX_ALERT_PILL_WIDTH_DP, PillLayout.maxAlertPillWidthDp(411f, 0f, iconsManaged = true), 0f)
    }

    @Test
    fun aLongAlertStopsAtTheDynamicBudgetButNeverBelowTheBaseWidth() {
        val wide = PillLayout.effectiveWidthDp(base, holeWidthDp = 30f, notificationCount = 1, contentWingDp = 400f, maxAlertWidthDp = 235f)
        assertEquals(235f, wide, 0.001f)
        val tightBudget = PillLayout.effectiveWidthDp(base, holeWidthDp = 30f, notificationCount = 1, contentWingDp = 400f, maxAlertWidthDp = 50f)
        assertEquals(base.width, tightBudget, 0.001f)
    }

    @Test
    fun stopwatchPillMakesRoomForTheCentisecondReadout() {
        val stopwatch = com.agupta07505.smartisland.model.IslandNotification(
            packageName = "com.google.android.deskclock",
            appName = "Clock",
            title = "Stopwatch",
            text = "",
            timeMillis = 0L,
            mode = com.agupta07505.smartisland.model.IslandMode.Stopwatch
        )
        assertEquals(PillLayout.STOPWATCH_WING_DP, PillLayout.contentWingDp(stopwatch), 0f)
        // Free wing (112 - 30) / 2 = 41dp, needs 60dp: both sides grow by 19dp.
        assertEquals(150f, PillLayout.effectiveWidthDp(base, 30f, 1, contentWingDp = PillLayout.contentWingDp(stopwatch)), 0.001f)
    }

    @Test
    fun thePrivacyLabelAndTheAlarmClockGetRoomBesideTheHole() {
        fun privacy(category: String) = com.agupta07505.smartisland.model.IslandNotification(
            key = "system_privacy_indicator", packageName = "p", appName = "", title = "", text = "", timeMillis = 0L,
            category = category, mode = com.agupta07505.smartisland.model.IslandMode.PrivacyIndicator
        )
        val camera = PillLayout.contentWingDp(privacy("privacy_camera"))
        val both = PillLayout.contentWingDp(privacy("privacy_camera_microphone"))
        assertTrue(camera > 0f)
        assertTrue(both > camera)
        val alarm = privacy("alarm").copy(mode = com.agupta07505.smartisland.model.IslandMode.Alarm)
        assertTrue(PillLayout.contentWingDp(alarm) > 0f)
        // The pill still stops at the alert cap.
        assertTrue(PillLayout.effectiveWidthDp(base, holeWidthDp = 30f, notificationCount = 1, contentWingDp = both) <= PillLayout.MAX_ALERT_PILL_WIDTH_DP)
    }
}
