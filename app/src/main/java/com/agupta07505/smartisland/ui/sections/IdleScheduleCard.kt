/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IdleSchedule
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.ui.glass.Card
import com.agupta07505.smartisland.ui.glass.Switch
import kotlinx.coroutines.launch

/**
 * Settings > Idle widgets > Schedules: which of the ticked widgets take turns during work hours, at night, or while
 * charging.
 */
@Composable
internal fun IdleScheduleCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    selected: List<IdleWidgetKind>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.idle_sched_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.idle_sched_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ScheduleGroup(
                    title = stringResource(R.string.idle_sched_charging_title),
                    desc = stringResource(R.string.idle_sched_charging_desc),
                    enabled = settings.idleScheduleChargingEnabled,
                    onEnabled = { scope.launch { repository.setIdleScheduleChargingEnabled(it) } },
                    hidden = settings.idleScheduleChargingHidden,
                    selected = selected,
                    onToggle = { scope.launch { repository.setIdleScheduleChargingHidden(IdleSchedule.toggleHidden(settings.idleScheduleChargingHidden, it)) } }
                ) { }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                ScheduleGroup(
                    title = stringResource(R.string.idle_sched_night_title),
                    desc = stringResource(R.string.idle_sched_night_desc),
                    enabled = settings.idleScheduleNightEnabled,
                    onEnabled = { scope.launch { repository.setIdleScheduleNightEnabled(it) } },
                    hidden = settings.idleScheduleNightHidden,
                    selected = selected,
                    onToggle = { scope.launch { repository.setIdleScheduleNightHidden(IdleSchedule.toggleHidden(settings.idleScheduleNightHidden, it)) } }
                ) {
                    Stepper(
                        text = stringResource(R.string.idle_sched_from, hourText(settings.idleScheduleNightStartHour)),
                        onChange = { scope.launch { repository.setIdleScheduleNightStartHour(Math.floorMod(settings.idleScheduleNightStartHour + it, 24)) } }
                    )
                    Stepper(
                        text = stringResource(R.string.idle_sched_to, hourText(settings.idleScheduleNightEndHour)),
                        onChange = { scope.launch { repository.setIdleScheduleNightEndHour(Math.floorMod(settings.idleScheduleNightEndHour + it, 24)) } }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                ScheduleGroup(
                    title = stringResource(R.string.idle_sched_work_title),
                    desc = stringResource(R.string.idle_sched_work_desc),
                    enabled = settings.idleScheduleWorkEnabled,
                    onEnabled = { scope.launch { repository.setIdleScheduleWorkEnabled(it) } },
                    hidden = settings.idleScheduleWorkHidden,
                    selected = selected,
                    onToggle = { scope.launch { repository.setIdleScheduleWorkHidden(IdleSchedule.toggleHidden(settings.idleScheduleWorkHidden, it)) } }
                ) {
                    Stepper(
                        text = stringResource(R.string.idle_sched_from, hourText(settings.idleScheduleWorkStartHour)),
                        onChange = { scope.launch { repository.setIdleScheduleWorkStartHour(Math.floorMod(settings.idleScheduleWorkStartHour + it, 24)) } }
                    )
                    Stepper(
                        text = stringResource(R.string.idle_sched_to, hourText(settings.idleScheduleWorkEndHour)),
                        onChange = { scope.launch { repository.setIdleScheduleWorkEndHour(Math.floorMod(settings.idleScheduleWorkEndHour + it, 24)) } }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.idle_sched_weekdays),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = settings.idleScheduleWorkWeekdaysOnly,
                            onCheckedChange = { scope.launch { repository.setIdleScheduleWorkWeekdaysOnly(it) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleGroup(
    title: String,
    desc: String,
    enabled: Boolean,
    onEnabled: (Boolean) -> Unit,
    hidden: String,
    selected: List<IdleWidgetKind>,
    onToggle: (IdleWidgetKind) -> Unit,
    extras: @Composable () -> Unit
) {
    val hiddenKinds = IdleSchedule.parseHidden(hidden)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onEnabled)
        }
        if (enabled) {
            extras()
            Text(
                text = stringResource(R.string.idle_sched_rotate),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            selected.forEach { kind ->
                val on = kind !in hiddenKinds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggle(kind) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = on, onCheckedChange = { onToggle(kind) })
                    Text(
                        text = stringResource(kindTitle(kind)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (selected.all { it in hiddenKinds }) {
                Text(
                    text = stringResource(R.string.idle_sched_none_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Stepper(text: String, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        OutlinedButton(onClick = { onChange(-1) }) { Text(stringResource(R.string.weather_cfg_step_minus)) }
        OutlinedButton(onClick = { onChange(1) }) { Text(stringResource(R.string.weather_cfg_step_plus)) }
    }
}

private fun hourText(hour: Int): String = String.format(java.util.Locale.US, "%02d:00", hour)
