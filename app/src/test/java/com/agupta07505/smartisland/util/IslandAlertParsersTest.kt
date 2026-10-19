/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure rules behind the alarm (Rule_Book §4.10), call (§4.7), voice recorder (§4.3), privacy indicator
 * (§4.15, §4.16) and landscape docking (§9.2) states.
 */
class IslandAlertParsersTest {

    private fun actions(vararg titles: String) = titles.map { IslandNotificationAction(it, null) }

    // ---- Action matching ----

    @Test
    fun alarmButtonsMatchTheClockAppsOwnLabels() {
        val google = actions("Snooze", "Stop")
        assertEquals("Snooze", NotificationActionMatcher.find(google, IslandControl.AlarmSnooze)?.title)
        assertEquals("Stop", NotificationActionMatcher.find(google, IslandControl.AlarmStop)?.title)
        val samsung = actions("Dismiss", "Snooze")
        assertEquals("Dismiss", NotificationActionMatcher.find(samsung, IslandControl.AlarmStop)?.title)
        // "Turn off snooze" is never the Stop button.
        assertNull(NotificationActionMatcher.find(actions("Turn off snooze"), IslandControl.AlarmStop))
    }

    @Test
    fun callControlsMatchByWholeWordsAndSemanticAction() {
        val ringing = actions("Decline", "Answer")
        assertEquals("Answer", NotificationActionMatcher.find(ringing, IslandControl.CallAnswer)?.title)
        assertEquals("Decline", NotificationActionMatcher.find(ringing, IslandControl.CallDecline)?.title)

        val ongoing = actions("Speaker", "Hang up")
        assertEquals("Hang up", NotificationActionMatcher.find(ongoing, IslandControl.CallEnd)?.title)
        assertEquals("Speaker", NotificationActionMatcher.find(ongoing, IslandControl.CallSpeaker)?.title)
        // No Mute action: the button is hidden.
        assertNull(NotificationActionMatcher.find(ongoing, IslandControl.CallMute))
        // "End" is a whole word: "Send" and "Speaker" are not End Call.
        assertNull(NotificationActionMatcher.find(actions("Send", "Speaker"), IslandControl.CallEnd))
        // A semantic mute action matches whatever its label says.
        val semantic = listOf(IslandNotificationAction("Stumm", null, semanticAction = SemanticAction.MUTE))
        assertEquals("Stumm", NotificationActionMatcher.find(semantic, IslandControl.CallMute)?.title)
    }

    @Test
    fun aRecorderIsPausedWhenItOffersResumeOrSaysSo() {
        assertFalse(NotificationActionMatcher.isRecorderPaused(actions("Pause", "Stop"), "Recording", "00:12"))
        assertTrue(NotificationActionMatcher.isRecorderPaused(actions("Resume", "Stop"), "Recording", "00:12"))
        assertTrue(NotificationActionMatcher.isRecorderPaused(actions("Stop"), "Recording paused", "00:12"))
        assertEquals("Save", NotificationActionMatcher.find(actions("Pause", "Save"), IslandControl.RecorderStop)?.title)
    }

    // ---- Ringing alarm ----

    @Test
    fun onlyAFiringAlarmIsRinging() {
        fun ringing(category: String?, fsi: Boolean, labels: List<String>, text: String, clock: Boolean = true) =
            RingingAlarm.isRinging(category, fsi, labels, text, clock)
        assertTrue(ringing("alarm", fsi = true, labels = emptyList(), text = "Alarm 7:00 AM"))
        assertTrue(ringing("alarm", fsi = false, labels = listOf("Snooze", "Dismiss"), text = "Alarm"))
        assertTrue(ringing(null, fsi = false, labels = listOf("Snooze", "Stop"), text = "Wake up"))
        assertFalse(ringing("alarm", fsi = false, labels = listOf("Dismiss"), text = "Upcoming alarm"))
        assertFalse(ringing("alarm", fsi = true, labels = listOf("Dismiss"), text = "Snoozed until 7:10 AM"))
        assertFalse(ringing("alarm", fsi = false, labels = emptyList(), text = "Missed alarm"))
        // Not a clock and no alarm category: never.
        assertFalse(ringing("msg", fsi = true, labels = listOf("Snooze", "Stop"), text = "Wake up", clock = false))
    }

    // ---- Voice recorder ----

    @Test
    fun voiceRecordersAreNotScreenRecorders() {
        assertTrue(OemDeviceRules.isVoiceRecording("com.sec.android.app.voicenote", "Recording 00:12"))
        assertTrue(OemDeviceRules.isVoiceRecording("com.google.android.apps.recorder", "Recording"))
        assertTrue(OemDeviceRules.isVoiceRecording("com.example.notes", "Voice recording in progress"))
        assertFalse(OemDeviceRules.isVoiceRecording("com.sec.android.app.screenrecorder", "Recording screen"))
        assertFalse(OemDeviceRules.isVoiceRecording("com.whatsapp", "Sam"))
        assertTrue(OemDeviceRules.isVoiceRecorderPackage("com.sec.android.app.voicenote"))
        assertFalse(OemDeviceRules.isVoiceRecorderPackage("com.sec.android.app.screenrecorder"))
    }

