/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalContext
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import com.agupta07505.smartisland.util.SystemAnimScale
import kotlin.coroutines.coroutineContext

/** Expand / collapse buzz, follows the real state change whatever caused it (tap, swipe, a card opening itself). */
@Composable
internal fun ExpandCollapseHapticEffect(expanded: Boolean, wiring: IslandWiring, launcherOpen: Boolean = false) {
    val hapticContext = LocalContext.current
    var hapticExpandedSeen by remember { mutableStateOf(expanded) }
    LaunchedEffect(expanded) {
        if (expanded != hapticExpandedSeen) {
            hapticExpandedSeen = expanded
            // the app-shortcuts launcher opens silently (it is not a card expanding)
            if (expanded && launcherOpen) return@LaunchedEffect
            IslandHaptics.play(
                hapticContext,
                HapticStrength.fromId(wiring.view.settings.hapticStrength),
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
        val carried = coroutineContext[MotionDurationScale]
        SystemAnimScale.composeAppliesScale = carried != null
    }
}

@Composable
internal fun MergeTouchHapticEffect(layout: IslandLayout, secondary: IslandSecondary, wiring: IslandWiring) {
    val hapticContext = LocalContext.current
    // light tick when the merging cutouts first touch, follows the haptic strength setting
    val mergeSecondary by rememberUpdatedState(secondary)
    val mergeLayout by rememberUpdatedState(layout)
    LaunchedEffect(Unit) {
        var previous = 0f
        snapshotFlow { mergeSecondary.merge.anim.value }.collect { now ->
            val contact = mergeContactFraction(
                (mergeLayout.animatedXOffset - mergeSecondary.offsets.state.value).value,
                mergeLayout.safeWidth.value,
                mergeSecondary.anims.bubbleWidth.value.value
            )
            if (!wiring.view.expanded && !SystemAnimScale.animationsOff() &&
                mergeTouchCrossed(previous, now, contact)
            ) {
                IslandHaptics.play(hapticContext, HapticStrength.fromId(wiring.view.settings.hapticStrength),
                    HapticKind.Merge)
            }
            previous = now
        }
    }
}
