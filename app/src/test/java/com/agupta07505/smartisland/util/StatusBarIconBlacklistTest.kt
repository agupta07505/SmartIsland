/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusBarIconBlacklistTest {

    private val slots = listOf("alarm_clock", "nfc", "rotate")

    @Test
    fun parseTrimsAndDropsEmpties() {
        assertEquals(listOf("a", "b"), StatusBarIconBlacklist.parse(" a, ,b,a "))
        assertEquals(emptyList<String>(), StatusBarIconBlacklist.parse(null))
    }

    @Test
    fun mergeKeepsTheExistingListFirstAndAddsOnlyMissingSlots() {
        assertEquals("alarm_clock,nfc,rotate", StatusBarIconBlacklist.merge(null, slots))
        assertEquals("headset,nfc,alarm_clock,rotate", StatusBarIconBlacklist.merge("headset,nfc", slots))
    }

    @Test
    fun restoreUnsetsTheKeyWhenItWasUnsetBefore() {
        val merged = StatusBarIconBlacklist.merge(null, slots)
        assertNull(StatusBarIconBlacklist.restore(merged, null, slots))
    }

    @Test
    fun restoreRemovesOnlyTheSlotsThisAppAdded() {
        val previous = "headset,nfc"
        val merged = StatusBarIconBlacklist.merge(previous, slots)
        assertEquals("headset,nfc", StatusBarIconBlacklist.restore(merged, previous, slots))
    }

    @Test
    fun restoreKeepsSlotsAddedByOthersWhileEnabled() {
        val merged = StatusBarIconBlacklist.merge(null, slots) + ",wifi"
        assertEquals("wifi", StatusBarIconBlacklist.restore(merged, null, slots))
    }

    @Test
    fun restoreOfAnEmptyPreviousValueStaysEmpty() {
        assertEquals("", StatusBarIconBlacklist.restore("alarm_clock,nfc,rotate", "", slots))
    }
}
