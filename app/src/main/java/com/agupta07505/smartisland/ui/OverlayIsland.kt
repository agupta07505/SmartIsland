/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.util.CameraAnchor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.agupta07505.smartisland.data.AppShortcutProvider
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.model.IslandNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OverlayIsland(
    viewModel: IslandViewModel,
    statusBarHeight: Float,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onOpenFloatingWindow: () -> Unit,
    onOpenNotificationShade: () -> Unit = {},
    modifier: Modifier = Modifier,
    isFullWidth: Boolean = true,
    cameraAnchor: CameraAnchor? = null
) {
    val settings by viewModel.settings.collectAsState()
    val expanded by viewModel.expanded.collectAsState()
    val notifications by viewModel.visibleNotifications.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val isInputActive by viewModel.isInputActive.collectAsState()
    val context = LocalContext.current

    val isContentRedacted = isLocked && settings.lockScreenPrivacy == "AppIconOnly"
    val processedNotifications = remember(notifications, isContentRedacted) {
        if (isContentRedacted) {
            notifications.map { notif ->
                if (notif.mode == com.agupta07505.smartisland.model.IslandMode.Notification) {
                    notif.copy(
                        title = notif.appName,
                        text = "Contents hidden",
                        actionIntents = emptyList()
                    )
                } else {
                    notif
                }
            }
        } else {
            notifications
        }
    }

    val selectedApps = remember(settings.shortcutPackages, settings.enableAppShortcuts) {
        if (settings.enableAppShortcuts) {
            AppShortcutProvider.selectedApps(context, settings.shortcutPackages)
        } else {
            emptyList()
        }
    }
    val launcherApps by produceState<List<LaunchableApp>?>(
        initialValue = when {
            !settings.enableAppShortcuts -> emptyList()
            selectedApps.isNotEmpty() -> selectedApps
            settings.shortcutPackages.isEmpty() && !settings.showRecentApps -> emptyList()
            !AppShortcutProvider.hasUsageAccess(context) -> emptyList()
            else -> null
        },
        settings.enableAppShortcuts,
        settings.shortcutPackages,
        settings.showRecentApps
    ) {
        value = if (!settings.enableAppShortcuts) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) {
                AppShortcutProvider.shortcuts(
                    context = context,
                    selectedPackages = settings.shortcutPackages,
                    includeRecent = settings.showRecentApps
                )
            }
        }
    }

    IslandOverlayView(
        settings = settings,
        expanded = expanded,
        notifications = processedNotifications,
        selectedIndex = selectedIndex,
        launcherApps = launcherApps,
        onPageSelected = { index -> viewModel.setSelectedNotificationIndex(index) },
        onOpenNotification = onOpenNotification,
        onLaunchApp = onLaunchApp,
        onToggleExpanded = { viewModel.toggleExpanded() },
        onDismissNotification = { viewModel.dismissCurrentNotification() },
        onDismissAllNotifications = { viewModel.dismissAllNotifications() },
        onOpenFloatingWindow = onOpenFloatingWindow,
        onOpenNotificationShade = onOpenNotificationShade,
        statusBarHeight = statusBarHeight,
        isInputActive = isInputActive,
        onReplyStateChanged = { viewModel.setInputActive(it) },
        isFullWidth = isFullWidth,
        cameraAnchor = cameraAnchor,
        modifier = modifier
    )
}
