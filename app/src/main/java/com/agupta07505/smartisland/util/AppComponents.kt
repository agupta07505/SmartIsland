/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * Class names of the app's manifest components, so this layer can address them without importing the service,
 * receiver or activity classes. `AppComponentsTest` pins each name to its class
 */
object AppComponents {
    const val MAIN_ACTIVITY = "com.agupta07505.smartisland.MainActivity"
    const val OVERLAY_SERVICE = "com.agupta07505.smartisland.service.SmartIslandOverlayService"
    const val LISTENER_SERVICE = "com.agupta07505.smartisland.service.SmartIslandNotificationListenerService"
    const val TIMER_RECEIVER = "com.agupta07505.smartisland.receiver.TimerActionReceiver"
    const val STOPWATCH_RECEIVER = "com.agupta07505.smartisland.receiver.StopwatchActionReceiver"
}
