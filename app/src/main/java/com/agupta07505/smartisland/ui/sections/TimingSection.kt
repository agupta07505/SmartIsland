/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.ui.glass.Card
import com.agupta07505.smartisland.ui.search.searchAnchor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** "7.5 s" style text for a value in milliseconds. */
internal fun formatSeconds(ms: Float): String {
    val tenths = (ms / 100f).roundToInt()
    val whole = tenths / 10
    val frac = tenths % 10
    return if (frac == 0) "$whole s" else "$whole.$frac s"
}

/** "1.25x" style text for a pulse speed in percent (100 is "1x"). */
internal fun formatSpeed(percent: Float): String {
    val p = percent.roundToInt()
    val whole = p / 100
    val frac = p % 100
    return when {
        frac == 0 -> "${whole}x"
        frac % 10 == 0 -> "$whole.${frac / 10}x"
        else -> "$whole.${frac.toString().padStart(2, '0')}x"
    }
}

internal fun formatMinutes(minutes: Float): String {
    val m = minutes.roundToInt()
    return if (m <= 0) "Never" else "$m min"
}

internal fun snapToStep(value: Float, step: Float, min: Float, max: Float): Float =
    ((value / step).roundToInt() * step).coerceIn(min, max)

/** Settings > Timing: the timers the person can adjust. */
@Composable
fun TimingSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val savedText = stringResource(R.string.timing_saved)
    val resetText = stringResource(R.string.timing_reset_done)
    val showSaved = { context.showToast(savedText) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.timing_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TimingControl(
                    searchKey = "timing.idle",
                    label = stringResource(R.string.timing_idle_label),
                    hint = stringResource(R.string.timing_idle_hint),
                    storedMs = settings.idleTimeoutMs,
                    never = settings.idleTimeoutNever,
                    minMs = SmartIslandSettings.MIN_IDLE_TIMEOUT_MS,
                    maxMs = SmartIslandSettings.MAX_IDLE_TIMEOUT_MS,
                    stepMs = 500,
                    presetsMs = listOf(3_000, 5_000, 10_000, 30_000),
                    onCommitMs = { value -> scope.launch { repository.setIdleTimeoutMs(value) }; showSaved() },
                    onNeverChange = { value -> scope.launch { repository.setIdleTimeoutNever(value) }; showSaved() }
                )
                SettingsDivider(0.2f)
                TimingControl(
                    searchKey = "timing.autohide",
                    label = stringResource(R.string.timing_autohide_label),
                    hint = stringResource(R.string.timing_autohide_hint),
                    storedMs = settings.autoHideTimeoutSeconds * 1000,
                    never = !settings.autoHidePill,
                    minMs = SmartIslandSettings.MIN_AUTO_HIDE_SECONDS * 1000,
                    maxMs = SmartIslandSettings.MAX_AUTO_HIDE_SECONDS * 1000,
                    stepMs = 1000,
                    presetsMs = listOf(3_000, 5_000, 10_000, 30_000),
                    // never on = the pill is never auto-hidden
                    onCommitMs = { value ->
                        scope.launch { repository.setAutoHideTimeoutSeconds(value / 1000) }; showSaved() },
                    onNeverChange = { value -> scope.launch { repository.setAutoHidePill(!value) }; showSaved() }
                )
                SettingsDivider(0.2f)
                TimingSlider(
                    searchKey = "timing.pulse",
                    label = stringResource(R.string.timing_pulse_label),
                    hint = stringResource(R.string.timing_pulse_hint),
                    stored = settings.pulseHoldPercent,
                    min = SmartIslandSettings.MIN_PULSE_HOLD_PERCENT,
                    max = SmartIslandSettings.MAX_PULSE_HOLD_PERCENT,
                    step = 5,
                    valueText = ::formatSpeed,
                    onCommit = { scope.launch { repository.setPulseHoldPercent(it) }; showSaved() }
                )
                SettingsDivider(0.2f)
                TimingSlider(
                    searchKey = "timing.battery",
                    label = stringResource(R.string.timing_battery_label),
                    hint = stringResource(R.string.timing_battery_hint),
                    stored = settings.batteryWarningMs,
                    min = SmartIslandSettings.MIN_BATTERY_WARNING_MS,
                    max = SmartIslandSettings.MAX_BATTERY_WARNING_MS,
                    step = 500,
                    onCommit = { scope.launch { repository.setBatteryWarningMs(it) }; showSaved() }
                )
                SettingsDivider(0.2f)
                TimingSlider(
                    searchKey = "timing.merge",
                    label = stringResource(R.string.timing_merge_label),
                    hint = stringResource(R.string.timing_merge_hint),
                    stored = settings.secondaryMergeMs,
                    min = SmartIslandSettings.MIN_SECONDARY_MERGE_MS,
                    max = SmartIslandSettings.MAX_SECONDARY_MERGE_MS,
                    step = 100,
                    onCommit = { scope.launch { repository.setSecondaryMergeMs(it) }; showSaved() }
                )
                SettingsDivider(0.2f)
                TimingSlider(
                    searchKey = "timing.transfer",
                    label = stringResource(R.string.timing_transfer_label),
                    hint = stringResource(R.string.timing_transfer_hint),
                    stored = settings.transferExpiryMinutes,
                    min = 0,
                    max = SmartIslandSettings.MAX_TRANSFER_EXPIRY_MINUTES,
                    step = 5,
                    valueText = ::formatMinutes,
                    onCommit = { scope.launch { repository.setTransferExpiryMinutes(it) }; showSaved() }
                )
            }
        }

        OutlinedButton(
            onClick = {
                scope.launch { repository.resetTimings() }
                context.showToast(resetText)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.timing_reset))
        }
    }
}

