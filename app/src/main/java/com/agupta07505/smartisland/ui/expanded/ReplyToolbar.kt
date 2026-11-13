/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.util.SelectionToolbarLog
import kotlinx.coroutines.awaitCancellation

/** What the reply box's toolbar offers right now, Compose hands it over with each [TextToolbar.showMenu]. */
internal class ReplyMenu(
    val rect: Rect,
    val copy: (() -> Unit)?,
    val paste: (() -> Unit)?,
    val cut: (() -> Unit)?,
    val selectAll: (() -> Unit)?
)

/**
 * The reply box's own Cut, Copy, Paste and Select all, drawn inside the island. The system toolbar is a window of its
 * own and does not reliably show from the overlay. It never marks the island as "selecting": typing a reply already
 * holds the auto-collapse, and Back closes the reply box first (see BackStepRules)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ReplySelectionToolbar(
    /** False once the selection is gone, the toolbar goes with it whatever Compose last said. */
    selectionActive: Boolean,
    /** After Copy: the selection is dropped (Cut and Paste end it by themselves, Select all keeps it). */
    onCopied: () -> Unit,
    content: @Composable () -> Unit
) {
    var menu by remember { mutableStateOf<ReplyMenu?>(null) }
    val toolbar = remember {
        object : TextToolbar {
            override val status: TextToolbarStatus
                get() = if (menu != null) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?
            ) {
                SelectionToolbarLog.log("reply showMenu rect=$rect cut=${onCutRequested != null} copy=${onCopyRequested != null} paste=${onPasteRequested != null}")
                menu = ReplyMenu(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested)
            }

            override fun hide() {
                menu = null
            }
        }
    }
    // same deal as the reader: TextToolbar never gets called, the request comes through the context menu provider.
    // the zero-size box is only there to get coordinates
    val anchor = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val menuProvider = remember {
        object : TextContextMenuProvider {
            override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider): Nothing {
                val coords = anchor[0]?.takeIf { it.isAttached } ?: awaitCancellation()
                val bounds = dataProvider.contentBounds(coords)
                val rect = Rect(coords.localToRoot(bounds.topLeft), coords.localToRoot(bounds.bottomRight))
                lateinit var mine: ReplyMenu
                val session = object : TextContextMenuSession {
                    override fun close() {
                        if (menu === mine) menu = null
                    }
                }
                val items = dataProvider.data().components.filterIsInstance<TextContextMenuItem>()
                fun action(key: Any): (() -> Unit)? =
                    items.firstOrNull { it.key == key }?.let { item -> { item.onClick(session) } }
                mine = ReplyMenu(
                    rect, action(TextContextMenuKeys.CopyKey), action(TextContextMenuKeys.PasteKey),
                    action(TextContextMenuKeys.CutKey), action(TextContextMenuKeys.SelectAllKey)
                )
                SelectionToolbarLog.log("reply showTextContextMenu rect=$rect cut=${mine.cut != null} copy=${mine.copy != null} paste=${mine.paste != null}")
                menu = mine
                try {
                    awaitCancellation()
                } finally {
                    if (menu === mine) menu = null
                }
            }
        }
    }
    CompositionLocalProvider(
        LocalTextToolbar provides toolbar,
        LocalTextContextMenuToolbarProvider provides menuProvider
    ) { content() }
    Box(Modifier.size(0.dp).onGloballyPositioned { anchor[0] = it })

    val shown = menu
    val host = LocalTextSelection.current?.barHost
    // empty box = no selection, but Paste still has to show
    val offered = (selectionActive || shown?.paste != null) && shown != null &&
        (shown.cut != null || shown.copy != null || shown.paste != null || shown.selectAll != null)
    // Each action hides the toolbar itself (Select all brings it back), so none is closed here
    DisposableEffect(host, shown, offered) {
        val request = if (host != null && shown != null && offered) {
            SelectionBarRequest(shown.rect) {
                Row(
                    modifier = Modifier
                        .background(Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
                        .padding(horizontal = 4.dp)
                ) {
                    shown.cut?.let { BarButton(stringResource(R.string.selection_cut), it) }
                    shown.copy?.let { copy -> BarButton(stringResource(R.string.selection_copy)) { copy(); onCopied() } }
                    shown.paste?.let { BarButton(stringResource(R.string.selection_paste), it) }
                    shown.selectAll?.let { BarButton(stringResource(R.string.selection_select_all), it) }
                }
            }
        } else null
        SelectionToolbarLog.log("reply request ${if (request == null) "cleared" else "set"} host=${host != null} active=$selectionActive offered=$offered")
        host?.request = request
        onDispose { if (host != null && host.request === request) host.request = null }
    }
}
