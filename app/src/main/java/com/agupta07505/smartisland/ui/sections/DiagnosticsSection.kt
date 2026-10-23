/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import com.agupta07505.smartisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.agupta07505.smartisland.ui.glass.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.util.AppLogRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Diagnostic log: switch recording on, reproduce the problem (a WhatsApp or VoIP call that the island did not pick
 * up), then export the report and send it in.
 */
@Composable
fun DiagnosticsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entryCount by remember { mutableIntStateOf(AppLogRecorder.getLogCount()) }

    // Keeps the entry counter live while the screen is open.
    LaunchedEffect(settings.recordLogs) {
        while (true) {
            entryCount = AppLogRecorder.getLogCount()
            delay(1000L)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = AppLogRecorder.exportLogsToUri(context, uri, settings)
            Toast.makeText(
                context,
                if (result.isSuccess) "Log exported" else "Export failed: ${result.exceptionOrNull()?.localizedMessage ?: "IO error"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FaceUnlockSection(settings, repository)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Record diagnostic log",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Turn this on, then reproduce the missed WhatsApp or VoIP call. " +
                                "The log keeps going after the app is restarted or killed.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.recordLogs,
                        onCheckedChange = { enabled ->
                            // Apply right away; the services follow the saved setting as well.
                            AppLogRecorder.applySetting(context, enabled)
                            scope.launch { repository.setRecordLogs(enabled) }
                        }
                    )
                }
                Text(
                    text = "${if (settings.recordLogs) "Recording" else "Not recording"} • $entryCount entries",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Export or clear",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "The report holds device details, permission status, your settings, the recorded " +
                        "events and the app's logcat. It can contain notification titles from the apps being " +
                        "logged, so check it before sharing.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        exportLauncher.launch("smartisland_log_$stamp.txt")
                    }) { Text("Export log") }
                    OutlinedButton(onClick = {
                        AppLogRecorder.clear(context)
                        entryCount = AppLogRecorder.getLogCount()
                    }) { Text("Clear") }
                }
            }
        }
    }
}
