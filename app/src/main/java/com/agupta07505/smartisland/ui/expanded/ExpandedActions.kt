/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.app.PendingIntent
import android.content.Context
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.ActionRole
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.classifyAction
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.ButtonPress
import com.agupta07505.smartisland.util.NotificationViewInspector
import com.agupta07505.smartisland.util.SilentActionGuard
import com.agupta07505.smartisland.util.sendIntentWithOptions

fun IslandNotification?.trySendFirstAction(context: Context, vararg keywords: String): Boolean {
    if (this == null) return false
    val action = this.actionIntents.firstOrNull { act ->
        keywords.any { keyword -> act.title.contains(keyword, ignoreCase = true) }
    } ?: return false
    return sendSelectedAction(context, action)
}

internal fun IslandNotification.sendSelectedAction(context: Context, action: IslandNotificationAction): Boolean {
    if (action.pendingIntent != null) {
        if (!triggerAction(context, packageName, action.pendingIntent, action.title, contentIntent)) return false
        val notificationRepository = SmartIslandRepositories.notificationRepository(context)
        notificationRepository.resetTimer()
        if (this.mode == IslandMode.Music) {
            // Prune stale music keys immediately
            notificationRepository.notifications.value
                .filter { it.mode == IslandMode.Music && it.packageName == this.packageName && it.key != this.key }
                .forEach { notificationRepository.removeNotification(it.key) }
        } else {
            notificationRepository.removeNotification(this.key)
            notificationRepository.sendCommand(SmartIslandCommand.CancelNotification(this.key))
        }
        return true
    }
    return false
}

fun IslandNotification?.sendFirstAction(context: Context, vararg keywords: String) {
    trySendFirstAction(context, *keywords)
}

fun formatDuration(valueMs: Long?): String {
    val totalSeconds = valueMs?.takeIf { it >= 0 }?.div(1000) ?: return "--:--"
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun triggerAction(context: Context, packageName: String, actionIntent: PendingIntent?, actionTitle: String,
        contentIntent: PendingIntent?): Boolean {
    if (actionIntent == null) return false

    // Typing in the overlay window is blocked by focus rules, so Reply opens the chat through the content intent
    if (actionTitle.contains("reply", ignoreCase = true) && contentIntent != null) {
        return sendIntentWithOptions(context, contentIntent)
    }
    return sendIntentWithOptions(context, actionIntent)
}

fun IslandNotification?.actionWithRole(vararg roles: ActionRole): IslandNotificationAction? {
    if (this == null) return null
    return actionIntents.firstOrNull { it.pendingIntent != null && classifyAction(it.title) in roles }
}

fun IslandNotification?.offersResume(): Boolean =
    this != null && actionIntents.any { classifyAction(it.title) == ActionRole.Resume }

fun IslandNotification.fireAction(context: Context, action: IslandNotificationAction): Boolean {
    val intent = action.pendingIntent
    val refusal = ButtonPress.refusal(
        hasIntent = intent != null,
        viaView = action.viaView,
        liveLabels = if (action.viaView) NotificationViewInspector.liveLabels(key) else emptyList(),
        title = action.title
    )
    if (refusal != null || intent == null) {
        AppLogRecorder.w(
            "ButtonPress",
            "pkg=$packageName '${action.title}' (${classifyAction(action.title)}) not sent: $refusal"
        )
        return false
    }
    val sent = triggerAction(context, packageName, intent, action.title, contentIntent)
    AppLogRecorder.d(
        "ButtonPress",
        "pkg=$packageName '${action.title}' (${classifyAction(action.title)}) via=${if (action.viaView) "view" else "app"} sent=$sent"
    )
    if (sent) SmartIslandRepositories.notificationRepository(context).resetTimer()
    return sent
}

/**
 * For buttons whose PendingIntent starts an activity (recorder Save / Stop)
 * If the app's window pops up the accessibility service sends the user straight back, so the recording is saved without
 * the app staying open
 */
fun IslandNotification.fireActionSilently(context: Context, action: IslandNotificationAction): Boolean {
    SilentActionGuard.arm(packageName)
    val fired = fireAction(context, action)
    if (!fired) SilentActionGuard.disarm()
    return fired
}
