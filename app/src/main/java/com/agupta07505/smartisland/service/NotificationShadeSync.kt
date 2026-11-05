/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.Context
import android.provider.Settings
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.NotificationFilter
import com.agupta07505.smartisland.util.StaleCardSweep
import com.agupta07505.smartisland.util.quietly
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.toIslandMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** Which active notifications the island picks up from the shade, by mode. */
internal object ShadeSyncRules {
    /** Waits between the passes that catch up on what the first one missed after a connect. */
    val INITIAL_RESYNC_DELAYS_MS = longArrayOf(1_500L, 3_500L)

    /** The first sync and the re-sync bring in persistent live cards and loud notifications; [isSilent] is read for a notification only. */
    fun isPersistentOrLoud(mode: IslandMode, isSilent: () -> Boolean): Boolean =
        NotificationModeRules.isPersistent(mode) || (mode == IslandMode.Notification && !isSilent())

    /** Modes the "import what is in the shade" command brings in. */
    fun isShadeImportMode(mode: IslandMode): Boolean = mode == IslandMode.Notification

    /** Modes the command that brings in 1 app's notifications accepts. */
    fun isSameAppImportMode(mode: IslandMode): Boolean =
        mode == IslandMode.Notification || mode == IslandMode.DownloadUpload
}

/** What the sync needs from the listener service: the shade, the live settings and the three things it does to a card. */
internal interface ShadeSyncHost {
    fun readActive(): Array<StatusBarNotification>?

    /** The settings the listener works with; the sync refreshes them from the repository on a full pass. */
    var settings: SmartIslandSettings

    fun overlayReady(): Boolean
    fun accessibilityEnabled(): Boolean
    fun alreadyShown(sbn: StatusBarNotification): Boolean
    fun hideSystemCopy(key: String)
    fun dropCard(key: String)
    fun post(sbn: StatusBarNotification, settings: SmartIslandSettings)
}

/**
 * Brings the island in line with the notifications in the system shade: the sync after the listener connects or the
 * island turns on, its later passes, the 2 import commands and the sweep of cards whose notification is gone.
 * It reads the shade and decides what to post; posting, hiding the system copy and dropping a card go through the
 * [ShadeSyncHost] the listener service implements, so the service keeps the listener lifecycle and the jobs.
 */
