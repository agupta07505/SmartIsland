/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import kotlinx.coroutines.launch

private val PRESET_COLORS = listOf(
    0xFF10B981L to "Green",
    0xFF2563EBL to "Blue",
    0xFFFF6B9AL to "Pink",
    0xFFEF4444L to "Red",
    0xFFF59E0BL to "Yellow",
    0xFF8B5CF6L to "Purple",
    0xFFF97316L to "Orange"
)

@Composable
fun CustomizationsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Battery Color
        ColorSelectorCard(
            title = "Battery charging color",
            description = "Color of the battery percentage, charging bolt, and progress ring",
            selectedColor = settings.batteryColor,
            onColorSelected = { scope.launch { repository.setBatteryColor(it) } }
        )

        // Card 2: Notification Dot Color
        ColorSelectorCard(
            title = "Notification dot color",
            description = "Color of the notification dot indicator visible when in collapsed mode",
            selectedColor = settings.notificationDotColor,
            onColorSelected = { scope.launch { repository.setNotificationDotColor(it) } }
        )

        // Card 3: Music Visualizer Color
        ColorSelectorCard(
            title = "Music visualizer color",
            description = "Color of the jumping audio frequency bars when a song is playing",
            selectedColor = settings.musicVisualizerColor,
            onColorSelected = { scope.launch { repository.setMusicVisualizerColor(it) } }
        )

        Spacer(Modifier.height(4.dp))

        // Reset Customizations button
        OutlinedButton(
            onClick = {
                scope.launch {
                    repository.setBatteryColor(0xFF10B981L)
                    repository.setNotificationDotColor(0xFF2563EBL)
                    repository.setMusicVisualizerColor(0xFFFF6B9AL)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Reset customizations")
        }
    }
}

@Composable
private fun ColorSelectorCard(
    title: String,
    description: String,
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PRESET_COLORS.forEach { (colorValue, name) ->
                    val isSelected = selectedColor == colorValue
                    val borderModifier = if (isSelected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .then(borderModifier)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color(colorValue))
                            .clickable { onColorSelected(colorValue) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
