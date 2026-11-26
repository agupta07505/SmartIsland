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
import android.util.Log
import com.agupta07505.smartisland.util.toIslandMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IslandEventTest {
    private fun notif(mode: IslandMode, category: String? = null, key: String = "k") =
        IslandNotification(key = key, packageName = "p", appName = "a", title = "t", text = "x", timeMillis = 0L, mode = mode, category = category)

    @Test
    fun connectivityEventMapping() {
        assertEquals(IslandEvent.PLUGGED_IN, IslandEvent.forNotification(notif(IslandMode.Battery, "battery_plugged_in")))
        assertEquals(IslandEvent.PLUGGED_OUT, IslandEvent.forNotification(notif(IslandMode.Battery, "battery_plugged_out")))
        assertEquals(IslandEvent.FULLY_CHARGED, IslandEvent.forNotification(notif(IslandMode.Battery, "battery_full")))
        assertEquals(IslandEvent.LOW_BATTERY, IslandEvent.forNotification(notif(IslandMode.Battery, "battery_low")))
        assertEquals(IslandEvent.BATTERY_SAVER, IslandEvent.forNotification(notif(IslandMode.Battery, "battery_saver")))

        assertEquals(IslandEvent.WIFI, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "wifi")))
        assertEquals(IslandEvent.VPN, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "vpn")))
        assertEquals(IslandEvent.BLUETOOTH, IslandEvent.forNotification(notif(IslandMode.Bluetooth)))

        assertEquals(IslandEvent.WIFI_DISCONNECTED, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "wifi_disconnected")))
        assertEquals(IslandEvent.VPN_DISCONNECTED, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "vpn_disconnected")))
        assertTrue(IslandEvent.WIFI_DISCONNECTED.canSwitchAutoExpand && IslandEvent.VPN_DISCONNECTED.canHideOnLockScreen)
        assertEquals("Connectivity", IslandEvent.WIFI_DISCONNECTED.group)

        assertEquals(IslandEvent.USB_CONNECTED, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "usb_connected")))
        assertEquals(IslandEvent.USB_DISCONNECTED, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "usb_disconnected")))
        assertEquals(IslandEvent.HEADPHONES_CONNECTED, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "headphones_connected")))
        assertEquals(IslandEvent.HEADPHONES_DISCONNECTED, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "headphones_disconnected")))
        assertTrue(IslandEvent.USB_CONNECTED.canSwitchAutoExpand && IslandEvent.HEADPHONES_DISCONNECTED.canHideOnLockScreen)
        assertEquals("Connectivity", IslandEvent.HEADPHONES_CONNECTED.group)

        assertEquals(IslandEvent.PLUGGED_OUT, IslandEvent.forNotification(notif(IslandMode.Battery, "battery_plugged_out")))
        assertEquals(IslandEvent.PLUGGED_IN, IslandEvent.forNotification(notif(IslandMode.Battery, "battery_plugged_in")))
    }

    @Test
    fun deviceStateEventSwitches() {
        assertEquals(IslandEvent.AIRPLANE_MODE, IslandEvent.forNotification(notif(IslandMode.Bluetooth, DeviceStates.AIRPLANE_ON)))
        assertEquals(IslandEvent.AIRPLANE_MODE, IslandEvent.forNotification(notif(IslandMode.Bluetooth, DeviceStates.AIRPLANE_OFF)))
        assertEquals(IslandEvent.RING, IslandEvent.forNotification(notif(IslandMode.Bluetooth, DeviceStates.RINGER_RING)))
        assertEquals(IslandEvent.VIBRATE, IslandEvent.forNotification(notif(IslandMode.Bluetooth, DeviceStates.RINGER_VIBRATE)))
        assertEquals(IslandEvent.SILENT, IslandEvent.forNotification(notif(IslandMode.Bluetooth, DeviceStates.RINGER_SILENT)))
        assertEquals(IslandEvent.DND, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "filter_dnd")))
        assertEquals(IslandEvent.MODE, IslandEvent.forNotification(notif(IslandMode.Bluetooth, "mode_driving_off")))
        assertEquals("Modes", IslandEvent.MODE.group)
        assertTrue(IslandEvent.DND.canSwitchAutoExpand && IslandEvent.SILENT.canHideOnLockScreen)
    }

    @Test
    fun deviceStateEventsOnlyPulseByDefault() {
        val ids = listOf(
            IslandEvent.AIRPLANE_MODE, IslandEvent.RING, IslandEvent.VIBRATE, IslandEvent.SILENT, IslandEvent.DND,
            IslandEvent.MODE
        ).map { it.id }
        assertTrue(IslandEvent.DEFAULT_NO_AUTO_EXPAND_IDS.containsAll(ids))
    }

    @Test
    fun systemEventMapping() {
        assertNull(IslandEvent.forNotification(notif(IslandMode.Empty)))

        assertEquals(IslandEvent.AUDIO_RECORDING, IslandEvent.forNotification(notif(IslandMode.AudioRecording)))
        assertEquals(IslandEvent.AUDIO_RECORDING, IslandEvent.forListenerMode(IslandMode.AudioRecording))
        assertEquals(IslandEvent.SCREEN_RECORDING, IslandEvent.forNotification(notif(IslandMode.ScreenRecording)))

        fun transfer(kind: TransferKind) = notif(IslandMode.DownloadUpload).copy(transferKind = kind)
        assertEquals(IslandEvent.DOWNLOAD, IslandEvent.forNotification(notif(IslandMode.DownloadUpload)))
        assertEquals(IslandEvent.UPLOAD, IslandEvent.forNotification(transfer(TransferKind.UPLOAD)))
        assertEquals(IslandEvent.BACKUP, IslandEvent.forNotification(transfer(TransferKind.BACKUP)))
        assertEquals(IslandEvent.FILE_COPY_MOVE, IslandEvent.forNotification(transfer(TransferKind.COPY_MOVE)))
        assertEquals(IslandEvent.FILE_DELETE, IslandEvent.forNotification(transfer(TransferKind.DELETE)))
        assertEquals(IslandEvent.APP_INSTALL, IslandEvent.forNotification(transfer(TransferKind.INSTALL)))
        assertEquals(IslandEvent.UPLOAD, IslandEvent.forListenerMode(IslandMode.DownloadUpload, TransferKind.UPLOAD))
        assertEquals(IslandEvent.DOWNLOAD, IslandEvent.forListenerMode(IslandMode.DownloadUpload))

        assertEquals(IslandEvent.UPLOAD, IslandEvent.forNotification(notif(IslandMode.DownloadUpload, key = "upload_7")))

        val running = notif(IslandMode.Timer)
        assertEquals(IslandEvent.TIMER, IslandEvent.forNotification(running))
        assertEquals(IslandEvent.TIMER_FINISHED, IslandEvent.forNotification(running.copy(timerFinished = true)))
        // only a timer card can be finished
        assertEquals(IslandEvent.STOPWATCH, IslandEvent.forNotification(notif(IslandMode.Stopwatch).copy(timerFinished = true)))
    }

    @Test
    fun eventIdsAreUnique() {
        assertEquals(IslandEvent.entries.size, IslandEvent.entries.map { it.id }.toSet().size)
        assertTrue(IslandEvent.entries.all { it.id.length <= 64 })
    }

    @Test
    fun everyTransferKindHasADistinctEvent() {
        val events = TransferKind.entries.map { IslandEvent.forTransfer(it) }
        assertEquals(TransferKind.entries.size, events.toSet().size)
    }

    @Test
    fun uploadsAndBackupsPointUp() {
        assertTrue(notif(IslandMode.DownloadUpload).copy(transferKind = TransferKind.UPLOAD).isUpload)
        assertTrue(notif(IslandMode.DownloadUpload).copy(transferKind = TransferKind.BACKUP).isUpload)
        assertFalse(notif(IslandMode.DownloadUpload).copy(transferKind = TransferKind.DOWNLOAD).isUpload)
    }

    @Test
    fun everyEventHasShortLabel() {
        IslandEvent.entries.forEach { event ->
            assertTrue("${event.id} short label is blank", event.shortLabel.isNotBlank())
            assertTrue("${event.id} short label too long: ${event.shortLabel}", event.shortLabel.length <= 14)
            assertTrue("${event.id} short label is not shorter than the long one", event.shortLabel.length <= event.label.length)
        }
    }

    @Test
    fun shortLabelsAreUniqueWithinAGroup() {
        IslandEvent.entries.groupBy { it.group }.forEach { (group, events) ->
            assertEquals("duplicate short label in $group", events.size, events.map { it.shortLabel }.toSet().size)
        }
    }

    @Test
    fun autoExpandSwitchForAllButFaceId() {
        assertTrue(IslandEvent.CALL.canSwitchAutoExpand)
        assertFalse(IslandEvent.FACE_UNLOCK.canSwitchAutoExpand)
        assertTrue(IslandEvent.PLUGGED_IN.canSwitchAutoExpand)
        assertTrue(IslandEvent.NOTIFICATION.canSwitchAutoExpand)
        assertTrue(IslandEvent.BLUETOOTH.canSwitchAutoExpand)
    }

    @Test
    fun loudMeansNotSilent() {
        assertTrue(notif(IslandMode.Notification).isLoud)
        assertFalse(notif(IslandMode.Notification).copy(isSilent = true).isLoud)
        // live and system cards are never flagged silent, so they are loud
        assertTrue(notif(IslandMode.Battery, "battery_plugged_in").isLoud)
    }

    @Test
    fun timerFinishedHasItsOwnSwitches() {
        assertEquals("timer_finished", IslandEvent.TIMER_FINISHED.id)
        assertEquals("Activities", IslandEvent.TIMER_FINISHED.group)
        assertTrue(IslandEvent.TIMER_FINISHED.canSwitchAutoExpand)
        assertTrue(IslandEvent.TIMER_FINISHED.canHideOnLockScreen)
    }

    @Test
    fun onlyFiveEventsAutoExpandByDefault() {
        val on = IslandEvent.entries.filter { it.canSwitchAutoExpand && it.id !in IslandEvent.DEFAULT_NO_AUTO_EXPAND_IDS }
        assertEquals(
            setOf(
                IslandEvent.CALL, IslandEvent.NOTIFICATION, IslandEvent.LOW_BATTERY,
                IslandEvent.VPN_DISCONNECTED, IslandEvent.TIMER_FINISHED
            ),
            on.toSet()
        )
    }

    @Test
    fun optionalEventDefaults() {
        val off = IslandEvent.entries
            .filter { it.canSwitchAutoExpand && it !in IslandEvent.AUTO_EXPAND_BY_DEFAULT }
            .map { it.id }
            .toSet()
        assertEquals(off, IslandEvent.DEFAULT_NO_AUTO_EXPAND_IDS)
        assertFalse(IslandEvent.FACE_UNLOCK.id in IslandEvent.DEFAULT_NO_AUTO_EXPAND_IDS)
    }
}

