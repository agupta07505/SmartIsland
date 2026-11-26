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
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agupta07505.smartisland.BuildConfig
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SettingsCard
import com.agupta07505.smartisland.ui.SettingsDescription
import com.agupta07505.smartisland.ui.SettingsGroupTitle
import com.agupta07505.smartisland.ui.glass.Switch
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.FoldDetector
import com.agupta07505.smartisland.util.FullscreenProbe
import com.agupta07505.smartisland.util.OemDeviceRules
import com.agupta07505.smartisland.util.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val RecordingRed = Color(0xFFEF4444)

@Composable
fun DeveloperOptionsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearLogsDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var logCount by remember { mutableIntStateOf(AppLogRecorder.getLogCount()) }

    LaunchedEffect(settings.recordLogs) {
        AppLogRecorder.updateRecordingState(context, settings.recordLogs)
        while (isActive) {
            logCount = AppLogRecorder.getLogCount()
            delay(1500L)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) {
            isExporting = false
            return@rememberLauncherForActivityResult
        }
        scope.launch(Dispatchers.IO) {
            isExporting = true
            val result = AppLogRecorder.exportLogsToUri(context, uri, settings)
            withContext(Dispatchers.Main) {
                isExporting = false
                if (result.isSuccess) {
                    context.showToast(context.getString(R.string.toast_logs_exported_success), long = true)
                } else {
                    context.showToast(
                        context.getString(R.string.toast_logs_export_failed,
                            result.exceptionOrNull()?.localizedMessage ?: "Unknown Error"),
                        long = true
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        FaceUnlockSection(settings = settings, repository = repository)

        // Try Foldable mode on a phone that does not fold
        val emulateFoldable by FoldDetector.emulationFlow(context).collectAsStateWithLifecycle()
        DevToggleCard(
            title = "Emulate foldable",
            description = "Treat this phone as a foldable so the Foldable mode switch and its four fold modes show in the Layout tab. " +
                "A phone-sized screen counts as folded. The island picks up the change the next time the screen turns.",
            checked = emulateFoldable,
            onCheckedChange = { FoldDetector.setEmulating(context, it) }
        )

        // Which signals flip in a fullscreen video, game or immersive page, read by the island with no root or Shizuku
        val fullscreenProbe by FullscreenProbe.enabled.collectAsStateWithLifecycle()
        DevToggleCard(
            title = "Fullscreen detection test",
            description = "Floats a small readout over every app showing whether the island can see the status bar " +
                "hidden (A: a plain overlay window, B: the island's own window, C: the app window against the screen, " +
                "S: the status bar's own window). Open a fullscreen video, game or immersive page, and the changes " +
                "are also written to the logs (turn Record logs on). Off after the app restarts.",
            checked = fullscreenProbe,
            onCheckedChange = { FullscreenProbe.enabled.value = it }
        )

        RecordLogsCard(
            settings = settings,
            repository = repository,
            scope = scope,
            logCount = logCount,
            onPickExportFile = { fileName -> exportLauncher.launch(fileName) },
            onClearClick = { showClearLogsDialog = true }
        )

        // The text selection toolbar issue's own log, always on
        SelectionToolbarLogSection()

        SystemSnapshotCard(settings)
    }

    if (showClearLogsDialog) {
        ClearLogsDialog(
            onConfirm = {
                AppLogRecorder.clear(context)
                logCount = 0
                showClearLogsDialog = false
                context.showToast(context.getString(R.string.toast_logs_cleared))
            },
            onDismiss = { showClearLogsDialog = false }
        )
    }
}

@Composable
private fun DevToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CardTitleBlock(title = title, description = description, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun RecordLogsCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    logCount: Int,
    onPickExportFile: (String) -> Unit,
    onClearClick: () -> Unit
) {
    SettingsCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RecordLogsHeader(settings, repository, scope)

            RecordingStatusRow(settings, logCount)

            SettingsDivider()

            Text(
                text = stringResource(R.string.dev_export_section_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            ExportLogsButton(onPickExportFile)

            ShareCopyRow(settings, scope)

            ClearLogsButton(onClearClick)
        }
    }
}

@Composable
private fun RecordLogsHeader(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    val context = LocalContext.current
    val recording = settings.recordLogs
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
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (recording) RecordingRed.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (recording) Icons.Rounded.RadioButtonChecked else Icons.Rounded.BugReport,
                    contentDescription = null,
                    tint = if (recording) RecordingRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            CardTitleBlock(
                title = stringResource(R.string.dev_record_logs_title),
                description = stringResource(R.string.dev_record_logs_desc)
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = recording,
            onCheckedChange = { enabled ->
                scope.launch {
                    repository.setRecordLogs(enabled)
                    AppLogRecorder.updateRecordingState(context, enabled)
                }
            }
        )
    }
}

@Composable
private fun RecordingStatusRow(settings: SmartIslandSettings, logCount: Int) {
    val recording = settings.recordLogs
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (recording) RecordingRed.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (recording) RecordingRed else Color(0xFF94A3B8))
            )
            Text(
                text = if (recording) stringResource(R.string.status_recording_active)
                else stringResource(R.string.status_recording_idle),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (recording) RecordingRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(R.string.dev_log_entries_count, logCount),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExportLogsButton(onPickFile: (String) -> Unit) {
    Button(
        onClick = { onPickFile("smartisland_logs_${fileTimestamp()}.txt") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(
            imageVector = Icons.Rounded.FileDownload,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.btn_export_logs_storage),
            fontWeight = FontWeight.Bold
        )
    }
}

private fun CoroutineScope.withDiagnosticReport(
    context: Context,
    settings: SmartIslandSettings,
    onReport: (String) -> Unit
) {
    launch(Dispatchers.IO) {
        val report = AppLogRecorder.generateDiagnosticReport(context, settings)
        withContext(Dispatchers.Main) { onReport(report) }
    }
}

@Composable
private fun ShareCopyRow(settings: SmartIslandSettings, scope: CoroutineScope) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LogActionButton(
            icon = Icons.Rounded.Share,
            label = stringResource(R.string.btn_share_logs),
            onClick = {
                scope.withDiagnosticReport(context, settings) { report ->
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, report)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_logs_title)))
                }
            }
        )
        LogActionButton(
            icon = Icons.Rounded.ContentCopy,
            label = stringResource(R.string.btn_copy_logs),
            onClick = {
                scope.withDiagnosticReport(context, settings) { report ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Smart Island Diagnostic Logs", report))
                    context.showToast(context.getString(R.string.toast_logs_copied))
                }
            }
        )
    }
}

