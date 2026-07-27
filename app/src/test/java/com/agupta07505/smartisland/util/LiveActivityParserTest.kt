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
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LiveActivityParserTest {

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.d(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(android.util.Log::class)
    }

    @Test
    fun testUberTrackingNotificationParsesCorrectly() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val extras = mockk<Bundle>()

        notification.extras = extras
        every { sbn.packageName } returns "com.ubercab"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Driver is on the way"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Arriving in 8 mins • 2.5 km away"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 40
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 100

        val info = LiveActivityParser.parse(sbn)
        assertNotNull(info)
        assertEquals("com.ubercab", info?.packageName)
        assertEquals("8 min", info?.etaText)
        assertEquals(0.4f, info?.progressRatio ?: 0f, 0.01f)
        assertEquals("Driver is on the way", info?.statusTitle)
    }

    @Test
    fun testZomatoFoodDeliveryParsesCorrectly() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val extras = mockk<Bundle>()

        notification.extras = extras
        every { sbn.packageName } returns "com.application.zomato"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Order Out For Delivery"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Your food will arrive in 15 mins"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0

        val info = LiveActivityParser.parse(sbn)
        assertNotNull(info)
        assertEquals("15 min", info?.etaText)
        assertEquals("Order Out For Delivery", info?.statusTitle)
    }

    @Test
    fun testZomatoPromoNotificationReturnsNull() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val extras = mockk<Bundle>()

        notification.extras = extras
        every { sbn.packageName } returns "com.application.zomato"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Flat 50% OFF!"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Craving biryani? Order now with code 50OFF"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0

        val info = LiveActivityParser.parse(sbn)
        assertNull(info)
    }

    @Test
    fun testNonWhitelistedAppReturnsNull() {
        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.random.app"

        val info = LiveActivityParser.parse(sbn)
        assertNull(info)
    }
}
