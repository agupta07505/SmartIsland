/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.app.NotificationManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.util.NotificationFilter
import com.agupta07505.smartisland.util.toIslandMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutoExpandRulesTest {
    private var now = 10_000L
    private fun slot() = AutoExpandSlot({ now }, 1_500L, 250L)

    @Test
    fun firstClaimSucceeds() {
        assertTrue(slot().claim("a"))
    }

    @Test
    fun sameCardInDebounceRefused() {
        val s = slot()
        assertTrue(s.claim("a"))
        now += 1_499L
        assertFalse(s.claim("a"))
        now += 1L
        assertTrue(s.claim("a"))
    }

    @Test
    fun sameInstantCardWaitsThenOpens() {
        val s = slot()
        assertTrue(s.claim("a"))
        now += 249L
        assertFalse(s.claim("b"))
        now += 1L
        assertTrue(s.claim("b"))
    }

    @Test
    fun aRefusedClaimDoesNotUseUpTheSlot() {
        val s = slot()
        assertTrue(s.claim("a"))
        now += 100L
        assertFalse(s.claim("b"))
        now += 150L
        assertTrue(s.claim("b"))
    }

    @Test
    fun resetFreesTheSlot() {
        val s = slot()
        assertTrue(s.claim("a"))
        s.reset()
        assertTrue(s.claim("a"))
    }

    private fun decide(
        miui: Boolean = false,
        initial: Boolean = false,
        mode: IslandMode = IslandMode.Notification,
        repost: Boolean = false,
        timerFinished: Boolean = false,
        islandOnly: Boolean = true,
        autoExpand: Boolean = true,
        may: () -> Boolean = { true },
        claim: () -> Boolean = { true }
    ) = AutoExpandDecision.shouldAutoExpand(
        miui, initial, mode, repost, timerFinished, islandOnly, autoExpand, may, claim
    )

    @Test
    fun aNewCardOpensTheIsland() {
        assertTrue(decide())
    }

    @Test
    fun anImportDoesNotOpenIt() {
        assertFalse(decide(initial = true))
        assertFalse(decide(miui = true, initial = true, mode = IslandMode.Timer))
    }

    @Test
    fun repostOpensOnlyFinishedTimer() {
        assertFalse(decide(repost = true))
        assertTrue(decide(repost = true, timerFinished = true))
    }

    @Test
    fun shadeCardDoesNotOpen() {
        assertFalse(decide(islandOnly = false))
        assertFalse(decide(miui = true, islandOnly = false))
    }

    @Test
    fun blockedAppNoOpenNoSlot() {
        var claims = 0
        assertFalse(decide(may = { false }, claim = { claims++; true }))
        assertEquals(0, claims)
    }

    @Test
    fun aRefusedSlotDoesNotOpenIt() {
        assertFalse(decide(claim = { false }))
    }

    @Test
    fun appRuleSkippedOnEarlierFail() {
        var reads = 0
        decide(initial = true, may = { reads++; true })
        decide(islandOnly = false, may = { reads++; true })
        decide(repost = true, may = { reads++; true })
        assertEquals(0, reads)
    }

    @Test
    fun miuiClocksOpenWithoutSlot() {
        var claims = 0
        assertTrue(decide(miui = true, mode = IslandMode.Timer, claim = { claims++; false }))
        assertTrue(decide(miui = true, mode = IslandMode.Stopwatch, claim = { claims++; false }))
        assertEquals(0, claims)
    }

    @Test
    fun miuiOthersFollowGlobalAndSlot() {
        assertTrue(decide(miui = true))
        assertFalse(decide(miui = true, autoExpand = false))
        assertFalse(decide(miui = true, claim = { false }))
    }

    @Test
    fun miuiIgnoresTheAppRule() {
        assertTrue(decide(miui = true, may = { false }))
    }
}

class NotificationAdmissionTest {
    private val service = mockk<NotificationListenerService>()
    private val pm = mockk<PackageManager>()
    private var settings = SmartIslandSettings.Default.copy(showDuringDnd = true)
    private val admission = NotificationAdmission(service) { settings }