@Composable
private fun RowScope.LogActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, fontSize = 13.sp)
    }
}

@Composable
private fun ClearLogsButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) {
        Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.width(6.dp))
        Text(text = stringResource(R.string.btn_clear_logs), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SystemSnapshotCard(settings: SmartIslandSettings) {
    val context = LocalContext.current
    SettingsCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                SettingsGroupTitle(stringResource(R.string.dev_system_snapshot_title))
            }

            SettingsDescription(stringResource(R.string.dev_system_snapshot_desc))

            SettingsDivider()

            DiagnosticItem(
                label = "Device",
                value = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})"
            )
            DiagnosticItem(
                label = "App Build",
                value = "v${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE})"
            )
            DiagnosticItem(
                label = "OEM Rules",
                value = OemDeviceRules.detectCurrentDevice().name
            )
            DiagnosticItem(
                label = "Overlay Permission",
                value = if (Settings.canDrawOverlays(context)) "Granted" else "Denied"
            )
            DiagnosticItem(
                label = "Shizuku Status",
                value = if (ShizukuManager.isBinderAvailable()
                    && ShizukuManager.hasPermission()) "Authorized" else "Not Connected"
            )
            DiagnosticItem(
                label = "Island Geometry",
                value = "${settings.width.toInt()}x${settings.height.toInt()}dp (Y: ${settings.yOffset.toInt()}dp, R: ${settings.cornerRadius.toInt()}dp, Opacity: ${(settings.opacity * 100).toInt()}%)"
            )
        }
    }
}

@Composable
private fun ClearLogsDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_clear_logs_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_clear_logs_desc),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmLabel = stringResource(R.string.btn_clear_logs),
        cancelLabel = stringResource(R.string.btn_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun DiagnosticItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
