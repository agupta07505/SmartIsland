/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

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
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val expanded by viewModel.expanded.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val context = LocalContext.current
    val selectedApps = remember(settings.shortcutPackages) {
        AppShortcutProvider.selectedApps(context, settings.shortcutPackages)
    }
    val launcherApps by produceState<List<LaunchableApp>?>(
        initialValue = when {
            selectedApps.isNotEmpty() -> selectedApps
            settings.shortcutPackages.isEmpty() && !settings.showRecentApps -> emptyList()
            !AppShortcutProvider.hasUsageAccess(context) -> emptyList()
            else -> null
        },
        settings.shortcutPackages,
        settings.showRecentApps
    ) {
        value = withContext(Dispatchers.IO) {
            AppShortcutProvider.shortcuts(
                context = context,
                selectedPackages = settings.shortcutPackages,
                includeRecent = settings.showRecentApps
            )
        }
    }

    IslandOverlayView(
        settings = settings,
        expanded = expanded,
        notifications = notifications,
        selectedIndex = selectedIndex,
        launcherApps = launcherApps,
        onPageSelected = { index -> viewModel.setSelectedNotificationIndex(index) },
        onOpenNotification = onOpenNotification,
        onLaunchApp = onLaunchApp,
        onToggleExpanded = { viewModel.toggleExpanded() },
        onDismissNotification = { viewModel.dismissCurrentNotification() },
        onOpenFloatingWindow = onOpenFloatingWindow,
        statusBarHeight = statusBarHeight,
        modifier = modifier
    )
}
