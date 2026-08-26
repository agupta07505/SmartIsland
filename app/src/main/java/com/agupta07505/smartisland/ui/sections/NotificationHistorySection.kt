/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.collection.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.AllInbox
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.agupta07505.smartisland.data.INotificationHistoryRepository
import com.agupta07505.smartisland.data.NotificationHistoryEntry
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class RetentionOption(val hours: Int, val label: String)

private val RETENTION_OPTIONS = listOf(
    RetentionOption(24, "24 Hours (1 Day)"),
    RetentionOption(72, "3 Days (Recommended)"),
    RetentionOption(168, "7 Days (1 Week)"),
    RetentionOption(720, "30 Days (1 Month)"),
    RetentionOption(-1, "Keep Forever")
)

private object AppIconMemoryCache {
    private val cache = LruCache<String, ImageBitmap>(120)
    fun get(packageName: String): ImageBitmap? = cache.get(packageName)
    fun put(packageName: String, bitmap: ImageBitmap) {
        cache.put(packageName, bitmap)
    }
}

@Composable
private fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    var iconBitmap by remember(packageName) { mutableStateOf(AppIconMemoryCache.get(packageName)) }

    LaunchedEffect(packageName) {
        if (iconBitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatchingLogged("IconCache", "Failed loading icon for $packageName") {
                    val drawable = context.packageManager.getApplicationIcon(packageName)
                    val bmp = drawable.toBitmap(width = 72, height = 72)
                    bmp.asImageBitmap()
                }
            }
            if (loaded != null) {
                AppIconMemoryCache.put(packageName, loaded)
                iconBitmap = loaded
            }
        }
    }
    return iconBitmap
}

