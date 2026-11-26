/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.agupta07505.smartisland.model.IslandNotificationAction

/** Swallows a missing or OEM-restricted destination instead of crashing. */
fun Context.safeStartActivity(
    intent: Intent,
    errorMessage: String? = "This setting is not available on your device.",
    fallbackIntent: Intent? = null
): Boolean {
    fun launch(candidate: Intent): Boolean {
        val resolvedIntent = Intent(candidate).apply {
            if (this@safeStartActivity !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return runCatchingLogged("SafeStartActivity", "Failed to open ${candidate.action}") {
            startActivity(resolvedIntent)
            true
        } ?: false
    }

    if (launch(intent)) return true
    if (fallbackIntent != null && launch(fallbackIntent)) return true

    if (errorMessage != null) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }
    return false
}

/** Sends the intent the way the shade would, false when every attempt failed (the app replaced it). */
fun sendIntentWithOptions(context: Context, pendingIntent: PendingIntent): Boolean {
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
    val sent = SendAttempts.anySucceeds(attempts)
    if (!sent) AppLogRecorder.e("ExpandedActions", "every PendingIntent send attempt failed")
    return sent
}

/**
 * Hands a reply to the action's intent through [send]. True only when the intent was there and [send] returned
 * normally, so a missing or cancelled intent never reads as a sent reply
 */
fun IslandNotificationAction?.deliverReply(send: (PendingIntent) -> Unit): Boolean {
    val intent = this?.pendingIntent ?: return false
    return runCatchingLogged("Reply", "Reply could not be sent") {
        send(intent)
        true
    } ?: false
}
