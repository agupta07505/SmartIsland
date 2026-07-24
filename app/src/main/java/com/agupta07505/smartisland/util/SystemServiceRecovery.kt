/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.agupta07505.smartisland.service.SmartIslandNotificationListenerService
import com.agupta07505.smartisland.service.SmartIslandOverlayService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reconnects system-managed services after the package leaves Android's force-stopped state.
 *
 * NotificationListenerService provides an official rebind API. AccessibilityService does not,
 * so for an already-authorized service we issue a harmless component-state change between
 * DEFAULT and ENABLED. Both states leave the manifest-enabled component available, while the
 * package-change signal gives AccessibilityManager/OEM managers a chance to bind it again.
 */
object SystemServiceRecovery {
    private val accessibilityRefreshAttempted = AtomicBoolean(false)

    fun requestRecovery(context: Context) {
        runCatchingLogged(TAG, "System-service recovery failed") {
            val appContext = context.applicationContext
            requestNotificationListenerRebind(appContext)

            if (!SmartIslandOverlayService.isSystemConnected &&
                isAccessibilityPermissionGranted(appContext) &&
                accessibilityRefreshAttempted.compareAndSet(false, true)
            ) {
                refreshAccessibilityComponent(appContext)
            }
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

    private fun isAccessibilityPermissionGranted(context: Context): Boolean {
        val expected = ComponentName(context, SmartIslandOverlayService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any {
            ComponentName.unflattenFromString(it) == expected
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
