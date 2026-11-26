/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SettingsCard
import com.agupta07505.smartisland.util.OemDeviceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun OemRulesCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    detectedDevice: OemDeviceType,
    effectiveDevice: OemDeviceType,
    isDeviceMenuExpanded: Boolean,
    onDeviceMenuExpandedChange: (Boolean) -> Unit
) {
    SettingsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OemRulesHeader()

            SettingsDivider(0.3f)

            DeviceDropdown(
                settings = settings,
                repository = repository,
                scope = scope,
                detectedDevice = detectedDevice,
                effectiveDevice = effectiveDevice,
                isDeviceMenuExpanded = isDeviceMenuExpanded,
                onDeviceMenuExpandedChange = onDeviceMenuExpandedChange
            )

            ActiveRulesBox(effectiveDevice = effectiveDevice)
        }
    }
}

@Composable
private fun OemRulesHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Smartphone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.oem_rules_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.oem_rules_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceDropdown(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    detectedDevice: OemDeviceType,
    effectiveDevice: OemDeviceType,
    isDeviceMenuExpanded: Boolean,
    onDeviceMenuExpandedChange: (Boolean) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = isDeviceMenuExpanded,
        onExpandedChange = onDeviceMenuExpandedChange,
        modifier = Modifier.fillMaxWidth()
    ) {
        val currentSelectionText = if (settings.deviceType == "AUTO") {
            stringResource(R.string.auto_detect_device, detectedDevice.displayName)
        } else {
            effectiveDevice.displayName
        }

        OutlinedTextField(
            value = currentSelectionText,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.select_device_oem)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDeviceMenuExpanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = isDeviceMenuExpanded,
            onDismissRequest = { onDeviceMenuExpandedChange(false) }
        ) {
            OemDeviceType.entries.forEach { deviceTypeOption ->
                val isSelected = (settings.deviceType == "AUTO" && deviceTypeOption == OemDeviceType.AUTO) ||
                    (settings.deviceType == deviceTypeOption.name)
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (deviceTypeOption == OemDeviceType.AUTO) stringResource(R.string.auto_detect_device, detectedDevice.displayName) else deviceTypeOption.displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        scope.launch { repository.setDeviceType(deviceTypeOption.name) }
                        onDeviceMenuExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun ActiveRulesBox(effectiveDevice: OemDeviceType) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.active_rules_title, effectiveDevice.displayName),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.active_rules_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
