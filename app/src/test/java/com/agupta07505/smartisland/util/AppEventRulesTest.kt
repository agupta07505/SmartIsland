/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppEventRulesTest {
    private val pkg = "com.example.chat"
    private val none = emptySet<String>()

    private fun autoExpand(
        global: Boolean = true,
        never: Set<String> = none,
        always: Set<String> = none,
        noExpand: Set<String> = none,
        mode: IslandMode = IslandMode.Notification,
        loud: Boolean
    ) = AppEventRules.mayAutoExpand(global, never, always, noExpand, pkg, mode, loud)

    @Test
    fun expandedCardSoundAndRulesStayDenySets() {
        assertTrue(AppEventRules.mayExpandCard(none, pkg, IslandMode.Notification))
        assertTrue(AppEventRules.maySound(none, pkg))
    }

    @Test
    fun noExpandBlocksNotificationsAndTransfersOnly() {
        val deny = setOf(pkg)
        assertFalse(AppEventRules.mayExpandCard(deny, pkg, IslandMode.Notification))
        assertFalse(AppEventRules.mayExpandCard(deny, pkg, IslandMode.DownloadUpload))
        assertTrue(AppEventRules.mayExpandCard(deny, pkg, IslandMode.IncomingCall))
        assertTrue(AppEventRules.mayExpandCard(deny, pkg, IslandMode.Timer))
        assertTrue(AppEventRules.mayExpandCard(deny, "other.app", IslandMode.Notification))
    }

    @Test
    fun soundUsesTheMutedSet() {
        assertFalse(AppEventRules.maySound(setOf(pkg), pkg))
        assertTrue(AppEventRules.maySound(setOf(pkg), "other.app"))
    }

    @Test
    fun anAppNobodyTouchedAutoExpandsLoudNotificationsOnly() {
        assertTrue(autoExpand(loud = true))
        assertFalse(autoExpand(loud = false))
    }

    @Test
    fun anAppSwitchedOnAutoExpandsEverything() {
        assertTrue(autoExpand(always = setOf(pkg), loud = false))
        assertTrue(autoExpand(always = setOf(pkg), loud = true))
        // Another app is still at its default.
        assertFalse(AppEventRules.mayAutoExpand(true, none, setOf("other.app"), none, pkg, IslandMode.Notification, false))
    }

    @Test
    fun anAppSwitchedOffNeverAutoExpandsNotEvenLoud() {
        assertFalse(autoExpand(never = setOf(pkg), loud = true))
        assertFalse(autoExpand(never = setOf(pkg), loud = false))
    }

    @Test
    fun anAppInBothSetsCountsAsOff() {
        assertEquals(AppAutoExpand.NEVER, AppEventRules.autoExpandChoice(setOf(pkg), setOf(pkg), pkg))
        assertFalse(autoExpand(never = setOf(pkg), always = setOf(pkg), loud = true))
    }

    @Test
    fun theGlobalSwitchBeatsEverything() {
        assertFalse(autoExpand(global = false, always = setOf(pkg), loud = true))
    }

    @Test
    fun anAppWhoseCardNeverOpensNeverAutoExpands() {
        assertFalse(autoExpand(noExpand = setOf(pkg), always = setOf(pkg), loud = true))
    }

    @Test
    fun callsAndLiveCardsAreLoudSoTheyStillOpenForADeniedExpandApp() {
        assertTrue(autoExpand(noExpand = setOf(pkg), mode = IslandMode.IncomingCall, loud = true))
    }

    @Test
    fun choiceReadsTheTwoSets() {
        assertEquals(AppAutoExpand.DEFAULT, AppEventRules.autoExpandChoice(none, none, pkg))
        assertEquals(AppAutoExpand.ALWAYS, AppEventRules.autoExpandChoice(none, setOf(pkg), pkg))
        assertEquals(AppAutoExpand.NEVER, AppEventRules.autoExpandChoice(setOf(pkg), none, pkg))
    }
}