class IslandExemptModeTest {
    private val off = setOf("a.off")
    private val stat = setOf("a.static")
    private val land = setOf("a.land", "a.static")

    private fun mode(pkg: String?, landscape: Boolean) = IslandExemption.modeFor(pkg, off, stat, land, landscape)

    @Test fun exemptionRules() {
        assertEquals(ExemptMode.Off, mode("a.off", false))
        assertEquals(ExemptMode.Static, mode("a.static", false))
        assertEquals(ExemptMode.None, mode("a.land", false))
        assertEquals(ExemptMode.Off, mode("a.land", true))
        assertEquals(ExemptMode.Off, mode("a.static", true))
        assertEquals(ExemptMode.None, mode("a.off", true))
        assertEquals(ExemptMode.None, mode(null, false))
        assertEquals(ExemptMode.None, mode("", true))
    }
}

class IslandModeMappingTest {

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

    private fun notificationWithCategory(category: String) = mockk<Notification>().also {
        it.category = category
        it.actions = null
        it.extras = createBaseExtras()
    }

    private fun createBaseExtras(): Bundle {
        val extras = mockk<Bundle>()
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        every { extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) } returns false
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns null
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null
        return extras
    }

    @Test
    fun categoryCallMapsToIncomingCall() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_CALL
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = null
        notification.extras = createBaseExtras()

        assertEquals(IslandMode.IncomingCall, notification.toIslandMode())
    }

    @Test
    fun categoryMapping() {
        val modes = mapOf(
            Notification.CATEGORY_MISSED_CALL to IslandMode.Notification,
            Notification.CATEGORY_TRANSPORT to IslandMode.Music,
            Notification.CATEGORY_PROGRESS to IslandMode.DownloadUpload,
            Notification.CATEGORY_MESSAGE to IslandMode.Notification
        )
        for ((category, expected) in modes) {
            assertEquals(category, expected, notificationWithCategory(category).toIslandMode())
        }
    }

    @Test
    fun answerDeclinePairIsIncomingCall() {
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
    fun mediaActionsWithoutSession() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_MESSAGE

        val playAction = mockk<Notification.Action>()
        playAction.title = "Play track"

        notification.actions = arrayOf(playAction)
        notification.extras = createBaseExtras()

        assertEquals(IslandMode.Notification, notification.toIslandMode())
    }

    @Test
    fun liveActivityNotificationMapsToLiveActivityMode() {
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
    fun disabledLiveActivitySettingMapsToNormalNotification() {
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
    fun navigationMapsToNavigationMode() {
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
    fun hotspotMapsToHotspotMode() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_STATUS
        notification.actions = null

        val sbn = mockk<StatusBarNotification>()
        val extras = createBaseExtras()
        notification.extras = extras
        // hotspot detection is gated on the posting package (system/tethering packages or known OEM hotspot apps)
        every { sbn.packageName } returns "com.android.systemui"
        every { sbn.notification } returns notification
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Mobile Hotspot"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Active • 2 devices connected"

        assertEquals(IslandMode.Hotspot, notification.toIslandMode(sbn))
    }

    @Test
    fun samsungScreenRecordingIsNotNavigation() {
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_NAVIGATION // even if category is CATEGORY_NAVIGATION or service
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = null

        val sbn = mockk<StatusBarNotification>()
        val extras = createBaseExtras()
        notification.extras = extras
        every { sbn.packageName } returns "com.sec.android.app.screenrecorder"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Recording screen..."
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Tap to stop recording. 05:20 left"

        // must map to ScreenRecording, not Navigation (Map UI)
        assertEquals(IslandMode.ScreenRecording, notification.toIslandMode(sbn, deviceType = "SAMSUNG"))
    }

    @Test
    fun vendorRecordingCategory() {
        // package, title, text, device type
        val vendors = listOf(
            listOf("com.miui.screenrecorder", "Screen Recorder", "Recording in progress", "XIAOMI_REDMI_POCO"),
            listOf("com.vivo.screenrecorder", "S-Capture", "Screen recording...", "VIVO_IQOO"),
            listOf("com.oplus.screenrecorder", "Screen Recording", "Recording screen", "REALME_OPPO_ONEPLUS")
        )
        for ((pkg, title, text, deviceType) in vendors) {
            val notification = mockk<Notification>()
            notification.category = Notification.CATEGORY_SERVICE
            notification.flags = Notification.FLAG_ONGOING_EVENT
            notification.actions = null

            val sbn = mockk<StatusBarNotification>()
            val extras = createBaseExtras()
            notification.extras = extras
            every { sbn.packageName } returns pkg
            every { sbn.notification } returns notification

            every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns title
            every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns text

            assertEquals(pkg, IslandMode.ScreenRecording, notification.toIslandMode(sbn, deviceType = deviceType))
        }
    }

    @Test
    fun samsungInCallMapsToIncomingCall() {
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
    fun timerNotificationMapsToTimer() {
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
    fun stopwatchMapsToStopwatch() {
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

    @Test
    fun alarmNotificationMapsToNotification() {
        val notification = mockk<Notification>()
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.category = Notification.CATEGORY_ALARM

        val snooze = mockk<Notification.Action>()
        snooze.title = "Snooze"
        val dismiss = mockk<Notification.Action>()
        dismiss.title = "Dismiss"
        notification.actions = arrayOf(snooze, dismiss)
        notification.`when` = 0L

        val sbn = mockk<StatusBarNotification>()
        val extras = createBaseExtras()
        notification.extras = extras
        every { sbn.packageName } returns "com.google.android.deskclock"
        every { sbn.notification } returns notification

        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Alarm"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "07:00 AM"

        assertEquals(IslandMode.Notification, notification.toIslandMode(sbn))
    }

    @Test
    fun voiceRecorderIsAudioRecordingNotScreen() {
        val testCases = listOf(
            Triple("com.sec.android.app.voicenote", "Voice recording", "SAMSUNG"),
            Triple("com.miui.soundrecorder", "Sound Recorder", "XIAOMI_REDMI_POCO"),
            Triple("com.google.android.apps.recorder", "Voice recording in progress", "GOOGLE_PIXEL")
        )

        for ((pkg, title, device) in testCases) {
            val notification = mockk<Notification>()
            notification.category = Notification.CATEGORY_SERVICE
            notification.flags = Notification.FLAG_ONGOING_EVENT
            notification.actions = null

            val sbn = mockk<StatusBarNotification>()
            val extras = createBaseExtras()
            notification.extras = extras
            every { sbn.packageName } returns pkg
            every { sbn.notification } returns notification

            every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns title
            every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Recording active"

            assertEquals(
                "Expected $pkg to resolve to AudioRecording mode",
                IslandMode.AudioRecording,
                notification.toIslandMode(sbn, deviceType = device)
            )
        }
    }
}
