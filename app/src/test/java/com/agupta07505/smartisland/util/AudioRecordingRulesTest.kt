/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRecordingRulesTest {
    @Test
    fun voiceRecorderAppsAreAudioRecording() {
        assertTrue(OemDeviceRules.isAudioRecording("com.google.android.apps.recorder", "Recorder"))
        assertTrue(OemDeviceRules.isAudioRecording("com.sec.android.app.voicenote", "Voice Recorder"))
        assertTrue(OemDeviceRules.isAudioRecording("com.miui.soundrecorder", "Recording"))
        assertTrue(OemDeviceRules.isAudioRecording("com.some.app", "Voice recording in progress"))
    }

    @Test
    fun screenRecordersAreNeverAudioRecording() {
        assertFalse(OemDeviceRules.isAudioRecording("com.sec.android.app.screenrecorder", "Recording screen"))
        assertFalse(OemDeviceRules.isAudioRecording("com.miui.screenrecorder", "Screen Recorder"))
        assertFalse(OemDeviceRules.isAudioRecording("com.vivo.videocapture", "Video capture"))
        assertFalse(OemDeviceRules.isAudioRecording("com.some.app", "Screen recording, voice recorder on"))
    }

    @Test
    fun unrelatedAppsAreNotAudioRecording() {
        assertFalse(OemDeviceRules.isAudioRecording("com.whatsapp", "New message"))
        assertFalse(OemDeviceRules.isAudioRecording("com.google.android.deskclock", "Timer"))
    }
}
