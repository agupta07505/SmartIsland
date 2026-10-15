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

fun IslandNotification?.trySendFirstAction(context: Context, vararg keywords: String): Boolean {
    if (this == null) return false
    val action = this.actionIntents.firstOrNull { act ->
        keywords.any { keyword -> act.title.contains(keyword, ignoreCase = true) }
    } ?: return false
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

fun triggerAction(context: Context, packageName: String, actionIntent: PendingIntent?, actionTitle: String, contentIntent: PendingIntent?): Boolean {
    android.util.Log.d("ExpandedActions", "triggerAction: actionTitle=$actionTitle, hasActionIntent=${actionIntent != null}, hasContentIntent=${contentIntent != null}")
    if (actionIntent == null) return false

    // If it is a Reply action, since typing inside the overlay window is blocked by focus rules,
    // trigger the main notification's content intent to open the target chat directly!
    if (actionTitle.contains("reply", ignoreCase = true) && contentIntent != null) {
        android.util.Log.d("ExpandedActions", "triggerAction: Firing contentIntent (reply fallback)")
        return sendIntentWithOptions(context, contentIntent)
    }
    android.util.Log.d("ExpandedActions", "triggerAction: Firing actionIntent")
    return sendIntentWithOptions(context, actionIntent)
}

/** Sends the intent the way the shade would. Returns false when every attempt failed (for example the app replaced it). */
fun sendIntentWithOptions(context: Context, pendingIntent: PendingIntent): Boolean {
    android.util.Log.d("ExpandedActions", "sendIntentWithOptions: sending pendingIntent=$pendingIntent")
    val attempts = buildList<() -> Unit> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    .toBundle()
                pendingIntent.send(context, 0, null, null, null, null, options)
            }
        }
        add { pendingIntent.send(context, 0, null) }
        add { pendingIntent.send() }
    }
    val sent = com.agupta07505.smartisland.util.SendAttempts.anySucceeds(attempts)
    if (!sent) android.util.Log.e("ExpandedActions", "All attempts to send PendingIntent failed")
    return sent
}

/** What a notification button does, derived from its label (multi-language keywords). */
enum class ActionRole { Pause, Resume, Stop, Lap, Reset, AddMinute, Other }

private val PAUSE_WORDS = listOf("pause", "pausa", "pausar", "sospendi", "interrompi", "onderbreek", "रोकें", "暂停", "一時停止", "пауза")
private val RESUME_WORDS = listOf("resume", "continue", "unpause", "play", "start", "reanudar", "reprendre", "weiter", "riprendi", "continuar", "शुरू", "继续", "再開", "возобновить")
private val STOP_WORDS = listOf("stop", "finish", "save", "detener", "parar", "arrêter", "beenden", "停止", "结束", "стоп")
private val RESET_WORDS = listOf("reset", "cancel", "clear", "delete", "dismiss", "annuler", "abbrechen", "eliminar", "borrar", "重置", "取消")
private val LAP_WORDS = listOf("lap", "split", "vuelta", "tour", "runde", "分段")

/** "+1:00", "+1 min", "+ 1m", "Add 1 minute", "Extend 1 min": the timer's add-a-minute button. */
private val ADD_MINUTE_SIGN = Regex("""(?<![\d:])\+\s*0?1(:00)?\s*(m|min|mins|minute|minutes)?(?![\d:a-z])""")
private val ADD_MINUTE_WORDS = Regex("""(add|extend|more|another)\s*(an?\s*)?(1|one)\s*(m|min|mins|minute|minutes)\b""")

fun classifyAction(title: String): ActionRole {
    val t = title.lowercase()
    return when {
        ADD_MINUTE_SIGN.containsMatchIn(t) || ADD_MINUTE_WORDS.containsMatchIn(t) -> ActionRole.AddMinute
        LAP_WORDS.any { t.contains(it) } -> ActionRole.Lap
        PAUSE_WORDS.any { t.contains(it) } -> ActionRole.Pause
        RESUME_WORDS.any { t.contains(it) } -> ActionRole.Resume
        RESET_WORDS.any { t.contains(it) } -> ActionRole.Reset
        STOP_WORDS.any { t.contains(it) } -> ActionRole.Stop
        else -> ActionRole.Other
    }
}

/** First real button (one that carries a PendingIntent) with one of the given roles. */
fun IslandNotification?.actionWithRole(vararg roles: ActionRole): com.agupta07505.smartisland.model.IslandNotificationAction? {
    if (this == null) return null
    return actionIntents.firstOrNull { it.pendingIntent != null && classifyAction(it.title) in roles }
}

/** True when the notification currently offers "resume" (so the underlying timer/recording is paused). */
fun IslandNotification?.offersResume(): Boolean =
    this != null && actionIntents.any { classifyAction(it.title) == ActionRole.Resume }

/**
 * Fires the notification's own button exactly like tapping it in the shade.
 * Does not touch island state: the listener re-posts the notification when the app updates it.
 */
fun IslandNotification.fireAction(context: Context, action: com.agupta07505.smartisland.model.IslandNotificationAction): Boolean {
    val intent = action.pendingIntent
    val refusal = com.agupta07505.smartisland.util.ButtonPress.refusal(
        hasIntent = intent != null,
        viaView = action.viaView,
        liveLabels = if (action.viaView) com.agupta07505.smartisland.util.NotificationViewInspector.liveLabels(key) else emptyList(),
        title = action.title
    )
    if (refusal != null || intent == null) {
        com.agupta07505.smartisland.util.AppLogRecorder.w(
            "ButtonPress",
            "pkg=$packageName '${action.title}' (${classifyAction(action.title)}) not sent: $refusal"
        )
        return false
    }
    val sent = triggerAction(context, packageName, intent, action.title, contentIntent)
    com.agupta07505.smartisland.util.AppLogRecorder.d(
        "ButtonPress",
        "pkg=$packageName '${action.title}' (${classifyAction(action.title)}) via=${if (action.viaView) "view" else "app"} sent=$sent"
    )
    if (sent) SmartIslandRepositories.notificationRepository(context).resetTimer()
    return sent
}

/**
 * Like [fireAction], but for buttons whose PendingIntent starts an activity (voice recorder Save / Stop):
 * if the app's window pops up because of the press, the accessibility service sends the user straight
 * back, so the recording is saved without the app staying open.
 */
fun IslandNotification.fireActionSilently(context: Context, action: com.agupta07505.smartisland.model.IslandNotificationAction): Boolean {
    com.agupta07505.smartisland.util.SilentActionGuard.arm(packageName)
    val fired = fireAction(context, action)
    if (!fired) com.agupta07505.smartisland.util.SilentActionGuard.disarm()
    return fired
}
