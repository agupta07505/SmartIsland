/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.CutoutTapIntent
import com.agupta07505.smartisland.model.ShellSwap
import com.agupta07505.smartisland.model.TapIntent
import com.agupta07505.smartisland.model.TapZone
import kotlinx.coroutines.CoroutineScope

/** Runs the intent of a tap on the camera band or cutout and returns it so a caller can carry on with the rest. */
internal fun routeIslandTap(
    ui: IslandInteractionState,
    wiring: IslandWiring,
    scope: CoroutineScope,
    zone: TapZone
): TapIntent {
    val phase = ui.islandPhase(wiring.view.expanded)
    val intent = CutoutTapIntent.resolve(phase, zone, wiring.view.notifications.isNotEmpty())
    when (intent) {
        TapIntent.OpenIsland, TapIntent.CloseIsland ->
            CutoutTapIntent.targetExpanded(intent)?.let { wiring.actions.expand.setExpanded(it) }
        TapIntent.PutAway -> wiring.actions.idle.dotTap()
        TapIntent.SwapFlip -> tapDuringSwap(ui, scope, ShellSwap.Intent.Flip, true)
        TapIntent.SelectCutout, TapIntent.OpenApp, TapIntent.None -> Unit
    }
    return intent
}

/**
 * A tap on a cutout while the island grows or shrinks turns the move around (no event picked, no app opened). Returns
 * true when it did, so the caller's usual select-and-toggle does not run on top
 */
internal fun turnAroundOnCutoutTap(
    ui: IslandInteractionState,
    wiring: IslandWiring,
    index: Int,
    onPageSelected: (Int) -> Unit
): Boolean {
    val phase = ui.islandPhase(wiring.view.expanded)
    if (!CutoutTapIntent.isTransition(phase)) return false
    val intent = CutoutTapIntent.resolve(phase, TapZone.Cutout, wiring.view.notifications.isNotEmpty())
    if (intent == TapIntent.OpenIsland && index >= 0) onPageSelected(index)
    CutoutTapIntent.targetExpanded(intent)?.let { wiring.actions.expand.setExpanded(it) }
    return true
}