@Composable
private fun TimingSlider(
    searchKey: String,
    label: String,
    hint: String,
    stored: Int,
    min: Int,
    max: Int,
    step: Int,
    onCommit: (Int) -> Unit,
    valueText: (Float) -> String = ::formatSeconds
) {
    // Local draft so dragging is smooth and only the final value is written
    var draft by remember(stored) { mutableFloatStateOf(stored.toFloat()) }
    Column(modifier = Modifier.searchAnchor(searchKey)) {
        SliderSettingItem(
            label = label,
            value = draft,
            range = min.toFloat()..max.toFloat(),
            step = step.toFloat(),
            valueText = valueText,
            onValueChange = { draft = snapToStep(it, step.toFloat(), min.toFloat(), max.toFloat()) },
            onValueChangeFinished = { onCommit(draft.roundToInt()) }
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Timing the person can set 3 ways. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimingControl(
    searchKey: String,
    label: String,
    hint: String,
    storedMs: Int,
    never: Boolean,
    minMs: Int,
    maxMs: Int,
    stepMs: Int,
    presetsMs: List<Int>,
    onCommitMs: (Int) -> Unit,
    onNeverChange: (Boolean) -> Unit
) {
    val neverText = stringResource(R.string.timing_never)
    var draft by remember(storedMs) { mutableFloatStateOf(storedMs.toFloat()) }
    Column(modifier = Modifier.searchAnchor(searchKey)) {
        SliderSettingItem(
            label = label,
            value = draft,
            range = minMs.toFloat()..maxMs.toFloat(),
            step = stepMs.toFloat(),
            valueText = { ms -> if (never) neverText else formatSeconds(ms) },
            onValueChange = { draft = snapToStep(it, stepMs.toFloat(), minMs.toFloat(), maxMs.toFloat()) },
            onValueChangeFinished = { onCommitMs(draft.roundToInt()) },
            enabled = !never,
            buttonSize = 36.dp
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetsMs.forEach { preset ->
                FilterChip(
                    selected = !never && storedMs == preset,
                    onClick = { onCommitMs(preset) },
                    enabled = !never,
                    label = { Text("${preset / 1000} s", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            FilterChip(
                selected = never,
                onClick = { onNeverChange(!never) },
                label = { Text(neverText, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
