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
}
