/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.VerticalSplit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.PinnedApps
import com.agupta07505.smartisland.model.AppShortcutAction
import com.agupta07505.smartisland.util.quietly
import com.agupta07505.smartisland.ui.IslandText as Text

@Composable
internal fun PinnedTile(
    app: LaunchableApp,
    pinnedOrder: List<String>,
    slotBounds: HashMap<String, Rect>,
    onLaunchApp: (String) -> Unit,
    openFolderState: MutableState<String?>,
    menuAppState: MutableState<LaunchableApp?>,
    draggedAppState: MutableState<LaunchableApp?>,
    dragCenterState: MutableState<Offset>,
    dropTargetState: MutableState<String?>,
    dropIntoFolderState: MutableState<Boolean>
) {
    var openFolder by openFolderState
    var menuApp by menuAppState
    var draggedApp by draggedAppState
    var dragCenter by dragCenterState
    var dropTarget by dropTargetState
    var dropIntoFolder by dropIntoFolderState
    val reorderPinned = LocalReorderPinnedApps.current
    val haptics = LocalHapticFeedback.current
    val pkg = app.packageName
    val isFolder = PinnedApps.isFolder(pkg)
    ShortcutApp(
        app = app,
        onClick = { if (isFolder) openFolder = pkg else onLaunchApp(pkg) },
        // a folder has no actions of its own; its apps have them once it is open
        onLongClick = if (isFolder) null else ({ menuApp = app }),
        highlighted = dropTarget == pkg && !dropIntoFolder,
        joinHint = dropTarget == pkg && dropIntoFolder,
        dimmed = draggedApp?.packageName == pkg,
        modifier = Modifier.onGloballyPositioned { slotBounds[pkg] = it.boundsInRoot() },
        // only pinned entries can be moved
        gestureModifier =
                if (pkg !in pinnedOrder) Modifier else Modifier.longPressDrag(
                    key = listOf(pkg, pinnedOrder),
                    topLeft = { slotBounds[pkg]?.topLeft ?: Offset.Zero },
                    onLift = {
                        menuApp = null
                        draggedApp = app
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onMove = { pointer ->
                        dragCenter = pointer
                        val target = pinnedOrder.firstOrNull { other ->
                            other != pkg && slotBounds[other]?.contains(pointer) == true
                        }
                        // Moving onto another app starts the folder timer over (the effect above)
                        if (target != dropTarget) dropIntoFolder = false
                        dropTarget = target
                    },
                    onEnd = { lifted, _ ->
                        val target = dropTarget
                        if (lifted && target != null) {
                            reorderPinned(
                                if (dropIntoFolder) PinnedApps.joinFolder(pinnedOrder, pkg, target)
                                else PinnedApps.swap(pinnedOrder, pkg, target)
                            )
                        }
                        draggedApp = null
                        dropTarget = null
                        dropIntoFolder = false
                    },
                    onCancel = {
                        draggedApp = null
                        dropTarget = null
                        dropIntoFolder = false
                    }
                )
    )
}

@Composable
internal fun AddAppTile(onClick: () -> Unit) {
    val palette = LocalIslandPalette.current
    Column(
        modifier = Modifier
            .size(width = 64.dp, height = 76.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(palette.text.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.pin_apps_add),
                tint = palette.text,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = stringResource(R.string.pin_apps_add),
            color = palette.text,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
internal fun ShortcutApp(
    app: LaunchableApp,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    joinHint: Boolean = false,
    dimmed: Boolean = false,
    gestureModifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFolder = PinnedApps.isFolder(app.packageName)
    val icon = remember(app.packageName) {
        if (isFolder) null else quietly {
            context.packageManager.getApplicationIcon(app.packageName)
                .toBitmap(width = 96, height = 96)
                .asImageBitmap()
        }
    }
    Column(
        modifier = modifier
            .size(width = 64.dp, height = 76.dp)
            .graphicsLayer {
                alpha = if (dimmed) 0.3f else 1f
                scaleX = if (joinHint) 1.1f else 1f
                scaleY = if (joinHint) 1.1f else 1f
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    joinHint -> LocalIslandPalette.current.text.copy(alpha = 0.3f)
                    highlighted -> LocalIslandPalette.current.text.copy(alpha = 0.16f)
                    else -> Color.Transparent
                }
            )
            .then(if (onClick != null) Modifier.combinedClickable(onClick = onClick,
                onLongClick = onLongClick) else Modifier)
            .then(gestureModifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isFolder) {
            FolderTile(PinnedApps.members(app.packageName))
        } else if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = app.label,
                modifier = Modifier.size(44.dp)
            )
        }
        Text(
            text = if (isFolder) stringResource(R.string.pinned_folder_label) else app.label,
            color = LocalIslandPalette.current.text,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun FolderTile(members: List<String>) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(LocalIslandPalette.current.text.copy(alpha = 0.14f))
            .padding(4.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            members.take(PinnedApps.MAX_FOLDER).chunked(2).forEach { rowMembers ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    rowMembers.forEach { memberPkg ->
                        val mini = remember(memberPkg) {
                            quietly {
                                context.packageManager.getApplicationIcon(memberPkg)
                                    .toBitmap(width = 48, height = 48)
                                    .asImageBitmap()
                            }
                        }
                        if (mini != null) {
                            Image(bitmap = mini, contentDescription = null, modifier = Modifier.size(17.dp))
                        } else {
                            Spacer(Modifier.size(17.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AppActionPanel(
    app: LaunchableApp,
    actions: List<AppShortcutAction>,
    onAction: (AppShortcutAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalIslandPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface.copy(alpha = 0.97f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                onClick = onDismiss)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShortcutApp(app = app, onClick = onDismiss)
        actions.forEach { action ->
            AppActionChip(action = action, onClick = { onAction(action) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AppActionChip(action: AppShortcutAction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalIslandPalette.current
    val icon: ImageVector
    val label: Int
    when (action) {
        AppShortcutAction.AppInfo -> { icon = Icons.Outlined.Info; label = R.string.shortcut_action_app_info }
        AppShortcutAction.FloatingWindow -> { icon = Icons.Outlined.PictureInPictureAlt; label =
            R.string.shortcut_action_floating_window }
        AppShortcutAction.SplitScreen -> { icon = Icons.Outlined.VerticalSplit; label =
            R.string.shortcut_action_split_screen }
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = palette.accentCyan, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(label),
            color = palette.text,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
