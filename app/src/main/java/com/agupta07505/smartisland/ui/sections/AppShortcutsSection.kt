/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.runtime.saveable.rememberSaveable
import android.content.Intent
import android.provider.Settings
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import com.agupta07505.smartisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import com.agupta07505.smartisland.ui.glass.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.abs
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.key
import com.agupta07505.smartisland.data.PinnedApps
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.AppShortcutProvider
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.pickers.rememberAppIcon
import com.agupta07505.smartisland.util.safeStartActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
    // Dragging works on the rows as they rest: the dragged row follows the finger and the order is saved once, when the finger lifts.
    var dragOrder by remember(pinnedApps) { mutableStateOf(pinnedApps) }
    var draggedPackage by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    // Where the dragged entry would land if let go now: the entry it is over, whether it is resting on one it could
    // make a folder with, and whether it has rested there long enough ([FOLDER_DWELL_MS]) for letting go to make the
    // folder.
    var dragTarget by remember { mutableStateOf<String?>(null) }
    var dragOverJoinable by remember { mutableStateOf(false) }
    var dragJoin by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    // The folder timer starts over whenever the dragged row moves onto another one, and arms the folder once it runs out.
    LaunchedEffect(draggedPackage, dragTarget, dragOverJoinable) {
        dragJoin = false
        if (draggedPackage == null || dragTarget == null || !dragOverJoinable) return@LaunchedEffect
        kotlinx.coroutines.delay(FOLDER_DWELL_MS)
        dragJoin = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    // An app of a folder being dragged (to reorder the folder or to take the app out of it).
    var draggedMember by remember { mutableStateOf<String?>(null) }
    var memberOffsetY by remember { mutableFloatStateOf(0f) }
    // Resting bounds of every entry (its row, and for a folder its apps too) in the list, and of every folder app in its folder.
    val entryBounds = remember { HashMap<String, androidx.compose.ui.geometry.Rect>() }
    val memberBounds = remember { HashMap<String, androidx.compose.ui.geometry.Rect>() }
    val usageAccess = AppShortcutProvider.hasUsageAccess(context)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
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
                                    if (settings.enableAppShortcuts) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
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
                            Text(
                                text = stringResource(R.string.toggle_enable_shortcuts_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Text(
                        text = stringResource(R.string.shortcuts_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.pinned_apps_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Up to 8 slots on your island pill. Drag one app onto another in the island to make a folder of up to 4.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = stringResource(R.string.card_app_shortcuts_status, settings.shortcutPackages.size),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                if (pinnedApps.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAppSelectionDialog = true }
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
                            Text(
                                text = "Tap here to select apps to pin to your island launcher.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Drag a handle to move an entry: letting go trades places with the entry it is over; resting on
                    // another app (or a folder with room) for 1.5 s first makes a folder.
                    fun dropFor(dragged: String, offsetY: Float): Pair<String?, Boolean> {
                        val from = entryBounds[dragged] ?: return null to false
                        val centerY = from.center.y + offsetY
                        var best: String? = null
                        var bestDistance = Float.MAX_VALUE
                        for (other in dragOrder) {
                            if (other.packageName == dragged) continue
                            val rect = entryBounds[other.packageName] ?: continue
                            val distance = abs(rect.center.y - centerY)
                            if (distance < bestDistance) {
                                best = other.packageName
                                bestDistance = distance
                            }
                        }
                        val target = best ?: return null to false
                        val rect = entryBounds[target] ?: return null to false
                        val join = PinnedApps.canJoin(settings.shortcutPackages, dragged, target) &&
                            centerY >= rect.top && centerY <= rect.bottom
                        return target to join
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dragOrder.forEach { app ->
                          key(app.packageName) {
                            val entry = app.packageName
                            val isDragged = draggedPackage == entry
                            val isFolder = PinnedApps.isFolder(entry)
                            val memberApps = remember(entry) {
                                if (isFolder) AppShortcutProvider.selectedApps(context, PinnedApps.members(entry)) else emptyList()
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { entryBounds[entry] = it.boundsInParent() }
                                    .zIndex(if (isDragged) 1f else 0f)
                                    .graphicsLayer { translationY = if (isDragged) dragOffsetY else 0f },
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isDragged -> MaterialTheme.colorScheme.surfaceVariant
                                            dragTarget == entry && dragJoin -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                            dragTarget == entry -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        }
                                    )
                                    .clickable(enabled = draggedPackage == null && draggedMember == null) { showAppSelectionDialog = true }
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
                                    enabled = draggedPackage == null && draggedMember == null,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove ${app.label}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Drag handle: the row follows the finger. Over the middle of another app it makes a folder with it,
                                // over an edge it moves there. The order is saved when the finger lifts.
                                if (dragOrder.size > 1) {
                                    Icon(
                                        imageVector = Icons.Rounded.DragHandle,
                                        contentDescription = "Reorder ${app.label}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .pointerInput(entry, settings.shortcutPackages) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        draggedPackage = entry
                                                        dragOffsetY = 0f
                                                        dragTarget = null
                                                        dragOverJoinable = false
                                                        dragJoin = false
                                                    },
                                                    onDrag = { change, amount ->
                                                        change.consume()
                                                        dragOffsetY += amount.y
                                                        val (target, joinable) = dropFor(entry, dragOffsetY)
                                                        // Moving onto another row starts the folder timer over (the effect above).
                                                        if (target != dragTarget || !joinable) dragJoin = false
                                                        dragTarget = target
                                                        dragOverJoinable = joinable
                                                    },
                                                    onDragEnd = {
                                                        val current = settings.shortcutPackages
                                                        val target = dropFor(entry, dragOffsetY).first
                                                        val updated = target?.let { t ->
                                                            if (dragJoin) PinnedApps.joinFolder(current, entry, t)
                                                            else PinnedApps.swap(current, entry, t)
                                                        } ?: current
                                                        draggedPackage = null
                                                        dragOffsetY = 0f
                                                        dragTarget = null
                                                        dragOverJoinable = false
                                                        dragJoin = false
                                                        if (updated != current) scope.launch { repository.setShortcutPackages(updated) }
                                                    },
                                                    onDragCancel = {
                                                        draggedPackage = null
                                                        dragOffsetY = 0f
                                                        dragTarget = null
                                                        dragOverJoinable = false
                                                        dragJoin = false
                                                    }
                                                )
                                            }
                                    )
                                }
                            }

                            memberApps.forEach { member ->
                                key(member.packageName) {
                                    val memberPkg = member.packageName
                                    val memberDragged = draggedMember == memberPkg
                                    val memberIcon = rememberAppIcon(memberPkg)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 24.dp)
                                            .onGloballyPositioned { memberBounds[memberPkg] = it.boundsInParent() }
                                            .zIndex(if (memberDragged) 1f else 0f)
                                            .graphicsLayer { translationY = if (memberDragged) memberOffsetY else 0f }
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
                                            enabled = draggedPackage == null && draggedMember == null,
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
                                                .pointerInput(memberPkg, entry, settings.shortcutPackages) {
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            draggedMember = memberPkg
                                                            memberOffsetY = 0f
                                                        },
                                                        onDrag = { change, amount ->
                                                            change.consume()
                                                            memberOffsetY += amount.y
                                                        },
                                                        onDragEnd = {
                                                            val current = settings.shortcutPackages
                                                            val block = entryBounds[entry]
                                                            val row = memberBounds[memberPkg]
                                                            var updated = current
                                                            if (block != null && row != null) {
                                                                val centerY = block.top + row.center.y + memberOffsetY
                                                                if (centerY < block.top || centerY > block.bottom) {
                                                                    // Past the folder: the app takes a slot of its
                                                                    // own, or is unpinned when none is free.
                                                                    if (PinnedApps.dragOutUnpins(current)) {
                                                                        android.widget.Toast.makeText(
                                                                            context,
                                                                            context.getString(R.string.pinned_folder_full_unpinned, member.label),
                                                                            android.widget.Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                                    updated = PinnedApps.dragOut(current, entry, memberPkg)
                                                                } else {
                                                                    val apps = PinnedApps.members(entry)
                                                                    val to = apps.indices.minByOrNull { i ->
                                                                        val r = memberBounds[apps[i]]
                                                                        if (r == null) Float.MAX_VALUE else abs(block.top + r.center.y - centerY)
                                                                    } ?: apps.indexOf(memberPkg)
                                                                    updated = PinnedApps.moveInFolder(current, entry, apps.indexOf(memberPkg), to)
                                                                }
                                                            }
                                                            draggedMember = null
                                                            memberOffsetY = 0f
                                                            if (updated != current) scope.launch { repository.setShortcutPackages(updated) }
                                                        },
                                                        onDragCancel = {
                                                            draggedMember = null
                                                            memberOffsetY = 0f
                                                        }
                                                    )
                                                }
                                        )
                                    }
                                }
                            }
                            }
                          }
                        }
                    }
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
        com.agupta07505.smartisland.ui.pickers.AppPickerDialog(
            settings = settings,
            repository = repository,
            onDismiss = { showAppSelectionDialog = false }
        )
    }
}
