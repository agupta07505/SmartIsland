/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInbox
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.INotificationHistoryRepository
import com.agupta07505.smartisland.data.NotificationHistoryEntry
import com.agupta07505.smartisland.ui.SettingsDescription
import com.agupta07505.smartisland.ui.SettingsGroupTitle
import com.agupta07505.smartisland.ui.glass.Card
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun HistoryDialogs(
    historyEntries: List<NotificationHistoryEntry>,
    appSummaries: List<AppNotificationSummary>,
    selectedEntryForDetails: NotificationHistoryEntry?,
    resolvedHistoryRepo: INotificationHistoryRepository,
    scope: CoroutineScope,
    context: Context,
    showClearAllConfirmState: MutableState<Boolean>,
    packageToDeleteConfirmState: MutableState<AppNotificationSummary?>,
    showAppFilterDialogState: MutableState<Boolean>,
    showDeleteByAppDialogState: MutableState<Boolean>,
    selectedFilterPackageState: MutableState<String?>,
    selectedEntryIdForDetailsState: MutableState<Long?>
) {
    var showClearAllConfirm by showClearAllConfirmState
    var packageToDeleteConfirm by packageToDeleteConfirmState
    var showAppFilterDialog by showAppFilterDialogState
    var showDeleteByAppDialog by showDeleteByAppDialogState
    var selectedFilterPackage by selectedFilterPackageState
    var selectedEntryIdForDetails by selectedEntryIdForDetailsState
    if (showClearAllConfirm) {
        ClearHistoryDialog(
            count = historyEntries.size,
            onConfirm = {
                scope.launch {
                    resolvedHistoryRepo.clearAll()
                    selectedFilterPackage = null
                }
                showClearAllConfirm = false
            },
            onDismiss = { showClearAllConfirm = false }
        )
    }

    packageToDeleteConfirm?.let { targetApp ->
        DeleteAppHistoryDialog(
            target = targetApp,
            onConfirm = {
                scope.launch {
                    val deleted = resolvedHistoryRepo.deleteByPackage(targetApp.packageName)
                    if (selectedFilterPackage == targetApp.packageName) {
                        selectedFilterPackage = null
                    }
                    context.showToast("Deleted $deleted notifications from ${targetApp.appName}")
                }
                packageToDeleteConfirm = null
            },
            onDismiss = { packageToDeleteConfirm = null }
        )
    }

    if (showAppFilterDialog) {
        AppFilterDialog(
            appSummaries = appSummaries,
            totalCount = historyEntries.size,
            selectedPackage = selectedFilterPackage,
            onDismiss = { showAppFilterDialog = false },
            onSelectFilter = { pkg ->
                selectedFilterPackage = pkg
                showAppFilterDialog = false
            }
        )
    }

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

    selectedEntryForDetails?.let { entry ->
        NotificationDetailDialog(
            entry = entry,
            onDismiss = { selectedEntryIdForDetails = null },
            onDelete = {
                scope.launch {
                    resolvedHistoryRepo.deleteEntry(entry.id)
                    selectedEntryIdForDetails = null
                }
            },
            onDeleteAllFromApp = {
                packageToDeleteConfirm = AppNotificationSummary(
                    packageName = entry.packageName,
                    appName = entry.appName,
                    count = historyEntries.count { it.packageName == entry.packageName }
                )
                selectedEntryIdForDetails = null
            }
        )
    }
}

