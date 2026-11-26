/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import com.agupta07505.smartisland.MainActivity
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.receiver.StopwatchActionReceiver
import com.agupta07505.smartisland.receiver.TimerActionReceiver
import com.agupta07505.smartisland.service.SmartIslandNotificationListenerService
import com.agupta07505.smartisland.service.SmartIslandOverlayService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The util layer addresses these components by name, so each name must stay the real class name */
class AppComponentsTest {
    @Test
    fun namesMatchTheClasses() {
        assertEquals(MainActivity::class.java.name, AppComponents.MAIN_ACTIVITY)
        assertEquals(SmartIslandOverlayService::class.java.name, AppComponents.OVERLAY_SERVICE)
        assertEquals(SmartIslandNotificationListenerService::class.java.name, AppComponents.LISTENER_SERVICE)
        assertEquals(TimerActionReceiver::class.java.name, AppComponents.TIMER_RECEIVER)
        assertEquals(StopwatchActionReceiver::class.java.name, AppComponents.STOPWATCH_RECEIVER)
    }
}

class AppsRulesTest {
    private val settings = SmartIslandSettings.Default.copy(
        disabledNotificationPackages = setOf("excluded"),
        disabledSoundPackages = setOf("muted", "excluded"),
        noExpandPackages = setOf("nocard"),
        noAutoExpandPackages = setOf("noauto"),
        autoExpandPackages = setOf("always"),
        hideOnLockScreenPackages = setOf("lock"),
        notificationCooldownExcludedPackages = setOf("cool")
    )

    private fun flags(pkg: String) = AppsRules.flagsFor(settings, pkg)

    @Test
    fun appOptionTags() {
        val defaults = flags("plain")
        assertTrue(defaults.isDefault)
        assertFalse(AppsRules.isCustomized(defaults))
        assertTrue(AppsRules.tagsFor(defaults).isEmpty())

        assertEquals(listOf(AppTag.Excluded, AppTag.Muted), AppsRules.tagsFor(flags("excluded")))
        assertEquals(listOf(AppTag.Muted), AppsRules.tagsFor(flags("muted")))
        assertEquals(listOf(AppTag.NoCard), AppsRules.tagsFor(flags("nocard")))
        assertEquals(listOf(AppTag.NoAutoExpand), AppsRules.tagsFor(flags("noauto")))
        assertEquals(listOf(AppTag.AlwaysExpands), AppsRules.tagsFor(flags("always")))
        assertEquals(listOf(AppTag.HiddenOnLock), AppsRules.tagsFor(flags("lock")))
        assertEquals(listOf(AppTag.NoCooldown), AppsRules.tagsFor(flags("cool")))

        val inactive = AppFlags(excluded = true, noCard = true, autoExpand = AppAutoExpand.ALWAYS)
        assertEquals(listOf(AppTag.Excluded), AppsRules.tagsFor(inactive))
        val g = AppFlags(noCard = true, autoExpand = AppAutoExpand.NEVER)
        assertEquals(listOf(AppTag.NoCard), AppsRules.tagsFor(g))

        val all = AppFlags(
            excluded = true, muted = true, noCard = true, hiddenOnLock = true, noCooldown = true
        )
        val tags = AppsRules.tagsFor(all)
        assertEquals(listOf(AppTag.Excluded, AppTag.Muted, AppTag.HiddenOnLock, AppTag.NoCooldown), tags)
        val visible = AppsRules.visibleTags(tags)
        assertEquals(listOf(AppTag.Excluded, AppTag.Muted), visible.shown)
        assertEquals(2, visible.overflow)
        assertEquals(0, AppsRules.visibleTags(listOf(AppTag.Muted)).overflow)
    }

    @Test
    fun appFilterFlags() {
        assertTrue(AppsRules.matches(AppsFilter.Muted, flags("muted")))
        assertFalse(AppsRules.matches(AppsFilter.Muted, flags("plain")))
        assertTrue(AppsRules.matches(AppsFilter.Excluded, flags("excluded")))
        assertTrue(AppsRules.matches(AppsFilter.NeverExpands, flags("nocard")))
        assertTrue(AppsRules.matches(AppsFilter.NeverExpands, flags("noauto")))
        assertFalse(AppsRules.matches(AppsFilter.NeverExpands, flags("always")))
        assertTrue(AppsRules.matches(AppsFilter.Customized, flags("lock")))
        assertTrue(AppsRules.matches(AppsFilter.All, flags("plain")))

        assertTrue(AppsRules.soundOn(flags("plain")))
        assertFalse(AppsRules.soundOn(flags("muted")))
        assertFalse(AppsRules.soundOn(AppFlags(excluded = true)))
    }

    @Test
    fun countsCoverTheWholeList() {
        val list = listOf("plain", "excluded", "muted", "nocard", "noauto", "always").map(::flags)
        val counts = AppsRules.counts(list)
        assertEquals(6, counts[AppsFilter.All])
        assertEquals(5, counts[AppsFilter.Customized])
        assertEquals(2, counts[AppsFilter.Muted])
        assertEquals(1, counts[AppsFilter.Excluded])
        assertEquals(2, counts[AppsFilter.NeverExpands])
    }

    @Test
    fun searchMatchesNameOrPackageIgnoringCase() {
        assertTrue(AppsRules.matchesQuery("WhatsApp", "com.whatsapp", ""))
        assertTrue(AppsRules.matchesQuery("WhatsApp", "com.whatsapp", " whats "))
        assertTrue(AppsRules.matchesQuery("Chat", "com.WhatsApp", "whatsapp"))
        assertFalse(AppsRules.matchesQuery("Chat", "com.example", "mail"))
    }
}

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
    fun soundPolicy() {
        assertTrue(AppEventRules.mayExpandCard(none, pkg, IslandMode.Notification))
        assertTrue(AppEventRules.maySound(none, pkg))

        assertFalse(AppEventRules.maySound(setOf(pkg), pkg))
        assertTrue(AppEventRules.maySound(setOf(pkg), "other.app"))
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
    fun autoExpandOverrides() {
        assertTrue(autoExpand(loud = true))
        assertFalse(autoExpand(loud = false))

        assertFalse(autoExpand(never = setOf(pkg), loud = true))
        assertFalse(autoExpand(never = setOf(pkg), loud = false))

        assertEquals(AppAutoExpand.NEVER, AppEventRules.autoExpandChoice(setOf(pkg), setOf(pkg), pkg))
        assertFalse(autoExpand(never = setOf(pkg), always = setOf(pkg), loud = true))

        assertFalse(autoExpand(noExpand = setOf(pkg), always = setOf(pkg), loud = true))

        assertTrue(autoExpand(noExpand = setOf(pkg), mode = IslandMode.IncomingCall, loud = true))
    }

    @Test
    fun autoExpandPrecedence() {
        assertTrue(autoExpand(always = setOf(pkg), loud = false))
        assertTrue(autoExpand(always = setOf(pkg), loud = true))
        // another app is still at its default
        assertFalse(AppEventRules.mayAutoExpand(true, none, setOf("other.app"), none, pkg, IslandMode.Notification, false))

        assertFalse(autoExpand(global = false, always = setOf(pkg), loud = true))
    }

    @Test
    fun choiceReadsTheTwoSets() {
        assertEquals(AppAutoExpand.DEFAULT, AppEventRules.autoExpandChoice(none, none, pkg))
        assertEquals(AppAutoExpand.ALWAYS, AppEventRules.autoExpandChoice(none, setOf(pkg), pkg))
        assertEquals(AppAutoExpand.NEVER, AppEventRules.autoExpandChoice(setOf(pkg), none, pkg))
    }
}
