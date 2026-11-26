/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class IslandPhase { Collapsed, Expanding, Expanded, Collapsing, Swapping }

enum class TapZone { Cutout, CameraBand, Card }

enum class TapIntent {
    OpenIsland,
    CloseIsland,
    PutAway,
    SwapFlip,
    SelectCutout,
    OpenApp,
    None
}

object CutoutTapIntent {
    const val SETTLED_PROGRESS = 0.92f

    const val REST_PROGRESS = 0.02f

    const val CARD_VISIBLE_PROGRESS = 0.5f

    fun phase(expanded: Boolean, progress: Float, swapActive: Boolean): IslandPhase = when {
        expanded && swapActive -> IslandPhase.Swapping
        expanded -> if (progress >= SETTLED_PROGRESS) IslandPhase.Expanded else IslandPhase.Expanding
        else -> if (progress <= REST_PROGRESS) IslandPhase.Collapsed else IslandPhase.Collapsing
    }

    fun isTransition(phase: IslandPhase): Boolean = phase == IslandPhase.Expanding || phase == IslandPhase.Collapsing

    fun resolve(phase: IslandPhase, zone: TapZone, hasCards: Boolean): TapIntent = when (phase) {
        IslandPhase.Swapping -> TapIntent.SwapFlip
        IslandPhase.Expanding -> TapIntent.CloseIsland
        IslandPhase.Collapsing -> if (hasCards) TapIntent.OpenIsland else TapIntent.None
        IslandPhase.Expanded -> when (zone) {
            TapZone.Card -> TapIntent.OpenApp
            TapZone.CameraBand -> TapIntent.PutAway
            TapZone.Cutout -> TapIntent.SelectCutout
        }
        IslandPhase.Collapsed -> when (zone) {
            TapZone.CameraBand -> TapIntent.PutAway
            TapZone.Cutout -> TapIntent.SelectCutout
            TapZone.Card -> if (hasCards) TapIntent.OpenIsland else TapIntent.None
        }
    }

    fun reversesOnShellTouch(phase: IslandPhase, progress: Float, yPx: Float, pillHeightPx: Float): Boolean =
            when (phase) {
        IslandPhase.Collapsing -> true
        IslandPhase.Expanding -> progress < CARD_VISIBLE_PROGRESS || yPx <= pillHeightPx
        else -> false
    }

    fun targetExpanded(intent: TapIntent): Boolean? = when (intent) {
        TapIntent.OpenIsland -> true
        TapIntent.CloseIsland -> false
        else -> null
    }
}

/**
 * The latest tap that could not run yet (a swap still pending or settling), kept by the key of the event it asked for.
 * Only the newest is kept, so a burst of taps ends on what the last one wanted
 */
class TapMemory {
    private var key: String? = null

    val isHeld: Boolean get() = key != null

    fun remember(eventKey: String) {
        key = eventKey
    }

    fun take(): String? {
        val held = key
        key = null
        return held
    }

    fun clear() {
        key = null
    }
}

/** Remembers the last cutout tap made while the bubble flies, so it can run once the bubble has landed. */
class FlightTapHold {
    var flying = false
        private set
    private var heldKey: String? = null

    fun hold(key: String) {
        heldKey = key
    }

    /** Returns the held card's key when the flight ends, otherwise null. */
    fun setFlying(active: Boolean): String? {
        if (flying == active) return null
        flying = active
        if (active) return null
        return heldKey.also { heldKey = null }
    }
}
