/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Expand / collapse buzz. It follows the real state change, whatever caused it (tap, swipe, a card opening itself). */
@Composable
internal fun ExpandCollapseHapticEffect(expanded: Boolean, cb: IslandCallbacks, launcherOpen: Boolean = false) {
    val hapticContext = LocalContext.current
    var hapticExpandedSeen by remember { mutableStateOf(expanded) }
    LaunchedEffect(expanded) {
        if (expanded != hapticExpandedSeen) {
            hapticExpandedSeen = expanded
            // The app-shortcuts launcher opens silently (it is not a card expanding).
            if (expanded && launcherOpen) return@LaunchedEffect
            IslandHaptics.play(
                hapticContext,
                HapticStrength.fromId(cb.currentSettings.hapticStrength),
                if (expanded) HapticKind.Expand else HapticKind.Collapse
            )
        }
    }
}

/** Finds out once whether Compose already scales its animations by the system animator scale. */
@Composable
internal fun AnimScaleProbeEffect() {
    // Does Compose already carry the system animator scale into its animations?
    LaunchedEffect(Unit) {
        val carried = kotlin.coroutines.coroutineContext[androidx.compose.ui.MotionDurationScale]
        com.agupta07505.smartisland.util.SystemAnimScale.composeAppliesScale = carried != null
        com.agupta07505.smartisland.util.AppLogRecorder.i(
            "AnimScale",
            "system=${com.agupta07505.smartisland.util.SystemAnimScale.timeScale()} composeApplies=${carried != null} factor=${carried?.scaleFactor}"
        )
    }
}

@Composable
internal fun MergeTouchHapticEffect(layout: IslandLayout, secondary: IslandSecondary, cb: IslandCallbacks) {
    val hapticContext = LocalContext.current
    // Light tick when the merging cutouts touch (first contact while merging in); follows the haptic strength setting.
    val mergeSecondary by rememberUpdatedState(secondary)
    val mergeLayout by rememberUpdatedState(layout)
    LaunchedEffect(Unit) {
        var previous = 0f
        snapshotFlow { mergeSecondary.secondaryMergeAnim.value }.collect { now ->
            val contact = mergeContactFraction(
                (mergeLayout.animatedXOffset - mergeSecondary.secondaryOffset).value,
                mergeLayout.safeWidth.value,
                mergeSecondary.secondaryBubbleWidth.value
            )
            if (!cb.currentExpanded && !com.agupta07505.smartisland.util.SystemAnimScale.animationsOff() &&
                mergeTouchCrossed(previous, now, contact)
            ) {
                IslandHaptics.play(hapticContext, HapticStrength.fromId(cb.currentSettings.hapticStrength), HapticKind.Merge)
            }
            previous = now
        }
    }
}
