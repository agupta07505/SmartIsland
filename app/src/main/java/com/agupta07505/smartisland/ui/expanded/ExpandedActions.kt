/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.util.runCatchingLogged

fun IslandNotification?.sendFirstAction(context: Context, vararg keywords: String) {
    if (this == null) return
    val action = this.actionIntents.firstOrNull { act ->
        keywords.any { keyword -> act.title.contains(keyword, ignoreCase = true) }
    } ?: return
    if (action.pendingIntent != null) {
        triggerAction(context, this.packageName, action.pendingIntent, action.title, this.contentIntent)
        val notificationRepository = SmartIslandRepositories.notificationRepository(context)
        notificationRepository.resetTimer()
        if (this.mode == IslandMode.Music) {
            // prune stale music keys immediately
            notificationRepository.notifications.value
                .filter { it.mode == IslandMode.Music && it.packageName == this.packageName && it.key != this.key }
                .forEach { notificationRepository.removeNotification(it.key) }
        } else {
            notificationRepository.removeNotification(this.key)
            notificationRepository.sendCommand(SmartIslandCommand.CancelNotification(this.key))
        }
    }
}

fun formatDuration(valueMs: Long?): String {
    val totalSeconds = valueMs?.takeIf { it >= 0 }?.div(1000) ?: return "--:--"
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun triggerAction(context: Context, packageName: String, actionIntent: PendingIntent?, actionTitle: String, contentIntent: PendingIntent?) {
    android.util.Log.d("ExpandedActions", "triggerAction: actionTitle=$actionTitle, hasActionIntent=${actionIntent != null}, hasContentIntent=${contentIntent != null}")
    if (actionIntent == null) return

    // If it is a Reply action, since typing inside the overlay window is blocked by focus rules,
    // trigger the main notification's content intent to open the target chat directly!
    if (actionTitle.contains("reply", ignoreCase = true) && contentIntent != null) {
        android.util.Log.d("ExpandedActions", "triggerAction: Firing contentIntent (reply fallback)")
        sendIntentWithOptions(context, contentIntent)
    } else {
        android.util.Log.d("ExpandedActions", "triggerAction: Firing actionIntent")
        sendIntentWithOptions(context, actionIntent)
    }
}

fun sendIntentWithOptions(context: Context, pendingIntent: PendingIntent) {
    android.util.Log.d("ExpandedActions", "sendIntentWithOptions: sending pendingIntent=$pendingIntent")
    var sent = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        try {
            val options = ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                .toBundle()
            pendingIntent.send(context, 0, null, null, null, null, options)
            sent = true
        } catch (e: Exception) {
            android.util.Log.w("ExpandedActions", "pendingIntent.send with ActivityOptions failed: ${e.message}, attempting direct send fallback")
        }
    }
    if (!sent) {
        try {
            pendingIntent.send(context, 0, null)
            sent = true
        } catch (e: Exception) {
            try {
                pendingIntent.send()
                sent = true
            } catch (e2: Exception) {
                android.util.Log.e("ExpandedActions", "All attempts to send PendingIntent failed", e2)
            }
        }
    }
}
