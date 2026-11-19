/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.AppShortcutProvider
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.PinnedApps
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SettingsCard
import com.agupta07505.smartisland.ui.SettingsDescription
import com.agupta07505.smartisland.ui.SettingsGroupTitle
import com.agupta07505.smartisland.ui.glass.Switch
import com.agupta07505.smartisland.ui.pickers.AppPickerDialog
import com.agupta07505.smartisland.ui.pickers.rememberAppIcon
import com.agupta07505.smartisland.util.safeStartActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private class PinnedDragState {
    var draggedPackage by mutableStateOf<String?>(null)
    var dragOffsetY by mutableFloatStateOf(0f)
    // Entry the drag is over, whether it rests on one it could make a folder with, and whether it rested long enough
    // ([FOLDER_DWELL_MS])
    var dragTarget by mutableStateOf<String?>(null)
    var dragOverJoinable by mutableStateOf(false)
    var dragJoin by mutableStateOf(false)
    // App of a folder being dragged (reorder, or take it out)
    var draggedMember by mutableStateOf<String?>(null)
    var memberOffsetY by mutableFloatStateOf(0f)
    // Resting bounds of every entry (and its apps for a folder), and of every folder app in its folder
    val entryBounds = HashMap<String, Rect>()
    val memberBounds = HashMap<String, Rect>()
}

private const val FOLDER_DWELL_MS = 1500L

