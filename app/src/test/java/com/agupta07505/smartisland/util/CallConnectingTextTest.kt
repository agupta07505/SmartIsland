/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallConnectingTextTest {
    @Test
    fun dialingStatusesCount() {
        assertTrue(CallStyleExtras.textLooksConnecting("Calling…"))
        assertTrue(CallStyleExtras.textLooksConnecting("Calling..."))
        assertTrue(CallStyleExtras.textLooksConnecting("Dialing"))
        assertTrue(CallStyleExtras.textLooksConnecting("  Connecting...  "))
        assertTrue(CallStyleExtras.textLooksConnecting("Ringing"))
    }

    @Test
    fun runningOrIncomingCallsDoNot() {
        assertFalse(CallStyleExtras.textLooksConnecting("Ongoing call"))
        assertFalse(CallStyleExtras.textLooksConnecting("Incoming call"))
        assertFalse(CallStyleExtras.textLooksConnecting("Calling you"))
        assertFalse(CallStyleExtras.textLooksConnecting("Abungo is calling"))
        assertFalse(CallStyleExtras.textLooksConnecting("00:19"))
        assertFalse(CallStyleExtras.textLooksConnecting(""))
    }

    @Test
    fun statusKeepsTheAppsOwnWords() {
        assertEquals("Calling…", CallStyleExtras.connectingStatusOf(listOf("  Calling…  ")))
        assertEquals("Ringing...", CallStyleExtras.connectingStatusOf(listOf("Abungo", "Ringing...")))
        assertEquals("Dialing", CallStyleExtras.connectingStatusOf(listOf("Dialing")))
        assertNull(CallStyleExtras.connectingStatusOf(listOf("Ongoing call", "00:19")))
    }
}
