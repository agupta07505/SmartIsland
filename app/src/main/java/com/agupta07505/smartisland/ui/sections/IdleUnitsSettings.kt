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
import com.agupta07505.smartisland.ui.glass.Card
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
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.ui.search.searchAnchor
import kotlinx.coroutines.launch

/**
 * Settings > Idle widgets > Units: the temperature unit (Auto, °C, °F) and the wind speed unit (Auto, km/h, m/s, mph, kn). Auto
 * follows the region of the phone. Always visible, so the choice does not hide behind the Weather widget being selected.
 */
@Composable
internal fun IdleUnitsCard(settings: SmartIslandSettings, repository: SmartIslandSettingsRepository) {
    val scope = rememberCoroutineScope()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().searchAnchor("idle.units")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.idle_units_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            UnitChoices(
                title = stringResource(R.string.idle_widgets_unit_title),
                options = listOf(
                    IdleWidgets.UNIT_AUTO to stringResource(R.string.idle_widgets_unit_auto),
                    IdleWidgets.UNIT_CELSIUS to stringResource(R.string.idle_widgets_unit_c),
                    IdleWidgets.UNIT_FAHRENHEIT to stringResource(R.string.idle_widgets_unit_f)
                ),
                selected = settings.idleWidgetTempUnit,
                onSelect = { id -> scope.launch { repository.setIdleWidgetTempUnit(id) } }
            )
            UnitChoices(
                title = stringResource(R.string.idle_units_wind_title),
                options = listOf(
                    IdleWidgets.UNIT_AUTO to stringResource(R.string.idle_widgets_unit_auto),
                    IdleWidgets.WIND_KMH to stringResource(R.string.idle_units_wind_kmh),
                    IdleWidgets.WIND_MS to stringResource(R.string.idle_units_wind_ms),
                    IdleWidgets.WIND_MPH to stringResource(R.string.idle_units_wind_mph),
                    IdleWidgets.WIND_KN to stringResource(R.string.idle_units_wind_kn)
                ),
                selected = settings.idleWidgetWindUnit,
                onSelect = { id -> scope.launch { repository.setIdleWidgetWindUnit(id) } }
            )
            Text(
                text = stringResource(R.string.idle_units_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UnitChoices(title: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
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
            options.forEach { (id, label) ->
                val on = selected == id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onSelect(id) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
