/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.NetSpeedRules
import com.agupta07505.smartisland.ui.glass.Card
import kotlinx.coroutines.launch

/**
 * Settings > Idle widgets > Network speed: the speed the busier direction must reach before the widget takes a turn
 * on the pill (Off, 50 KB/s, 200 KB/s, 1 MB/s).
 */
@Composable
internal fun NetSpeedSettingsCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val selected = NetSpeedRules.normalize(settings.netSpeedThresholdKbps)
    val options = listOf(
        NetSpeedRules.OFF to stringResource(R.string.net_cfg_off),
        NetSpeedRules.KBPS_50 to stringResource(R.string.net_cfg_50k),
        NetSpeedRules.KBPS_200 to stringResource(R.string.net_cfg_200k),
        NetSpeedRules.KBPS_1000 to stringResource(R.string.net_cfg_1m)
    )
    val connection = NetSpeedRules.normalizeConnection(settings.netSpeedConnection)
    val connectionOptions = listOf(
        NetSpeedRules.CONN_ANY to stringResource(R.string.net_cfg_conn_any),
        NetSpeedRules.CONN_WIFI to stringResource(R.string.net_cfg_conn_wifi),
        NetSpeedRules.CONN_MOBILE to stringResource(R.string.net_cfg_conn_mobile)
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.net_cfg_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.net_cfg_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.net_cfg_threshold_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    options.forEach { (kbps, label) ->
                        val on = selected == kbps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { scope.launch { repository.setNetSpeedThresholdKbps(kbps) } }
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(if (selected == NetSpeedRules.OFF) R.string.net_cfg_note_off else R.string.net_cfg_note_on),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Which connection the widget shows on: any, Wi-Fi only, or mobile data only (a data-saver view).
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.net_cfg_conn_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    connectionOptions.forEach { (mode, label) ->
                        val on = connection == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { scope.launch { repository.setNetSpeedConnection(mode) } }
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.net_cfg_conn_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