    init {
        every { service.packageManager } returns pm
    }

    private fun sbnFrom(packageName: String, category: String): StatusBarNotification {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        notification.category = category
        every { sbn.notification } returns notification
        every { sbn.packageName } returns packageName
        return sbn
    }

    @Test
    fun systemUiSuppression() {
        val sbn = sbnFrom("com.android.systemui", Notification.CATEGORY_MESSAGE)
        assertEquals("system-level-package-list", admission.suppressionReason(sbn))
        assertTrue(admission.shouldSuppress(sbn))
    }

    @Test
    fun appDisabledReadsLiveSettings() {
        val sbn = sbnFrom("com.example.chat", Notification.CATEGORY_MESSAGE)
        settings = settings.copy(disabledNotificationPackages = setOf("com.example.chat"))
        assertEquals("app-disabled-in-settings", admission.suppressionReason(sbn))
    }

    @Test
    fun musicNeverIslandOnly() {
        val notification = mockk<Notification>()
        assertFalse(admission.shouldBeIslandOnly(notification, IslandMode.Music))
        assertFalse(admission.shouldHideFromShade(notification, IslandMode.Music, "com.spotify.music"))
    }
}

class NotificationModeRulesTest {
    @Test
    fun islandOnlyModes() {
        listOf(
            IslandMode.Music, IslandMode.Navigation, IslandMode.Timer, IslandMode.Stopwatch,
            IslandMode.ScreenRecording, IslandMode.AudioRecording
        ).forEach { assertFalse("$it", NotificationModeRules.isIslandOnly(it) { true }) }

        assertTrue(NotificationModeRules.isIslandOnly(IslandMode.Notification) { false })
        assertTrue(NotificationModeRules.isIslandOnly(IslandMode.DownloadUpload) { false })
        assertTrue(NotificationModeRules.isIslandOnly(IslandMode.LiveActivity) { false })

        assertTrue(NotificationModeRules.isIslandOnly(IslandMode.IncomingCall) { true })
        assertFalse(NotificationModeRules.isIslandOnly(IslandMode.IncomingCall) { false })

        var reads = 0
        NotificationModeRules.isIslandOnly(IslandMode.Notification) { reads++; true }
        NotificationModeRules.isIslandOnly(IslandMode.Music) { reads++; true }
        assertEquals(0, reads)
        NotificationModeRules.isIslandOnly(IslandMode.IncomingCall) { reads++; true }
        assertEquals(1, reads)
    }

    @Test
    fun persistentModes() {
        listOf(
            IslandMode.Music, IslandMode.IncomingCall, IslandMode.Timer, IslandMode.Stopwatch,
            IslandMode.Navigation, IslandMode.LiveActivity, IslandMode.DownloadUpload, IslandMode.Hotspot,
            IslandMode.ScreenRecording, IslandMode.AudioRecording
        ).forEach { assertTrue("$it", NotificationModeRules.isPersistent(it)) }
        assertFalse(NotificationModeRules.isPersistent(IslandMode.Notification))
    }

    @Test
    fun recordingLeavesAtOnce() {
        assertEquals(0L, NotificationModeRules.removalDelayMs(IslandMode.ScreenRecording))
        assertEquals(0L, NotificationModeRules.removalDelayMs(IslandMode.AudioRecording))
        assertEquals(350L, NotificationModeRules.removalDelayMs(IslandMode.Notification))
        assertEquals(350L, NotificationModeRules.removalDelayMs(null))
    }

    @Test
    fun removalReasonNames() {
        assertEquals("click", NotificationModeRules.removalReasonName(NotificationListenerService.REASON_CLICK))
        assertEquals("cancel", NotificationModeRules.removalReasonName(NotificationListenerService.REASON_CANCEL))
        assertEquals("cancel-all", NotificationModeRules.removalReasonName(NotificationListenerService.REASON_CANCEL_ALL))
        assertEquals("listener-cancel", NotificationModeRules.removalReasonName(NotificationListenerService.REASON_LISTENER_CANCEL))
        assertEquals("app-cancel", NotificationModeRules.removalReasonName(NotificationListenerService.REASON_APP_CANCEL))
        assertEquals("timeout", NotificationModeRules.removalReasonName(NotificationListenerService.REASON_TIMEOUT))
        assertEquals("other", NotificationModeRules.removalReasonName(-12345))
    }