@Composable
private fun ClearHistoryDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = { Text(stringResource(R.string.confirm_clear_history_title)) },
        text = { Text(stringResource(R.string.confirm_clear_history_msg, count)) },
        confirmLabel = stringResource(R.string.btn_clear_history),
        cancelLabel = stringResource(R.string.btn_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun DeleteAppHistoryDialog(target: AppNotificationSummary, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = { Text(stringResource(R.string.confirm_delete_app_history_title, target.appName)) },
        text = { Text(stringResource(R.string.confirm_delete_app_history_msg, target.count, target.appName)) },
        confirmLabel = stringResource(R.string.btn_delete_app_history),
        cancelLabel = stringResource(R.string.btn_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun HistoryDialogFrame(
    onDismiss: () -> Unit,
    maxHeight: Dp,
    minHeight: Dp = Dp.Unspecified,
    fillHeight: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight, max = maxHeight)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .then(if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun HistoryListDialog(
    icon: ImageVector,
    title: String,
    hint: String,
    onDismiss: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    HistoryDialogFrame(onDismiss = onDismiss, minHeight = 260.dp, maxHeight = 520.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
            }
        }

        SettingsDescription(hint)

        SettingsDivider()

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun AppFilterDialog(
    appSummaries: List<AppNotificationSummary>,
    totalCount: Int,
    selectedPackage: String?,
    onDismiss: () -> Unit,
    onSelectFilter: (String?) -> Unit
) {
    HistoryListDialog(
        icon = Icons.Rounded.FilterList,
        title = stringResource(R.string.filter_by_app),
        hint = "Select an app to view its notifications, or choose All Apps to see all logs together.",
        onDismiss = onDismiss
    ) {
        item(key = "filter_dialog_all_apps") {
            AllAppsFilterRow(selectedPackage = selectedPackage, totalCount = totalCount,
                onSelectFilter = onSelectFilter)
        }

        items(appSummaries, key = { it.packageName }) { app ->
            AppFilterRow(app = app, selectedPackage = selectedPackage, onSelectFilter = onSelectFilter)
        }
    }
}

@Composable
private fun FilterOptionCard(
    selected: Boolean,
    padding: Dp,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(padding),
            content = content
        )
    }
}

@Composable
private fun SelectedCheck() {
    Icon(
        Icons.Rounded.Check,
        contentDescription = "Selected",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
    )
}

private fun notificationCountLabel(count: Int) = "$count notification${if (count == 1) "" else "s"}"

@Composable
private fun RowScope.AppNameAndCount(app: AppNotificationSummary) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        HintText(notificationCountLabel(app.count))
    }
}

@Composable
private fun AllAppsFilterRow(
    selectedPackage: String?,
    totalCount: Int,
    onSelectFilter: (String?) -> Unit
) {
    val isAllSelected = selectedPackage == null
    FilterOptionCard(selected = isAllSelected, padding = 12.dp, onClick = { onSelectFilter(null) }) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.AllInbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.filter_all_apps),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            HintText(stringResource(R.string.filter_all_apps_desc) + " ($totalCount)")
        }

        if (isAllSelected) SelectedCheck()
    }
}

@Composable
private fun AppFilterRow(
    app: AppNotificationSummary,
    selectedPackage: String?,
    onSelectFilter: (String?) -> Unit
) {
    val isSelected = selectedPackage == app.packageName
    FilterOptionCard(selected = isSelected, padding = 10.dp, onClick = { onSelectFilter(app.packageName) }) {
        AppAvatar(
            packageName = app.packageName,
            label = app.appName,
            size = 36.dp,
            shape = CircleShape,
            letterStyle = MaterialTheme.typography.labelMedium
        )
        AppNameAndCount(app)
        if (isSelected) SelectedCheck()
    }
}

@Composable
private fun DeleteByAppDialog(
    appSummaries: List<AppNotificationSummary>,
    onDismiss: () -> Unit,
    onSelectFilter: (String) -> Unit,
    onDeleteApp: (AppNotificationSummary) -> Unit
) {
    HistoryListDialog(
        icon = Icons.Rounded.Apps,
        title = "Delete by App",
        hint = "Select an app to filter or delete all recorded notification logs for that app.",
        onDismiss = onDismiss
    ) {
        items(appSummaries, key = { it.packageName }) { app ->
            Card(
                colors = CardDefaults.cardColors(containerColor =
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
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
                    AppAvatar(
                        packageName = app.packageName,
                        label = app.appName,
                        size = 32.dp,
                        shape = CircleShape,
                        letterStyle = MaterialTheme.typography.labelSmall
                    )
                    AppNameAndCount(app)
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

    HistoryDialogFrame(onDismiss = onDismiss, maxHeight = 560.dp, fillHeight = false) {
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
            SettingsGroupTitle("Message Inspection Details")
        }

        SettingsDivider()

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

        SettingsDivider()

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
