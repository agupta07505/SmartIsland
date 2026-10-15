/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.agupta07505.smartisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.ui.search.searchAnchor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** "7.5 s" style text for a value in milliseconds. Whole seconds drop the decimal. */
internal fun formatSeconds(ms: Float): String {
    val tenths = (ms / 100f).roundToInt()
    val whole = tenths / 10
    val frac = tenths % 10
    return if (frac == 0) "$whole s" else "$whole.$frac s"
}

/** "15 min" style text for a value in minutes; 0 means the expiry is off. */
internal fun formatMinutes(minutes: Float): String {
    val m = minutes.roundToInt()
    return if (m <= 0) "Never" else "$m min"
}

/** Rounds [value] to the nearest multiple of [step] and keeps it inside [min]..[max]. */
internal fun snapToStep(value: Float, step: Float, min: Float, max: Float): Float =
    ((value / step).roundToInt() * step).coerceIn(min, max)

/**
 * Settings > Timing: the timers the person can adjust. Sliders keep a local draft while
 * dragging and write to the settings once, when the finger lifts (or a +/- button is pressed).
 */
@Composable
fun TimingSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    val neverText = stringResource(R.string.timing_never)

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

                TimingSlider(
                    searchKey = "timing.idle",
                    label = stringResource(R.string.timing_idle_label),
                    hint = stringResource(R.string.timing_idle_hint),
                    stored = settings.idleTimeoutMs,
                    min = SmartIslandSettings.MIN_IDLE_TIMEOUT_MS,
                    max = SmartIslandSettings.MAX_IDLE_TIMEOUT_MS,
                    step = 500,
                    onCommit = { scope.launch { repository.setIdleTimeoutMs(it) } },
                    valueText = { ms ->
                        if (ms.roundToInt() >= SmartIslandSettings.IDLE_TIMEOUT_NEVER_MS) neverText else formatSeconds(ms)
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                TimingSlider(
                    searchKey = "timing.battery",
                    label = stringResource(R.string.timing_battery_label),
                    hint = stringResource(R.string.timing_battery_hint),
                    stored = settings.batteryWarningMs,
                    min = SmartIslandSettings.MIN_BATTERY_WARNING_MS,
                    max = SmartIslandSettings.MAX_BATTERY_WARNING_MS,
                    step = 500,
                    onCommit = { scope.launch { repository.setBatteryWarningMs(it) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                TimingSlider(
                    searchKey = "timing.popup",
                    label = stringResource(R.string.timing_popup_label),
                    hint = stringResource(R.string.timing_popup_hint),
                    stored = settings.popupMs,
                    min = SmartIslandSettings.MIN_POPUP_MS,
                    max = SmartIslandSettings.MAX_POPUP_MS,
                    step = 100,
                    onCommit = { scope.launch { repository.setPopupMs(it) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                TimingSlider(
                    searchKey = "timing.merge",
                    label = stringResource(R.string.timing_merge_label),
                    hint = stringResource(R.string.timing_merge_hint),
                    stored = settings.secondaryMergeMs,
                    min = SmartIslandSettings.MIN_SECONDARY_MERGE_MS,
                    max = SmartIslandSettings.MAX_SECONDARY_MERGE_MS,
                    step = 100,
                    onCommit = { scope.launch { repository.setSecondaryMergeMs(it) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                TimingSlider(
                    searchKey = "timing.transfer",
                    label = stringResource(R.string.timing_transfer_label),
                    hint = stringResource(R.string.timing_transfer_hint),
                    stored = settings.transferExpiryMinutes,
                    min = 0,
                    max = SmartIslandSettings.MAX_TRANSFER_EXPIRY_MINUTES,
                    step = 5,
                    valueText = ::formatMinutes,
                    onCommit = { scope.launch { repository.setTransferExpiryMinutes(it) } }
                )
            }
        }

        OutlinedButton(
            onClick = { scope.launch { repository.resetTimings() } },
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
    // Local draft so dragging is smooth and only the final value is written. It follows the
    // stored value again whenever that changes (reset, restore from backup).
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
