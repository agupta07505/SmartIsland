/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.toIslandMode

internal object NotificationDndPolicy {
    private const val TAG = "SmartIslandNotificationListener"

    /** Do Not Disturb / Bedtime mode keeps [sbn] off the island. */
    fun isHidden(
        service: NotificationListenerService,
        settings: SmartIslandSettings,
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): Boolean {
        if (!isBlocked(service, settings, sbn, rankingMap, filterOverride)) return false
        return hidesNotificationList(service)
    }

    private fun hidesNotificationList(service: NotificationListenerService): Boolean {
        // the "hide from the shade" effect only exists from Android 9 (API 28)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val policy = runCatchingLogged(TAG, "Failed to read the DND policy") {
            (service.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.notificationPolicy
        } ?: return false
        return (policy.suppressedVisualEffects and NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST) != 0
    }

    fun isBlocked(
        service: NotificationListenerService,
        settings: SmartIslandSettings,
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): Boolean {
        val filter = filterOverride ?: runCatchingLogged(TAG, "Failed to get interruption filter") {
            service.currentInterruptionFilter
        }?.takeIf { it != NotificationListenerService.INTERRUPTION_FILTER_UNKNOWN }
            ?: runCatchingLogged(TAG, "Failed to get interruption filter from NotificationManager") {
                (service.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                    ?.currentInterruptionFilter
            } ?: NotificationListenerService.INTERRUPTION_FILTER_ALL

        if (filter == NotificationListenerService.INTERRUPTION_FILTER_ALL
            || filter == NotificationListenerService.INTERRUPTION_FILTER_UNKNOWN) {
            return false
        }

        val notification = sbn.notification ?: return true
        val mode = notification.toIslandMode(
            sbn,
            settings.liveActivitiesEnabled,
            settings.navigationEnabled,
            settings.deviceType
        )

        if (mode == IslandMode.Music || mode == IslandMode.Hotspot || mode == IslandMode.ScreenRecording
            || mode == IslandMode.AudioRecording) {
            return false
        }

        val rMap = rankingMap ?: runCatchingLogged(TAG, "Failed to get current ranking") { service.currentRanking }
        if (rMap != null) {
            val ranking = NotificationListenerService.Ranking()
            if (rMap.getRanking(sbn.key, ranking)) {
                return !ranking.matchesInterruptionFilter()
            }
        }

        if (filter == NotificationListenerService.INTERRUPTION_FILTER_NONE) {
            return true
        }

        if (filter == NotificationListenerService.INTERRUPTION_FILTER_ALARMS) {
            return notification.category != Notification.CATEGORY_ALARM &&
                mode != IslandMode.Timer &&
                mode != IslandMode.Stopwatch
        }

        return true
    }
}
