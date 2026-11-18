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
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.ui.glass.Card
import com.agupta07505.smartisland.util.SelectionToolbarLog

/**
 * The log of the text selection toolbar issue, apart from the main diagnostic log: long-press a text in the island (or
 * select in the reply box), come back here and copy or export what it shows
 */
@Composable
fun SelectionToolbarLogSection() {
    val context = LocalContext.current
    val lines by SelectionToolbarLog.lines.collectAsState()
    val scroll = rememberScrollState()
    // the newest line stays in view
    LaunchedEffect(lines.size) { scroll.scrollTo(scroll.maxValue) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(SelectionToolbarLog.asText().toByteArray()) }
                ?: error("no output stream")
        }
        context.showToast(
            if (ok.isSuccess) "Log exported" else "Export failed: ${ok.exceptionOrNull()?.localizedMessage}",
            long = true
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Selection toolbar log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Always on, no recording switch needed. Long-press text in the island and in the reply " +
                    "box, then come back here. Each line shows one step of the toolbar: asked for by the text, " +
                    "handed to the island, placed on screen. Send it in with the toolbar issue.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${lines.size} lines",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (lines.isEmpty()) "Nothing logged yet." else lines.joinToString("\n"),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(scroll)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Selection toolbar log", SelectionToolbarLog.asText()))
                    context.showToast("Log copied")
                }) { Text("Copy") }
                OutlinedButton(onClick = {
                    val stamp = fileTimestamp()
                    exportLauncher.launch("smartisland_selection_toolbar_$stamp.txt")
                }) { Text("Export") }
                OutlinedButton(onClick = { SelectionToolbarLog.clear() }) { Text("Clear") }
            }
        }
    }
}
