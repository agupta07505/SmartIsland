/*
 * Realistic Dynamic Island Fork
 * Expanded state – dispatches to mode-specific cards
 */

package com.srcpolygot.dynamicisland.ui.overlay

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.srcpolygot.dynamicisland.domain.model.ActiveIslandState
import com.srcpolygot.dynamicisland.domain.model.IslandMode
import com.srcpolygot.dynamicisland.ui.expanded.CallExpanded
import com.srcpolygot.dynamicisland.ui.expanded.MusicExpanded
import com.srcpolygot.dynamicisland.ui.expanded.TimerExpanded

/**
 * Expanded Dynamic Island card.
 * Routes to the correct mode-specific UI (Music, Call, Timer, …).
 */
@Composable
fun IslandExpandedContent(
    state: ActiveIslandState,
    modifier: Modifier = Modifier
) {
    val primary = state.primary ?: return

    when (primary.mode) {
        IslandMode.Music -> {
            MusicExpanded(
                notification = primary,
                modifier = modifier.fillMaxWidth()
            )
        }
        IslandMode.IncomingCall -> {
            CallExpanded(
                notification = primary,
                isIncoming = true,
                modifier = modifier.fillMaxWidth()
            )
        }
        IslandMode.OngoingCall -> {
            CallExpanded(
                notification = primary,
                isIncoming = false,
                modifier = modifier.fillMaxWidth()
            )
        }
        IslandMode.Timer -> {
            TimerExpanded(
                notification = primary,
                isStopwatch = false,
                modifier = modifier.fillMaxWidth()
            )
        }
        IslandMode.Stopwatch -> {
            TimerExpanded(
                notification = primary,
                isStopwatch = true,
                modifier = modifier.fillMaxWidth()
            )
        }
        else -> {
            // Fallback generic card for remaining modes
            GenericExpandedFallback(primary = primary, modifier = modifier)
        }
    }
}

@Composable
private fun GenericExpandedFallback(
    primary: com.srcpolygot.dynamicisland.domain.model.IslandNotification,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                Modifier
            )
    ) {
        androidx.compose.material3.Text(
            text = primary.title ?: primary.mode.name,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}
