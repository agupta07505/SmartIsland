/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetSpeedRulesTest {
    @Test fun normalizeKeepsChoicesAndFixesTheRest() {
        NetSpeedRules.CHOICES.forEach { assertEquals(it, NetSpeedRules.normalize(it)) }
        assertEquals(NetSpeedRules.DEFAULT_KBPS, NetSpeedRules.normalize(75))
        assertEquals(NetSpeedRules.DEFAULT_KBPS, NetSpeedRules.normalize(-1))
    }

    @Test fun offAlwaysShows() {
        assertTrue(NetSpeedRules.shouldShow(NetSpeedRules.OFF, 0, 0, wasShown = false))
    }

    @Test fun belowThresholdStaysOut() {
        assertFalse(NetSpeedRules.shouldShow(50, 49_999, 10_000, wasShown = false))
    }

    @Test fun atThresholdComesIn() {
        assertTrue(NetSpeedRules.shouldShow(50, 50_000, 0, wasShown = false))
        assertTrue(NetSpeedRules.shouldShow(1000, 0, 1_000_000, wasShown = false))
    }

    @Test fun uploadCountsToo() {
        assertTrue(NetSpeedRules.shouldShow(200, 1_000, 250_000, wasShown = false))
    }

    @Test fun directionFollowsTheLargerSide() {
        assertTrue(NetSpeedRules.isUpload(1_000, 2_000))
        assertFalse(NetSpeedRules.isUpload(2_000, 1_000))
        assertFalse(NetSpeedRules.isUpload(1_000, 1_000))
        assertEquals(2_000L, NetSpeedRules.busier(1_000, 2_000))
    }

    @Test fun shownWidgetHoldsUntilWellBelow() {
        // 45 KB/s: under 50 but over 80% of it (40 KB/s), so a shown widget stays and a hidden one stays out.
        assertTrue(NetSpeedRules.shouldShow(50, 45_000, 0, wasShown = true))
        assertFalse(NetSpeedRules.shouldShow(50, 45_000, 0, wasShown = false))
        assertFalse(NetSpeedRules.shouldShow(50, 39_999, 0, wasShown = true))
    }
}
