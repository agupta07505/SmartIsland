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
import androidx.compose.ui.Modifier
import com.agupta07505.smartisland.model.IslandNotification

@Composable
fun OverlayIsland(
    viewModel: IslandViewModel,
    statusBarHeight: Float,
    onOpenNotification: (IslandNotification) -> Unit,
    onOpenFloatingWindow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val expanded by viewModel.expanded.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()

    IslandOverlayView(
        settings = settings,
        expanded = expanded,
        notifications = notifications,
        selectedIndex = selectedIndex,
        onPageSelected = { index -> viewModel.setSelectedNotificationIndex(index) },
        onOpenNotification = onOpenNotification,
        onToggleExpanded = { viewModel.toggleExpanded() },
        onDismissNotification = { viewModel.dismissCurrentNotification() },
        onOpenFloatingWindow = onOpenFloatingWindow,
        statusBarHeight = statusBarHeight,
        modifier = modifier
    )
}
