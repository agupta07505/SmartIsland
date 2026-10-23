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

class ShellStateGuardTest {
    private fun reason(
        expanded: Boolean = true,
        atRest: Boolean = true,
        overlayFlag: Boolean = false,
        splitWanted: Boolean = false,
        drawn: Boolean = false,
        alpha: Float = 1f,
        radius: Float = 28f
    ) = ShellStateGuard.stuckReason(expanded, atRest, overlayFlag, splitWanted, drawn, alpha, radius)

    @Test
    fun clipAndSurfaceNeedBothTheFlagAndTheContent() {
        assertTrue(ShellStateGuard.twoIslandsDrawn(true, true))
        assertFalse(ShellStateGuard.twoIslandsDrawn(true, false))
        assertFalse(ShellStateGuard.twoIslandsDrawn(false, true))
        assertFalse(ShellStateGuard.twoIslandsDrawn(false, false))
    }

    @Test
    fun aHealthySingleIslandIsLeftAlone() {
        assertNull(reason())
    }

    @Test
    fun aHealthyTwoIslandLayoutIsLeftAlone() {
        assertNull(reason(overlayFlag = true, splitWanted = true, drawn = true, alpha = 0f))
    }

    @Test
    fun nothingIsJudgedWhileCollapsedOrMoving() {
        assertNull(reason(expanded = false, alpha = 0f, radius = 0f))
        assertNull(reason(atRest = false, alpha = 0f, radius = 0f))
    }

    @Test
    fun theFlagHeldWithNothingOpenIsStuckAndRepairable() {
        val r = reason(overlayFlag = true, splitWanted = false, drawn = true, alpha = 0f)
        assertEquals(ShellStateGuard.REASON_HOLD, r)
        assertTrue(ShellStateGuard.canForceSingle(r))
    }

    @Test
    fun aSingleIslandWithoutItsSurfaceIsStuckAndRepairable() {
        val r = reason(alpha = 0f)
        assertEquals(ShellStateGuard.REASON_NO_SURFACE, r)
        assertTrue(ShellStateGuard.canForceSingle(r))
        // Still fading in: not yet counted as back.
        assertEquals(ShellStateGuard.REASON_NO_SURFACE, reason(alpha = 0.9f))
        assertNull(reason(alpha = 0.99f))
    }

    @Test
    fun aSingleIslandWithSquareCornersIsOnlyLogged() {
        val r = reason(radius = 0f)
        assertEquals(ShellStateGuard.REASON_NO_RADIUS, r)
        assertFalse(ShellStateGuard.canForceSingle(r))
        assertFalse(ShellStateGuard.canForceSingle(null))
    }

    @Test
    fun theHoldGetsItsOwnCloseOnTopOfTheRestTime() {
        assertEquals(ShellStateGuard.REST_MS, ShellStateGuard.waitMs(false, 240))
        assertEquals(ShellStateGuard.REST_MS + 240L, ShellStateGuard.waitMs(true, 240))
    }
}