internal class NotificationShadeSync(
    private val context: Context,
    private val repository: SmartIslandSettingsRepository,
    private val notificationRepository: INotificationRepository,
    private val admission: NotificationAdmission,
    private val host: ShadeSyncHost
) {
    private val packageName: String get() = context.packageName

    suspend fun initialSync(trigger: String, onlyMissing: Boolean) {
        val settings = repository.listenerSettings.first()
        host.settings = settings
        if (!settings.enabled) {
            AppLogRecorder.i("ListenerLifecycle", "initial sync ($trigger) skipped: the island is switched off")
            return
        }

        val overlayReady = host.overlayReady()
        AppLogRecorder.i(
            "ListenerLifecycle",
            "initial sync ($trigger): overlayReady=$overlayReady (accessibilityService=${host.accessibilityEnabled()}, canDrawOverlays=${Settings.canDrawOverlays(context)})" +
                " shadeCount=${quietly { host.readActive()?.size }}"
        )
        val active = runCatchingLogged(TAG, "Failed to get active notifications") {
            host.readActive()?.toList()
        }?.filter { it.packageName != packageName }
            ?.filterNot { admission.shouldSuppress(it) }
            ?.filter { sbn ->
                !onlyMissing || notificationRepository.notifications.value.none { it.key == sbn.key }
            }
            ?.sortedBy { it.postTime }
            .orEmpty()

        active.forEach { sbn ->
            val mode = sbn.notification.toIslandMode(
                sbn,
                settings.liveActivitiesEnabled,
                settings.navigationEnabled,
                settings.deviceType
            )
            // On listener connect or rebind
            if (!ShadeSyncRules.isPersistentOrLoud(mode) { admission.isSilent(sbn) }) {
                return@forEach
            }
            if (settings.hideFromNotificationShade &&
                admission.shouldHideFromShade(sbn.notification, mode, sbn.packageName)
            ) {
                host.hideSystemCopy(sbn.key)
            }
            host.post(sbn, settings)
        }
        sweepStaleCards()
        for (waitMs in ShadeSyncRules.INITIAL_RESYNC_DELAYS_MS) {
            delay(waitMs)
            resyncMissingPersistentCards()
        }
    }

    private fun resyncMissingPersistentCards() {
        val settings = host.settings
        if (!settings.enabled) return
        val stored = notificationRepository.notifications.value.mapTo(HashSet()) { it.key }
        val missing = runCatchingLogged(TAG, "Failed to get active notifications for the startup re-sync") {
            host.readActive()?.toList()
        }.orEmpty()
            .filter { it.packageName != packageName && it.key !in stored }
            .filterNot { admission.shouldSuppress(it) }
            .filter { sbn ->
                val mode = sbn.notification.toIslandMode(sbn, settings.liveActivitiesEnabled,
                    settings.navigationEnabled, settings.deviceType)
                // The same rule as the first pass: persistent live cards and loud notifications
                ShadeSyncRules.isPersistentOrLoud(mode) { admission.isSilent(sbn) }
            }
            .sortedBy { it.postTime }
        if (missing.isEmpty()) return
        AppLogRecorder.i(
            "ListenerLifecycle",
            "startup re-sync imported ${missing.size} card(s) the first pass missed: ${missing.map { it.key }}"
        )
        missing.forEach { host.post(it, settings) }
    }

    private fun sweepStaleCards() {
        val activeKeys = runCatchingLogged(TAG, "Stale card sweep could not read the shade") {
            host.readActive()?.mapTo(HashSet()) { it.key }
        } ?: return // the shade could not be read: drop nothing
        val stale = StaleCardSweep.keysToDrop(notificationRepository.notifications.value, activeKeys)
        stale.forEach { key ->
            host.dropCard(key)
            notificationRepository.removeNotification(key)
        }
        if (stale.isNotEmpty()) {
            AppLogRecorder.i("ListenerLifecycle", "stale card sweep dropped ${stale.size}: $stale")
        }
    }

    fun importShadeNotifications() {
        val settings = host.settings
        if (!settings.enabled) return
        val active = runCatchingLogged(TAG, "Failed to get active notifications for shade import") {
            host.readActive()?.toList()
        }.orEmpty()
        val stored = notificationRepository.notifications.value.mapTo(HashSet()) { it.key }
        active
            .filter { it.packageName != packageName && it.key !in stored && !host.alreadyShown(it) }
            .filterNot {
                NotificationModeRules.isGroupSummary(
                    it.notification.flags, NotificationFilter.isCallNotification(it.notification)
                )
            }
            .filterNot { admission.shouldSuppress(it) }
            .filter { sbn ->
                ShadeSyncRules.isShadeImportMode(
                    sbn.notification.toIslandMode(
                        sbn,
                        settings.liveActivitiesEnabled,
                        settings.navigationEnabled,
                        settings.deviceType
                    )
                )
            }
            .sortedBy { it.postTime }
            .forEach { host.post(it, settings) }
    }

    fun importSameAppNotifications(pkg: String) {
        val settings = host.settings
        if (!settings.enabled || pkg == packageName) return
        val active = runCatchingLogged(TAG, "Failed to get active notifications for same-app import") {
            host.readActive()?.toList()
        }.orEmpty()
        val stored = notificationRepository.notifications.value.mapTo(HashSet()) { it.key }
        active
            .filter { it.packageName == pkg && it.key !in stored && !host.alreadyShown(it) }
            .filterNot {
                NotificationModeRules.isGroupSummary(
                    it.notification.flags, NotificationFilter.isCallNotification(it.notification)
                )
            }
            .filterNot { admission.shouldSuppress(it) }
            .filter { sbn ->
                ShadeSyncRules.isSameAppImportMode(
                    sbn.notification.toIslandMode(
                        sbn,
                        settings.liveActivitiesEnabled,
                        settings.navigationEnabled,
                        settings.deviceType
                    )
                )
            }
            .sortedBy { it.postTime }
            .forEach { host.post(it, settings) }
    }

    private companion object {
        const val TAG = "SmartIslandNotificationListener"
    }
}
