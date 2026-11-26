/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.agupta07505.smartisland.ui.expanded

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.PinnedApps
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.AppShortcutAction
import com.agupta07505.smartisland.model.appShortcutMenu
import com.agupta07505.smartisland.model.floatingWindowRoute
import com.agupta07505.smartisland.ui.UtilityNavigation
import com.agupta07505.smartisland.util.PickerRequest
import com.agupta07505.smartisland.util.quietly
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
internal fun LauncherGrid(
    apps: List<LaunchableApp>,
    settings: SmartIslandSettings,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, AppShortcutAction) -> Unit,
    // Pinned page with a free slot
    canAdd: Boolean = false
) {
    val context = LocalContext.current
    val menuAppState = remember(apps) { mutableStateOf<LaunchableApp?>(null) }
    var menuApp by menuAppState
    val haptics = LocalHapticFeedback.current
    val pinnedOrder = settings.shortcutPackages
    val slotBounds = remember { HashMap<String, Rect>() }
    var gridOrigin by remember { mutableStateOf(Offset.Zero) }
    val draggedAppState = remember { mutableStateOf<LaunchableApp?>(null) }
    val draggedApp by draggedAppState
    val dragCenterState = remember { mutableStateOf(Offset.Zero) }
    val dragCenter by dragCenterState
    val dropTargetState = remember { mutableStateOf<String?>(null) }
    val dropTarget by dropTargetState
    val dropIntoFolderState = remember { mutableStateOf(false) }
    var dropIntoFolder by dropIntoFolderState
    val openFolderState = remember(apps) { mutableStateOf<String?>(null) }
    val openFolder by openFolderState
    val folderPanelBoundsState = remember { mutableStateOf(Rect.Zero) }
    LaunchedEffect(draggedApp?.packageName, dropTarget) {
        dropIntoFolder = false
        val dragged = draggedApp?.packageName ?: return@LaunchedEffect
        val target = dropTarget ?: return@LaunchedEffect
        if (!PinnedApps.canJoin(pinnedOrder, dragged, target)) return@LaunchedEffect
        delay(FOLDER_DWELL_MS)
        dropIntoFolder = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { gridOrigin = it.positionInRoot() }
            ) {
                Column(
                    // The pinned page (the one with the plus tile) sits in the middle of the window
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (canAdd) Modifier.align(Alignment.Center) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // a null entry is the plus tile
                    val tiles: List<LaunchableApp?> = if (canAdd) apps + listOf<LaunchableApp?>(null) else apps
                    tiles.chunked(4).forEach { rowApps ->
                        val centred = canAdd && rowApps.size < 4
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (centred) Arrangement.spacedBy(20.dp,
                                Alignment.CenterHorizontally) else Arrangement.SpaceEvenly
                        ) {
                            rowApps.forEach { appOrNull ->
                                if (appOrNull == null) {
                                    AddAppTile(onClick = { UtilityNavigation.openPicker(context, PickerRequest.APPS) })
                                    return@forEach
                                }
                                val app: LaunchableApp = appOrNull
                                PinnedTile(
                                    app = app,
                                    pinnedOrder = pinnedOrder,
                                    slotBounds = slotBounds,
                                    onLaunchApp = onLaunchApp,
                                    openFolderState = openFolderState,
                                    menuAppState = menuAppState,
                                    draggedAppState = draggedAppState,
                                    dragCenterState = dragCenterState,
                                    dropTargetState = dropTargetState,
                                    dropIntoFolderState = dropIntoFolderState
                                )
                            }
                            if (!centred) repeat(4 - rowApps.size) { Box(Modifier.size(width = 64.dp, height = 1.dp)) }
                        }
                    }
                }
                // The open folder: its apps on a panel over the grid
                openFolder?.takeIf { it in pinnedOrder }?.let { folder ->
                    OpenFolderPanel(
                        folder = folder,
                        pinnedOrder = pinnedOrder,
                        slotBounds = slotBounds,
                        onLaunchApp = onLaunchApp,
                        openFolderState = openFolderState,
                        menuAppState = menuAppState,
                        draggedAppState = draggedAppState,
                        dragCenterState = dragCenterState,
                        folderPanelBoundsState = folderPanelBoundsState
                    )
                }
                draggedApp?.let { app ->
                    ShortcutApp(
                        app = app,
                        onClick = null,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (dragCenter.x - gridOrigin.x - 32.dp.toPx()).roundToInt(),
                                    (dragCenter.y - gridOrigin.y - 38.dp.toPx()).roundToInt()
                                )
                            }
                            .graphicsLayer { scaleX = 1.12f; scaleY = 1.12f }
                    )
                }
                menuApp?.let { app ->
                    AppActionPanel(
                        app = app,
                        actions = appShortcutMenu(floatingWindowRoute(settings.floatingWindowEnabled,
                            settings.floatingWindowMode)),
                        onAction = { action ->
                            menuApp = null
                            onAppAction(app.packageName, action)
                        },
                        onDismiss = { menuApp = null },
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
}

@Composable
private fun BoxScope.OpenFolderPanel(
    folder: String,
    pinnedOrder: List<String>,
    slotBounds: HashMap<String, Rect>,
    onLaunchApp: (String) -> Unit,
    openFolderState: MutableState<String?>,
    menuAppState: MutableState<LaunchableApp?>,
    draggedAppState: MutableState<LaunchableApp?>,
    dragCenterState: MutableState<Offset>,
    folderPanelBoundsState: MutableState<Rect>
) {
    val context = LocalContext.current
    var openFolder by openFolderState
    var menuApp by menuAppState
    var draggedApp by draggedAppState
    var dragCenter by dragCenterState
    var folderPanelBounds by folderPanelBoundsState
    val reorderPinned = LocalReorderPinnedApps.current
    val haptics = LocalHapticFeedback.current
    val palette = LocalIslandPalette.current
    val folderApps = remember(folder) {
        PinnedApps.members(folder).mapNotNull { memberPkg ->
            quietly {
                val pm = context.packageManager
                LaunchableApp(memberPkg, pm.getApplicationLabel(pm.getApplicationInfo(memberPkg, 0)).toString())
            }
        }
    }
    Box(
        modifier = Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface.copy(alpha = 0.97f))
            .clickable(interactionSource = remember { MutableInteractionSource() },
                indication = null) { openFolder = null },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .onGloballyPositioned { folderPanelBounds = it.boundsInRoot() }
                .clip(RoundedCornerShape(16.dp))
                .background(palette.text.copy(alpha = 0.08f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            folderApps.forEach { member ->
                val memberPkg = member.packageName
                ShortcutApp(
                    app = member,
                    onClick = { onLaunchApp(memberPkg) },
                    onLongClick = { menuApp = member },
                    dimmed = draggedApp?.packageName == memberPkg,
                    modifier = Modifier.onGloballyPositioned { slotBounds[memberPkg] = it.boundsInRoot() },
                    gestureModifier = Modifier
                        .longPressDrag(
                            key = listOf(memberPkg, folder, pinnedOrder, folderPanelBounds),
                            topLeft = { slotBounds[memberPkg]?.topLeft ?: Offset.Zero },
                            onLift = {
                                menuApp = null
                                draggedApp = member
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onMove = { pointer -> dragCenter = pointer },
                            onEnd = { lifted, pointer ->
                                draggedApp = null
                                if (lifted && !folderPanelBounds.contains(pointer)) {
                                    if (PinnedApps.dragOutUnpins(pinnedOrder)) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.pinned_folder_full_unpinned, member.label),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    reorderPinned(PinnedApps.dragOut(pinnedOrder, folder, memberPkg))
                                    openFolder = null
                                }
                            },
                            onCancel = { draggedApp = null }
                        )
                )
            }
        }
    }
}

internal fun Modifier.longPressDrag(
    key: Any?,
    topLeft: () -> Offset,
    onLift: () -> Unit,
    onMove: (Offset) -> Unit,
    onEnd: (lifted: Boolean, pointer: Offset) -> Unit,
    onCancel: () -> Unit,
    onStart: (Offset) -> Unit = {}
): Modifier = pointerInput(key) {
    var start = Offset.Zero
    var pointer = Offset.Zero
    var lifted = false
    detectDragGesturesAfterLongPress(
        onDragStart = { local ->
            pointer = topLeft() + local
            start = pointer
            lifted = false
            onStart(start)
        },
        onDrag = { change, amount ->
            change.consume()
            pointer += amount
            if (!lifted && (pointer - start).getDistance() > viewConfiguration.touchSlop) {
                lifted = true
                onLift()
            }
            if (lifted) onMove(pointer)
        },
        onDragEnd = { onEnd(lifted, pointer) },
        onDragCancel = onCancel
    )
}
