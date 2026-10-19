/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.Dimens
import com.agupta07505.smartisland.ui.SpringPreset
import com.agupta07505.smartisland.ui.components.GroupedRow
import com.agupta07505.smartisland.ui.components.GroupedSection
import com.agupta07505.smartisland.ui.components.SwitchRow
import kotlinx.coroutines.launch

/**
 * How the island's size springs, and whether it glows while it moves fast (Rule_Book 11.3 Motion Physics):
 * the presets are one grouped list with an accent checkmark on the active one, and the kinetic glow toggle
 * sits alone in its own group at the bottom.
 */
@Composable
fun MotionSettings(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    val selected = SpringPreset.fromId(settings.springPreset)
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)) {
        GroupedSection(
            header = stringResource(R.string.layout_header_motion).uppercase(),
            footer = stringResource(R.string.motion_footer)
        ) {
            SpringPreset.entries.forEach { preset ->
                GroupedRow(
                    title = stringResource(presetTitle(preset)),
                    subtitle = if (preset.isPhysical) {
                        stringResource(R.string.motion_preset_values, preset.stiffness.toInt(), formatDamping(preset.damping))
                    } else {
                        stringResource(R.string.motion_standard_desc)
                    },
                    showChevron = false,
                    onClick = { scope.launch { repository.setSpringPreset(preset.id) } },
                    trailing = if (preset == selected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = stringResource(R.string.motion_selected),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else null
                )
            }
        }
        GroupedSection(footer = stringResource(R.string.motion_glow_desc)) {
            SwitchRow(
                title = stringResource(R.string.motion_glow_title),
                checked = settings.kineticGlow,
                onCheckedChange = { scope.launch { repository.setKineticGlow(it) } }
            )
        }
    }
}

private fun presetTitle(preset: SpringPreset): Int = when (preset) {
    SpringPreset.Standard -> R.string.motion_standard
    SpringPreset.Bouncy -> R.string.motion_bouncy
    SpringPreset.Snappy -> R.string.motion_snappy
    SpringPreset.Critical -> R.string.motion_critical
    SpringPreset.Loose -> R.string.motion_loose
}

/** 12.0 -> "12", 27.5 -> "27.5". */
private fun formatDamping(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else value.toString()
