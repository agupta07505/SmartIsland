/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.model.LabIdle
import com.agupta07505.smartisland.model.LabPill
import com.agupta07505.smartisland.model.LabRotation
import com.agupta07505.smartisland.model.labStageDurationMs
import com.agupta07505.smartisland.model.labStages
import com.agupta07505.smartisland.util.LandscapeDemo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Landscape Lab: plays every landscape situation on the real island (entry, expand, second and third app with both
 * merge styles, sibling picker, call, popup, exit, interrupted exit, idle circle tap) for every combination of
 * rotation (normal / reverse), idle circle position and pill position.
 */
@Composable
fun LandscapeLabSection(notificationRepository: INotificationRepository?) {
    val context = LocalContext.current
    val progress by LandscapeDemo.progress.collectAsState()
    val landscape by LandscapeDemo.deviceLandscape.collectAsState()

    // Saved as names so a rotation (the point of this screen) does not reset the choices.
    var rotationName by rememberSaveable { mutableStateOf("ALL") }
    var idleName by rememberSaveable { mutableStateOf("ALL") }
    var pillName by rememberSaveable { mutableStateOf("ALL") }
    var quick by rememberSaveable { mutableStateOf(false) }

    val rotation = LabRotation.entries.firstOrNull { it.name == rotationName }
    val idle = LabIdle.entries.firstOrNull { it.name == idleName }
    val pill = LabPill.entries.firstOrNull { it.name == pillName }
    val stages = labStages(rotation, idle, pill)
    val totalSeconds = stages.size * labStageDurationMs(quick) / 1000L

    // No stop on leaving the screen: turning the phone recreates this activity and the lab must keep playing.
    // It ends by itself, with Stop, or when the island service stops (IslandViewModel.onCleared).

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Landscape Lab",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Plays every landscape animation on the real island: card arrives, expand, second and third app " +
                    "(both merge styles), sibling picker, call, popup, exit, interrupted exit and the idle circle tap. " +
                    "Rotation, camera and positions are simulated, so you can watch normal and reverse landscape without " +
                    "turning the phone. Turn the phone to landscape and keep this tab open. Your settings are not changed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ChipRow("Rotation", rotationName == "ALL", { rotationName = "ALL" }, LabRotation.entries.map { it.name to it.label },
                rotationName) { rotationName = it }
            ChipRow("Idle circle", idleName == "ALL", { idleName = "ALL" }, LabIdle.entries.map { it.name to it.label },
                idleName) { idleName = it }
            ChipRow("Pill with a card", pillName == "ALL", { pillName = "ALL" }, LabPill.entries.map { it.name to it.label },
                pillName) { pillName = it }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick run (entry, expand, exit only)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = quick, onCheckedChange = { quick = it }, enabled = !progress.running)
            }

            if (progress.running) {
                val shown = progress.stage
                Text(
                    text = when {
                        progress.waitingForLandscape || !landscape -> "Waiting for landscape. Turn the phone."
                        shown != null -> "${progress.stageIndex + 1} / ${progress.stageCount}: ${shown.rotation.label}, " +
                            "${shown.idle.label.lowercase()}, ${shown.pill.label.lowercase()}"
                        else -> "Starting"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (progress.stepTitle.isNotEmpty()) {
                    Text(
                        text = progress.stepTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (progress.stageCount > 0) {
                    LinearProgressIndicator(
                        progress = { progress.stageIndex.toFloat() / progress.stageCount },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedButton(onClick = { LandscapeDemo.stop(notificationRepository) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Stop")
                }
            } else {
                Text(
                    text = when {
                        notificationRepository == null -> "The island is not ready yet. Open the app again."
                        stages.isEmpty() -> "Nothing selected."
                        progress.finished -> "Done. ${stages.size} layouts played."
                        else -> "${stages.size} layout${if (stages.size == 1) "" else "s"}, about " +
                            "${totalSeconds / 60} min ${totalSeconds % 60} s."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { notificationRepository?.let { LandscapeDemo.start(context, it, stages, quick) } },
                    enabled = notificationRepository != null && stages.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (landscape) "Play" else "Play (starts when you turn to landscape)")
                }
            }
        }
    }
}

@Composable
private fun ChipRow(
    title: String,
    allSelected: Boolean,
    onAll: () -> Unit,
    options: List<Pair<String, String>>,
    selected: String,
    onPick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = allSelected, onClick = onAll, label = { Text("All") })
            options.forEach { (name, label) ->
                FilterChip(selected = !allSelected && selected == name, onClick = { onPick(name) }, label = { Text(label) })
            }
        }
    }
}
