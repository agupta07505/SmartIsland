/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 * Modified for Static Boat (2026)
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StackRulesTest {

    private fun card(key: String, mode: IslandMode, category: String? = null) = IslandNotification(
        key = key, packageName = "p", appName = "a", title = "t", text = "x", timeMillis = 0L,
        mode = mode, category = category
    )

    @Test
    fun musicAndStopwatchMakeAStack() {
        val visible = listOf(card("m", IslandMode.Music), card("s", IslandMode.Stopwatch))
        assertTrue(StackRules.useStack("Stack", visible))
    }

    @Test
    fun oneLiveCardIsNotAStack() {
        assertFalse(StackRules.useStack("Stack", listOf(card("m", IslandMode.Music))))
    }

    @Test
    fun aRingingCallDisablesTheStack() {
        val visible = listOf(card("c", IslandMode.IncomingCall), card("m", IslandMode.Music), card("s", IslandMode.Stopwatch))
        assertFalse(StackRules.useStack("Stack", visible))
    }

    @Test
    fun singleStyleNeverStacks() {
        assertFalse(StackRules.useStack("Single", listOf(card("m", IslandMode.Music), card("s", IslandMode.Stopwatch))))
    }

    @Test
    fun newestMessageGoesFirstAboveLiveCards() {
        val visible = listOf(card("m", IslandMode.Music), card("n", IslandMode.Notification))
        val stack = StackRules.build(visible)
        assertEquals(listOf("n", "m"), stack.cards.map { it.key })
    }

    @Test
    fun capsAtFourAndCountsTheRest() {
        val live = listOf(
            IslandMode.Music, IslandMode.Timer, IslandMode.Stopwatch,
            IslandMode.Flashlight, IslandMode.Hotspot, IslandMode.AudioRecording
        ).mapIndexed { i, m -> card("k", m) }
        val stack = StackRules.build(live)
        assertEquals(StackRules.MAX_CARDS, stack.cards.size)
        assertEquals(2, stack.hiddenCount)
        assertEquals(6, StackRules.build(live, showAll = true).cards.size)
    }

    @Test
    fun lowBatteryIsNotAStackCardButPluggedInIs() {
        assertFalse(StackRules.isStackCard(card("b", IslandMode.Battery, "battery_low")))
        assertTrue(StackRules.isStackCard(card("b", IslandMode.Battery, "battery_plugged_in")))
    }

    @Test
    fun hitIndexFindsTheCardUnderTheFingerAndNullInGaps() {
        val bounds = listOf(0f to 100f, 110f to 200f)
        assertEquals(1, StackRules.hitIndex(150f, bounds))
        assertNull(StackRules.hitIndex(105f, bounds))
    }
}