    @Test
    fun theRecordingClockUsesTheChronometerThenTheShownDuration() {
        val now = 100_000L
        assertEquals(40_000L, VoiceRecordingClock.startMillis(now, chronometerWhenMs = 40_000L, shownElapsedSeconds = 5, previousStartMs = null))
        assertEquals(58_000L, VoiceRecordingClock.startMillis(now, chronometerWhenMs = null, shownElapsedSeconds = 42, previousStartMs = 1L))
        assertEquals(90_000L, VoiceRecordingClock.startMillis(now, chronometerWhenMs = null, shownElapsedSeconds = null, previousStartMs = 90_000L))
        assertEquals(now, VoiceRecordingClock.startMillis(now, chronometerWhenMs = 200_000L, shownElapsedSeconds = null, previousStartMs = null))
    }

    @Test
    fun aPausedRecordingStandsStill() {
        // Running: counts from the start.
        assertEquals(42L, VoiceRecordingClock.displaySeconds(nowMs = 52_000L, startMs = 10_000L, paused = false, shownElapsedSeconds = null, frozenSeconds = null))
        // Paused: the app's shown duration, else what the island showed when it paused.
        assertEquals(30L, VoiceRecordingClock.displaySeconds(nowMs = 99_000L, startMs = 10_000L, paused = true, shownElapsedSeconds = 30L, frozenSeconds = 12L))
        assertEquals(12L, VoiceRecordingClock.displaySeconds(nowMs = 99_000L, startMs = 10_000L, paused = true, shownElapsedSeconds = null, frozenSeconds = 12L))
        assertEquals("00:42", VoiceRecordingClock.format(42))
        assertEquals("1:01:05", VoiceRecordingClock.format(3665))
    }

    // ---- Privacy indicator ----

    private fun label(pkg: String) = when (pkg) {
        "com.instagram.android" -> "Instagram"
        "com.whatsapp" -> "WhatsApp"
        else -> pkg
    }

    @Test
    fun cameraAndMicrophoneAreOneMergedAlert() {
        val own = "com.agupta07505.smartisland"
        assertNull(PrivacyIndicatorAlert.build(PrivacyUsage(), own, ::label, nowMs = 0L))

        val camera = PrivacyIndicatorAlert.build(PrivacyUsage(camera = setOf("com.instagram.android")), own, ::label, nowMs = 5L)!!
        assertEquals(PrivacyIndicatorAlert.KEY, camera.key)
        assertEquals(IslandMode.PrivacyIndicator, camera.mode)
        assertEquals(PrivacyIndicatorAlert.CATEGORY_CAMERA, camera.category)
        assertEquals("Instagram", camera.title)
        // Its own package: opening Instagram must not clear the indicator.
        assertEquals(own, camera.packageName)

        val both = PrivacyIndicatorAlert.build(
            PrivacyUsage(camera = setOf("com.instagram.android"), microphone = setOf("com.whatsapp")),
            own, ::label, nowMs = 9L, startedAtMs = 5L
        )!!
        assertEquals(PrivacyIndicatorAlert.CATEGORY_BOTH, both.category)
        assertTrue(PrivacyIndicatorAlert.sensorsOf(both).both)
        assertEquals("Instagram", both.title)
        assertEquals("WhatsApp", both.text)
        assertEquals(5L, both.timeMillis)
    }

    @Test
    fun theIslandItselfAndUnknownUsersAreHandled() {
        val own = "com.agupta07505.smartisland"
        assertNull(PrivacyIndicatorAlert.build(PrivacyUsage(camera = setOf(own)), own, ::label, nowMs = 0L))
        val unknownMic = PrivacyIndicatorAlert.build(PrivacyUsage(microphone = setOf(PrivacyUsage.UNKNOWN_USER)), own, ::label, nowMs = 0L)!!
        assertEquals(PrivacyIndicatorAlert.CATEGORY_MICROPHONE, unknownMic.category)
        assertEquals("", unknownMic.text)

        val usage = PrivacyUsage().with(PrivacySensor.Camera, "a", true).with(PrivacySensor.Microphone, "b", true)
            .with(PrivacySensor.Camera, "a", false)
        assertEquals(PrivacyUsage(microphone = setOf("b")), usage)
        val none: IslandNotification? = null
        assertFalse(PrivacyIndicatorAlert.sensorsOf(none).camera)
    }

    // ---- Landscape docking ----

    @Test
    fun inLandscapeThePillDocksOnTheLateralHole() {
        val leftHole = LateralAnchor(LateralSide.Left, centerYPx = 540f, box = CutoutBox(0, 500, 80, 580))
        val left = LandscapeAnchoring.windowPosition(
            leftHole, screenWidthPx = 2400, screenHeightPx = 1080, mainWidthPx = 300f, groupWidthPx = 300f,
            mainOffsetInGroupPx = 0f, pillHeightPx = 90f, windowPaddingPx = 40f, marginPx = 15f
        )
        // The group cannot leave the screen: its left edge is clamped to 0, minus the window's padding.
        assertEquals(AnchoredWindowPosition(xPx = -40, yPx = 495), left)

        val rightHole = LateralAnchor(LateralSide.Right, centerYPx = 540f, box = CutoutBox(2320, 500, 2380, 580))
        val right = LandscapeAnchoring.windowPosition(
            rightHole, screenWidthPx = 2400, screenHeightPx = 1080, mainWidthPx = 300f, groupWidthPx = 300f,
            mainOffsetInGroupPx = 0f, pillHeightPx = 90f, windowPaddingPx = 40f, marginPx = 15f
        )
        // Pill right edge = hole right + margin = 2395, so it starts at 2095.
        assertEquals(AnchoredWindowPosition(xPx = 2055, yPx = 495), right)
    }
}
