/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventScopeTest {
    private val base = SmartIslandSettings.Default

    @Test
    fun portraitOnlySwitchIsNotDisabledEverywhere() {
        val s = base.copy(disabledEvents = setOf("music"), showInLandscape = true)
        assertFalse(s.isEventDisabledEverywhere("music"))
        assertTrue(s.isEventDisabledSomewhere("music"))
    }

    @Test
    fun bothProfilesOffMeansDisabledEverywhere() {
        val s = base.copy(
            disabledEvents = setOf("music"),
            disabledEventsLandscape = setOf("music"),
            showInLandscape = true
        )
        assertTrue(s.isEventDisabledEverywhere("music"))
    }

    @Test
    fun landscapeProfileIsIgnoredWhenLandscapeIsHidden() {
        val portraitOff = base.copy(disabledEvents = setOf("music"), showInLandscape = false)
        assertTrue(portraitOff.isEventDisabledEverywhere("music"))

        val landscapeOff = base.copy(disabledEventsLandscape = setOf("music"), showInLandscape = false)
        assertFalse(landscapeOff.isEventDisabledSomewhere("music"))
    }

    @Test
    fun landscapeOnlySwitchKeepsNotificationInTheShadeButStillShowsInPortrait() {
        val s = base.copy(disabledEventsLandscape = setOf("timer"), showInLandscape = true)
        assertFalse(s.isEventDisabledEverywhere("timer"))
        assertTrue(s.isEventDisabledSomewhere("timer"))
    }
}
