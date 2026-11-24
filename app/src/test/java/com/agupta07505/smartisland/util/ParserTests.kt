/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.media.AudioManager
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import com.agupta07505.smartisland.model.CallActionRole
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandTimings
import com.agupta07505.smartisland.model.VoipCallDetector
import com.agupta07505.smartisland.model.VoipNotificationFacts
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CallCardRulesTest {

    private fun card(key: String, pkg: String, mode: IslandMode = IslandMode.IncomingCall) = IslandNotification(
        key = key, packageName = pkg, appName = pkg, title = key, text = "", timeMillis = 0L, mode = mode
    )

    @Test
    fun supersededCallKeys() {
        val ongoingCards = listOf(card("ringing", "wa"), card("msg", "wa", IslandMode.Notification))
        assertEquals(listOf("ringing"), CallCardRules.supersededKeys(ongoingCards, "ongoing", "wa", newIsRinging = false))

        val ringingCards = listOf(card("ongoing", "wa"))
        assertEquals(emptyList<String>(), CallCardRules.supersededKeys(ringingCards, "second", "wa", newIsRinging = true))

        val unrelatedCards = listOf(card("tg", "telegram"), card("music", "wa", IslandMode.Music), card("ringing", "wa"))
        assertEquals(listOf("ringing"), CallCardRules.supersededKeys(unrelatedCards, "ongoing", "wa", newIsRinging = false))

        val repostCards = listOf(card("call", "wa"))
        assertEquals(emptyList<String>(), CallCardRules.supersededKeys(repostCards, "call", "wa", newIsRinging = false))
    }
}

class CallConnectingTextTest {
    @Test
    fun connectingTextRecognizesDialingStatuses() {
        val cases = listOf(
            "Calling…" to true,
            "Calling..." to true,
            "Dialing" to true,
            "  Connecting...  " to true,
            "Ringing" to true,
            "Ongoing call" to false,
            "Incoming call" to false,
            "Calling you" to false,
            "Abungo is calling" to false,
            "00:19" to false,
            "" to false
        )
        for ((text, expected) in cases) {
            assertEquals("Input: $text", expected, CallStyleExtras.textLooksConnecting(text))
        }
    }


    @Test
    fun statusKeepsTheAppsOwnWords() {
        assertEquals("Calling…", CallStyleExtras.connectingStatusOf(listOf("  Calling…  ")))
        assertEquals("Ringing...", CallStyleExtras.connectingStatusOf(listOf("Abungo", "Ringing...")))
        assertEquals("Dialing", CallStyleExtras.connectingStatusOf(listOf("Dialing")))
        assertNull(CallStyleExtras.connectingStatusOf(listOf("Ongoing call", "00:19")))
    }
}

class DemoEventsTest {

    @Test
    fun everyEventIsListed() {
        assertEquals(IslandEvent.entries.size, DemoEvents.available.size)
    }

    @Test
    fun sampleKeysAreRecognisedAndUnique() {
        val keys = DemoEvents.available.map { DemoEvents.keyFor(it) }
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all { DemoEvents.isDemoKey(it) })
        assertFalse(DemoEvents.isDemoKey("system_battery"))
    }

    @Test
    fun batteryWarningSamplesFollowTheConfiguredLength() {
        assertEquals(2_500L, DemoEvents.lifetimeMs(IslandEvent.LOW_BATTERY, 2_500L))
        assertEquals(2_500L, DemoEvents.lifetimeMs(IslandEvent.BATTERY_SAVER, 2_500L))
        assertEquals(2_500L, DemoEvents.lifetimeMs(IslandEvent.FULLY_CHARGED, 2_500L))
        // other brief samples keep their own fixed lengths
        assertEquals(IslandTimings.PLUGGED_IN_CARD_MS, DemoEvents.lifetimeMs(IslandEvent.PLUGGED_IN, 2_500L))
        assertEquals(0L, DemoEvents.lifetimeMs(IslandEvent.MUSIC, 2_500L))
    }
}

class LiveActivityParserTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun uberTrackingNotificationParsesCorrectly() {
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
    fun zomatoFoodDeliveryParsesCorrectly() {
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
    fun zomatoPromoNotificationReturnsNull() {
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
    fun nonWhitelistedAppReturnsNull() {
        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.random.app"

        val info = LiveActivityParser.parse(sbn)
        assertNull(info)
    }

    @Test
    fun moreDeliveryAndRideAppsAreSupported() {
        listOf("com.ubercab.eats", "com.dd.doordash", "me.lyft.android", "com.grabtaxi.passenger", "com.gojek.app", "com.deliveroo.orderapp", "com.wolt.android", "com.glovo")
            .forEach { assertEquals(it, true, LiveActivityParser.isSupportedApp(it)) }
        assertEquals(false, LiveActivityParser.isSupportedApp("com.waze"))
    }

    @Test
    fun brandColourFallsBackForUnknownPackages() {
        assertEquals(0xFF06C167L, LiveActivityParser.getBrandColor("com.ubercab.eats"))
        assertEquals(0xFF38BDF8L, LiveActivityParser.getBrandColor("com.unknown.app"))
    }
}

class NavigationParserTest {

    @Test
    fun googleMapsNavigationParsedCorrectly() {
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
    fun wazeUTurnParsedCorrectly() {
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
    fun mapsTimelineNotificationIsNotNavigation() {
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
    fun nonNavigationAppReturnsNull() {
        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.whatsapp"
        every { sbn.notification } returns null

        val info = NavigationParser.parse(sbn)
        assertNull(info)
    }

    @Test
    fun screenRecordingIsNotNavigation() {
        val extras = mockk<Bundle>()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Screen recording"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Tap to stop • 5 mins left"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null

        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_NAVIGATION // even if category was navigation
        notification.extras = extras

        val sbn = mockk<StatusBarNotification>()
        every { sbn.packageName } returns "com.sec.android.app.screenrecorder"
        every { sbn.notification } returns notification

        val info = NavigationParser.parse(sbn)
        assertNull(info)
    }

    @Test
    fun localizedTurnDirections() {
        val timeLeftDirection = NavigationParser.parseTurnDirection("Screen recording in progress. 5 min left")
        assertEquals(TurnDirection.STRAIGHT, timeLeftDirection)

        val turnLeftDirection = NavigationParser.parseTurnDirection("Turn left onto Broadway")
        assertEquals(TurnDirection.LEFT, turnLeftDirection)

        val cases = listOf(
            "dans 200 m, tournez à gauche" to TurnDirection.LEFT,
            "in 400 metern links abbiegen" to TurnDirection.LEFT,
            "gire a la derecha en 300 m" to TurnDirection.RIGHT,
            "retorno em 300 m" to TurnDirection.U_TURN,
            "prenez le rond-point" to TurnDirection.ROUNDABOUT
        )
        for ((text, expected) in cases) {
            assertEquals(text, expected, NavigationParser.parseTurnDirection(text))
        }
    }

    @Test
    fun navigationTextLayout() {
        val splitCard = NavigationParser.cardText("In 300 m - Turn left", "onto MG Road \u2022 24 min \u2022 8.5 km")
        assertEquals("In 300 m", splitCard.distance)
        assertEquals("Turn left", splitCard.instruction)
        assertEquals("onto MG Road", splitCard.detail)
        assertEquals("24 min \u2022 8.5 km", splitCard.eta)

        val tripCard = NavigationParser.cardText("Head north on Main St", "24 min \u2022 8.5 km")
        assertEquals("In 200 m", tripCard.distance)
        assertEquals("Head north on Main St", tripCard.instruction)
        assertEquals("", tripCard.detail)
        assertEquals("24 min \u2022 8.5 km", tripCard.eta)

        val blankTitleCard = NavigationParser.cardText("", "")
        assertEquals("Navigation", blankTitleCard.instruction)
        assertEquals("", blankTitleCard.detail)
        assertEquals("", blankTitleCard.eta)

        val durationCard = NavigationParser.cardText("Через 300 м поверните направо", "24 мин • 8,5 км")
        assertEquals("300 м", durationCard.distance)
        assertEquals("поверните направо", durationCard.instruction)
        assertEquals("24 мин • 8,5 км", durationCard.eta)
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
    fun readsDistanceAndTurnsFromMapNotifications() {
        val yandexInfo = NavigationParser.parse(sbn("ru.yandex.yandexnavi", "Через 300 м", "Поверните налево на улицу Ленина"))
        assertNotNull(yandexInfo)
        assertEquals("300 м", yandexInfo?.distanceToTurnText)
        assertEquals(TurnDirection.LEFT, yandexInfo?.turnDirection)

        val slightRightInfo = NavigationParser.parse(sbn("ru.yandex.yandexmaps", "Через 1,2 км", "Держитесь правее"))
        assertEquals("1,2 км", slightRightInfo?.distanceToTurnText)
        assertEquals(TurnDirection.SLIGHT_RIGHT, slightRightInfo?.turnDirection)

        assertEquals(TurnDirection.LEFT, NavigationParser.parse(sbn("com.autonavi.minimap", "前方300米", "左转进入中山路"))?.turnDirection)
        assertEquals("300m", NavigationParser.parse(sbn("com.google.android.apps.maps", "300m先、右折です", ""))?.distanceToTurnText)
        assertEquals(TurnDirection.RIGHT, NavigationParser.parseTurnDirection("500m 앞에서 우회전"))

        assertNull(NavigationParser.parse(sbn("com.ubercab.eats", "Your courier is 500 m away", "Turn left at the corner")))
        assertNull(NavigationParser.parse(sbn("com.grabtaxi.passenger", "Driver is 300 m away", "")))
        assertNull(NavigationParser.parse(sbn("com.dd.doordash", "Dasher arriving in 2 min", "")))

        val ongoing = Notification.FLAG_ONGOING_EVENT
        assertNull(NavigationParser.parse(sbn("com.waze", "Waze is running", "Tap to open", category = null, flags = ongoing)))
        assertNotNull(NavigationParser.parse(sbn("com.waze", "Turn left", "in 300 m", category = null, flags = ongoing)))
    }

    @Test
    fun unknownNavigationApp() {
        // A distance alone is not guidance
        assertNull(NavigationParser.parse(sbn("com.example.tracker", "Package is 500 m away", "")))
        // order vocabulary rules it out even with a turn word in it
        assertNull(NavigationParser.parse(sbn("com.example.food", "Your order: rider will turn left in 200 m", "")))
        // A genuine guidance notification from an unlisted map app still works
        assertNotNull(NavigationParser.parse(sbn("com.example.newmaps", "In 200 m turn right", "onto Park Ave")))
    }

    @Test
    fun moreMapAppsAreKnown() {
        listOf(
            "ru.yandex.yandexnavi", "ru.dublgis.dgismobile", "com.mapswithme.maps.pro", "app.organicmaps", "app.comaps.google",
            "net.osmand.plus", "com.generalmagic.magicearth", "cz.seznam.mapy", "com.mmi.maps", "com.tencent.map"
        ).forEach { assertEquals(it, true, NavigationParser.isNavigationApp(it)) }
        assertEquals(false, NavigationParser.isNavigationApp("com.ubercab"))
    }

    @Test
    fun theArrowAnglesMatchTheDirections() {
        assertEquals(-90f, TurnDirection.LEFT.arrowAngleDeg, 0f)
        assertEquals(90f, TurnDirection.RIGHT.arrowAngleDeg, 0f)
        assertEquals(-45f, TurnDirection.SLIGHT_LEFT.arrowAngleDeg, 0f)
        assertEquals(45f, TurnDirection.SLIGHT_RIGHT.arrowAngleDeg, 0f)
        assertEquals(180f, TurnDirection.U_TURN.arrowAngleDeg, 0f)
        assertEquals(0f, TurnDirection.STRAIGHT.arrowAngleDeg, 0f)
    }

    @Test
    fun theArrowSwingsTheShortWayRound() {
        assertEquals(-90f, TurnArrow.nextAngle(0f, TurnDirection.LEFT), 0f)
        assertEquals(270f, TurnArrow.nextAngle(180f, TurnDirection.LEFT), 0f)
        assertEquals(-180f, TurnArrow.nextAngle(-90f, TurnDirection.U_TURN), 0f)
        assertEquals(90f, TurnArrow.nextAngle(90f, TurnDirection.RIGHT), 0f)
        assertEquals(360f, TurnArrow.nextAngle(270f, TurnDirection.STRAIGHT), 0f)
    }
}

class PictureFolderPickTest {
    private val posted = 1_000_000_000_000L // ms
    private val now = posted / 1000

    private fun line(m: Long, a: Long, c: Long, path: String) = "$m|$a|$c|$path"

    @Test
    fun stickerFileSelection() {
        // modified a week ago, but accessed / changed when the sticker arrived again
        val reusedListing = line(now - 600_000, now - 5, now - 5, "/x/WhatsApp Stickers/abc.webp")
        val reusedPick = PictureFolderPick.pick(reusedListing, posted, sticker = true)
        assertNotNull(reusedPick)
        assertEquals("/x/WhatsApp Stickers/abc.webp", reusedPick!!.path)

        val extensionlessListing = line(now, now, now, "/x/WhatsApp Stickers/3f9c2a")
        assertNotNull(PictureFolderPick.pick(extensionlessListing, posted, sticker = true))

        val outsideListing = line(now, now, now, "/x/WhatsApp Images/IMG-1.webp")
        assertNull(PictureFolderPick.pick(outsideListing, posted, sticker = true))

        val photoListing = line(now - 600_000, now - 5, now - 5, "/x/WhatsApp Images/IMG-1.jpg")
        assertNull(PictureFolderPick.pick(photoListing, posted, sticker = false))

        val newestListing = listOf(
            line(now - 30, now, now, "/x/WhatsApp Stickers/old.webp"),
            line(now - 3, now, now, "/x/WhatsApp Stickers/new.webp")
        ).joinToString("\n")
        assertEquals("/x/WhatsApp Stickers/new.webp", PictureFolderPick.pick(newestListing, posted, sticker = true)!!.path)
    }

    @Test
    fun sentStickersAndThumbnailsAreSkipped() {
        val listing = listOf(
            line(now, now, now, "/x/WhatsApp Stickers/Sent/a.webp"),
            line(now, now, now, "/x/WhatsApp Stickers/thumb/b.webp")
        ).joinToString("\n")
        assertNull(PictureFolderPick.pick(listing, posted, sticker = true))
    }

    @Test
    fun brokenLinesAreIgnored() {
        assertEquals(0, PictureFolderPick.parse("garbage\n12|x", sticker = true).size)
    }
}

class PictureMessageTextTest {
    @Test
    fun pictureMessageFiltering() {
        val text = textWithoutPictureMessages(
            listOf(
                MessageLine("see you at 6", hasPicture = false),
                MessageLine("Photo", hasPicture = true),
                MessageLine("look at this caption", hasPicture = true),
                MessageLine("  nice!  ", hasPicture = false)
            )
        )
        assertEquals("see you at 6\nnice!", text)

        assertEquals("", textWithoutPictureMessages(listOf(MessageLine("Sticker", true))))

        val lines = (1..8).map { MessageLine("m$it", false) }
        assertEquals("m3\nm4\nm5\nm6\nm7\nm8", textWithoutPictureMessages(lines))
    }

    @Test
    fun pictureLabelsIncludePlaceholdersAndExcludeCaptions() {
        val cases = listOf(
            "Photo" to true,
            "📷 Photo" to true,
            "Sticker" to true,
            "2 photos" to true,
            "  " to true,
            "Photo from the trip" to false,
            "Lunch at noon?" to false
        )
        for ((text, expected) in cases) {
            assertEquals("Input: $text", expected, isPictureLabel(text))
        }
    }


    @Test
    fun previewShowsTheLastNonBlankLine() {
        assertEquals("nice!", lastReaderLine("see you at 6\n nice! \n\n"))
        assertEquals("", lastReaderLine(""))
    }
}

class PictureRetryPlanTest {
    @Test
    fun firstAttemptIsImmediateAndTheGapsGrow() {
        val d = PictureRetryPlan.DELAYS_MS
        assertEquals(0L, d.first())
        assertTrue(d.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun pictureRetryRequiresAnImageHint() {
        assertFalse(PictureRetryPlan.expectsPicture(hasImageLink = false, hasBigPicture = false, textIsPictureLabel = false))
        assertTrue(PictureRetryPlan.expectsPicture(true, false, false))
        assertTrue(PictureRetryPlan.expectsPicture(false, true, false))
        assertTrue(PictureRetryPlan.expectsPicture(false, false, true))
    }


    @Test
    fun hasNextAttemptStopsAtTheLastEntry() {
        val last = PictureRetryPlan.DELAYS_MS.lastIndex
        assertTrue(PictureRetryPlan.hasNextAttempt(0))
        assertFalse(PictureRetryPlan.hasNextAttempt(last))
    }
}

class SendAttemptsTest {
    @Test
    fun intentDeliveryFallback() {
        val failedAttempts = List<() -> Unit>(3) { { throw IllegalStateException("canceled") } }
        assertFalse(SendAttempts.anySucceeds(failedAttempts))

        var recoveredCalls = 0
        val recoveredAttempts = listOf<() -> Unit>(
            { recoveredCalls++; throw IllegalStateException("options rejected") },
            { recoveredCalls++ }
        )
        assertTrue(SendAttempts.anySucceeds(recoveredAttempts))
        assertEquals(2, recoveredCalls)

        var deliveredCalls = 0
        val deliveredAttempts = listOf<() -> Unit>({ deliveredCalls++ }, { deliveredCalls++ }, { deliveredCalls++ })
        assertTrue(SendAttempts.anySucceeds(deliveredAttempts))
        assertEquals(1, deliveredCalls)

        assertFalse(SendAttempts.anySucceeds(emptyList()))
    }
}

class StickerLabelTest {
    @Test
    fun stickerLabelMustStandAlone() {
        val cases = listOf(
            "Sticker" to true,
            "Animated sticker" to true,
            "\uD83D\uDE42 Sticker" to true,
            "Robertosaurus: Sticker" to true,
            "my sticker collection" to false,
            "Sticker pack is ready, check it" to false,
            "" to false,
            null to false
        )
        for ((text, expected) in cases) {
            assertEquals("Input: $text", expected, StickerLabel.isLabel(text))
        }
    }

}

class VoipCallDetectorTest {
    private fun facts(
        actions: List<String> = emptyList(),
        ongoing: Boolean = true,
        category: String? = null,
        callStyle: Boolean = false,
        chronometer: Boolean = false,
        media: Boolean = false,
        callText: Boolean = false
    ) = VoipNotificationFacts("com.example.voip", ongoing, category, callStyle, actions, chronometer, media, callText)

    @Test
    fun audioModeInCommunicationIsVoip() {
        assertTrue(VoipCallDetector.isVoipAudioMode(AudioManager.MODE_IN_COMMUNICATION))
        assertFalse(VoipCallDetector.isVoipAudioMode(AudioManager.MODE_NORMAL))
        assertFalse(VoipCallDetector.isVoipAudioMode(AudioManager.MODE_IN_CALL))
    }

    @Test
    fun callActionLabels() {
        val cases = listOf(
            "Mute" to CallActionRole.Mute,
            "Unmute" to CallActionRole.Unmute,
            "Speaker" to CallActionRole.Speaker,
            "Hang up" to CallActionRole.HangUp,
            "End call" to CallActionRole.HangUp,
            "Leave" to CallActionRole.HangUp,
            "Decline" to CallActionRole.HangUp,
            "Answer" to CallActionRole.Answer,
            "Accept call" to CallActionRole.Answer,
            "Answer with voicemail" to CallActionRole.Other,
            "Take a message" to CallActionRole.Other,
            "Silenciar" to CallActionRole.Mute,
            "挂断" to CallActionRole.HangUp,
            "Send" to CallActionRole.Other,
            "Reply" to CallActionRole.Other
        )
        for ((input, expected) in cases) {
            assertEquals("input=$input", expected, VoipCallDetector.classifyCallAction(input))
        }
    }

    @Test
    fun speakerLabelThatOffersToSwitchBackMeansSpeakerIsOn() {
        assertTrue(VoipCallDetector.isSpeakerOnLabel("Earpiece"))
        assertTrue(VoipCallDetector.isSpeakerOnLabel("Speaker off"))
        assertFalse(VoipCallDetector.isSpeakerOnLabel("Speaker"))
    }

    @Test
    fun voipSignals() {
        assertTrue(VoipCallDetector.isVoipCall(facts(actions = listOf("Mute", "Hang up"))))

        assertTrue(VoipCallDetector.isVoipCall(facts(category = Notification.CATEGORY_CALL)))
        assertTrue(VoipCallDetector.isVoipCall(facts(callStyle = true)))

        assertFalse(VoipCallDetector.isVoipCall(facts()))
        assertFalse(VoipCallDetector.isVoipCall(facts(actions = listOf("Open"))))

        assertTrue(VoipCallDetector.isVoipCall(facts(chronometer = true, callText = true)))
        assertFalse(VoipCallDetector.isVoipCall(facts(chronometer = true)))

        assertFalse(VoipCallDetector.isVoipCall(facts(actions = listOf("Mute", "End"), media = true)))
    }

    @Test
    fun voipCandidateSelection() {
        val best = VoipCallDetector.pickBest(
            listOf(
                "chat" to facts(actions = listOf("Reply")),
                "weak" to facts(actions = listOf("Hang up")),
                "call" to facts(actions = listOf("Mute", "Speaker", "Hang up"), chronometer = true)
            )
        )
        assertEquals("call", best)

        assertNull(VoipCallDetector.pickBest(listOf("chat" to facts(actions = listOf("Reply")))))
    }

    @Test
    fun signalTracksStartAndResets() {
        VoipCallSignal.setActive(true, nowMillis = 1000L)
        assertTrue(VoipCallSignal.active)
        assertEquals(1000L, VoipCallSignal.sinceMillis)
        VoipCallSignal.setActive(true, nowMillis = 5000L)
        assertEquals(1000L, VoipCallSignal.sinceMillis)
        VoipCallSignal.voiceCapture = true
        VoipCallSignal.setActive(false)
        assertFalse(VoipCallSignal.active)
        assertFalse(VoipCallSignal.voiceCapture)
        assertEquals(0L, VoipCallSignal.sinceMillis)
    }
}
