/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationParserTest {

    @Test
    fun testGoogleMapsNavigationParsedCorrectly() {
        val extras = mockk<Bundle>()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "In 300 m - Turn left"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "onto MG Road"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns "24 min • 8.5 km"

        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_NAVIGATION
        notification.extras = extras

        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.google.android.apps.maps"
        every { sbn.notification } returns notification

        val info = NavigationParser.parse(sbn)
        assertNotNull(info)
        assertEquals("300 m", info?.distanceToTurnText)
        assertEquals(TurnDirection.LEFT, info?.turnDirection)
        assertEquals("In 300 m - Turn left", info?.maneuverTitle)
        assertEquals("24 min • 8.5 km", info?.subText)
    }

    @Test
    fun testWazeUTurnParsedCorrectly() {
        val extras = mockk<Bundle>()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Make a U-Turn"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "In 1.2 km on Ring Road"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null

        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_NAVIGATION
        notification.extras = extras

        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.waze"
        every { sbn.notification } returns notification

        val info = NavigationParser.parse(sbn)
        assertNotNull(info)
        assertEquals("1.2 km", info?.distanceToTurnText)
        assertEquals(TurnDirection.U_TURN, info?.turnDirection)
    }

    @Test
    fun testNonNavigationAppReturnsNull() {
        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.whatsapp"
        every { sbn.notification } returns null

        val info = NavigationParser.parse(sbn)
        assertNull(info)
    }

    @Test
    fun testScreenRecordingNotificationReturnsNullFromNavigationParser() {
        val extras = mockk<Bundle>()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Screen recording"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Tap to stop • 5 mins left"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null

        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_NAVIGATION // Even if category was navigation
        notification.extras = extras

        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.sec.android.app.screenrecorder"
        every { sbn.notification } returns notification

        val info = NavigationParser.parse(sbn)
        assertNull(info)
    }

    @Test
    fun testTurnDirectionExcludesTimeLeftPhrase() {
        val direction = NavigationParser.parseTurnDirection("Screen recording in progress. 5 min left")
        assertEquals(TurnDirection.STRAIGHT, direction)
    }

    @Test
    fun testTurnDirectionMatchesActualTurnLeft() {
        val direction = NavigationParser.parseTurnDirection("Turn left onto Broadway")
        assertEquals(TurnDirection.LEFT, direction)
    }
}
