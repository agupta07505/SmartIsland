package com.agupta07505.smartisland.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction

/**
 * Debug-only test hook: posts a synthetic alert straight into the island's repository so the alert rules
 * (priority, grouping, the seven-second timeout, collisions) can be exercised from adb without a
 * second app. It is compiled into debug builds only.
 *
 *   adb shell am broadcast -n <applicationId>/com.agupta07505.smartisland.debug.DebugAlertReceiver \
 *     --es kind message --es pkg com.whatsapp --es app WhatsApp --es title Sam --es text "hi" [--ez reply true]
 *   ... --es kind clear
 *
 * kind: message | promo | call | alarm | nav | music | timer | stopwatch | record | live | battery | bluetooth |
 * flash | clear.  Optional: key, group, --ez expand true.
 */
class DebugAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repo = SmartIslandRepositories.notificationRepository(context)
        val kind = intent.getStringExtra("kind") ?: "message"
        if (kind == "clear") {
            repo.removeAllNotifications()
            return
        }
        val pkg = intent.getStringExtra("pkg") ?: "com.whatsapp"
        val app = intent.getStringExtra("app") ?: pkg.substringAfterLast('.')
        val now = System.currentTimeMillis()
        val key = intent.getStringExtra("key") ?: "dbg_${pkg}_$now"
        val reply = intent.getBooleanExtra("reply", kind == "message")
        val (mode, category) = when (kind) {
            "call" -> IslandMode.IncomingCall to "call"
            "alarm" -> IslandMode.Notification to "alarm"
            "nav" -> IslandMode.Navigation to "navigation"
            "music" -> IslandMode.Music to "transport"
            "timer" -> IslandMode.Timer to null
            "stopwatch" -> IslandMode.Stopwatch to null
            "record" -> IslandMode.ScreenRecording to null
            "live" -> IslandMode.LiveActivity to null
            "battery" -> IslandMode.Battery to "battery_low"
            "bluetooth" -> IslandMode.Bluetooth to null
            "flash" -> IslandMode.Flashlight to null
            "promo" -> IslandMode.Notification to "promo"
            else -> IslandMode.Notification to "msg"
        }
        val actions = buildList {
            if (reply) add(IslandNotificationAction("Reply", null, isQuickReply = true, remoteInputKey = "key_text_reply"))
            if (kind == "call") add(IslandNotificationAction("Answer", null))
        }
        repo.postNotification(
            IslandNotification(
                key = key,
                packageName = pkg,
                appName = app,
                title = intent.getStringExtra("title") ?: "Sam",
                text = intent.getStringExtra("text") ?: "hello",
                timeMillis = now,
                category = category,
                mode = mode,
                actionIntents = actions,
                mediaIsPlaying = kind == "music",
                groupId = intent.getStringExtra("group")
            ),
            autoExpand = intent.getBooleanExtra("expand", false)
        )
    }
}
