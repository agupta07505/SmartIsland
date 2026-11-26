/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.content.ClipData
import android.content.Context
import android.os.PersistableBundle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.model.CardLinks
import com.agupta07505.smartisland.model.SelectionBarPlacement
import com.agupta07505.smartisland.util.LinkOpener
import com.agupta07505.smartisland.util.SelectionActions
import com.agupta07505.smartisland.util.SelectionToolbarLog
import com.agupta07505.smartisland.util.runSuspendCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * What selectable card text needs from the island. The selection state lives in the view model (auto-collapse waits
 * for it, Back and a tap anywhere drop it), see IslandViewModel.textSelecting
 */
class TextSelectionAccess(
    /** Counts up when the selection must go, the text rebuilds itself without it. */
    val clearTick: Int,
    /** The white pill holds the island: the short card text can be selected too, not only the reader. */
    val heldByPill: Boolean,
    val setSelecting: (Boolean) -> Unit,
    /** After Share or Search opened another app. */
    val onActionDone: () -> Unit,
    /** Drops the selection, the island stays as it is (after Copy, or a Share or Search nothing took). */
    val clear: () -> Unit = {},
    val barHost: SelectionBarHost = SelectionBarHost()
)

val LocalTextSelection = compositionLocalOf<TextSelectionAccess?> { null }

/** A selection is checked this long after the toolbar went and none came back (a handle drag shows no toolbar). */
private const val SELECTION_VERIFY_MS = 2500L

/** How long after the finger lifts the log checks whether Compose asked for the toolbar (it does so after the lift). */
private const val TOOLBAR_CHECK_MS = 500L

// only in newer foundation versions, so which ones exist tells us roughly what we're on
private val PROBE_CLASSES = listOf(
    "androidx.compose.foundation.text.contextmenu.modifier.ToolbarRequester",
    "androidx.compose.foundation.text.contextmenu.provider.TextContextMenuToolbarProvider",
    "androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope",
    "androidx.compose.foundation.text.selection.SelectionManager"
)

private var composeProbed = false

// debug only: the build strips the META-INF version files, so we check for classes instead
@Suppress("DEPRECATION")
private fun logComposeProbe(defaultToolbar: Any, context: Context) {
    if (composeProbed) return
    composeProbed = true
    SelectionToolbarLog.log("compose probe: default toolbar=${defaultToolbar::class.java.name} sdk=${android.os.Build.VERSION.SDK_INT}")
    PROBE_CLASSES.forEach { name ->
        val present = runCatching { Class.forName(name) }.isSuccess
        SelectionToolbarLog.log("compose probe: ${name.substringAfterLast('.')} ${if (present) "present" else "absent"}")
    }
    // which toolbar / context menu classes actually made it into the dex
    runCatching {
        val dex = dalvik.system.DexFile(context.packageCodePath)
        val names = dex.entries().toList().filter {
            it.startsWith("androidx.compose.foundation.text.contextmenu") ||
                (it.startsWith("androidx.compose.foundation.text") && it.contains("Toolbar")) ||
                (it.startsWith("androidx.compose.ui.platform") && it.contains("Toolbar"))
        }.sorted()
        SelectionToolbarLog.log("compose probe: ${names.size} toolbar/context-menu classes")
        names.take(130).forEach { SelectionToolbarLog.log("compose probe: class ${it.removePrefix("androidx.compose.")}") }
    }.onFailure { SelectionToolbarLog.log("compose probe: class list failed ${it.javaClass.simpleName}") }
}

/** Compose copies on its own coroutine, the selection stays until that has run. */
private const val COPY_SETTLE_MS = 150L

/** Longest wait for the copied text to show up on the clipboard (the copy runs asynchronously). */
private const val CLIPBOARD_WAIT_MS = 600L
private const val CLIPBOARD_POLL_MS = 30L
private const val CLIP_SENTINEL = "\u0000smartisland-selection\u0000"

/** Where the toolbar goes and what its buttons do, Compose hands them over with each [TextToolbar.showMenu]. */
internal class SelectionMenu(
    val rect: Rect,
    val copy: (() -> Unit)?,
    val selectAll: (() -> Unit)?
)

/** A toolbar waiting to be drawn: where the selection is and the buttons, the text that selected owns it. */
internal class SelectionBarRequest(
    val rect: Rect,
    val content: @Composable () -> Unit
)

/** Holds the toolbar of the one selection that rests, drawn by [SelectionBarLayer] above the island's card. */
class SelectionBarHost {
    internal var request by mutableStateOf<SelectionBarRequest?>(null)
}

/**
 * The toolbar of a text selection, drawn inside the island so it needs no window of its own. Compose calls
 * [TextToolbar.showMenu] whenever a selection rests (long press, or a handle let go) and [TextToolbar.hide] while it
 * is dragged or gone
 */
