/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.BuildConfig
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SettingsCard
import com.agupta07505.smartisland.util.quietly
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Kept as JSON text so it survives rotation, and parsed again instead of holding parsed objects
    var showRestoreDialog by rememberSaveable { mutableStateOf(false) }
    var pendingRestoreJson by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingRestoreSettings = remember(pendingRestoreJson) {
        pendingRestoreJson?.let { quietly { SmartIslandSettings.fromJson(it) } }
    }
    val pendingRestoreMetadata = remember(pendingRestoreJson) {
        pendingRestoreJson?.let { SmartIslandSettings.parseBackupMetadata(it) }
    }

    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        launchBackupExport(scope = scope, context = context, settings = settings, uri = uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        launchBackupImport(
            scope = scope,
            context = context,
            uri = uri,
            onPicked = { jsonString ->
                pendingRestoreJson = jsonString
                showRestoreDialog = true
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BackupOverviewCard(settings = settings)

        ExportBackupCard(onExport = { exportLauncher.launch("smartisland_backup_${fileTimestamp()}.json") })

        RestoreBackupCard(
            onRestore = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
        )

        ResetSettingsCard(onReset = { showResetDialog = true })
    }

    if (showRestoreDialog && pendingRestoreSettings != null) {
        RestoreBackupDialog(
            metadata = pendingRestoreMetadata,
            onConfirm = {
                scope.launch {
                    repository.restoreSettings(pendingRestoreSettings)
                    context.showToast(context.getString(R.string.toast_backup_restored_success), long = true)
                }
                showRestoreDialog = false
                pendingRestoreJson = null
            },
            onDismiss = {
                showRestoreDialog = false
                pendingRestoreJson = null
            }
        )
    }

    if (showResetDialog) {
        ResetSettingsDialog(
            onConfirm = {
                scope.launch {
                    repository.resetAllSettings()
                    context.showToast(context.getString(R.string.toast_reset_all_success))
                }
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

private fun launchBackupExport(
    scope: CoroutineScope,
    context: Context,
    settings: SmartIslandSettings,
    uri: Uri
) {
    scope.launch(Dispatchers.IO) {
        try {
            val jsonString = settings.toJson(appVersion = BuildConfig.VERSION_NAME)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            context.showToastOnMain(context.getString(R.string.toast_backup_exported_success), long = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            context.showToastOnMain(
                context.getString(R.string.toast_backup_export_failed, e.localizedMessage ?: "IO Error"),
                long = true
            )
        }
    }
}

private fun launchBackupImport(
    scope: CoroutineScope,
    context: Context,
    uri: Uri,
    onPicked: (String) -> Unit
) {
    scope.launch(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            }?.takeIf { it.isNotBlank() }
            val restored = jsonString?.let { quietly { SmartIslandSettings.fromJson(it) } }

            if (jsonString == null || restored == null) {
                context.showToastOnMain(context.getString(R.string.toast_backup_invalid_file))
                return@launch
            }

            withContext(Dispatchers.Main) {
                onPicked(jsonString)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            context.showToastOnMain(
                context.getString(R.string.toast_backup_restore_failed, e.localizedMessage ?: "Read Error"),
                long = true
            )
        }
    }
}

@Composable
private fun BackupCard(
    icon: ImageVector,
    title: String,
    description: String,
    iconBoxSize: Dp = 42.dp,
    iconBoxShape: Shape = CircleShape,
    iconSize: Dp = 22.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(iconBoxSize)
                        .clip(iconBoxShape)
                        .background(tint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(iconSize)
                    )
                }
                CardTitleBlock(title = title, description = description)
            }
            content()
        }
    }
}

@Composable
private fun BackupActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors()
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = colors
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BackupOverviewCard(settings: SmartIslandSettings) {
    BackupCard(
        icon = Icons.Rounded.SettingsBackupRestore,
        title = stringResource(R.string.backup_overview_title),
        description = stringResource(R.string.backup_overview_desc),
        iconBoxSize = 46.dp,
        iconBoxShape = RoundedCornerShape(14.dp),
        iconSize = 24.dp
    ) {
        SettingsDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(
                    R.string.backup_status_details,
                    settings.width.toInt(),
                    (settings.opacity * 100).toInt(),
                    settings.shadowElevation.toInt(),
                    settings.shortcutPackages.size
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ExportBackupCard(onExport: () -> Unit) {
    BackupCard(
        icon = Icons.Rounded.FileUpload,
        title = stringResource(R.string.backup_export_card_title),
        description = stringResource(R.string.backup_export_card_desc)
    ) {
        SettingsDivider()
        BackupActionButton(
            icon = Icons.Rounded.FileUpload,
            label = stringResource(R.string.btn_export_backup),
            onClick = onExport
        )
    }
}

@Composable
private fun RestoreBackupCard(onRestore: () -> Unit) {
    BackupCard(
        icon = Icons.Rounded.FileDownload,
        title = stringResource(R.string.backup_restore_card_title),
        description = stringResource(R.string.backup_restore_card_desc)
    ) {
        BackupActionButton(
            icon = Icons.Rounded.FileDownload,
            label = stringResource(R.string.btn_restore_backup),
            onClick = onRestore
        )
    }
}

@Composable
private fun ResetSettingsCard(onReset: () -> Unit) {
    BackupCard(
        icon = Icons.Rounded.Warning,
        title = stringResource(R.string.backup_reset_card_title),
        description = stringResource(R.string.backup_reset_card_desc),
        tint = MaterialTheme.colorScheme.error
    ) {
        BackupActionButton(
            icon = Icons.Rounded.Refresh,
            label = stringResource(R.string.btn_reset_all_settings),
            onClick = onReset,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        )
    }
}

@Composable
private fun RestoreBackupDialog(
    metadata: SmartIslandSettings.BackupMetadata?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateStr = metadata?.let {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it.timestamp))
    } ?: "Unknown"

    ConfirmDialog(
        icon = {
            Icon(
                imageVector = Icons.Rounded.SettingsBackupRestore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_restore_confirm_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(
                    R.string.dialog_restore_confirm_desc,
                    metadata?.settingsCount ?: 40,
                    dateStr,
                    metadata?.appVersion ?: "7.0.0"
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmLabel = stringResource(R.string.btn_restore_backup),
        cancelLabel = stringResource(android.R.string.cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        destructive = false
    )
}

@Composable
private fun ResetSettingsDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_reset_confirm_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_reset_confirm_desc),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmLabel = stringResource(R.string.btn_reset_all_settings),
        cancelLabel = stringResource(android.R.string.cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