    @Test
    fun silentBelowDefaultImportance() {
        assertTrue(NotificationModeRules.isSilentImportance(NotificationManager.IMPORTANCE_LOW))
        assertTrue(NotificationModeRules.isSilentImportance(NotificationManager.IMPORTANCE_MIN))
        assertFalse(NotificationModeRules.isSilentImportance(NotificationManager.IMPORTANCE_UNSPECIFIED))
        assertFalse(NotificationModeRules.isSilentImportance(NotificationManager.IMPORTANCE_DEFAULT))
        assertFalse(NotificationModeRules.isSilentImportance(NotificationManager.IMPORTANCE_HIGH))
    }

    @Test
    fun aGroupSummaryIsOneUnlessItIsACall() {
        assertTrue(NotificationModeRules.isGroupSummary(Notification.FLAG_GROUP_SUMMARY, isCall = false))
        assertTrue(NotificationModeRules.isGroupSummary(Notification.FLAG_GROUP_SUMMARY or Notification.FLAG_ONGOING_EVENT, isCall = false))
        assertFalse(NotificationModeRules.isGroupSummary(Notification.FLAG_GROUP_SUMMARY, isCall = true))
        assertFalse(NotificationModeRules.isGroupSummary(0, isCall = false))
        assertFalse(NotificationModeRules.isGroupSummary(Notification.FLAG_ONGOING_EVENT, isCall = false))
    }
}

class NotificationPriorityTest {
    private fun chatExtras(): Bundle {
        val extras = mockk<Bundle>()
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Title"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Text"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        return extras
    }


    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun ignoreSystemCategory() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val pm = mockk<PackageManager>()
        
        notification.category = Notification.CATEGORY_SYSTEM
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.example.app"