@OptIn(ExperimentalFoundationApi::class)
internal class SelectionToolbarState(
    private val scope: CoroutineScope,
    private val setSelecting: (Boolean) -> Unit
) {
    var menu by mutableStateOf<SelectionMenu?>(null)
        private set

    var fingerDown = false

    /** A long press is live: its lift is logged once with what the pointer passes did to it. */
    var longPressLive = false

    /** How often Compose read the toolbar's status since the long press began (0: it never looked at the toolbar). */
    private var statusReads = 0

    /** Compose's own focus on the selection container, the toolbar needs it. */
    var composeFocused = false

    /** Focus and input mode of the island's view for the log: the selection only shows its toolbar while it has focus. */
    var diagnostics: () -> String = { "" }

    private var verifyJob: Job? = null

    /** The selection container's place in the window, the context menu's bounds come in its coordinates. */
    var containerCoords: LayoutCoordinates? = null

    // TextToolbar never gets called on this Compose, the request comes here instead. The default provider opens a
    // floating action mode, which doesn't show in an overlay window, so we draw our own bar
    val menuProvider: TextContextMenuProvider = object : TextContextMenuProvider {
        override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider): Nothing {
            val coords = containerCoords?.takeIf { it.isAttached } ?: awaitCancellation()
            val bounds = dataProvider.contentBounds(coords)
            val rect = Rect(coords.localToRoot(bounds.topLeft), coords.localToRoot(bounds.bottomRight))
            lateinit var mine: SelectionMenu
            val session = object : TextContextMenuSession {
                override fun close() {
                    if (menu === mine) {
                        menu = null
                        verifyLater()
                    }
                }
            }
            val items = dataProvider.data().components.filterIsInstance<TextContextMenuItem>()
            fun action(key: Any): (() -> Unit)? =
                items.firstOrNull { it.key == key }?.let { item -> { item.onClick(session) } }
            mine = SelectionMenu(rect, action(TextContextMenuKeys.CopyKey), action(TextContextMenuKeys.SelectAllKey))
            verifyJob?.cancel()
            setSelecting(true)
            SelectionToolbarLog.log("reader showTextContextMenu rect=$rect copy=${mine.copy != null} all=${mine.selectAll != null}")
            menu = mine
            try {
                awaitCancellation()
            } finally {
                if (menu === mine) {
                    menu = null
                    verifyLater()
                }
            }
        }
    }

    val toolbar: TextToolbar = object : TextToolbar {
        override val status: TextToolbarStatus
            get() {
                statusReads++
                return if (menu != null) TextToolbarStatus.Shown else TextToolbarStatus.Hidden
            }

        override fun showMenu(
            rect: Rect,
            onCopyRequested: (() -> Unit)?,
            onPasteRequested: (() -> Unit)?,
            onCutRequested: (() -> Unit)?,
            onSelectAllRequested: (() -> Unit)?
        ) {
            verifyJob?.cancel()
            setSelecting(true)
            SelectionToolbarLog.log("reader showMenu rect=$rect copy=${onCopyRequested != null} all=${onSelectAllRequested != null}")
            menu = SelectionMenu(rect, onCopyRequested, onSelectAllRequested)
        }

        override fun hide() {
            SelectionToolbarLog.log("reader hide (menu was ${if (menu == null) "null" else "set"})")
            if (menu == null) return
            menu = null
            verifyLater()
        }
    }

    /** A long press began, whether it selected anything is known once the toolbar shows. */
    fun longPressStarted() {
        SelectionToolbarLog.log("reader long press started")
        statusReads = 0
        longPressLive = true
        verifyJob?.cancel()
        setSelecting(true)
    }

    /** The finger of a long press lifted: no toolbar by now (and none to come) means nothing was selected. */
    fun longPressEnded() {
        // The lift reaches this before it reaches the selection, so the menu is checked a moment later
        SelectionToolbarLog.log("reader long press ended")
        fingerDown = false
        // Compose decides on the toolbar at the lift: what the focus was right then
        SelectionToolbarLog.log("reader lift: composeFocus=$composeFocused statusReads=$statusReads " + diagnostics())
        scope.launch {
            delay(TOOLBAR_CHECK_MS)
            SelectionToolbarLog.log(
                "reader ${TOOLBAR_CHECK_MS}ms after lift: toolbar menu ${if (menu == null) "NOT asked for" else "asked for"} " +
                    "composeFocus=$composeFocused statusReads=$statusReads " + diagnostics()
            )
        }
        if (menu == null) verifyLater()
    }

    private fun verifyLater() {
        verifyJob?.cancel()
        verifyJob = scope.launch {
            while (fingerDown) delay(50)
            delay(SELECTION_VERIFY_MS)
            if (menu == null) setSelecting(false)
        }
    }
}