@Composable
fun AppShortcutsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAppSelectionDialog by rememberSaveable { mutableStateOf(false) }

    val installedApps by produceState(initialValue = emptyList<LaunchableApp>(), context) {
        value = withContext(Dispatchers.IO) { AppShortcutProvider.installedApps(context) }
    }
    val pinnedApps = remember(installedApps, settings.shortcutPackages) {
        AppShortcutProvider.selectedApps(context, settings.shortcutPackages)
    }
    // The dragged row follows the finger, the order is saved once when it lifts
    var dragOrder by remember(pinnedApps) { mutableStateOf(pinnedApps) }
    val drag = remember { PinnedDragState() }
    val haptics = LocalHapticFeedback.current
    // Restarts whenever the row moves onto another one, arms the folder when it runs out
    LaunchedEffect(drag.draggedPackage, drag.dragTarget, drag.dragOverJoinable) {
        drag.dragJoin = false
        if (drag.draggedPackage == null || drag.dragTarget == null || !drag.dragOverJoinable) return@LaunchedEffect
        delay(FOLDER_DWELL_MS)
        drag.dragJoin = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    val usageAccess = AppShortcutProvider.hasUsageAccess(context)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShortcutsToggleCard(settings = settings, repository = repository, scope = scope, usageAccess = usageAccess)

        SettingsCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PinnedAppsHeader(count = settings.shortcutPackages.size)

                SettingsDivider(0.3f)

                if (pinnedApps.isEmpty()) {
                    NoPinnedAppsPrompt(onClick = { showAppSelectionDialog = true })
                } else {
                    PinnedAppsList(
                        dragOrder = dragOrder,
                        settings = settings,
                        repository = repository,
                        scope = scope,
                        context = context,
                        drag = drag,
                        onEdit = { showAppSelectionDialog = true }
                    )
                }

                FilledTonalButton(
                    onClick = { showAppSelectionDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (pinnedApps.size < 8) Icons.Rounded.Add else Icons.Rounded.Apps,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (pinnedApps.size < 8) "Select / Add Pinned Apps" else "Manage Pinned Apps",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        UtilitiesEditorCard(settings = settings, repository = repository)
    }

    if (showAppSelectionDialog) {
        AppPickerDialog(
            settings = settings,
            repository = repository,
            onDismiss = { showAppSelectionDialog = false }
        )
    }
}

@Composable
private fun ShortcutsToggleCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    usageAccess: Boolean
) {
    val context = LocalContext.current
    SettingsCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (settings.enableAppShortcuts) MaterialTheme.colorScheme.primaryContainer.copy(alpha =
                                    0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            tint = if (settings.enableAppShortcuts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.toggle_enable_shortcuts_title),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        SettingsDescription(stringResource(R.string.toggle_enable_shortcuts_desc))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = settings.enableAppShortcuts,
                    onCheckedChange = { enabled ->
                        scope.launch { repository.setEnableAppShortcuts(enabled) }
                    }
                )
            }

            if (settings.enableAppShortcuts) {
                SettingsDivider(0.3f)

                SettingsDescription(stringResource(R.string.shortcuts_desc))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.toggle_recent_apps_title),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (usageAccess) stringResource(R.string.toggle_recent_apps_desc_granted)
                                else stringResource(R.string.toggle_recent_apps_desc_needed),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = settings.showRecentApps,
                        onCheckedChange = { enabled ->
                            if (enabled && !usageAccess) {
                                context.safeStartActivity(
                                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                                    "Cannot open Usage access settings on this device."
                                )
                            }
                            scope.launch { repository.setShowRecentApps(enabled) }
                        }
                    )
                }

                if (!usageAccess && settings.showRecentApps) {
                    OutlinedButton(
                        onClick = {
                            context.safeStartActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                                "Cannot open Usage access settings on this device."
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Usage Access Permission", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PinnedAppsHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SettingsGroupTitle(stringResource(R.string.pinned_apps_title))
            SettingsDescription("Up to 8 slots on your island pill. Drag one app onto another in the island to make a folder of up to 4.")
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ) {
            Text(
                text = stringResource(R.string.card_app_shortcuts_status, count),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun NoPinnedAppsPrompt(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "No Apps Pinned Yet",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            SettingsDescription("Tap here to select apps to pin to your island launcher.")
        }
    }
}

@Composable
private fun PinnedAppsList(
    dragOrder: List<LaunchableApp>,
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    context: Context,
    drag: PinnedDragState,
    onEdit: () -> Unit
) {
    // Letting go trades places with the entry it is over, resting 1.5 s on another app (or a folder with room) makes a
    // folder
    fun dropFor(dragged: String, offsetY: Float): Pair<String?, Boolean> {
        val from = drag.entryBounds[dragged] ?: return null to false
        val centerY = from.center.y + offsetY
        var best: String? = null
        var bestDistance = Float.MAX_VALUE
        for (other in dragOrder) {
            if (other.packageName == dragged) continue
            val rect = drag.entryBounds[other.packageName] ?: continue
            val distance = abs(rect.center.y - centerY)
            if (distance < bestDistance) {
                best = other.packageName
                bestDistance = distance
            }
        }
        val target = best ?: return null to false
        val rect = drag.entryBounds[target] ?: return null to false
        val join = PinnedApps.canJoin(settings.shortcutPackages, dragged, target) &&
            centerY >= rect.top && centerY <= rect.bottom
        return target to join
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        dragOrder.forEach { app ->
            key(app.packageName) {
                val entry = app.packageName
                val isDragged = drag.draggedPackage == entry
                val isFolder = PinnedApps.isFolder(entry)
                val memberApps = remember(entry) {
                    if (isFolder) AppShortcutProvider.selectedApps(context, PinnedApps.members(entry)) else emptyList()
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { drag.entryBounds[entry] = it.boundsInParent() }
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragged) drag.dragOffsetY else 0f },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PinnedEntryRow(
                        app = app,
                        isFolder = isFolder,
                        isDragged = isDragged,
                        canReorder = dragOrder.size > 1,
                        settings = settings,
                        repository = repository,
                        scope = scope,
                        drag = drag,
                        dropFor = ::dropFor,
                        onEdit = onEdit
                    )

                    memberApps.forEach { member ->
                        key(member.packageName) {
                            FolderMemberRow(
                                member = member,
                                folder = entry,
                                settings = settings,
                                repository = repository,
                                scope = scope,
                                context = context,
                                drag = drag
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinnedEntryRow(
    app: LaunchableApp,
    isFolder: Boolean,
    isDragged: Boolean,
    canReorder: Boolean,
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    drag: PinnedDragState,
    dropFor: (String, Float) -> Pair<String?, Boolean>,
    onEdit: () -> Unit
) {
    val entry = app.packageName
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isDragged -> MaterialTheme.colorScheme.surfaceVariant
                    drag.dragTarget == entry && drag.dragJoin -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    drag.dragTarget == entry -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                }
            )
            .clickable(enabled = drag.draggedPackage == null && drag.draggedMember == null) { onEdit() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val icon = if (isFolder) null else rememberAppIcon(entry)
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = app.label,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFolder) Icons.Rounded.Folder else Icons.Rounded.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isFolder) {
                    "${stringResource(R.string.pinned_folder_label)} · ${PinnedApps.members(entry).size}"
                } else {
                    entry
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = {
                val updated = settings.shortcutPackages - entry
                scope.launch { repository.setShortcutPackages(updated) }
            },
            enabled = drag.draggedPackage == null && drag.draggedMember == null,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove ${app.label}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        // Over the middle of another app it makes a folder, over an edge it moves there
        // Order is saved when the finger lifts
        if (canReorder) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Reorder ${app.label}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(28.dp)
                    .pointerInput(entry, settings.shortcutPackages) {
                        detectDragGestures(
                            onDragStart = {
                                drag.draggedPackage = entry
                                drag.dragOffsetY = 0f
                                drag.dragTarget = null
                                drag.dragOverJoinable = false
                                drag.dragJoin = false
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                drag.dragOffsetY += amount.y
                                val (target, joinable) = dropFor(entry, drag.dragOffsetY)
                                // Moving onto another row starts the folder timer over (the effect above)
                                if (target != drag.dragTarget || !joinable) drag.dragJoin = false
                                drag.dragTarget = target
                                drag.dragOverJoinable = joinable
                            },
                            onDragEnd = {
                                val current = settings.shortcutPackages
                                val target = dropFor(entry, drag.dragOffsetY).first
                                val updated = target?.let { t ->
                                    if (drag.dragJoin) PinnedApps.joinFolder(current, entry, t)
                                    else PinnedApps.swap(current, entry, t)
                                } ?: current
                                drag.draggedPackage = null
                                drag.dragOffsetY = 0f
                                drag.dragTarget = null
                                drag.dragOverJoinable = false
                                drag.dragJoin = false
                                if (updated != current) scope.launch { repository.setShortcutPackages(updated) }
                            },
                            onDragCancel = {
                                drag.draggedPackage = null
                                drag.dragOffsetY = 0f
                                drag.dragTarget = null
                                drag.dragOverJoinable = false
                                drag.dragJoin = false
                            }
                        )
                    }
            )
        }
    }
}

@Composable
private fun FolderMemberRow(
    member: LaunchableApp,
    folder: String,
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    context: Context,
    drag: PinnedDragState
) {
    val memberPkg = member.packageName
    val memberDragged = drag.draggedMember == memberPkg
    val memberIcon = rememberAppIcon(memberPkg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp)
            .onGloballyPositioned { drag.memberBounds[memberPkg] = it.boundsInParent() }
            .zIndex(if (memberDragged) 1f else 0f)
            .graphicsLayer { translationY = if (memberDragged) drag.memberOffsetY else 0f }
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (memberDragged) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (memberIcon != null) {
            Image(
                bitmap = memberIcon,
                contentDescription = member.label,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
            )
        } else {
            Box(Modifier.size(28.dp))
        }
        Text(
            text = member.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = {
                val updated = PinnedApps.unpin(settings.shortcutPackages, memberPkg)
                scope.launch { repository.setShortcutPackages(updated) }
            },
            enabled = drag.draggedPackage == null && drag.draggedMember == null,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove ${member.label}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = "Reorder ${member.label}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .pointerInput(memberPkg, folder, settings.shortcutPackages) {
                    detectDragGestures(
                        onDragStart = {
                            drag.draggedMember = memberPkg
                            drag.memberOffsetY = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            drag.memberOffsetY += amount.y
                        },
                        onDragEnd = {
                            val current = settings.shortcutPackages
                            val block = drag.entryBounds[folder]
                            val row = drag.memberBounds[memberPkg]
                            var updated = current
                            if (block != null && row != null) {
                                val centerY = block.top + row.center.y + drag.memberOffsetY
                                if (centerY < block.top || centerY > block.bottom) {
                                    // Takes a slot of its own, or is unpinned when none is free.
                                    if (PinnedApps.dragOutUnpins(current)) {
                                        context.showToast(
                                            context.getString(R.string.pinned_folder_full_unpinned, member.label)
                                        )
                                    }
                                    updated = PinnedApps.dragOut(current, folder, memberPkg)
                                } else {
                                    val apps = PinnedApps.members(folder)
                                    val to = apps.indices.minByOrNull { i ->
                                        val r = drag.memberBounds[apps[i]]
                                        if (r == null) Float.MAX_VALUE else abs(block.top + r.center.y - centerY)
                                    } ?: apps.indexOf(memberPkg)
                                    updated = PinnedApps.moveInFolder(current, folder, apps.indexOf(memberPkg), to)
                                }
                            }
                            drag.draggedMember = null
                            drag.memberOffsetY = 0f
                            if (updated != current) scope.launch { repository.setShortcutPackages(updated) }
                        },
                        onDragCancel = {
                            drag.draggedMember = null
                            drag.memberOffsetY = 0f
                        }
                    )
                }
        )
    }
}
