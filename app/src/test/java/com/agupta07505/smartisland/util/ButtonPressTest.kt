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

class ButtonPressTest {

    @Test
    fun theAppsOwnButtonIsAlwaysSent() {
        assertNull(ButtonPress.refusal(hasIntent = true, viaView = false, liveLabels = emptyList(), title = "Pause"))
    }

    @Test
    fun aButtonWithoutAnIntentIsRefused() {
        assertEquals(
            ButtonPress.Refusal.NoIntent,
            ButtonPress.refusal(hasIntent = false, viaView = false, liveLabels = listOf("Pause"), title = "Pause")
        )
    }

    @Test
    fun aProxyButtonIsSentWhileItsViewIsKept() {
        assertNull(ButtonPress.refusal(true, true, listOf("Pause", "+1:00"), "+1:00"))
    }

    @Test
    fun aProxyButtonWhoseViewIsGoneIsRefused() {
        assertEquals(ButtonPress.Refusal.ViewGone, ButtonPress.refusal(true, true, emptyList(), "Pause"))
        assertEquals(ButtonPress.Refusal.ViewGone, ButtonPress.refusal(true, true, listOf("Lap"), "Pause"))
    }

    @Test
    fun labelsAreComparedWithoutCaseOrPadding() {
        assertTrue(ButtonPress.hasLive(listOf(" Pause "), "pause"))
        assertFalse(ButtonPress.hasLive(listOf("Pause"), "Resume"))
    }
}
