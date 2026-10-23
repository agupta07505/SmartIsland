/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import org.junit.Assert.assertEquals
import org.junit.Test

class CallCardRulesTest {

    private fun card(key: String, pkg: String, mode: IslandMode = IslandMode.IncomingCall) = IslandNotification(
        key = key, packageName = pkg, appName = pkg, title = key, text = "", timeMillis = 0L, mode = mode
    )

    @Test
    fun ongoingCallReplacesTheRingingCardOfTheSameApp() {
        val cards = listOf(card("ringing", "wa"), card("msg", "wa", IslandMode.Notification))
        assertEquals(listOf("ringing"), CallCardRules.supersededKeys(cards, "ongoing", "wa", newIsRinging = false))
    }

    @Test
    fun aRingingCallReplacesNothing() {
        val cards = listOf(card("ongoing", "wa"))
        assertEquals(emptyList<String>(), CallCardRules.supersededKeys(cards, "second", "wa", newIsRinging = true))
    }

    @Test
    fun otherAppsAndOtherCardTypesStay() {
        val cards = listOf(card("tg", "telegram"), card("music", "wa", IslandMode.Music), card("ringing", "wa"))
        assertEquals(listOf("ringing"), CallCardRules.supersededKeys(cards, "ongoing", "wa", newIsRinging = false))
    }

    @Test
    fun theSameKeyIsAnUpdateNotAReplacement() {
        val cards = listOf(card("call", "wa"))
        assertEquals(emptyList<String>(), CallCardRules.supersededKeys(cards, "call", "wa", newIsRinging = false))
    }
}
