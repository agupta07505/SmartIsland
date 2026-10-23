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

class IdleWidgetOffTest {
    @Test
    fun tapCloseOnlyCountsWhenItsSwitchIsOn() {
        assertTrue(IdleWidgetOff.suppressed(closedByTap = true, pillHidden = false, offAfterTapClose = true, offAfterLongPress = false))
        assertFalse(IdleWidgetOff.suppressed(closedByTap = true, pillHidden = false, offAfterTapClose = false, offAfterLongPress = true))
        assertFalse(IdleWidgetOff.suppressed(closedByTap = false, pillHidden = false, offAfterTapClose = true, offAfterLongPress = true))
    }

    @Test
    fun longPressHideOnlyCountsWhenItsSwitchIsOn() {
        assertTrue(IdleWidgetOff.suppressed(closedByTap = false, pillHidden = true, offAfterTapClose = false, offAfterLongPress = true))
        assertFalse(IdleWidgetOff.suppressed(closedByTap = false, pillHidden = true, offAfterTapClose = true, offAfterLongPress = false))
    }

    @Test
    fun storedNotificationsOnlyCountWhenTheirSwitchIsOn() {
        assertTrue(IdleWidgetOff.suppressed(false, false, false, false, hasStoredNotifications = true, offWhenStored = true))
        assertFalse(IdleWidgetOff.suppressed(false, false, false, false, hasStoredNotifications = true, offWhenStored = false))
        assertFalse(IdleWidgetOff.suppressed(false, false, false, false, hasStoredNotifications = false, offWhenStored = true))
    }

    @Test
    fun timersAreKeptInRange() {
        assertEquals(1.5f, IdleSlots.normalizeTickSeconds(0.2f), 1e-6f)
        assertEquals(10f, IdleSlots.normalizeTickSeconds(99f), 1e-6f)
        assertEquals(2.5f, IdleSlots.normalizeTickSeconds(Float.NaN), 1e-6f)
        assertEquals(2500L, IdleSlots.tickPeriodMs(2.5f))
        assertEquals(3000L, IdleSlots.tickPeriodMs(2.9f))
        assertEquals(IdleWidgets.MIN_CYCLE_SECONDS, IdleWidgetOff.normalizeCycleSeconds(0))
        assertEquals(IdleWidgets.MAX_CYCLE_SECONDS, IdleWidgetOff.normalizeCycleSeconds(500))
    }

    @Test
    fun selectAllListsEveryWidgetOnce() {
        val parsed = IdleWidgets.parse(IdleWidgetOff.allIds())
        assertEquals(IdleWidgetKind.values().toList(), parsed)
        // The last widget cannot be switched off with the selector.
        assertEquals("clock", IdleWidgets.toggle("clock", IdleWidgetKind.Clock))
    }
}
