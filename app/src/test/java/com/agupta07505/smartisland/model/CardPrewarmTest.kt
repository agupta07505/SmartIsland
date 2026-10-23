/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardPrewarmTest {
    @Test
    fun messageAndCallCardsArePreComposed() {
        assertTrue(CardPrewarm.shouldPrewarm(IslandMode.Notification))
        assertTrue(CardPrewarm.shouldPrewarm(IslandMode.IncomingCall))
        assertTrue(CardPrewarm.shouldPrewarm(IslandMode.DownloadUpload))
    }

    @Test
    fun cardsWithEndlessLoopsAreNot() {
        listOf(
            IslandMode.Music, IslandMode.Battery, IslandMode.Bluetooth
        ).forEach { assertFalse(it.name, CardPrewarm.shouldPrewarm(it)) }
    }

    @Test
    fun liveCardsWithAPulseArePreComposedToo() {
        listOf(
            IslandMode.Timer, IslandMode.Stopwatch, IslandMode.ScreenRecording, IslandMode.AudioRecording,
            IslandMode.Navigation, IslandMode.LiveActivity, IslandMode.Hotspot, IslandMode.Flashlight
        ).forEach { assertTrue(it.name, CardPrewarm.shouldPrewarm(it)) }
    }

    @Test
    fun theEmptyIslandAndNoModeAreNot() {
        assertFalse(CardPrewarm.shouldPrewarm(IslandMode.Empty))
        assertFalse(CardPrewarm.shouldPrewarm(null))
    }

    @Test
    fun mediaCardIsPreComposedOnlyWhileTheScreenIsOn() {
        assertTrue(CardPrewarm.shouldPrewarm(IslandMode.Music, screenOn = true))
        assertFalse(CardPrewarm.shouldPrewarm(IslandMode.Music, screenOn = false))
    }

    @Test
    fun screenStateDoesNotChangeTheOtherCards() {
        assertTrue(CardPrewarm.shouldPrewarm(IslandMode.Notification, screenOn = false))
        assertTrue(CardPrewarm.shouldPrewarm(IslandMode.Timer, screenOn = false))
        assertFalse(CardPrewarm.shouldPrewarm(IslandMode.Battery, screenOn = true))
        assertFalse(CardPrewarm.shouldPrewarm(null, screenOn = true))
    }
}

