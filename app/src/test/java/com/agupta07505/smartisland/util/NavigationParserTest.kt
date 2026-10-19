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

    // ---- Ride-sharing (Rule_Book 4.14) ----

    @Test
    fun testUberPickupFromBigText() {
        val ride = NavigationParser.parseRide(
            packageName = "com.ubercab",
            title = "Rahul is arriving in 3 min",
            text = "White Swift Dzire • KA01AB1234",
            bigText = "Rahul (4.9★) is on the way in a White Swift Dzire, KA01AB1234. Meet at Gate 2."
        )
        assertNotNull(ride)
        ride!!
        assertEquals("Uber", ride.provider)
        assertEquals("Rahul", ride.driverName)
        assertEquals("White Swift Dzire", ride.vehicle)
        assertEquals("KA01AB1234", ride.licencePlate)
        assertEquals(3, ride.etaMinutes)
        assertEquals(RidePhase.Pickup, ride.phase)
        assertEquals("Pickup in 3m", ride.etaLabel)
        assertEquals("3 min", ride.etaBadge)
    }

    @Test
    fun testUberOnTripToTheDrop() {
        val ride = NavigationParser.parseRide(
            packageName = "com.ubercab",
            title = "Heading to Indiranagar",
            text = "Drop-off at 10:42 PM",
            bigText = "Drop-off in 12 min · Arriving at 10:42 PM. You're riding with Rahul in KA 01 AB 1234."
        )
        assertNotNull(ride)
        assertEquals(RidePhase.Drop, ride!!.phase)
        assertEquals(12, ride.etaMinutes)
        assertEquals("KA 01 AB 1234", ride.licencePlate)
        assertEquals("Drop in 12m", ride.etaLabel)
    }

    @Test
    fun testOlaDriverNameVehicleInParensAndSpacedPlate() {
        val ride = NavigationParser.parseRide(
            packageName = "com.olacabs.customer",
            title = "Your Ola is arriving",
            text = null,
            bigText = "Suresh Kumar is arriving in 5 mins in KA05MN4321 (Wagon R). Share OTP 4821 to start the ride."
        )
        assertNotNull(ride)
        assertEquals("Ola", ride!!.provider)
        assertEquals("Suresh Kumar", ride.driverName)
        assertEquals("Wagon R", ride.vehicle)
        assertEquals("KA05MN4321", ride.licencePlate)
        assertEquals(5, ride.etaMinutes)
        assertEquals(RidePhase.Pickup, ride.phase)
    }

    @Test
    fun testRapidoCaptainWithBikeLabel() {
        val ride = NavigationParser.parseRide(
            packageName = "com.rapido.passenger",
            title = "Captain Ramesh is on the way",
            text = "Bike: Honda Activa, KA 03 HX 9988 • 2 mins away",
            bigText = null
        )
        assertNotNull(ride)
        assertEquals("Rapido", ride!!.provider)
        assertEquals("Ramesh", ride.driverName)
        assertEquals("Honda Activa", ride.vehicle)
        assertEquals("KA 03 HX 9988", ride.licencePlate)
        assertEquals(2, ride.etaMinutes)
    }

    @Test
    fun testDriverArrivedIsAZeroMinutePickup() {
        val ride = NavigationParser.parseRide(
            packageName = "com.ubercab",
            title = "Your driver has arrived",
            text = "Meet Rahul at the pickup point • MH-12-DE-1433",
            bigText = null
        )
        assertNotNull(ride)
        assertEquals(0, ride!!.etaMinutes)
        assertEquals("MH 12 DE 1433", ride.licencePlate)
        assertEquals("Driver arrived", ride.etaLabel)
        assertEquals("Here", ride.etaBadge)
    }

    @Test
    fun testRidePromotionsAndOtherAppsAreNotRides() {
        assertNull(NavigationParser.parseRide("com.ubercab", "50% off your next 3 rides", "Use code RIDE50 before Sunday", null))
        assertNull(NavigationParser.parseRide("com.olacabs.customer", "Rate your last trip", "How was your ride with Ola?", null))
        assertNull(NavigationParser.parseRide("com.whatsapp", "Rahul is arriving in 3 min", "KA01AB1234", null))
        assertNull(NavigationParser.parseRide("com.ubercab", null, null, null))
    }

    @Test
    fun testParseRideFromTheNotificationPrefersBigText() {
        val extras = mockk<Bundle>()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Your Ola is arriving"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Tap for details"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns "Suresh is arriving in 4 mins in KA05MN4321"
        val notification = mockk<Notification>()
        notification.extras = extras
        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.olacabs.customer"
        every { sbn.notification } returns notification

        val ride = NavigationParser.parseRide(sbn)
        assertNotNull(ride)
        assertEquals("Suresh", ride!!.driverName)
        assertEquals(4, ride.etaMinutes)
        assertEquals("KA05MN4321", ride.licencePlate)
    }
}
