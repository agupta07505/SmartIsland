/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.accessibilityservice.AccessibilityService
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.model.AppShortcutAction
import com.agupta07505.smartisland.model.FloatingWindowRoute
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.SplitPlan
import com.agupta07505.smartisland.model.floatingWindowRoute
import com.agupta07505.smartisland.model.splitScreenPlan
import com.agupta07505.smartisland.ui.IslandViewModel
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.LmoFreeform
import com.agupta07505.smartisland.util.ShizukuManager
import com.agupta07505.smartisland.util.UtilityBridge
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.sendIntentWithOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Notification taps, shortcut actions, floating windows (system freeform and LMO Freeform), split screen and the
 * utility bridge
 * The service owns the lifecycle, the view model is only read after [isReady]
 */
internal class OverlayAppLauncher(
    private val service: AccessibilityService,
    private val notificationRepository: INotificationRepository,
    private val serviceScope: CoroutineScope,
    private val windowHandler: Handler,
    private val viewModelHolder: OverlayViewModelHolder,
    private val isReady: () -> Boolean,
) {
    private val viewModel: IslandViewModel get() = viewModelHolder.get()
    private val packageManager get() = service.packageManager
    private val resources get() = service.resources

    private fun startActivity(intent: Intent) = service.startActivity(intent)
    private fun startActivity(intent: Intent, options: Bundle?) = service.startActivity(intent, options)
    private fun sendBroadcast(intent: Intent) = service.sendBroadcast(intent)
    private fun getString(id: Int): String = service.getString(id)
    private fun performGlobalAction(action: Int): Boolean = service.performGlobalAction(action)

    fun openNotification(notification: IslandNotification) {
        // The torch card has no app behind it: a tap on its body does nothing, the island stays open
        if (notification.mode == IslandMode.Flashlight) return
        if (notification.contentIntent != null) {
            sendIntentWithOptions(service, notification.contentIntent)
        } else {
            runCatchingLogged(TAG, "Failed to launch package activity") {
                when (notification.mode) {
                    IslandMode.Bluetooth -> {
                        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    }
                    IslandMode.Battery -> {
                        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                        } else {
                            val altIntent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(altIntent)
                        }
                    }
                    IslandMode.Hotspot -> {
                        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    }
                    IslandMode.Timer,
                    IslandMode.Stopwatch -> {
                        val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                            ?: Intent(AlarmClock.ACTION_SHOW_TIMERS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        if (launchIntent.resolveActivity(packageManager) != null) {
                            startActivity(launchIntent)
                        } else {
                            val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(clockIntent)
                        }
                    }
                    else -> {
                        val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(launchIntent)
                        } else {
                            Toast.makeText(service, "Can't open ${notification.appName}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        notificationRepository.removeNotificationsForPackage(notification.packageName)
        viewModel.collapse()
    }

    fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Toast.makeText(service, "App is no longer available", Toast.LENGTH_SHORT).show()
            return
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatchingLogged(TAG, "Failed to launch shortcut app") {
            startActivity(launchIntent)
        }
        notificationRepository.removeNotificationsForPackage(packageName)
        viewModel.collapse()
    }

    fun installUtilityBridge() {
        val bridge = UtilityBridge
        bridge.collapse = {
            windowHandler.post { if (isReady()) viewModel.collapse() }
        }
        bridge.globalAction =
            { action -> runCatchingLogged(TAG, "Global action $action failed") { performGlobalAction(action) }
            ?: false }
        bridge.openIntentFloating = { intent ->
            val settings = viewModel.settings.value
            when (floatingWindowRoute(settings.floatingWindowEnabled, settings.floatingWindowMode)) {
                FloatingWindowRoute.Off -> false
                FloatingWindowRoute.System -> {
                    runCatchingLogged(TAG, "Failed to open a page in a floating window") {
                        startActivity(Intent(intent)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
                            systemFreeformBundle())
                        true
                    } ?: false
                }
                // LMO Freeform opens an activity by package and class
                FloatingWindowRoute.Lmo -> {
                    val lmo = LmoFreeform
                    val component = if (lmo.isInstalled(service)) {
                        intent.component
                            ?: packageManager.resolveActivity(intent, 0)
                                ?.activityInfo?.let { ComponentName(it.packageName, it.name) }
                    } else null
                    if (component == null) {
                        false
                    } else {
                        runCatchingLogged(TAG, "Failed to ask LMO Freeform for a page") {
                            sendBroadcast(lmo.startIntent(component))
                            true
                        } ?: false
                    }
                }
            }
        }
        bridge.openFloating = { packageName ->
            val settings = viewModel.settings.value
            val route = floatingWindowRoute(settings.floatingWindowEnabled, settings.floatingWindowMode)
            if (route == FloatingWindowRoute.Off) {
                false
            } else {
                openAppInFloatingWindow(packageName)
                true
            }
        }
    }

    fun launchAppAction(packageName: String, action: AppShortcutAction) {
        when (action) {
            AppShortcutAction.AppInfo -> openAppInfo(packageName)
            AppShortcutAction.FloatingWindow -> openAppInFloatingWindow(packageName)
            AppShortcutAction.SplitScreen -> openAppInSplitScreen(packageName)
        }
    }

    private fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatchingLogged(TAG, "Failed to open app info") {
            startActivity(intent)
        }
        viewModel.collapse()
    }

    private fun openAppInFloatingWindow(packageName: String) {
        val settings = viewModel.settings.value
        val launched = when (floatingWindowRoute(settings.floatingWindowEnabled, settings.floatingWindowMode)) {
            FloatingWindowRoute.Off -> {
                Toast.makeText(service, getString(R.string.shortcut_floating_window_off), Toast.LENGTH_SHORT).show()
                false
            }
            FloatingWindowRoute.Lmo -> openPackageInLmoFreeform(packageName)
            FloatingWindowRoute.System -> openPackageInSystemFreeform(packageName)
        }
        if (launched) notificationRepository.removeNotificationsForPackage(packageName)
        viewModel.collapse()
    }

    private fun openPackageInSystemFreeform(packageName: String): Boolean {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Toast.makeText(service, "App is no longer available", Toast.LENGTH_SHORT).show()
            return false
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        val started = runCatchingLogged(TAG, "Failed to launch app in a floating window") {
            startActivity(launchIntent, systemFreeformBundle())
        } != null
        return started
    }

    private fun openPackageInLmoFreeform(packageName: String): Boolean {
        val lmo = LmoFreeform
        if (!lmo.isInstalled(service)) {
            Toast.makeText(service, "LMO Freeform is not installed on this device.", Toast.LENGTH_SHORT).show()
            return false
        }
        val component = lmo.launcherComponent(packageManager, packageName)
        if (component == null) {
            Toast.makeText(service, "App is no longer available", Toast.LENGTH_SHORT).show()
            return false
        }
        val sent = runCatchingLogged(TAG, "Failed to ask LMO Freeform for a window") {
            sendBroadcast(lmo.startIntent(component))
        } != null
        return sent
    }

    private fun openAppInSplitScreen(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Toast.makeText(service, "App is no longer available", Toast.LENGTH_SHORT).show()
            viewModel.collapse()
            return
        }
        val plan = splitScreenPlan(viewModel.foregroundPackage.value, homePackageNames(), packageName)
        viewModel.collapse()
        when (plan) {
            SplitPlan.PairWithForeground -> {
                // The open app goes to one half first
                if (!performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)) {
                    Toast.makeText(service, getString(R.string.shortcut_split_unavailable), Toast.LENGTH_SHORT).show()
                    return
                }
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                windowHandler.postDelayed({
                    runCatchingLogged(TAG, "Failed to launch app next to the open one") { startActivity(launchIntent) }
                }, SPLIT_LAUNCH_DELAY_MS)
            }
            SplitPlan.LaunchThenSystemPicker -> {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatchingLogged(TAG, "Failed to launch app for split screen") {
                    startActivity(launchIntent)
                }
                windowHandler.postDelayed({
                    if (!performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)) {
                        Toast.makeText(service, getString(R.string.shortcut_split_unavailable), Toast.LENGTH_SHORT)
                            .show()
                    }
                }, SPLIT_PICKER_DELAY_MS)
            }
        }
        notificationRepository.removeNotificationsForPackage(packageName)
    }

    private fun homePackageNames(): Set<String> = runCatchingLogged(TAG, "Failed to list home apps") {
        packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
            .toSet()
    } ?: emptySet()

    fun openCurrentNotificationInFloatingWindow() {
        val route = floatingWindowRoute(
            viewModel.settings.value.floatingWindowEnabled,
            viewModel.settings.value.floatingWindowMode
        )
        if (route == FloatingWindowRoute.Off) return
        // The card on screen: selectedIndex indexes the island's own list
        val notification = viewModel.currentShownNotification()
        if (notification != null) {
            val launched = when (route) {
                FloatingWindowRoute.Lmo -> openInLmoFreeform(notification)
                else -> openInSystemFreeform(notification)
            }
            // Only an ordinary alert is consumed by opening it
            if (launched && notification.mode == IslandMode.Notification) {
                notificationRepository.removeNotification(notification.key)
                notificationRepository.sendCommand(SmartIslandCommand.CancelNotification(notification.key))
            }
        }
        viewModel.collapse()
    }

    private fun systemFreeformBundle(): Bundle {
        val options = ActivityOptions.makeBasic()
        runCatchingLogged(TAG, "Failed to set launch bounds") {
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val w = (screenWidth * 0.90f).toInt()
            val h = (screenHeight * 0.65f).toInt()
            val left = (screenWidth - w) / 2
            val top = (screenHeight - h) / 2
            options.setLaunchBounds(Rect(left, top, left + w, top + h))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatchingLogged(TAG, "Failed to set background activity start mode") {
                options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }
        }
        val bundle = options.toBundle() ?: Bundle()
        bundle.putInt("android.activity.windowingMode", WINDOWING_MODE_FREEFORM)
        return bundle
    }

    private fun openInSystemFreeform(notification: IslandNotification): Boolean {
        val bundle = systemFreeformBundle()

        val fillInIntent = Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        var started = false
        if (notification.contentIntent != null) {
            runCatchingLogged(TAG, "Failed to send content intent") {
                notification.contentIntent.send(service, 0, fillInIntent, null, null, null, bundle)
                started = true
            }
        } else {
            runCatchingLogged(TAG, "Failed to launch package activity") {
                val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    startActivity(launchIntent, bundle)
                    started = true
                } else {
                    Toast.makeText(service, "Can't open ${notification.appName}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return started
    }

    private fun openInLmoFreeform(notification: IslandNotification): Boolean {
        val lmo = LmoFreeform
        if (!lmo.isInstalled(service)) {
            Toast.makeText(service, "LMO Freeform is not installed on this device.", Toast.LENGTH_SHORT).show()
            return false
        }
        val pending = notification.contentIntent
        if (pending == null || !ShizukuManager.hasPermission()) return openInLmoFallback(notification)
        serviceScope.launch {
            // preferred: open the chat straight on LMO's own display
            if (openInLmoDisplay(notification, pending)) return@launch
            val beforeOut = ShizukuManager.runShellForOutput(lmo.LIST_TASKS_COMMAND).getOrDefault("")
            val beforeTasks = lmo.parseTasks(beforeOut)
            val before = beforeTasks.map { it.taskId }.toSet()
            val sent = runCatchingLogged(TAG, "Failed to open the notification's chat") {
                val fill = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ActivityOptions.makeBasic().apply {
                        setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    }.toBundle()
                } else null
                pending.send(service, 0, fill, null, null, null, options)
            } != null
            if (!sent) {
                openInLmoFallback(notification)
                return@launch
            }
            // The chat is on screen full size from here until LMO takes the task
            var task: LmoFreeform.TaskInfo? = null
            var pollsUsed = 0
            val pollDeadline = SystemClock.elapsedRealtime() + LMO_TASK_POLL_WINDOW_MS
            while (task == null && (pollsUsed == 0 || SystemClock.elapsedRealtime() < pollDeadline)) {
                pollsUsed++
                val after = lmo.parseTasks(
                    ShizukuManager.runShellForOutput(lmo.LIST_TASKS_COMMAND).getOrDefault("")
                )
                task = lmo.pickTask(after, before, notification.packageName)
                    ?.takeIf { it.taskId !in before }
                if (task == null) delay(LMO_TASK_POLL_MS)
            }
            // No new task appeared: the app reused one it had, so take its front-most task
            if (task == null) {
                val reusedOut = ShizukuManager.runShellForOutput(lmo.LIST_TASKS_COMMAND).getOrDefault("")
                task = lmo.pickTask(lmo.parseTasks(reusedOut), before, notification.packageName)
            }
            val found = task
            if (found == null) {
                AppLogRecorder.w(TAG, "LMO: no task for ${notification.packageName}, launching it in LMO instead")
                openInLmoFallback(notification)
            } else {
                runCatchingLogged(TAG, "Failed to ask LMO Freeform to move task ${found.taskId}") {
                    sendBroadcast(lmo.startIntent(found.component, found.userId, found.taskId))
                }
            }
        }
        return true
    }

    private suspend fun openInLmoDisplay(notification: IslandNotification, pending: PendingIntent): Boolean {
        val lmo = LmoFreeform
        val shizuku = ShizukuManager
        val pkg = notification.packageName
        val component = lmo.pendingIntentActivity(pending) ?: lmo.launcherComponent(packageManager, pkg)
        if (component == null) {
            return false
        }
        val knownDisplays = lmo.parseDisplayIds(shizuku.runShellForOutput(lmo.LIST_DISPLAYS_COMMAND).getOrDefault(""),
            pkg)
        val before = lmo.parseTasks(shizuku.runShellForOutput(lmo.LIST_TASKS_COMMAND).getOrDefault(""))
            .map { it.taskId }.toSet()
        runCatchingLogged(TAG, "Failed to ask LMO Freeform for a window") {
            sendBroadcast(lmo.startIntent(component))
        } ?: return false
        var displayId: Int? = null
        val deadline = SystemClock.elapsedRealtime() + LMO_DISPLAY_WAIT_MS
        while (displayId == null && SystemClock.elapsedRealtime() < deadline) {
            val ids = lmo.parseDisplayIds(shizuku.runShellForOutput(lmo.LIST_DISPLAYS_COMMAND).getOrDefault(""), pkg)
            displayId = (ids - knownDisplays).maxOrNull()
            if (displayId == null) delay(LMO_TASK_POLL_MS)
        }
        if (displayId == null) {
            return false
        }
        val options = ActivityOptions.makeBasic().apply {
            setLaunchDisplayId(displayId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }
        }.toBundle()
        val sent = runCatchingLogged(TAG, "Failed to send the chat to LMO's display") {
            pending.send(service, 0, Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), null, null, null, options)
        } != null
        if (!sent) {
            return true
        }
        // Did Android honour the display?
        delay(500)
        val out = shizuku.runShellForOutput(lmo.LIST_TASKS_COMMAND).getOrDefault("")
        val displays = lmo.taskDisplays(out)
        val tasks = lmo.parseTasks(out).filter { it.packageName == pkg }
        val misplaced = tasks.filter { it.taskId !in before && displays[it.taskId] == 0 }.maxByOrNull { it.taskId }
        if (misplaced != null) {
            runCatchingLogged(TAG, "Failed to move task ${misplaced.taskId}") {
                sendBroadcast(lmo.startIntent(misplaced.component, misplaced.userId, misplaced.taskId))
            }
        }
        return true
    }

    private fun openInLmoFallback(notification: IslandNotification): Boolean {
        val lmo = LmoFreeform
        val component = notification.contentIntent?.let { lmo.pendingIntentActivity(it) }
            ?: lmo.launcherComponent(packageManager, notification.packageName)
        if (component == null) {
            Toast.makeText(service, "Can't open ${notification.appName}", Toast.LENGTH_SHORT).show()
            return false
        }
        return runCatchingLogged(TAG, "Failed to ask LMO Freeform for a window") {
            sendBroadcast(lmo.startIntent(component))
        } != null
    }

    private companion object {
        const val TAG = "SmartIslandOverlayService"
        const val LMO_TASK_POLL_WINDOW_MS = 3_000L
        const val LMO_TASK_POLL_MS = 30L
        const val LMO_DISPLAY_WAIT_MS = 2_000L
        const val WINDOWING_MODE_FREEFORM = 5
        const val SPLIT_LAUNCH_DELAY_MS = 450L
        const val SPLIT_PICKER_DELAY_MS = 600L
    }
}
