/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertTrue(VoipCallDetector.isVoipAudioMode(android.media.AudioManager.MODE_IN_COMMUNICATION))
        assertFalse(VoipCallDetector.isVoipAudioMode(android.media.AudioManager.MODE_NORMAL))
        assertFalse(VoipCallDetector.isVoipAudioMode(android.media.AudioManager.MODE_IN_CALL))
    }

    @Test
    fun classifiesButtonLabels() {
        assertEquals(CallActionRole.Mute, VoipCallDetector.classifyCallAction("Mute"))
        assertEquals(CallActionRole.Unmute, VoipCallDetector.classifyCallAction("Unmute"))
        assertEquals(CallActionRole.Speaker, VoipCallDetector.classifyCallAction("Speaker"))
        assertEquals(CallActionRole.HangUp, VoipCallDetector.classifyCallAction("Hang up"))
        assertEquals(CallActionRole.HangUp, VoipCallDetector.classifyCallAction("End call"))
        assertEquals(CallActionRole.HangUp, VoipCallDetector.classifyCallAction("Leave"))
        assertEquals(CallActionRole.HangUp, VoipCallDetector.classifyCallAction("Decline"))
        assertEquals(CallActionRole.Answer, VoipCallDetector.classifyCallAction("Answer"))
        assertEquals(CallActionRole.Mute, VoipCallDetector.classifyCallAction("Silenciar"))
        assertEquals(CallActionRole.HangUp, VoipCallDetector.classifyCallAction("挂断"))
    }

    @Test
    fun endMustBeAWholeWord() {
        assertEquals(CallActionRole.Other, VoipCallDetector.classifyCallAction("Send"))
        assertEquals(CallActionRole.Other, VoipCallDetector.classifyCallAction("Reply"))
    }

    @Test
    fun speakerLabelThatOffersToSwitchBackMeansSpeakerIsOn() {
        assertTrue(VoipCallDetector.isSpeakerOnLabel("Earpiece"))
        assertTrue(VoipCallDetector.isSpeakerOnLabel("Speaker off"))
        assertFalse(VoipCallDetector.isSpeakerOnLabel("Speaker"))
    }

    @Test
    fun callNotificationWithMuteAndHangUpQualifies() {
        assertTrue(VoipCallDetector.isVoipCall(facts(actions = listOf("Mute", "Hang up"))))
    }

    @Test
    fun callCategoryOrCallStyleAloneQualifies() {
        assertTrue(VoipCallDetector.isVoipCall(facts(category = Notification.CATEGORY_CALL)))
        assertTrue(VoipCallDetector.isVoipCall(facts(callStyle = true)))
    }

    @Test
    fun plainOngoingNotificationDoesNotQualify() {
        assertFalse(VoipCallDetector.isVoipCall(facts()))
        assertFalse(VoipCallDetector.isVoipCall(facts(actions = listOf("Open"))))
    }

    @Test
    fun chronometerWithCallWordingQualifies() {
        assertTrue(VoipCallDetector.isVoipCall(facts(chronometer = true, callText = true)))
        assertFalse(VoipCallDetector.isVoipCall(facts(chronometer = true)))
    }

    @Test
    fun mediaNotificationNeverQualifies() {
        assertFalse(VoipCallDetector.isVoipCall(facts(actions = listOf("Mute", "End"), media = true)))
    }

    @Test
    fun pickBestChoosesTheHighestScore() {
        val best = VoipCallDetector.pickBest(
            listOf(
                "chat" to facts(actions = listOf("Reply")),
                "weak" to facts(actions = listOf("Hang up")),
                "call" to facts(actions = listOf("Mute", "Speaker", "Hang up"), chronometer = true)
            )
        )
        assertEquals("call", best)
    }

    @Test
    fun pickBestReturnsNullWhenNothingQualifies() {
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
