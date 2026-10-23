/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandNotification

@Composable
fun ScreenRecordingExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onCollapse: () -> Unit,
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    RecorderCard(
        notification = notification,
        bottomPadding = bottomPadding,
        onCollapse = onCollapse,
        accent = Color(settings.screenRecordingColor),
        defaultTitle = "Screen recording",
        statusLabel = "Screen recording",
        leadingIcon = null
    )
}
