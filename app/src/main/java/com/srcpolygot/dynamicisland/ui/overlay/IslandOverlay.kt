/*
 * Realistic Dynamic Island Fork
 * Main overlay composable – orchestrates collapsed / expanded states
 */

package com.srcpolygot.dynamicisland.ui.overlay

import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.srcpolygot.dynamicisland.domain.model.ActiveIslandState

/**
 * Top-level Dynamic Island overlay.
 * Handles the transition between the black collapsed pill (covers punch-hole)
 * and the expanded rich card.
 */
@Composable
fun IslandOverlay(
    state: ActiveIslandState,
    widthDp: Float = 120f,
    heightDp: Float = 38f,
    cornerRadiusDp: Float = 20f,
    onToggleExpanded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(
        targetState = state.isExpanded,
        label = "islandExpand"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (state.isExpanded) {
            IslandExpandedContent(
                state = state,
                modifier = Modifier
            )
        } else {
            IslandCollapsedContent(
                state = state,
                widthDp = widthDp,
                heightDp = heightDp,
                cornerRadiusDp = cornerRadiusDp
            )
        }
    }
}
