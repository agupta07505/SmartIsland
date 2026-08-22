/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.util.toIslandMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class IslandModeMappingTest {

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

    private fun createBaseExtras(): Bundle {
        val extras = mockk<Bundle>()
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns null
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null
        return extras
    }

    @Test
    fun testCategoryCallMapsToIncomingCall() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_CALL
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = null
        notification.extras = createBaseExtras()

        assertEquals(IslandMode.IncomingCall, notification.toIslandMode())
    }

    @Test
    fun testCategoryMissedCallMapsToRegularNotification() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_MISSED_CALL
        notification.actions = null
        notification.extras = createBaseExtras()

        assertEquals(IslandMode.Notification, notification.toIslandMode())
    }

    @Test
    fun testAnswerAndDeclineActionPairMapsToIncomingCall() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_MESSAGE

        val answerAction = mockk<Notification.Action>()
        answerAction.title = "Answer"
        val declineAction = mockk<Notification.Action>()
        declineAction.title = "Decline"
        notification.actions = arrayOf(answerAction, declineAction)
        notification.extras = createBaseExtras()

        assertEquals(IslandMode.IncomingCall, notification.toIslandMode())
    }

    @Test
    fun testCategoryTransportMapsToMusic() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_TRANSPORT
        notification.actions = null
        notification.extras = createBaseExtras()

        assertEquals(IslandMode.Music, notification.toIslandMode())
    }

    @Test
    fun testCategoryProgressMapsToDownloadUpload() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_PROGRESS
        notification.actions = null
        notification.extras = createBaseExtras()

        assertEquals(IslandMode.DownloadUpload, notification.toIslandMode())
    }

    @Test
    fun testMediaActionsWithoutMediaSessionRemainRegularNotification() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_MESSAGE

        val playAction = mockk<Notification.Action>()
        playAction.title = "Play track"

        notification.actions = arrayOf(playAction)
        notification.extras = createBaseExtras()

        assertEquals(IslandMode.Notification, notification.toIslandMode())
    }

    @Test
    fun testGenericNotificationMapsToNotification() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_MESSAGE
        notification.actions = null
        notification.extras = createBaseExtras()

        assertEquals(IslandMode.Notification, notification.toIslandMode())
    }

    @Test
    fun testLiveActivityNotificationMapsToLiveActivityMode() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_MESSAGE
        notification.actions = null

        val sbn = mockk<StatusBarNotification>()
        val extras = createBaseExtras()
        notification.extras = extras
        every { sbn.packageName } returns "in.swiggy.android"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Swiggy Food Delivery"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Arriving in 12 mins"
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 50
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 100

        assertEquals(IslandMode.LiveActivity, notification.toIslandMode(sbn, true))
    }

    @Test
    fun testDisabledLiveActivitySettingMapsToNormalNotification() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_MESSAGE
        notification.actions = null

        val sbn = mockk<StatusBarNotification>()
        val extras = createBaseExtras()
        notification.extras = extras
        every { sbn.packageName } returns "in.swiggy.android"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Swiggy Food Delivery"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Arriving in 12 mins"
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0

        assertEquals(IslandMode.Notification, notification.toIslandMode(sbn, liveActivitiesEnabled = false))
    }

    @Test
    fun testNavigationNotificationMapsToNavigationMode() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_NAVIGATION
        notification.actions = null

        val sbn = mockk<StatusBarNotification>()
        val extras = createBaseExtras()
        notification.extras = extras
        every { sbn.packageName } returns "com.google.android.apps.maps"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "In 300 m - Turn left"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "onto MG Road"
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns "24 min • 8.5 km"

        assertEquals(IslandMode.Navigation, notification.toIslandMode(sbn, liveActivitiesEnabled = true, navigationEnabled = true))
    }

    @Test
    fun testHotspotNotificationMapsToHotspotMode() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_STATUS
        notification.actions = null

        val extras = createBaseExtras()
        notification.extras = extras
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Mobile Hotspot"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Active • 2 devices connected"

        assertEquals(IslandMode.Hotspot, notification.toIslandMode())
    }

    @Test
    fun testSamsungScreenRecordingNotificationMapsToScreenRecordingAndNotMap() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_NAVIGATION // Even if category is CATEGORY_NAVIGATION or service
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = null

        val sbn = mockk<StatusBarNotification>()
        val extras = createBaseExtras()
        notification.extras = extras
        every { sbn.packageName } returns "com.sec.android.app.screenrecorder"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Recording screen..."
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Tap to stop recording. 05:20 left"

        // Must map to ScreenRecording, NEVER Navigation (Map UI)
        assertEquals(IslandMode.ScreenRecording, notification.toIslandMode(sbn, deviceType = "SAMSUNG"))
    }

    @Test
    fun testXiaomiScreenRecordingNotificationMapsToScreenRecording() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_SERVICE
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = null

        val sbn = mockk<StatusBarNotification>()
        val extras = createBaseExtras()
        notification.extras = extras
        every { sbn.packageName } returns "com.miui.screenrecorder"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Screen Recorder"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Recording in progress"

        assertEquals(IslandMode.ScreenRecording, notification.toIslandMode(sbn, deviceType = "XIAOMI_REDMI_POCO"))
    }

    @Test
    fun testVivoScreenRecordingNotificationMapsToScreenRecording() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_SERVICE
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = null

        val sbn = mockk<StatusBarNotification>()
        val extras = createBaseExtras()
        notification.extras = extras
        every { sbn.packageName } returns "com.vivo.screenrecorder"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "S-Capture"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Screen recording..."

        assertEquals(IslandMode.ScreenRecording, notification.toIslandMode(sbn, deviceType = "VIVO_IQOO"))
    }

    @Test
    fun testRealmeScreenRecordingNotificationMapsToScreenRecording() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_SERVICE
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = null

        val sbn = mockk<StatusBarNotification>()
        val extras = createBaseExtras()
        notification.extras = extras
        every { sbn.packageName } returns "com.oplus.screenrecorder"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Screen Recording"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Recording screen"

        assertEquals(IslandMode.ScreenRecording, notification.toIslandMode(sbn, deviceType = "REALME_OPPO_ONEPLUS"))
    }

    @Test
    fun testSamsungInCallMapsToIncomingCall() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_CALL
        notification.flags = Notification.FLAG_ONGOING_EVENT

        val answerAction = mockk<Notification.Action>()
        answerAction.title = "Answer"
        val declineAction = mockk<Notification.Action>()
        declineAction.title = "Decline"
        notification.actions = arrayOf(answerAction, declineAction)

        val sbn = mockk<StatusBarNotification>()
        val extras = createBaseExtras()
        notification.extras = extras
        every { sbn.packageName } returns "com.samsung.android.incallui"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Mom"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Incoming call"

        assertEquals(IslandMode.IncomingCall, notification.toIslandMode(sbn, deviceType = "SAMSUNG"))
    }

    @Test
    fun testTimerNotificationMapsToTimer() {
        val notification = mockk<Notification>()
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.category = Notification.CATEGORY_ALARM

        val pause = mockk<Notification.Action>()
        pause.title = "Pause"
        val reset = mockk<Notification.Action>()
        reset.title = "Reset"
        notification.actions = arrayOf(pause, reset)
        notification.`when` = 0L

        val sbn = mockk<StatusBarNotification>()
        val extras = createBaseExtras()
        notification.extras = extras
        every { sbn.packageName } returns "com.google.android.deskclock"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Timer"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "04:59"

        assertEquals(IslandMode.Timer, notification.toIslandMode(sbn))
    }

    @Test
    fun testStopwatchNotificationMapsToStopwatch() {
        val notification = mockk<Notification>()
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.category = Notification.CATEGORY_STOPWATCH

        val lap = mockk<Notification.Action>()
        lap.title = "Lap"
        val pause = mockk<Notification.Action>()
        pause.title = "Pause"
        notification.actions = arrayOf(lap, pause)
        notification.`when` = 0L

        val sbn = mockk<StatusBarNotification>()
        val extras = createBaseExtras()
        notification.extras = extras
        every { sbn.packageName } returns "com.google.android.deskclock"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Stopwatch"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "00:14.28"

        assertEquals(IslandMode.Stopwatch, notification.toIslandMode(sbn))
    }
}
