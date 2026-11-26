/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.PickerSwap
import com.agupta07505.smartisland.model.ShellStateGuard
import com.agupta07505.smartisland.model.SwapCoordinator
import com.agupta07505.smartisland.model.siblingIndices
import com.agupta07505.smartisland.ui.expanded.LocalMediaExpansion
import com.agupta07505.smartisland.ui.expanded.LocalStepDrag
import com.agupta07505.smartisland.ui.expanded.StepDragState
import com.agupta07505.smartisland.ui.expanded.pictureShownBesideCard
import com.agupta07505.smartisland.util.SwapTrace
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

internal class SideBySide(
    val now: Boolean,
    val hold: State<Boolean>,
    val holdMs: Int,
    val stepDrag: StepDragState,
    val forceSingle: MutableState<Boolean>,
    val active: Boolean,
    val twoIslandsDrawn: Boolean
)

@Composable
internal fun rememberSideBySide(
    ui: IslandInteractionState,
    settings: SmartIslandSettings,
    isLandscape: Boolean,
    expanded: Boolean,
    siblingPickerOpen: Boolean,
    notifications: List<IslandNotification>,
    safeIndex: Int
): SideBySide {
    // landscape: an open sibling picker sits beside the card as a SEPARATE island
    val openPicture = LocalMediaExpansion.current
    val pictureBeside = expanded && pictureShownBesideCard(
        isLandscape = isLandscape,
        openKey = openPicture.key,
        openIndex = openPicture.index,
        cardKey = notifications.getOrNull(safeIndex)?.key,
        cardMediaCount = notifications.getOrNull(safeIndex)?.media?.size ?: 0,
        selectorOpen = openPicture.selectorOpen
    )
    val pickerSideBySideNow = pictureBeside || (isLandscape && expanded && siblingPickerOpen &&
        siblingIndices(notifications, safeIndex).isNotEmpty())
    // landscape: when the picker closes, the two-island layout
    val holdState = remember { mutableStateOf(false) }
    var sideBySideHold by holdState
    val sideBySideHoldMs = settings.animationSpeed(isLandscape).durationMs(PickerSwap.CLOSE_MS)
    // a picture island is held like the picker's
    val stepDragForHold = LocalStepDrag.current
    LaunchedEffect(pickerSideBySideNow) {
        if (pickerSideBySideNow) {
            sideBySideHold = true
        } else if (sideBySideHold) {
            snapshotFlow { stepDragForHold.active }.first { !it }
            withTimeoutOrNull(sideBySideHoldMs * 3L + 400L) {
                snapshotFlow { stepDragForHold.splitShown }.first { !it }
            }
            delay(SIDE_BY_SIDE_HOLD_MARGIN_MS)
            sideBySideHold = false
        }
    }
    val pickerSideBySideRaw = pickerSideBySideNow || (sideBySideHold && isLandscape && expanded
        && stepDragForHold.splitShown)
    // Set by the watchdog below when the flag was still on with nothing open
    val forceSingleState = remember { mutableStateOf(false) }
    var forceSingle by forceSingleState
    LaunchedEffect(pickerSideBySideNow, expanded) { forceSingle = false }
    val pickerSideBySide = pickerSideBySideRaw && !forceSingle
    // A running swap keeps the flags it began with, so its shell, clip and surface do not change under it
    val live = SwapCoordinator.Flags(
        tapSwapAllowed = !pickerSideBySide,
        twoIslandsDrawn = ShellStateGuard.twoIslandsDrawn(pickerSideBySide, stepDragForHold.splitShown)
    )
    val flags = ui.swap.flags(live, ui.swapBusy)
    // A cutout tap swaps with 2 shells trading places (not while 2 islands are drawn side by side)
    SideEffect { ui.tapSwapAllowed = flags.tapSwapAllowed }
    LaunchedEffect(isLandscape) { ui.endTapSwap("orientation") }
    // What the shell's clip, shadow and surface follow
    val twoIslandsDrawn = flags.twoIslandsDrawn
    SideEffect {
        val seen = "$pickerSideBySide $live $flags"
        if (SwapTrace.enabled() && ui.swapTraceFlags != seen) {
            ui.swapTraceFlags = seen
            SwapTrace.emit("flags", "sideBySide=$pickerSideBySide live=$live used=$flags " +
                "swapActive=${ui.tapSwapActive} landscape=$isLandscape")
        }
    }
    return SideBySide(
        now = pickerSideBySideNow,
        hold = holdState,
        holdMs = sideBySideHoldMs,
        stepDrag = stepDragForHold,
        forceSingle = forceSingleState,
        active = pickerSideBySide,
        twoIslandsDrawn = twoIslandsDrawn
    )
}

internal class SplitWidth(
    val cardWidthDp: MutableFloatState,
    val cache: HashMap<String, Float>,
    val effective: Float
)

@Composable
internal fun rememberSplitWidth(pickerSideBySide: Boolean, activeKey: String?): SplitWidth {
    val cardWidth = remember { mutableFloatStateOf(0f) }
    var splitCardWidthDp by cardWidth
    LaunchedEffect(pickerSideBySide) { if (!pickerSideBySide) splitCardWidthDp = 0f }
    val splitWidthCache = remember { HashMap<String, Float>() }
    val splitWidthSeed = activeKey?.let { splitWidthCache[it] } ?: 0f
    val splitWidthEffective = if (splitCardWidthDp > 0f) splitCardWidthDp else splitWidthSeed
    return SplitWidth(cardWidth, splitWidthCache, splitWidthEffective)
}

internal class CutoutCards(val notifications: List<IslandNotification>, val index: Int)

@Composable
internal fun rememberCutoutCards(
    holdCutouts: Boolean,
    notifications: List<IslandNotification>,
    safeIndex: Int
): CutoutCards {
    val cutoutSnapshot = remember { arrayOfNulls<Pair<List<IslandNotification>, Int>>(1) }
    val heldCutouts = cutoutSnapshot[0]
    if (!holdCutouts || heldCutouts == null) cutoutSnapshot[0] = notifications to safeIndex
    val cutoutNotifications = if (holdCutouts && heldCutouts != null) heldCutouts.first else notifications
    val cutoutIndex = if (holdCutouts && heldCutouts != null) heldCutouts.second else safeIndex
    return CutoutCards(cutoutNotifications, cutoutIndex)
}

private const val SIDE_BY_SIDE_HOLD_MARGIN_MS = 0L

internal const val SPLIT_WIDTH_CACHE_MAX = 32
internal const val SPLIT_WIDTH_SAME_DP = 0.5f