/**
 * Makes the text inside selectable: a long press selects, the handles resize it, the toolbar offers Copy, Share, Search
 * and Select all. [modifier] goes on the container (give it the weight the text had). Off, the text is plain
 */
@Composable
internal fun IslandSelectionContainer(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    /** Links in the text, a selection toolbar with them offers Open. */
    links: List<String> = emptyList(),
    content: @Composable () -> Unit
) {
    val access = LocalTextSelection.current
    if (!enabled || access == null) {
        Box(modifier) { content() }
        return
    }
    // A new tick builds all of it again, toolbar state included, which is how a selection is dropped
    key(access.clearTick) {
        SelectableBody(access, modifier, links, content)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectableBody(
    access: TextSelectionAccess,
    modifier: Modifier,
    links: List<String>,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val setSelecting = rememberUpdatedState(access.setSelecting)
    val state = remember { SelectionToolbarState(scope) { setSelecting.value(it) } }
    val viewConfig = LocalViewConfiguration.current
    val view = LocalView.current
    val inputMode = LocalInputModeManager.current
    val defaultToolbar = LocalTextToolbar.current
    val probeContext = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { logComposeProbe(defaultToolbar, probeContext) } }
    state.diagnostics = {
        "viewFocused=${view.isFocused} windowFocus=${view.hasWindowFocus()} touchMode=${view.isInTouchMode} " +
            "inputMode=${inputMode.inputMode}"
    }

    // The island's own state follows the text: gone from the screen, nothing is selected
    DisposableEffect(Unit) { onDispose { setSelecting.value(false) } }

    // Compose buzzes when a selection starts and when a handle moves: a line here says the selection really began
    val haptic = LocalHapticFeedback.current
    val tracedHaptic = remember(haptic) {
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                SelectionToolbarLog.log("reader haptic $hapticFeedbackType")
                haptic.performHapticFeedback(hapticFeedbackType)
            }
        }
    }
    CompositionLocalProvider(
        LocalTextToolbar provides state.toolbar,
        LocalTextContextMenuToolbarProvider provides state.menuProvider,
        LocalHapticFeedback provides tracedHaptic
    ) {
        SelectionContainer(
            modifier = modifier.onGloballyPositioned { state.containerCoords = it }.onFocusChanged {
                state.composeFocused = it.hasFocus
                SelectionToolbarLog.log("reader compose focus hasFocus=${it.hasFocus} isFocused=${it.isFocused}")
            }.pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                    while (true) {
                        val atInitial = awaitPointerEvent(PointerEventPass.Initial).changes.any { it.isConsumed }
                        val atMain = awaitPointerEvent(PointerEventPass.Main).changes.any { it.isConsumed }
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val atFinal = event.changes.any { it.isConsumed }
                        val lifted = event.changes.none { it.pressed }
                        if (state.fingerDown && !lifted) {
                            SelectionToolbarLog.log("reader move consumed initial=$atInitial main=$atMain final=$atFinal")
                        }
                        if (lifted) {
                            if (state.longPressLive) {
                                state.longPressLive = false
                                SelectionToolbarLog.log("reader lift consumed initial=$atInitial main=$atMain final=$atFinal")
                            }
                            break
                        }
                    }
                }
            }.pointerInput(viewConfig) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    // The selection shows its toolbar only while it has focus, and that needs the Compose view itself to
                    // hold the window's focus: the service gives it to the island's outer view (Back), not to this one
                    if (!view.isFocused) {
                        val taken = view.requestFocus()
                        SelectionToolbarLog.log("reader requestFocus on the text's view -> $taken ${state.diagnostics()}")
                    }
                    var longPressed = true
                    withTimeoutOrNull(viewConfig.longPressTimeoutMillis) {
                        while (true) {
                            val change = awaitPointerEvent(PointerEventPass.Initial).changes
                                .firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed ||
                                (change.position - down.position).getDistance() > viewConfig.touchSlop
                            ) {
                                longPressed = false
                                break
                            }
                        }
                    }
                    if (!longPressed) return@awaitEachGesture
                    // The island hears of it before the lift, so this touch is no swipe and no tap on the card
                    state.fingerDown = true
                    state.longPressStarted()
                    while (true) {
                        val changes = awaitPointerEvent(PointerEventPass.Initial).changes
                        if (changes.none { it.pressed }) break
                    }
                    state.longPressEnded()
                }
            }
        ) {
            content()
        }
    }

    val menu = state.menu
    DisposableEffect(menu, links, access) {
        val request = menu?.let {
            SelectionBarRequest(it.rect) { SelectionBar(it, scope, links, access.onActionDone, access.clear) }
        }
        SelectionToolbarLog.log("reader request ${if (request == null) "cleared" else "set rect=${request.rect}"}")
        access.barHost.request = request
        onDispose { if (access.barHost.request === request) access.barHost.request = null }
    }
}

