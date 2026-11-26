/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.ShellStateGuard
import com.agupta07505.smartisland.model.appGroupPageOf
import com.agupta07505.smartisland.util.AppLogRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

@NonRestartableComposable
@Composable
internal fun BubbleOriginEffects(
    ui: IslandInteractionState,
    layoutState: State<IslandLayout>,
    expandedNowState: State<Boolean>,
    isLandscape: Boolean
) {
    val layoutForSurface by layoutState
    SideEffect { ui.expandProgressNow = { layoutForSurface.expandProgress } }
    LaunchedEffect(Unit) {
        snapshotFlow { layoutForSurface.expandProgress to expandedNowState.value }
            .collect { (progress, open) -> ui.settleBubbleOrigin(open, progress) }
    }
    LaunchedEffect(isLandscape) { ui.clearBubbleOrigin() }
}

@NonRestartableComposable
@Composable
internal fun ShellStateEffects(
    ui: IslandInteractionState,
    wiring: IslandWiring,
    layoutState: State<IslandLayout>,
    sideBySide: SideBySide,
    surfaceBack: Boolean,
    expanded: Boolean,
    notifications: List<IslandNotification>,
    safeIndex: Int,
    selectedIndex: Int
) {
    val layoutForSurface by layoutState
    val pickerSideBySide = sideBySide.active
    val pickerSideBySideNow = sideBySide.now
    val twoIslandsDrawn = sideBySide.twoIslandsDrawn
    val stepDragForHold = sideBySide.stepDrag
    val sideBySideHold by sideBySide.hold
    val sideBySideHoldMs = sideBySide.holdMs
    var forceSingle by sideBySide.forceSingle
    LaunchedEffect(pickerSideBySideNow, twoIslandsDrawn, surfaceBack) {
        if (!pickerSideBySideNow && !twoIslandsDrawn && surfaceBack) {
            withFrameNanos { }
            withFrameNanos { }
            stepDragForHold.shellSingleReady = true
        }
    }
    // Watchdog
    val notificationsNow by rememberUpdatedState(notifications)
    val safeIndexNow by rememberUpdatedState(safeIndex)
    val selectedIndexNow by rememberUpdatedState(selectedIndex)
    LaunchedEffect(expanded, pickerSideBySide, pickerSideBySideNow, twoIslandsDrawn, sideBySideHold) {
        fun reasonNow(): String? = ShellStateGuard.stuckReason(
            expanded = expanded && layoutForSurface.expandedShown,
            atRest = shellAtRest(ui),
            overlayFlag = pickerSideBySide,
            splitWanted = pickerSideBySideNow,
            twoIslandsDrawn = twoIslandsDrawn,
            singleSurfaceAlpha = layoutForSurface.singleSurfaceAlpha,
            radiusDp = layoutForSurface.safeRadius.value
        )
        snapshotFlow { reasonNow() }.collectLatest { first ->
            if (first == null) return@collectLatest
            delay(ShellStateGuard.waitMs(sideBySideHold, sideBySideHoldMs))
            val reason = reasonNow() ?: return@collectLatest
            AppLogRecorder.w(
                "ShellState",
                "Shell state stuck: reason=$reason pickerSideBySide=$pickerSideBySide now=$pickerSideBySideNow hold=$sideBySideHold " +
                    "splitShown=${stepDragForHold.splitShown} radius=${layoutForSurface.safeRadius.value} " +
                    "singleSurfaceAlpha=${layoutForSurface.singleSurfaceAlpha} cardPop=${ui.cardPop.value} cardDragX=${ui.cardDragX} " +
                    "card=${notificationsNow.getOrNull(safeIndexNow)?.key} " +
                    "page=${appGroupPageOf(notificationsNow, safeIndexNow)} selected=$selectedIndexNow"
            )
            if (ShellStateGuard.canForceSingle(reason)) forceSingle = true
        }
    }
    // No single expanded island (collapsed, or the two landscape islands)
    LaunchedEffect(expanded, pickerSideBySide) {
        if (!expanded || pickerSideBySide) wiring.actions.expand.reportSurfaceBounds(null)
    }
}

@NonRestartableComposable
@Composable
internal fun SecondaryApartEffect(
    secondary: IslandSecondary,
    ui: IslandInteractionState,
    wiring: IslandWiring,
    onSecondaryApartChange: (Boolean) -> Unit
) {
    val onSecondaryApartNow = rememberUpdatedState(onSecondaryApartChange)
    val secondaryNow = rememberUpdatedState(secondary)
    LaunchedEffect(Unit) {
        snapshotFlow {
            val sec = secondaryNow.value
            !wiring.view.expanded && sec.anims.secondaryAlpha.value > 0.5f && sec.merge.anim.value < 0.1f
                || ui.edgeBubbleApart
        }.collect { onSecondaryApartNow.value(it) }
    }
}