data class AppNotificationSummary(
    val packageName: String,
    val appName: String,
    val count: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationHistorySection(
    settings: SmartIslandSettings = SmartIslandSettings.Default,
    repository: SmartIslandSettingsRepository? = null,
    historyRepository: INotificationHistoryRepository? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resolvedHistoryRepo = remember(historyRepository, context) {
        historyRepository ?: runCatching {
            SmartIslandRepositories.historyRepository(context)
        }.getOrElse {
            com.agupta07505.smartisland.data.NotificationHistoryRepository(context)
        }
    }

    val historyEntries by resolvedHistoryRepo.history.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterPackage by remember { mutableStateOf<String?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showDeleteByAppDialog by remember { mutableStateOf(false) }
    var packageToDeleteConfirm by remember { mutableStateOf<AppNotificationSummary?>(null) }
    var selectedEntryForDetails by remember { mutableStateOf<NotificationHistoryEntry?>(null) }
    var retentionMenuExpanded by remember { mutableStateOf(false) }

    // Aggregate unique apps for "Delete by App" feature
    val appSummaries = remember(historyEntries) {
        historyEntries
            .groupBy { it.packageName }
            .map { (pkg, entries) ->
                AppNotificationSummary(
                    packageName = pkg,
                    appName = entries.firstOrNull()?.appName ?: pkg,
                    count = entries.size
                )
            }
            .sortedByDescending { it.count }
    }

    val filteredEntries = remember(historyEntries, searchQuery, selectedFilterPackage) {
        var list = historyEntries
        if (!selectedFilterPackage.isNullOrBlank()) {
            list = list.filter { it.packageName == selectedFilterPackage }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            list = list.filter {
                it.appName.lowercase(Locale.getDefault()).contains(q) ||
                    it.title.lowercase(Locale.getDefault()).contains(q) ||
                    it.text.lowercase(Locale.getDefault()).contains(q) ||
                    it.packageName.lowercase(Locale.getDefault()).contains(q)
            }
        }
        list
    }

    val groupedByDay = remember(filteredEntries) {
        groupEntriesByDay(filteredEntries)
    }

    // Confirm Clear All Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Clear All Notification History?") },
            text = { Text("This will permanently delete all ${historyEntries.size} saved notification logs from this device. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            resolvedHistoryRepo.clearAll()
                            selectedFilterPackage = null
                        }
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Delete By Package Dialog
    packageToDeleteConfirm?.let { targetApp ->
        AlertDialog(
            onDismissRequest = { packageToDeleteConfirm = null },
            title = { Text("Delete All from ${targetApp.appName}?") },
            text = { Text("This will permanently delete all ${targetApp.count} notifications from ${targetApp.appName} (${targetApp.packageName}).") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val deleted = resolvedHistoryRepo.deleteByPackage(targetApp.packageName)
                            if (selectedFilterPackage == targetApp.packageName) {
                                selectedFilterPackage = null
                            }
                            Toast.makeText(context, "Deleted $deleted notifications from ${targetApp.appName}", Toast.LENGTH_SHORT).show()
                        }
                        packageToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All from App")
                }
            },
            dismissButton = {
                TextButton(onClick = { packageToDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete By App Manager Dialog
    if (showDeleteByAppDialog) {
        DeleteByAppDialog(
            appSummaries = appSummaries,
            onDismiss = { showDeleteByAppDialog = false },
            onSelectFilter = { pkg ->
                selectedFilterPackage = pkg
                showDeleteByAppDialog = false
            },
            onDeleteApp = { summary ->
                packageToDeleteConfirm = summary
            }
        )
    }

    // Notification Details Dialog
    selectedEntryForDetails?.let { entry ->
        NotificationDetailDialog(
            entry = entry,
            onDismiss = { selectedEntryForDetails = null },
            onDelete = {
                scope.launch {
                    resolvedHistoryRepo.deleteEntry(entry.id)
                    selectedEntryForDetails = null
                }
            },
            onDeleteAllFromApp = {
                packageToDeleteConfirm = AppNotificationSummary(
                    packageName = entry.packageName,
                    appName = entry.appName,
                    count = historyEntries.count { it.packageName == entry.packageName }
                )
                selectedEntryForDetails = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // Master Toggle Hero Card
        item(key = "header_master_toggle") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Notification History Log",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (settings.enableNotificationHistory) "Logging active • On-device private" else "Disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (settings.enableNotificationHistory) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (repository != null) {
                            Switch(
                                checked = settings.enableNotificationHistory,
                                onCheckedChange = { isChecked ->
                                    scope.launch {
                                        repository.setEnableNotificationHistory(isChecked)
                                    }
                                }
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Security,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "100% on-device private SQLite storage. Never uploaded, synced, or shared.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (!settings.enableNotificationHistory) {
            item(key = "header_disabled_banner") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.AllInbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Never Miss a Notification",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Turn on Notification History to record notifications from all apps. Search past alerts and review full message details at any time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (repository != null) {
                            Button(
                                onClick = { scope.launch { repository.setEnableNotificationHistory(true) } },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Turn On History")
                            }
                        }
                    }
                }
            }
        } else {
            // Settings & Retention & Search Card
            item(key = "header_controls") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Retention row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Auto-Clean Retention:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box {
                                val currentRetention = RETENTION_OPTIONS.find { it.hours == settings.notificationHistoryRetentionHours }
                                    ?: RETENTION_OPTIONS[1]

                                OutlinedButton(
                                    onClick = { retentionMenuExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(currentRetention.label, style = MaterialTheme.typography.labelSmall)
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }

                                DropdownMenu(
                                    expanded = retentionMenuExpanded,
                                    onDismissRequest = { retentionMenuExpanded = false }
                                ) {
                                    RETENTION_OPTIONS.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
                                            onClick = {
                                                retentionMenuExpanded = false
                                                if (repository != null) {
                                                    scope.launch {
                                                        repository.setNotificationHistoryRetentionHours(option.hours)
                                                        resolvedHistoryRepo.cleanupOldEntries(option.hours)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                        // Search Box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by app, sender, or content...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Active Filter Badge (if any)
                        if (!selectedFilterPackage.isNullOrBlank()) {
                            val activeAppName = appSummaries.find { it.packageName == selectedFilterPackage }?.appName ?: selectedFilterPackage
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Filter: $activeAppName",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    IconButton(
                                        onClick = { selectedFilterPackage = null },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear app filter", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        // Action Buttons: Delete By App & Clear All
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${filteredEntries.size} saved",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (appSummaries.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { showDeleteByAppDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Delete by App", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                if (historyEntries.isNotEmpty()) {
                                    TextButton(
                                        onClick = { showClearAllConfirm = true },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (groupedByDay.isEmpty()) {
                item(key = "empty_history_placeholder") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedFilterPackage != null) "No matching notifications found" else "No notifications recorded yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedFilterPackage != null) "Try changing your search term or clearing the active app filter." else "Incoming notifications will automatically appear here grouped by date.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // High-performance virtualized items grouped by day
                groupedByDay.forEach { (dateHeader, entriesForDay) ->
                    item(key = "date_header_$dateHeader") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${entriesForDay.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(
                        items = entriesForDay,
                        key = { it.id }
                    ) { entry ->
                        NotificationHistoryItemCard(
                            entry = entry,
                            onCardClick = { selectedEntryForDetails = entry },
                            onDelete = {
                                scope.launch { resolvedHistoryRepo.deleteEntry(entry.id) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationHistoryItemCard(
    entry: NotificationHistoryEntry,
    onCardClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val appIcon = rememberAppIcon(entry.packageName)

    val timeFormatted = remember(entry.postTimeMillis) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(entry.postTimeMillis))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: App icon, app name, mode badge, time, delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = entry.appName,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = entry.appName.firstOrNull()?.uppercase() ?: "A",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = entry.appName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (entry.mode.isNotBlank() && entry.mode != "Notification") {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = entry.mode,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete entry",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Message Title & Body
            if (entry.title.isNotBlank()) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (entry.text.isNotBlank()) {
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            if (entry.text.length > 120 || entry.title.length > 80) {
                Text(
                    text = if (isExpanded) "Show less" else "Show more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                )
            }

            // Action Intent Buttons List (if any)
            if (entry.actionTitles.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entry.actionTitles.forEach { action ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = action,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Action Bar: Copy, Launch App
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(entry.packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                        } else {
                            Toast.makeText(context, "App not installed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Launch, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Open App", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        val fullText = buildString {
                            if (entry.title.isNotBlank()) appendLine(entry.title)
                            if (entry.text.isNotBlank()) append(entry.text)
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("SmartIsland Notification", fullText)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "Notification text copied", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun DeleteByAppDialog(
    appSummaries: List<AppNotificationSummary>,
    onDismiss: () -> Unit,
    onSelectFilter: (String) -> Unit,
    onDeleteApp: (AppNotificationSummary) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp, max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Delete by App",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Text(
                    text = "Select an app to filter or delete all recorded notification logs for that app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(appSummaries, key = { it.packageName }) { app ->
                        val icon = rememberAppIcon(app.packageName)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFilter(app.packageName) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (icon != null) {
                                    Image(
                                        bitmap = icon,
                                        contentDescription = app.appName,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = app.appName.firstOrNull()?.uppercase() ?: "A",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${app.count} notification${if (app.count == 1) "" else "s"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteApp(app) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Delete all from ${app.appName}",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationDetailDialog(
    entry: NotificationHistoryEntry,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onDeleteAllFromApp: () -> Unit
) {
    val fullDateFormatted = remember(entry.postTimeMillis) {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm:ss a", Locale.getDefault())
        sdf.format(Date(entry.postTimeMillis))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Message Inspection Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Scrollable Detail Fields
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailFieldItem(label = "Application", value = "${entry.appName} (${entry.packageName})")
                    if (entry.title.isNotBlank()) {
                        DetailFieldItem(label = "Title", value = entry.title)
                    }
                    if (entry.text.isNotBlank()) {
                        DetailFieldItem(label = "Text Content", value = entry.text)
                    }
                    if (!entry.subText.isNullOrBlank()) {
                        DetailFieldItem(label = "SubText", value = entry.subText)
                    }
                    DetailFieldItem(label = "Received Time", value = fullDateFormatted)
                    if (!entry.category.isNullOrBlank()) {
                        DetailFieldItem(label = "Category", value = entry.category)
                    }
                    if (!entry.channelId.isNullOrBlank()) {
                        DetailFieldItem(label = "Channel ID", value = entry.channelId)
                    }
                    DetailFieldItem(label = "Island Mode", value = entry.mode)
                    if (entry.actionTitles.isNotEmpty()) {
                        DetailFieldItem(label = "Action Buttons", value = entry.actionTitles.joinToString(", "))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Footer Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete Log")
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Close")
                        }
                    }

                    TextButton(
                        onClick = onDeleteAllFromApp,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete All from ${entry.appName}", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailFieldItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 16.sp
        )
    }
}

private fun groupEntriesByDay(entries: List<NotificationHistoryEntry>): Map<String, List<NotificationHistoryEntry>> {
    val result = linkedMapOf<String, MutableList<NotificationHistoryEntry>>()
    val calToday = Calendar.getInstance()
    val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val calEntry = Calendar.getInstance()
    val fullDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

    for (entry in entries) {
        calEntry.timeInMillis = entry.postTimeMillis
        val header = when {
            isSameDay(calToday, calEntry) -> "Today"
            isSameDay(calYesterday, calEntry) -> "Yesterday"
            else -> fullDateFormat.format(Date(entry.postTimeMillis))
        }
        val list = result.getOrPut(header) { mutableListOf() }
        list.add(entry)
    }
    return result
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