@Composable
private fun SelectionBar(
    menu: SelectionMenu,
    /** The text's own scope: the bar can leave the screen while Copy or Share still runs. */
    scope: CoroutineScope,
    links: List<String>,
    onDone: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val busy = remember { mutableStateOf(false) }

    /** Copies through the selection's own Copy, then reads what landed on the clipboard and puts the old clip back. */
    fun withSelectedText(action: (String) -> Boolean) {
        val copy = menu.copy ?: return
        // A second tap while the first still waits would take the marker for the user's clip and leave it behind
        if (busy.value) return
        busy.value = true
        scope.launch {
            val previous = runSuspendCatching { clipboard.getClipEntry() }.getOrNull()
            var text: String? = null
            try {
                runSuspendCatching { clipboard.setClipEntry(sentinelEntry()) }
                copy()
                var waited = 0L
                while (waited <= CLIPBOARD_WAIT_MS) {
                    delay(CLIPBOARD_POLL_MS)
                    waited += CLIPBOARD_POLL_MS
                    val read = runSuspendCatching { clipboard.getClipEntry()?.firstText(context) }.getOrNull()
                    if (read != null && read != CLIP_SENTINEL) {
                        text = read
                        break
                    }
                }
            } finally {
                // Also when the text left the screen meanwhile (the scope is cancelled): the user's clip comes back
                withContext(NonCancellable) {
                    runCatching { clipboard.setClipEntry(previous) }
                    busy.value = false
                }
            }
            val picked = text?.takeIf { it.isNotBlank() }
            if (picked != null && action(picked)) onDone() else onClear()
        }
    }

    Row(
        modifier = Modifier
            .background(Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
            .padding(horizontal = 4.dp)
    ) {
        if (menu.copy != null) {
            BarButton(stringResource(R.string.selection_copy)) {
                scope.launch {
                    menu.copy.invoke()
                    delay(COPY_SETTLE_MS)
                    onClear()
                }
            }
            // Which link the selection means is known only once it is copied, so the button is there whenever the
            // text holds one and a selection with no link in it just closes the bar
            if (links.isNotEmpty()) {
                BarButton(stringResource(R.string.selection_open)) {
                    withSelectedText { picked ->
                        CardLinks.inSelection(picked, links)?.let { LinkOpener.open(context, it) } ?: false
                    }
                }
            }
            BarButton(stringResource(R.string.selection_share)) {
                withSelectedText { SelectionActions.share(context, it) }
            }
            BarButton(stringResource(R.string.selection_search)) {
                withSelectedText { SelectionActions.search(context, it) }
            }
        }
        if (menu.selectAll != null) {
            BarButton(stringResource(R.string.selection_select_all)) { menu.selectAll.invoke() }
        }
    }
}

/** Draws the resting selection's toolbar over the island's card, kept inside [area] (the island's surface, the whole window while it is unknown) so the touches that reach the island reach it too. */
@Composable
internal fun SelectionBarLayer(host: SelectionBarHost, area: () -> android.graphics.Rect?) {
    val request = host.request ?: return
    SelectionToolbarLog.log("layer draws bar for rect=${request.rect} area=${area()}")
    val density = LocalDensity.current
    val gapPx = with(density) { 10.dp.roundToPx() }
    val edgePx = with(density) { 8.dp.roundToPx() }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.layout { measurable, constraints ->
                val bar = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                val bounds = area()
                val rect = request.rect
                val spot = SelectionBarPlacement.place(
                    selectionCentreX = rect.center.x.toInt(),
                    selectionTop = rect.top.toInt(),
                    selectionBottom = rect.bottom.toInt(),
                    barWidth = bar.width,
                    barHeight = bar.height,
                    areaLeft = bounds?.left ?: 0,
                    areaTop = bounds?.top ?: 0,
                    areaRight = bounds?.right ?: constraints.maxWidth,
                    areaBottom = bounds?.bottom ?: constraints.maxHeight,
                    gap = gapPx,
                    edge = edgePx
                )
                SelectionToolbarLog.log("layer places bar at (${spot.x},${spot.y}) size=${bar.width}x${bar.height} box=${constraints.maxWidth}x${constraints.maxHeight}")
                layout(constraints.maxWidth, constraints.maxHeight) { bar.place(spot.x, spot.y) }
            }
        ) {
            request.content()
        }
    }
}

@Composable
internal fun BarButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 14.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

private fun ClipEntry.firstText(context: Context): String? =
    clipData.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()

/** The marker the clipboard holds while the selection copies, hidden from the clipboard preview of Android 13+. */
private fun sentinelEntry(): ClipEntry {
    val data = ClipData.newPlainText("", CLIP_SENTINEL)
    data.description.extras = PersistableBundle().apply {
        putBoolean("android.content.extra.IS_SENSITIVE", true)
    }
    return ClipEntry(data)
}
