/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.IslandMode

/** The height the expanded card is (or is expected to be). */
@Composable
internal fun rememberExpandedHeight(
    activeMode: IslandMode,
    noNotifications: Boolean,
    expanded: Boolean,
    prewarmWanted: Boolean
): MutableState<Dp> {
    val initialEstimatedHeight = remember(activeMode, noNotifications) {
        if (noNotifications) 135.dp else defaultEstimatedHeightForMode(activeMode)
    }
    val expandedHeight = remember { mutableStateOf(initialEstimatedHeight) }
    LaunchedEffect(activeMode, noNotifications, prewarmWanted) {
        if (!expanded && !prewarmWanted) {
            expandedHeight.value = if (noNotifications) 135.dp else defaultEstimatedHeightForMode(activeMode)
        }
    }
    return expandedHeight
}
