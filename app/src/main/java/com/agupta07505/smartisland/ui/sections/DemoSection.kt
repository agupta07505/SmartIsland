/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.SettingsCard
import com.agupta07505.smartisland.ui.SettingsDescription
import com.agupta07505.smartisland.ui.SettingsGroupTitle
import com.agupta07505.smartisland.ui.components.CollapsibleCard
import com.agupta07505.smartisland.ui.glass.Switch
import com.agupta07505.smartisland.util.DemoEvents
import com.agupta07505.smartisland.util.DemoNotification
import com.agupta07505.smartisland.util.FaceIdController
import com.agupta07505.smartisland.util.PulseDemo
import com.agupta07505.smartisland.util.PulseScenario
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A button per event that puts a sample card on the island, so every cutout, card and switch can be tried
 * Sample cards never touch the system shade
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
        DemoControlsCard(
            settings = settings,
            notificationRepository = notificationRepository,
            openExpanded = openExpanded,
            onOpenExpandedChange = { openExpanded = it },
            showingKeys = showingKeys
        )

        PulseScenarioCard(notificationRepository, settings.enabled, context)

        val faceIdState by FaceIdController.state.collectAsState()
        DemoEvents.available.groupBy { it.group }.entries.forEachIndexed { index, (group, events) ->
            DemoGroupCard(
                group = group,
                events = events,
                settings = settings,
                notificationRepository = notificationRepository,
                openExpanded = openExpanded,
                showingKeys = showingKeys,
                faceIdState = faceIdState,
                context = context
            )
        }
    }
}

@Composable
private fun DemoControlsCard(
    settings: SmartIslandSettings,
    notificationRepository: INotificationRepository?,
    openExpanded: Boolean,
    onOpenExpandedChange: (Boolean) -> Unit,
    showingKeys: Set<String>
) {
    val context = LocalContext.current
    SettingsCard(cornerRadius = 20.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsGroupTitle("Sample cards")
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
                Switch(checked = openExpanded, onCheckedChange = onOpenExpandedChange)
            }
            FilledTonalButton(
                onClick = { notificationRepository?.let { DemoEvents.clearAll(it, context) } },
                enabled = notificationRepository != null && showingKeys.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.ClearAll, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (showingKeys.isEmpty()) "No demo cards showing" else "Clear all demo cards (${showingKeys.size})")
            }
        }
    }
}

@Composable
private fun PulseScenarioCard(
    notificationRepository: INotificationRepository?,
    enabled: Boolean,
    context: android.content.Context
) {
    SettingsCard(cornerRadius = 20.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsGroupTitle("Pulse scenarios")
            PulseScenario.entries.forEach { scenario ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = scenario.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        SettingsDescription(scenario.hint)
                    }
                    FilledTonalButton(
                        onClick = { notificationRepository?.let { PulseDemo.play(context, it, scenario) } },
                        enabled = enabled && notificationRepository != null
                    ) {
                        Text("Play")
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoGroupCard(
    group: String,
    events: List<IslandEvent>,
    settings: SmartIslandSettings,
    notificationRepository: INotificationRepository?,
    openExpanded: Boolean,
    showingKeys: Set<String>,
    faceIdState: FaceIdController.State,
    context: Context
) {
    fun isShowing(e: IslandEvent) =
        if (e == IslandEvent.NOTIFICATION) showingKeys.any { DemoNotification.isDemoKey(it) }
        else DemoEvents.keyFor(e) in showingKeys
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) DemoNotification.post(context) }
    val showingCount = events.count { isShowing(it) }
    CollapsibleCard(
        title = group,
        stateKey = "demo_group_$group",
        summary = if (showingCount > 0) "$showingCount showing" else "${events.size} samples",
        icon = iconFor(events.first()),
        initiallyExpanded = false
    ) {
        events.forEachIndexed { rowIndex, event ->
            if (rowIndex > 0) {
                SettingsDivider(0.2f)
            }
            val showing = if (event == IslandEvent.FACE_UNLOCK) faceIdState.active else isShowing(event)
            val off = !settings.isEventEnabled(event.id)
            DemoRow(
                title = if (event == IslandEvent.FACE_UNLOCK) "Face ID expanded / mix (no face found)" else event.label,
                subtitle = if (off) "Switched off on the Home tab" else null,
                event = event,
                showing = showing,
                enabled = notificationRepository != null || event == IslandEvent.FACE_UNLOCK,
                onShow = {
                    if (event == IslandEvent.FACE_UNLOCK) {
                        if (!off) FaceIdController.start()
                    } else if (event == IslandEvent.NOTIFICATION) {
                        if (off) return@DemoRow
                        if (!DemoNotification.post(context) && Build.VERSION.SDK_INT >= 33) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        notificationRepository?.let { DemoEvents.show(context, it, event, openExpanded,
                            settings.batteryWarningMs.toLong()) }
                    }
                },
                onClear = {
                    if (event == IslandEvent.FACE_UNLOCK) {
                        FaceIdController.stop()
                    } else {
                        notificationRepository?.let { DemoEvents.clear(it, event, context) }
                    }
                }
            )
            if (event == IslandEvent.FACE_UNLOCK) {
                FaceIdDemoRows(event = event, off = off, faceIdState = faceIdState)
            }
        }
    }
}

@Composable
private fun FaceIdDemoRows(
    event: IslandEvent,
    off: Boolean,
    faceIdState: FaceIdController.State
) {
    SettingsDivider(0.2f)
    DemoRow(
        title = "Face ID expanded (face accepted)",
        subtitle = if (off) "Switched off on the Home tab" else null,
        event = event,
        showing = faceIdState.active,
        enabled = true,
        onShow = {
            if (!off) FaceIdController.start(accepted = true, style = FaceIdController.Style.SquareTick)
        },
        onClear = { FaceIdController.stop() }
    )
    SettingsDivider(0.2f)
    DemoRow(
        title = "Face ID mix (face accepted)",
        subtitle = if (off) "Switched off on the Home tab" else null,
        event = event,
        showing = faceIdState.active,
        enabled = true,
        onShow = {
            if (!off) FaceIdController.start(accepted = true)
        },
        onClear = { FaceIdController.stop() }
    )
    SettingsDivider(0.2f)
    DemoRow(
        title = "Face ID cutout mode (no face found)",
        subtitle = if (off) "Switched off on the Home tab" else null,
        event = event,
        showing = faceIdState.active,
        enabled = true,
        onShow = {
            if (!off) FaceIdController.start(style = FaceIdController.Style.Cutout)
        },
        onClear = { FaceIdController.stop() }
    )
    SettingsDivider(0.2f)
    DemoRow(
        title = "Face ID cutout mode (face accepted)",
        subtitle = if (off) "Switched off on the Home tab" else null,
        event = event,
        showing = faceIdState.active,
        enabled = true,
        onShow = {
            if (!off) FaceIdController.start(accepted = true, style = FaceIdController.Style.Cutout)
        },
        onClear = { FaceIdController.stop() }
    )
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
                    SettingsDescription(subtitle)
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
