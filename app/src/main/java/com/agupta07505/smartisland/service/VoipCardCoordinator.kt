/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.VoipCallDetector
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.NotificationFilter
import com.agupta07505.smartisland.util.VoipCallMonitor
import com.agupta07505.smartisland.util.VoipCallSignal
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import com.agupta07505.smartisland.util.toVoipFacts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/** What the coordinator needs from the listener service that owns it. */
internal interface VoipCardHost {
    val repository: INotificationRepository
    val ownPackageName: String
    val settings: SmartIslandSettings
    val isConnected: Boolean

    /** Null when the listener isn't connected */
    fun activeNotifications(): List<StatusBarNotification>?

    /** The call is over: remove the card and leave a call-end card behind. */
    fun endCard(key: String)

    /** Run a promoted call notification through the normal post path. */
    fun handlePost(sbn: StatusBarNotification, settings: SmartIslandSettings)
}

internal class VoipCardCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val host: VoipCardHost
) {
    private companion object {
        const val TAG = "SmartIslandNotificationListener"
        const val VOIP_CARD_KEY = "voip_call_audio"
    }

    private val notificationRepository: INotificationRepository get() = host.repository
    private val ownPackage: String get() = host.ownPackageName

    private var voipMonitor: VoipCallMonitor? = null
    private val voipPromotedKeys = ConcurrentHashMap.newKeySet<String>()

    fun start() {
        if (voipMonitor != null) return
        voipMonitor = VoipCallMonitor(
            context = context,
            scope = scope,
            onChanged = { reconcile() }
        ).also { it.start() }
    }

    private fun reconcile() {
        scope.launch {
            runSuspendCatchingLogged(TAG, "VoIP reconcile failed") {
                val settings = host.settings
                val enabled = settings.enabled &&
                    settings.isEventEnabled(IslandEvent.CALL.id)
                AppLogRecorder.i(
                    "VoipReconcile",
                    "signalActive=${VoipCallSignal.active}" +
                        " voiceCapture=${VoipCallSignal.voiceCapture}" +
                        " islandEnabled=${settings.enabled} callEventEnabled=${settings.isEventEnabled(IslandEvent.CALL.id)}" +
                        " listenerConnected=${host.isConnected}"
                )
                if (!VoipCallSignal.active || !enabled) {
                    AppLogRecorder.i("VoipReconcile", "-> clearing VoIP cards (signal off or call event disabled)")
                    clearCards(callOver = !VoipCallSignal.active)
                    return@runSuspendCatchingLogged
                }
                if (!host.isConnected) {
                    AppLogRecorder.w("VoipReconcile", "-> skipped: notification listener not connected, no way to read the call notification")
                    return@runSuspendCatchingLogged
                }

                val candidates = runCatchingLogged(TAG, "Failed to get active notifications") {
                    host.activeNotifications()
                }.orEmpty().filter {
                    it.packageName != ownPackage &&
                        ((it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 ||
            NotificationFilter.isCallNotification(it.notification))
                }
                val bestKey = VoipCallDetector.pickBest(
                    candidates.map { it.key to it.notification.toVoipFacts(it.packageName) }
                )
                val best = candidates.firstOrNull { it.key == bestKey }
                if (AppLogRecorder.isRecording) {
                    // every active notification with its score
                    candidates.forEach { c ->
                        AppLogRecorder.i(
                            "VoipReconcile",
                            "candidate pkg=${c.packageName} key=${c.key} " +
                                VoipCallDetector.explain(c.notification.toVoipFacts(c.packageName)) +
                                if (c.key == bestKey) " <== BEST" else ""
                        )
                    }
                    AppLogRecorder.i(
                        "VoipReconcile",
                        "${candidates.size} candidates, best=${bestKey ?: "none"} -> " + when {
                            best != null -> "promoting the app's own call notification to a call card"
                            VoipCallSignal.voiceCapture -> "no qualifying notification, showing the button-less 'Call in progress' card"
                            else -> "no qualifying notification and no voice capture: no card"
                        }
                    )
                }
                // The scan above can take seconds on a busy shade; the call may be over by now
                if (!VoipCallSignal.active) {
                    AppLogRecorder.i("VoipReconcile", "-> call ended during the scan, dropping the result")
                    clearCards(callOver = true)
                    return@runSuspendCatchingLogged
                }
                if (best != null) {
                    notificationRepository.removeNotification(VOIP_CARD_KEY)
                    host.handlePost(best, settings)
                } else if (VoipCallSignal.voiceCapture) {
                    notificationRepository.postNotification(
                        IslandNotification(
                            key = VOIP_CARD_KEY,
                            packageName = ownPackage,
                            appName = "Voice call",
                            title = "Call in progress",
                            text = "",
                            timeMillis = VoipCallSignal.sinceMillis
                                .takeIf { it > 0 } ?: System.currentTimeMillis(),
                            mode = IslandMode.IncomingCall
                        )
                    )
                } else {
                    notificationRepository.removeNotification(VOIP_CARD_KEY)
                }
            }
        }
    }

    /** [callOver]: the call itself ended, so its card leaves a call-end card behind; otherwise the card just goes. */
    private fun clearCards(callOver: Boolean) {
        val keys = voipPromotedKeys.toList() + VOIP_CARD_KEY
        voipPromotedKeys.clear()
        keys.forEach { if (callOver) host.endCard(it) else notificationRepository.removeNotification(it) }
    }

    fun stop() {
        voipMonitor?.stop()
        voipMonitor = null
    }

    fun forgetPromoted(key: String) {
        voipPromotedKeys.remove(key)
    }

    fun onVoipCallPost(key: String) {
        if (VoipCallSignal.active) {
            voipPromotedKeys.add(key)
            notificationRepository.removeNotification(VOIP_CARD_KEY)
        }
    }
}
