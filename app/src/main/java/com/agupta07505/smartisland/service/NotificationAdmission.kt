/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.TransferKind
import com.agupta07505.smartisland.util.CallStyleExtras
import com.agupta07505.smartisland.util.NotificationFilter
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.toIslandMode
import com.agupta07505.smartisland.util.transferKind

/**
 * Whether a notification may reach the island and whether it leaves the system shade: the Do Not Disturb,
 * disabled-event and package rules, the silent check, the island-only rule. Decides and changes nothing, cancelling,
 * tracing and scheduling stay with the listener service, which hands in the live [settings]
 */
internal class NotificationAdmission(
    private val service: NotificationListenerService,
    private val settings: () -> SmartIslandSettings
) {
    fun isHiddenByDoNotDisturb(
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): Boolean = NotificationDndPolicy.isHidden(service, settings(), sbn, rankingMap, filterOverride)

    fun isBlockedByDoNotDisturb(
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): Boolean = NotificationDndPolicy.isBlocked(service, settings(), sbn, rankingMap, filterOverride)

    fun isSilent(sbn: StatusBarNotification, rankingMap: RankingMap? = null): Boolean {
        val rMap = rankingMap ?: runCatchingLogged(TAG, "Failed to get current ranking") { service.currentRanking }
            ?: return false
        val ranking = Ranking()
        if (!rMap.getRanking(sbn.key, ranking)) return false
        return NotificationModeRules.isSilentImportance(ranking.importance)
    }

    fun shouldSuppress(
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): Boolean = suppressionReason(sbn, rankingMap, filterOverride) != null

    /** Rule that keeps [sbn] off the island, or null when it may be shown. */
    fun suppressionReason(
        sbn: StatusBarNotification,
        rankingMap: RankingMap? = null,
        filterOverride: Int? = null
    ): String? {
        if (!settings().showDuringDnd && isHiddenByDoNotDisturb(sbn, rankingMap, filterOverride)) {
            return "do-not-disturb"
        }
        val pm = service.packageManager
        val disabled = settings().disabledEvents
        if (disabled.isNotEmpty()) {
            val mode = sbn.notification.toIslandMode(
                sbn,
                settings().liveActivitiesEnabled,
                settings().navigationEnabled,
                settings().deviceType
            )
            val event = listenerEventFor(sbn.notification, mode, sbn.packageName)
            if (event != null && settings().isEventDisabledEverywhere(event.id)) {
                return "event-disabled-everywhere(${event.id})"
            }
        }
        return NotificationFilter.suppressionReason(
            sbn,
            pm,
            settings().liveActivitiesEnabled,
            settings().navigationEnabled,
            settings().disabledNotificationPackages,
            settings().deviceType
        )
    }

    fun listenerEventFor(notification: Notification, mode: IslandMode, packageName: String): IslandEvent? =
        IslandEvent.forListenerMode(
            mode,
            transferKind = if (mode == IslandMode.DownloadUpload) {
                notification.transferKind(packageName)
            } else {
                TransferKind.DOWNLOAD
            }
        )

    fun shouldBeIslandOnly(notification: Notification, mode: IslandMode): Boolean =
        NotificationModeRules.isIslandOnly(mode) { CallStyleExtras.isRinging(notification) }

    fun shouldHideFromShade(notification: Notification, mode: IslandMode, packageName: String): Boolean {
        if (!shouldBeIslandOnly(notification, mode)) return false
        val event = listenerEventFor(notification, mode, packageName) ?: return true
        return !settings().isEventDisabledSomewhere(event.id)
    }

    private companion object {
        const val TAG = "SmartIslandNotificationListener"
    }
}
