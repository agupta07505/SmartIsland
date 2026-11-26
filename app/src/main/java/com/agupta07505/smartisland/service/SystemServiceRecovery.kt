/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.isOverlayAccessibilityServiceEnabled
import com.agupta07505.smartisland.util.runCatchingLogged
import java.util.concurrent.atomic.AtomicBoolean

/** Reconnects system-managed services after Android clears the package's force-stopped state. */
object SystemServiceRecovery {
    private val accessibilityRefreshAttempted = AtomicBoolean(false)

    fun resetAccessibilityRefreshAttempt() {
        accessibilityRefreshAttempted.set(false)
    }

    fun requestRecovery(context: Context) {
        try {
            val appContext = context.applicationContext
            requestNotificationListenerRebind(appContext)

            if (!SmartIslandOverlayService.isSystemConnected &&
                appContext.isOverlayAccessibilityServiceEnabled() &&
                accessibilityRefreshAttempted.compareAndSet(false, true)
            ) {
                refreshAccessibilityComponent(appContext)
            }
        } catch (e: Exception) {
            AppLogRecorder.e(TAG, "System-service recovery failed", e)
        }
    }

    private fun requestNotificationListenerRebind(context: Context) {
        if (!isNotificationListenerPermissionGranted(context) ||
            SmartIslandNotificationListenerService.isSystemConnected
        ) return

        runCatchingLogged(TAG, "Notification-listener rebind failed") {
            NotificationListenerService.requestRebind(
                ComponentName(context, SmartIslandNotificationListenerService::class.java)
            )
        }
    }

    private fun refreshAccessibilityComponent(context: Context) {
        runCatchingLogged(TAG, "Accessibility component refresh failed") {
            val component = ComponentName(context, SmartIslandOverlayService::class.java)
            val packageManager = context.packageManager
            val currentState = packageManager.getComponentEnabledSetting(component)
            val refreshedState = when (currentState) {
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ->
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED ->
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                else -> return@runCatchingLogged
            }
            packageManager.setComponentEnabledSetting(
                component,
                refreshedState,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun isNotificationListenerPermissionGranted(context: Context): Boolean {
        val expected = ComponentName(context, SmartIslandNotificationListenerService::class.java)
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabledListeners.split(':').any {
            ComponentName.unflattenFromString(it) == expected
        }
    }

    private const val TAG = "SystemServiceRecovery"
}
