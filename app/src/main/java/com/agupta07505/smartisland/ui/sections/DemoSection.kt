/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Close
import com.agupta07505.smartisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.agupta07505.smartisland.ui.glass.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import com.agupta07505.smartisland.model.IslandNotification
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.ui.components.CollapsibleCard
import com.agupta07505.smartisland.util.DemoEvents
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Demo tab: a button per island event that puts a sample card on the island, so every cutout, expanded card and
 * switch can be tried at any time. Sample cards never touch the system shade.
 */
@Composable
fun DemoSection(
    settings: SmartIslandSettings,
    notificationRepository: INotificationRepository?
) {
    val context = LocalContext.current
    var openExpanded by rememberSaveable { mutableStateOf(true) }
    val noCards = remember { MutableStateFlow<List<IslandNotification>>(emptyList()) }
    val cards by (notificationRepository?.notifications ?: noCards).collectAsState()
    val showingKeys = cards.filter { DemoEvents.isDemoKey(it.key) }.map { it.key }.toSet()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Sample cards",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        notificationRepository == null -> "The island is not ready yet. Open the app again."
                        !settings.enabled -> "The island is off. Turn it on from the Home tab, then tap a button to see that card."
                        else -> "Tap Show to put a sample card on the island. Short events leave by themselves; " +
                            "the others stay until you clear them. Events switched off on the Home tab stay hidden."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Open the card expanded",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = openExpanded, onCheckedChange = { openExpanded = it })
                }
                FilledTonalButton(
                    onClick = { notificationRepository?.let { DemoEvents.clearAll(it) } },
                    enabled = notificationRepository != null && showingKeys.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.ClearAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (showingKeys.isEmpty()) "No demo cards showing" else "Clear all demo cards (${showingKeys.size})")
                }
            }
        }

        val faceIdState by com.agupta07505.smartisland.util.FaceIdController.state.collectAsState()
        DemoEvents.available.groupBy { it.group }.entries.forEachIndexed { index, (group, events) ->
            val showingCount = events.count { DemoEvents.keyFor(it) in showingKeys }
            CollapsibleCard(
                title = group,
                stateKey = "demo_group_$group",
                summary = if (showingCount > 0) "$showingCount showing" else "${events.size} samples",
                icon = iconFor(events.first()),
                initiallyExpanded = false
            ) {
                events.forEachIndexed { rowIndex, event ->
                    if (rowIndex > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    }
                    val showing = if (event == IslandEvent.FACE_UNLOCK) faceIdState.active else DemoEvents.keyFor(event) in showingKeys
                    val off = !settings.isEventEnabled(event.id)
                    DemoRow(
                        title = if (event == IslandEvent.FACE_UNLOCK) "Face ID expanded / mix (no face found)" else event.label,
                        subtitle = if (off) "Switched off on the Home tab" else null,
                        event = event,
                        showing = showing,
                        enabled = notificationRepository != null || event == IslandEvent.FACE_UNLOCK,
                        onShow = {
                            if (event == IslandEvent.FACE_UNLOCK) {
                                if (!off) com.agupta07505.smartisland.util.FaceIdController.start()
                            } else {
                                notificationRepository?.let { DemoEvents.show(context, it, event, openExpanded, settings.batteryWarningMs.toLong()) }
                            }
                        },
                        onClear = {
                            if (event == IslandEvent.FACE_UNLOCK) {
                                com.agupta07505.smartisland.util.FaceIdController.stop()
                            } else {
                                notificationRepository?.let { DemoEvents.clear(it, event) }
                            }
                        }
                    )
                    if (event == IslandEvent.FACE_UNLOCK) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        DemoRow(
                            title = "Face ID expanded (face accepted)",
                            subtitle = if (off) "Switched off on the Home tab" else null,
                            event = event,
                            showing = faceIdState.active,
                            enabled = true,
                            onShow = {
                                if (!off) com.agupta07505.smartisland.util.FaceIdController.start(accepted = true, style = com.agupta07505.smartisland.util.FaceIdController.Style.SquareTick)
                            },
                            onClear = { com.agupta07505.smartisland.util.FaceIdController.stop() }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        DemoRow(
                            title = "Face ID mix (face accepted)",
                            subtitle = if (off) "Switched off on the Home tab" else null,
                            event = event,
                            showing = faceIdState.active,
                            enabled = true,
                            onShow = {
                                if (!off) com.agupta07505.smartisland.util.FaceIdController.start(accepted = true)
                            },
                            onClear = { com.agupta07505.smartisland.util.FaceIdController.stop() }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        DemoRow(
                            title = "Face ID cutout mode (no face found)",
                            subtitle = if (off) "Switched off on the Home tab" else null,
                            event = event,
                            showing = faceIdState.active,
                            enabled = true,
                            onShow = {
                                if (!off) com.agupta07505.smartisland.util.FaceIdController.start(style = com.agupta07505.smartisland.util.FaceIdController.Style.Cutout)
                            },
                            onClear = { com.agupta07505.smartisland.util.FaceIdController.stop() }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        DemoRow(
                            title = "Face ID cutout mode (face accepted)",
                            subtitle = if (off) "Switched off on the Home tab" else null,
                            event = event,
                            showing = faceIdState.active,
                            enabled = true,
                            onShow = {
                                if (!off) com.agupta07505.smartisland.util.FaceIdController.start(accepted = true, style = com.agupta07505.smartisland.util.FaceIdController.Style.Cutout)
                            },
                            onClear = { com.agupta07505.smartisland.util.FaceIdController.stop() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoRow(
    title: String,
    subtitle: String?,
    event: IslandEvent,
    showing: Boolean,
    enabled: Boolean,
    onShow: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (showing) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconFor(event),
                    contentDescription = null,
                    tint = if (showing) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (showing) {
            IconButton(onClick = onClear, enabled = enabled) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear $title")
            }
        } else {
            FilledTonalButton(onClick = onShow, enabled = enabled) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Show")
            }
        }
    }
}
