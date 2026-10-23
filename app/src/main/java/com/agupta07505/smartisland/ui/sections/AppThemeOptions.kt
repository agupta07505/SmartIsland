/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.ui.glass.Switch

// Settings > Appearance: the liquid glass switch, Material You, and the Material colour picker for the app's accent.

private val MaterialAccentSwatches = listOf(
    0xFFF44336L to "Red",
    0xFFE91E63L to "Pink",
    0xFF9C27B0L to "Purple",
    0xFF673AB7L to "Deep purple",
    0xFF3F51B5L to "Indigo",
    0xFF2196F3L to "Blue",
    0xFF03A9F4L to "Light blue",
    0xFF00BCD4L to "Cyan",
    0xFF009688L to "Teal",
    0xFF4CAF50L to "Green",
    0xFF8BC34AL to "Light green",
    0xFFCDDC39L to "Lime",
    0xFFFFC107L to "Amber",
    0xFFFF9800L to "Orange",
    0xFFFF5722L to "Deep orange",
    0xFF795548L to "Brown",
    0xFF607D8BL to "Blue grey",
    0xFF4A8CFFL to "Azure (default)"
)

private const val SWATCHES_PER_ROW = 6

private fun Long.toComposeColor(): Color = Color(this.toInt())

private fun Color.toArgbLong(): Long = this.toArgb().toLong() and 0xFFFFFFFFL

private fun hueOf(argb: Long): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb.toInt(), hsv)
    return hsv[0]
}

/** The rows that go inside the Appearance group of the Settings tab, below the Light / Dark / System selector. */
@Composable
fun AppThemeOptions(
    settings: SmartIslandSettings,
    onLiquidGlassChange: (Boolean) -> Unit,
    onMaterialYouChange: (Boolean) -> Unit,
    onAccentChange: (Long) -> Unit
) {
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val usingDynamic = settings.materialYou && dynamicAvailable
    Column {
        ThemeToggleRow(
            title = "Vibrant colors",
            subtitle = "Translucent glass panels with a lit rim over a soft colour backdrop. Off gives the flat Material look.",
            checked = settings.liquidGlassTheme,
            onChange = onLiquidGlassChange
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        if (dynamicAvailable) {
            ThemeToggleRow(
                title = "Material You colours",
                subtitle = "Take the colours from your wallpaper instead of the accent colour below.",
                checked = settings.materialYou,
                onChange = onMaterialYouChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .alpha(if (usingDynamic) 0.40f else 1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = "Accent colour",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (usingDynamic) "Not used while Material You is on." else "Pick a Material colour. The whole app palette is built from it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val selected = settings.accentColor
            MaterialAccentSwatches.chunked(SWATCHES_PER_ROW).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { (argb, name) ->
                        AccentSwatch(
                            color = argb.toComposeColor(),
                            name = name,
                            selected = selected == argb,
                            enabled = !usingDynamic,
                            onClick = { onAccentChange(argb) }
                        )
                    }
                }
            }
            CustomHueSlider(accent = selected, enabled = !usingDynamic, onAccentChange = onAccentChange)
        }
    }
}

@Composable
private fun ThemeToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun AccentSwatch(color: Color, name: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val ring = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .then(if (selected) Modifier.border(BorderStroke(2.5.dp, ring), CircleShape) else Modifier)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = "$name, selected", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CustomHueSlider(accent: Long, enabled: Boolean, onAccentChange: (Long) -> Unit) {
    var hue by remember(accent) { mutableFloatStateOf(hueOf(accent)) }
    val rainbow = remember { (0..12).map { Color.hsv(it * 30f, 0.80f, 0.95f) } }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Custom hue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(modifier = Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(rainbow))
            )
            Slider(
                value = hue,
                onValueChange = { hue = it },
                onValueChangeFinished = { onAccentChange(Color.hsv(hue, 0.72f, 0.95f).toArgbLong()) },
                valueRange = 0f..360f,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    disabledActiveTrackColor = Color.Transparent,
                    disabledInactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                    thumbColor = Color.White,
                    disabledThumbColor = Color.White
                )
            )
        }
    }
}
