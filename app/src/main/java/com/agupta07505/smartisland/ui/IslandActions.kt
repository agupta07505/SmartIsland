/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CardDepth
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.expanded.MediaExpansion

/** What the island draws from and what gestures look at: plain values, no callbacks. */
internal class IslandViewState(
    val settings: SmartIslandSettings,
    val notifications: List<IslandNotification>,
    val selectedIndex: Int,
    val expanded: Boolean,
    val cardDepth: CardDepth,
    val mediaExpansion: MediaExpansion,
    val readingExpandedText: Boolean,
    val siblingPickerOpen: Boolean,
    val hasIdleStoredCards: Boolean,
    /** A text on the card is selected: a touch only drops the selection, it opens and swipes nothing. */
    val textSelecting: Boolean = false
)

/** Opening, closing and dragging the card */
internal class ExpandActions(
    val toggle: () -> Unit,
    val setExpanded: (Boolean) -> Unit,
    /** True when the drag-open was accepted */
    val dragExpand: (Boolean) -> Boolean,
    val reportSurfaceBounds: (Rect?) -> Unit
)

/** What a card does: pick, step through, go deeper, leave. */
internal class CardActions(
    val selectPage: (Int) -> Unit,
    val switchEvent: (Boolean) -> Unit,
    /** Null while the stepping between notifications is off. */
    val switchNotification: ((Boolean) -> Boolean)?,
    val deeper: () -> Boolean,
    val back: () -> Boolean,
    val open: (IslandNotification) -> Unit,
    val dismiss: () -> Unit,
    val dismissAll: () -> Unit,
    val clearTextSelection: () -> Unit = {}
)

/** The picker over the notifications of one app. */
internal class SiblingActions(
    val switchSibling: (Boolean) -> Boolean,
    val openPicker: () -> Boolean,
    val closePicker: () -> Unit
)

/** The island with nothing live on it. */
internal class IdleActions(
    val revealCards: () -> Unit,
    val dotTap: () -> Unit,
    val togglePillHidden: () -> Unit
)

/** The pulse over a card that just arrived. */
internal class PulseActions(
    val touch: (Boolean) -> Unit = {},
    val repulseCurrent: () -> Unit = {}
)

/** Things that leave the island: the launcher, the floating window, the shade. */
internal class SystemActions(
    val openAppsLauncher: () -> Unit,
    val openFloatingWindow: () -> Unit,
    val openNotificationShade: () -> Unit
)

internal class IslandActions(
    val expand: ExpandActions,
    val card: CardActions,
    val sibling: SiblingActions,
    val idle: IdleActions,
    val pulse: PulseActions,
    val system: SystemActions
)

/**
 * The one handle gesture code holds on to. Both halves sit behind a [State], so a long-lived lambda (swipe, drag,
 * hold) that reads `wiring.actions.card.back()` at call time gets the latest composition's code, never the one it
 * was created in. Do not copy `wiring.actions.card.back` into a local val that outlives a gesture
 */
internal class IslandWiring(
    private val actionsState: State<IslandActions>,
    private val viewState: State<IslandViewState>
) {
    val actions: IslandActions get() = actionsState.value
    val view: IslandViewState get() = viewState.value
}

@Composable
internal fun rememberIslandWiring(actions: IslandActions, view: IslandViewState): IslandWiring {
    val actionsState = rememberUpdatedState(actions)
    val viewState = rememberUpdatedState(view)
    return remember { IslandWiring(actionsState, viewState) }
}
