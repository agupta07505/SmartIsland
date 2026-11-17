/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.ui.SettingsGroupCard
import com.agupta07505.smartisland.ui.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun ColorStudioCard(
    featureConfigs: List<FeatureColorConfig>,
    scope: CoroutineScope,
    onPickColor: (String, Int, Long) -> Unit,
    settings: SmartIslandSettings,
    onSetEventColor: (EventColorRow, Long) -> Unit,
    onPickEventCustom: (EventColorRow, Long) -> Unit,
    onResetEvent: (EventColorRow) -> Unit,
    onResetAllEvents: () -> Unit
) {
    val eventItem: @Composable (EventColorRow, Boolean) -> Unit = { row, indented ->
        EventColorRowItem(
            row = row,
            settings = settings,
            indented = indented,
            onSetColor = onSetEventColor,
            onPickCustom = onPickEventCustom,
            onReset = onResetEvent
        )
    }

    SettingsGroupCard(spacing = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.card_color_studio_title).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            if (settings.eventColors.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.event_colors_reset_all),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .bounceClick { onResetAllEvents() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }

        featureConfigs.forEachIndexed { index, config ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(config.color).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = config.icon,
                            contentDescription = null,
                            tint = Color(config.color),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = stringResource(config.titleRes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                ColorPresetRow(
                    selectedColor = config.color,
                    swatches = PRESET_COLORS,
                    onColorSelected = { newColor -> scope.launch { config.onColorChange(newColor) } },
                    onCustomClicked = { onPickColor(config.id, config.titleRes, config.color) }
                )
            }

            eventRowsOf(config.id).forEach { eventRow ->
                SettingsDivider(alpha = 0.15f, modifier = Modifier.padding(start = EVENT_ROW_INDENT))
                eventItem(eventRow, true)
            }

            if (index < featureConfigs.lastIndex) {
                SettingsDivider(alpha = 0.15f)
            }
        }

        SettingsDivider(alpha = 0.15f)
        Text(
            text = "Other",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        OTHER_EVENT_ROWS.forEachIndexed { index, eventRow ->
            eventItem(eventRow, false)
            if (index < OTHER_EVENT_ROWS.lastIndex) {
                SettingsDivider(alpha = 0.15f)
            }
        }
    }
}

@Composable
internal fun ColorPresetRow(
    selectedColor: Long,
    swatches: List<Pair<Long, String>>,
    onColorSelected: (Long) -> Unit,
    onCustomClicked: () -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        swatches.take(4).forEach { (colorValue, _) ->
            val isSelected = selectedColor == colorValue
            SwatchDot(
                selected = isSelected,
                fill = Modifier.background(Color(colorValue)),
                checkTint = Color.White,
                checkDescription = "Selected",
                onClick = { onColorSelected(colorValue) }
            )
        }

        val isCustom = swatches.take(4).none { it.first == selectedColor }
        val rainbowBrush = remember {
            Brush.linearGradient(
                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta)
            )
        }
        SwatchDot(
            selected = isCustom,
            fill = if (isCustom) Modifier.background(Color(selectedColor)) else Modifier.background(rainbowBrush),
            checkTint = if (Color(selectedColor).isLight()) Color.Black else Color.White,
            checkDescription = "Custom",
            onClick = onCustomClicked
        )
    }
}

@Composable
private fun SwatchDot(
    selected: Boolean,
    fill: Modifier,
    checkTint: Color,
    checkDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            )
            .padding(2.dp)
            .clip(CircleShape)
            .then(fill)
            .bounceClick(onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = checkDescription,
                tint = checkTint,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

private fun Color.isLight(): Boolean = red * 0.299 + green * 0.587 + blue * 0.114 > 0.5

@Composable
internal fun RgbColorPickerDialog(
    title: String,
    initialColor: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    val initialColorObj = Color(initialColor)
    var red by rememberSaveable { mutableIntStateOf(initialColorObj.red.toChannel()) }
    var green by rememberSaveable { mutableIntStateOf(initialColorObj.green.toChannel()) }
    var blue by rememberSaveable { mutableIntStateOf(initialColorObj.blue.toChannel()) }
    var hexInput by rememberSaveable { mutableStateOf(String.format("%02X%02X%02X", red, green, blue)) }
    var isHexError by rememberSaveable { mutableStateOf(false) }

    fun updateFromRgb(newR: Int, newG: Int, newB: Int) {
        red = newR.coerceIn(0, 255)
        green = newG.coerceIn(0, 255)
        blue = newB.coerceIn(0, 255)
        hexInput = String.format("%02X%02X%02X", red, green, blue)
        isHexError = false
    }

    fun updateFromHex(input: String) {
        val clean = input.removePrefix("#").trim().uppercase()
        hexInput = clean
        if (clean.length == 6 && clean.all { it in "0123456789ABCDEF" }) {
            isHexError = false
            val parsedR = clean.substring(0, 2).toInt(16)
            val parsedG = clean.substring(2, 4).toInt(16)
            val parsedB = clean.substring(4, 6).toInt(16)
            red = parsedR
            green = parsedG
            blue = parsedB
        } else {
            isHexError = clean.isNotEmpty()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ColorPreviewBox(Color(packRgb(red, green, blue)), hexInput)

                HexColorField(
                    hexInput = hexInput,
                    isHexError = isHexError,
                    onValueChange = { updateFromHex(it) }
                )

                ColorSwatchStrip { colorVal ->
                    val c = Color(colorVal)
                    updateFromRgb(
                        (c.red * 255f).toInt(),
                        (c.green * 255f).toInt(),
                        (c.blue * 255f).toInt()
                    )
                }

                ChannelSlider(stringResource(R.string.color_red), red) { updateFromRgb(it.toInt(), green, blue) }
                ChannelSlider(stringResource(R.string.color_green), green) { updateFromRgb(red, it.toInt(), blue) }
                ChannelSlider(stringResource(R.string.color_blue), blue) { updateFromRgb(red, green, it.toInt()) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(packRgb(red, green, blue)) }
            ) {
                Text(stringResource(R.string.btn_save_color), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

private fun Float.toChannel(): Int = (this * 255f).toInt().coerceIn(0, 255)

private fun packRgb(red: Int, green: Int, blue: Int): Long =
    0xFF000000L or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()

@Composable
private fun ColorPreviewBox(previewColor: Color, hexInput: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(previewColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "#$hexInput",
            fontWeight = FontWeight.Bold,
            color = if (previewColor.isLight()) Color.Black else Color.White
        )
    }
}

@Composable
private fun HexColorField(
    hexInput: String,
    isHexError: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = hexInput,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.color_hex_input)) },
        placeholder = { Text(stringResource(R.string.color_hex_hint)) },
        prefix = { Text("#", fontWeight = FontWeight.Bold) },
        isError = isHexError,
        supportingText = if (isHexError) {
            { Text(stringResource(R.string.color_hex_error), color = MaterialTheme.colorScheme.error) }
        } else null,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun ColorSwatchStrip(onPick: (Long) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(
            0xFF000000L,
            0xFF1C1C1EL,
            0xFF0A192FL,
            0xFF1E1B4BL,
            0xFF10B981L,
            0xFF38BDF8L,
            0xFFEF4444L
        ).forEach { colorVal ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(colorVal))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(6.dp)
                    )
                    .bounceClick { onPick(colorVal) }
            )
        }
    }
}

@Composable
private fun ChannelSlider(label: String, value: Int, onValueChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text("$value", style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
