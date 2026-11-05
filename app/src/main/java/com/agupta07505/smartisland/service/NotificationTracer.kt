/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.NotificationTrace
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.toIslandMode

internal class NotificationTracer {
    private companion object {
        const val TAG = "SmartIslandNotificationListener"
    }

    private var lastLoggedSettingsSignature: String? = null

    fun logRelevantSettings(s: SmartIslandSettings) {
        if (!AppLogRecorder.isRecording) return
        val signature = "enabled=${s.enabled} hideFromShade=${s.hideFromNotificationShade}" +
            " disabledPackages=${s.disabledNotificationPackages.sorted()}" +
            " disabledEvents=${s.disabledEvents.sorted()} disabledEventsLandscape=${s.disabledEventsLandscape.sorted()}" +
            " cooldown=${s.enableNotificationCooldown}(threshold=${s.notificationCooldownThreshold}," +
            "minutes=${s.notificationCooldownDurationMinutes},excluded=${s.notificationCooldownExcludedPackages.sorted()})" +
            " showInLandscape=${s.showInLandscape} showOnLockScreen=${s.showOnLockScreen} autoExpand=${s.autoExpandOnNotification}"
        if (signature == lastLoggedSettingsSignature) return
        lastLoggedSettingsSignature = signature
        AppLogRecorder.i("SettingsTrace", signature)
    }

    private fun rankingImportance(service: NotificationListenerService, sbn: StatusBarNotification,
            rankingMap: RankingMap?): Int? {
        val rMap = rankingMap ?: service.currentRanking ?: return null
        val ranking = NotificationListenerService.Ranking()
        return if (rMap.getRanking(sbn.key, ranking)) ranking.importance else null
    }

    fun tracePost(
        service: NotificationListenerService,
        s: SmartIslandSettings,
        sbn: StatusBarNotification,
        rankingMap: RankingMap?,
        suppressionReason: () -> String?,
        eventFor: (Notification, IslandMode, String) -> IslandEvent?
    ) {
        if (!AppLogRecorder.isRecording) return
        runCatchingLogged(TAG, "Notification trace failed") {
            val n = sbn.notification
            val detailed = NotificationTrace.isDetailed(sbn.packageName, n)
            val reason = suppressionReason()
            val mode = n.toIslandMode(sbn, s.liveActivitiesEnabled, s.navigationEnabled, s.deviceType)
            val signature = "$reason|$mode|${n.flags}"
            if (!NotificationTrace.shouldLog(sbn.key, signature, detailed)) return
            val event = eventFor(n, mode, sbn.packageName)
            val dndFilter = service.currentInterruptionFilter
            AppLogRecorder.i(
                "NotifTrace",
                "POSTED " + NotificationTrace.brief(sbn, rankingImportance(service, sbn, rankingMap)) +
                    " mode=$mode filter=${reason ?: "passed"} dndFilter=$dndFilter" +
                    " | islandEnabled=${s.enabled} hideFromShade=${s.hideFromNotificationShade}" +
                    " appDisabled=${sbn.packageName in s.disabledNotificationPackages}" +
                    " event=${event?.id} eventEnabled=${event?.let { s.isEventEnabled(it.id) }}" +
                    " eventOffSomewhere=${event?.let { s.isEventDisabledSomewhere(it.id) }}" +
                    " cooldown=${s.enableNotificationCooldown}(threshold=${s.notificationCooldownThreshold}," +
                    "excluded=${sbn.packageName in s.notificationCooldownExcludedPackages})"
            )
        }
    }

    fun traceStage(sbn: StatusBarNotification, stage: String, detail: String = "") {
        if (!AppLogRecorder.isRecording) return
        runCatchingLogged(TAG, "Notification trace failed") {
            val detailed = NotificationTrace.isDetailed(sbn.packageName, sbn.notification)
            if (!NotificationTrace.shouldLog(sbn.key, "$stage|$detail", detailed)) return
            AppLogRecorder.i(
                "NotifTrace",
                "$stage pkg=${sbn.packageName} key=${sbn.key}${if (detail.isEmpty()) "" else " $detail"}"
            )
        }
    }
}
