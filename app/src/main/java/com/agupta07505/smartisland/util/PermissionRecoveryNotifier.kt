/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.agupta07505.smartisland.MainActivity
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.service.SmartIslandOverlayService

/**
 * The recovery path of Rule_Book 7.1: when the island stops because a permission was revoked, a plain
 * Android notification, "Restore Smart Island Permissions", opens the app's permissions screen. Skipped
 * silently when the app may not post notifications (Android 13+ without POST_NOTIFICATIONS).
 */
object PermissionRecoveryNotifier {

    enum class Reason { OverlayStopped, NotificationAccessRevoked }

    const val CHANNEL_ID = "smart_island_permissions"
    const val NOTIFICATION_ID = 8106

    /** MainActivity opens straight on the permissions screen when launched with this extra set to true. */
    const val EXTRA_OPEN_PERMISSIONS = "com.agupta07505.smartisland.extra.OPEN_PERMISSIONS"

    private const val TAG = "PermissionRecovery"

    // POST_NOTIFICATIONS is checked in canPostNotifications() before posting.
    @SuppressLint("MissingPermission")
    fun notify(context: Context, reason: Reason) {
        runCatchingLogged(TAG, "Recovery notification failed") {
            if (!canPostNotifications(context)) return@runCatchingLogged
            val manager = context.getSystemService(NotificationManager::class.java) ?: return@runCatchingLogged
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.recovery_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = context.getString(R.string.recovery_channel_desc)
                        setShowBadge(true)
                    }
                )
            }
            val open = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_PERMISSIONS, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val text = context.getString(
                when (reason) {
                    Reason.OverlayStopped -> R.string.recovery_text_overlay
                    Reason.NotificationAccessRevoked -> R.string.recovery_text_notifications
                }
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_smart_island)
                .setContentTitle(context.getString(R.string.recovery_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    /** Clears the prompt once both the overlay (accessibility) service and notification access are back. */
    fun cancelIfRestored(context: Context) {
        runCatchingLogged(TAG, "Recovery notification cancel failed") {
            if (isAccessibilityServiceEnabled(context) && isNotificationListenerEnabled(context)) {
                context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
            }
        }
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun isNotificationListenerEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, SmartIslandOverlayService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}
