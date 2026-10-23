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
    fun testMapsTimelineNotificationIsNotNavigation() {
        val extras = mockk<Bundle>()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Your September Timeline"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "See new travel & visit insights"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null

        val notification = mockk<Notification>()
        notification.category = null
        notification.flags = 0
        notification.extras = extras

        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.google.android.apps.maps"
        every { sbn.notification } returns notification

        assertNull(NavigationParser.parse(sbn))
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

    @Test
    fun cardTextSplitsTripStatsFromInstructionDetail() {
        val card = NavigationParser.cardText("In 300 m - Turn left", "onto MG Road \u2022 24 min \u2022 8.5 km")
        assertEquals("In 300 m", card.distance)
        assertEquals("Turn left", card.instruction)
        assertEquals("onto MG Road", card.detail)
        assertEquals("24 min \u2022 8.5 km", card.eta)
    }

    @Test
    fun cardTextNeverTakesTheTripDistanceForTheTurnDistance() {
        val card = NavigationParser.cardText("Head north on Main St", "24 min \u2022 8.5 km")
        assertEquals("In 200 m", card.distance)
        assertEquals("Head north on Main St", card.instruction)
        assertEquals("", card.detail)
        assertEquals("24 min \u2022 8.5 km", card.eta)
    }

    @Test
    fun cardTextWithBlankTitleFallsBackToNavigation() {
        val card = NavigationParser.cardText("", "")
        assertEquals("Navigation", card.instruction)
        assertEquals("", card.detail)
        assertEquals("", card.eta)
    }

    private fun sbn(pkg: String, title: String, text: String, category: String? = Notification.CATEGORY_NAVIGATION, flags: Int = 0): StatusBarNotification {
        val extras = mockk<Bundle>()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns title
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns text
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null
        val notification = mockk<Notification>()
        notification.category = category
        notification.flags = flags
        notification.extras = extras
        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns pkg
        every { sbn.notification } returns notification
        return sbn
    }

    @Test
    fun yandexNavigatorRussianGuidanceGivesDistanceAndDirection() {
        val info = NavigationParser.parse(sbn("ru.yandex.yandexnavi", "Через 300 м", "Поверните налево на улицу Ленина"))
        assertNotNull(info)
        assertEquals("300 м", info?.distanceToTurnText)
        assertEquals(TurnDirection.LEFT, info?.turnDirection)
    }

    @Test
    fun russianSlightRightAndDecimalCommaDistance() {
        val info = NavigationParser.parse(sbn("ru.yandex.yandexmaps", "Через 1,2 км", "Держитесь правее"))
        assertEquals("1,2 км", info?.distanceToTurnText)
        assertEquals(TurnDirection.SLIGHT_RIGHT, info?.turnDirection)
    }

    @Test
    fun chineseJapaneseKoreanGuidanceIsRecognised() {
        assertEquals(TurnDirection.LEFT, NavigationParser.parse(sbn("com.autonavi.minimap", "前方300米", "左转进入中山路"))?.turnDirection)
        assertEquals("300m", NavigationParser.parse(sbn("com.google.android.apps.maps", "300m先、右折です", ""))?.distanceToTurnText)
        assertEquals(TurnDirection.RIGHT, NavigationParser.parseTurnDirection("500m 앞에서 우회전"))
    }

    @Test
    fun europeanLanguageDirections() {
        assertEquals(TurnDirection.LEFT, NavigationParser.parseTurnDirection("dans 200 m, tournez à gauche"))
        assertEquals(TurnDirection.LEFT, NavigationParser.parseTurnDirection("in 400 metern links abbiegen"))
        assertEquals(TurnDirection.RIGHT, NavigationParser.parseTurnDirection("gire a la derecha en 300 m"))
        assertEquals(TurnDirection.U_TURN, NavigationParser.parseTurnDirection("retorno em 300 m"))
        assertEquals(TurnDirection.ROUNDABOUT, NavigationParser.parseTurnDirection("prenez le rond-point"))
    }

    @Test
    fun minutesAreNeverReadAsMetres() {
        val card = NavigationParser.cardText("Через 300 м поверните направо", "24 мин • 8,5 км")
        assertEquals("300 м", card.distance)
        assertEquals("поверните направо", card.instruction)
        assertEquals("24 мин • 8,5 км", card.eta)
    }

    @Test
    fun deliveryAndRideAppsAreNeverNavigationEvenWithTheNavigationCategory() {
        assertNull(NavigationParser.parse(sbn("com.ubercab.eats", "Your courier is 500 m away", "Turn left at the corner")))
        assertNull(NavigationParser.parse(sbn("com.grabtaxi.passenger", "Driver is 300 m away", "")))
        assertNull(NavigationParser.parse(sbn("com.dd.doordash", "Dasher arriving in 2 min", "")))
    }

    @Test
    fun unknownAppWithNavigationCategoryNeedsRealManeuverWording() {
        // A distance alone is not guidance.
        assertNull(NavigationParser.parse(sbn("com.example.tracker", "Package is 500 m away", "")))
        // Order vocabulary rules it out even with a turn word in it.
        assertNull(NavigationParser.parse(sbn("com.example.food", "Your order: rider will turn left in 200 m", "")))
        // A genuine guidance notification from an unlisted map app still works.
        assertNotNull(NavigationParser.parse(sbn("com.example.newmaps", "In 200 m turn right", "onto Park Ave")))
    }

    @Test
    fun runningMapAppWithoutGuidanceIsNotNavigation() {
        val ongoing = Notification.FLAG_ONGOING_EVENT
        assertNull(NavigationParser.parse(sbn("com.waze", "Waze is running", "Tap to open", category = null, flags = ongoing)))
        assertNotNull(NavigationParser.parse(sbn("com.waze", "Turn left", "in 300 m", category = null, flags = ongoing)))
    }

    @Test
    fun moreMapAppsAreKnown() {
        listOf(
            "ru.yandex.yandexnavi", "ru.dublgis.dgismobile", "com.mapswithme.maps.pro", "app.organicmaps", "app.comaps.google",
            "net.osmand.plus", "com.generalmagic.magicearth", "cz.seznam.mapy", "com.mmi.maps", "com.tencent.map"
        ).forEach { assertEquals(it, true, NavigationParser.isNavigationApp(it)) }
        assertEquals(false, NavigationParser.isNavigationApp("com.ubercab"))
    }
}
