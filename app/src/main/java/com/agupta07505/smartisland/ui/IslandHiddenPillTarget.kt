/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings

/**
 * Invisible touch target over the pill's spot while it is hidden: a tap reveals the pill (auto-hide), brings back
 * put-away idle cards, or opens the shortcuts
 */
@Composable
internal fun HiddenPillTouchTarget(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    wiring: IslandWiring,
    ui: IslandInteractionState
) {
    if (layout.isHiding && !wiring.view.expanded) {
        Box(
            modifier = Modifier
                .width(settings.width.dp)
                .height(settings.height.dp)
                .graphicsLayer {
                    translationX = layout.collapsedMainOffset.toPx()
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (settings.autoHidePill && ui.isAutoHidden) {
                            ui.isAutoHidden = false
                            ui.touch()
                        } else if (wiring.view.notifications.isEmpty() && wiring.view.hasIdleStoredCards) {
                            wiring.actions.idle.revealCards()
                        } else if (wiring.view.notifications.isNotEmpty()) {
                            wiring.actions.expand.toggle()
                        }
                    }
                }
        )
    }
}