        assertTrue(NotificationFilter.shouldSuppressFromIsland(sbn, pm))
    }

    @Test
    fun ignoreSystemPackage() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val pm = mockk<PackageManager>()
        
        notification.category = Notification.CATEGORY_MESSAGE
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.android.systemui"

        assertTrue(NotificationFilter.shouldSuppressFromIsland(sbn, pm))
    }

    @Test
    fun ignoreSystemFlagPackage() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val pm = mockk<PackageManager>()
        val appInfo = mockk<ApplicationInfo>()
        
        notification.category = Notification.CATEGORY_MESSAGE
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.android.customsettings"
        
        appInfo.flags = ApplicationInfo.FLAG_SYSTEM
        every { pm.getApplicationInfo("com.android.customsettings", 0) } returns appInfo

        assertTrue(NotificationFilter.shouldSuppressFromIsland(sbn, pm))
    }

    @Test
    fun nonSystemIsNotIgnored() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val pm = mockk<PackageManager>()
        val appInfo = mockk<ApplicationInfo>()
        
        notification.category = Notification.CATEGORY_MESSAGE
        notification.flags = 0
        notification.extras = chatExtras()
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.example.chat"
        
        appInfo.flags = 0
        every { pm.getApplicationInfo("com.example.chat", 0) } returns appInfo

        assertFalse(NotificationFilter.shouldSuppressFromIsland(sbn, pm))
    }

    @Test
    fun pkgInfoFailureNotIgnored() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val pm = mockk<PackageManager>()
        
        notification.category = Notification.CATEGORY_MESSAGE
        notification.flags = 0
        notification.extras = chatExtras()
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.example.chat"
        
        every { pm.getApplicationInfo("com.example.chat", 0) } throws PackageManager.NameNotFoundException()

        assertFalse(NotificationFilter.shouldSuppressFromIsland(sbn, pm))
    }

    @Test
    fun ongoingProgressNotIgnored() {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val pm = mockk<PackageManager>()
        val appInfo = mockk<ApplicationInfo>()
        val extras = mockk<Bundle>()

        notification.category = Notification.CATEGORY_PROGRESS
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.extras = extras
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Downloading"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "42%"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 100
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 42
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        notification.actions = null
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.example.downloader"
        appInfo.flags = 0
        every { pm.getApplicationInfo("com.example.downloader", 0) } returns appInfo

        assertFalse(NotificationFilter.shouldSuppressFromIsland(sbn, pm))
    }

    @Test
    fun mediaLabelsWithoutSession() {
        val testLabels = listOf("PAUSE", "Next Track", "PLAY", "previous song")
        for (label in testLabels) {
            val notification = mockk<Notification>()
            notification.category = Notification.CATEGORY_MESSAGE
            notification.flags = 0
            val action = mockk<Notification.Action>()
            action.title = label
            notification.actions = arrayOf(action)

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
            notification.extras = extras

            val mode = notification.toIslandMode()
            assertEquals(IslandMode.Notification, mode)
        }
    }

    @Test
    fun musicNotIslandOnlyNoPause() {
        val service = SmartIslandNotificationListenerService()
        val notification = mockk<Notification>()
        assertFalse(service.shouldBeIslandOnly(notification, IslandMode.Music))
    }

    @Test
    fun dndOffAllowsNotifications() {
        val service = SmartIslandNotificationListenerService()
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.whatsapp"
        every { sbn.key } returns "0|com.whatsapp|1|null|1000"
        val extras = mockk<Bundle>()
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Alice"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Hey there"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_INFO_TEXT) } returns null
        notification.extras = extras
        notification.category = Notification.CATEGORY_MESSAGE
        notification.flags = 0
        notification.actions = emptyArray()
        notification.tickerText = null

        assertFalse(service.isBlockedByDoNotDisturb(sbn, filterOverride = NotificationListenerService.INTERRUPTION_FILTER_ALL))
    }

    @Test
    fun dndTotalSilenceBlocksNotifications() {
        val service = SmartIslandNotificationListenerService()
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.whatsapp"
        every { sbn.key } returns "0|com.whatsapp|1|null|1000"
        val extras = mockk<Bundle>()
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns false
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Alice"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Hey there"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_INFO_TEXT) } returns null
        notification.extras = extras
        notification.category = Notification.CATEGORY_MESSAGE
        notification.flags = 0
        notification.actions = emptyArray()
        notification.tickerText = null

        assertTrue(service.isBlockedByDoNotDisturb(sbn, filterOverride = NotificationListenerService.INTERRUPTION_FILTER_NONE))
    }

    @Test
    fun dndMusicPlaybackAlwaysAllowed() {
        val service = SmartIslandNotificationListenerService()
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.spotify.music"
        every { sbn.key } returns "0|com.spotify.music|1|null|1000"
        val extras = mockk<Bundle>()
        every { extras.getString(Notification.EXTRA_TEMPLATE) } returns null
        every { extras.containsKey(Notification.EXTRA_MEDIA_SESSION) } returns true
        every { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) } returns 0
        every { extras.getInt(Notification.EXTRA_PROGRESS, 0) } returns 0
        every { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) } returns false
        every { extras.getCharSequence(Notification.EXTRA_TITLE) } returns "Song Title"
        every { extras.getCharSequence(Notification.EXTRA_TEXT) } returns "Artist"
        every { extras.getCharSequence(Notification.EXTRA_BIG_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_SUB_TEXT) } returns null
        every { extras.getCharSequence(Notification.EXTRA_INFO_TEXT) } returns null
        notification.extras = extras
        notification.category = Notification.CATEGORY_TRANSPORT
        notification.flags = Notification.FLAG_ONGOING_EVENT
        notification.actions = emptyArray()
        notification.tickerText = null

        assertFalse(service.isBlockedByDoNotDisturb(sbn, filterOverride = NotificationListenerService.INTERRUPTION_FILTER_PRIORITY))
        assertFalse(service.isBlockedByDoNotDisturb(sbn, filterOverride = NotificationListenerService.INTERRUPTION_FILTER_NONE))
    }
}
