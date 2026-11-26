/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.agupta07505.smartisland.ui.expanded

import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.util.UtilityActions
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
internal fun UtilityBrightnessSlider() {
    val context = LocalContext.current
    val autoBrightness by produceState(false) {
        while (true) { value = UtilityActions.autoBrightnessEnabled(context); delay(500) }
    }
    UtilitySlider(
        icon = if (autoBrightness) Icons.Rounded.BrightnessAuto else Icons.Rounded.BrightnessMedium,
        description = stringResource(R.string.utility_auto_brightness),
        read = { UtilityActions.brightnessFraction(context) },
        write = { UtilityActions.setBrightness(context, it) },
        onIconClick = { UtilityActions.toggleAutoBrightness(context) },
        iconActive = autoBrightness
    )
}

@Composable
internal fun UtilityRingerSlider() {
    val context = LocalContext.current
    val ringer by produceState(AudioManager.RINGER_MODE_NORMAL) {
        while (true) { value = UtilityActions.ringerMode(context); delay(500) }
    }
    UtilitySlider(
        icon = when (ringer) {
            AudioManager.RINGER_MODE_SILENT -> Icons.Rounded.VolumeOff
            AudioManager.RINGER_MODE_VIBRATE -> Icons.Rounded.Vibration
            else -> Icons.Rounded.VolumeUp
        },
        description = stringResource(R.string.utility_ringer),
        read = { UtilityActions.volumeFraction(context) },
        write = { UtilityActions.setVolume(context, it) },
        onIconClick = { UtilityActions.toggleRinger(context) },
        iconActive = ringer == AudioManager.RINGER_MODE_NORMAL
    )
}

@Composable
private fun UtilitySlider(
    icon: ImageVector,
    description: String,
    read: () -> Float,
    write: (Float) -> Unit,
    onIconClick: (() -> Unit)? = null,
    // A dimmed icon tells that the button's setting is off.
    iconActive: Boolean = true
) {
    val palette = LocalIslandPalette.current
    val currentRead by rememberUpdatedState(read)
    val currentWrite by rememberUpdatedState(write)
    var shown by remember { mutableFloatStateOf(read().coerceIn(0f, 1f)) }
    var dragging by remember { mutableStateOf(false) }
    var widthPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            if (!dragging) shown = currentRead().coerceIn(0f, 1f)
            delay(400)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(UTILITY_SLIDER_HEIGHT)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.text.copy(alpha = 0.10f))
            .onSizeChanged { widthPx = it.width }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var total = Offset.Zero
                    var owned = false
                    fun fractionAt(x: Float): Float = if (widthPx > 0) (x / widthPx).coerceIn(0f, 1f) else shown
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.isConsumed && !owned) break
                        if (!change.pressed) {
                            if (!owned) {
                                val level = fractionAt(down.position.x)
                                shown = level
                                currentWrite(level)
                                change.consume()
                            }
                            break
                        }
                        total += change.position - change.previousPosition
                        if (!owned && total.getDistance() > viewConfiguration.touchSlop) {
                            // Sideways is ours, mostly vertical is left to the page's swipes
                            if (abs(total.x) >= abs(total.y)) {
                                owned = true
                                dragging = true
                            } else {
                                break
                            }
                        }
                        if (owned) {
                            change.consume()
                            val level = fractionAt(change.position.x)
                            shown = level
                            currentWrite(level)
                        }
                    }
                    dragging = false
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = shown)
                .background(palette.accentCyan.copy(alpha = 0.28f))
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(30.dp)
                .then(if (onIconClick != null) Modifier.clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onIconClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (iconActive) palette.accentCyan else palette.accentCyan.copy(alpha = 0.55f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
internal fun CalculatorPicker(
    choices: List<LaunchableApp>,
    onPick: (String) -> Unit,
    onCancel: () -> Unit
) {
    val palette = LocalIslandPalette.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(end = UTILITY_SETTINGS_GUTTER),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            stringResource(R.string.utility_pick_calculator),
            color = palette.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            choices.forEach { app ->
                Text(
                    text = app.label,
                    color = palette.text,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.text.copy(alpha = 0.10f))
                        .clickable { onPick(app.packageName) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
        Text(
            stringResource(R.string.utility_pick_cancel),
            color = palette.accentCyan,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onCancel)
        )
    }
}

@Composable
internal fun TimerPicker(onPick: (Int) -> Unit, onCancel: () -> Unit) {
    val palette = LocalIslandPalette.current
    Row(
        Modifier
            .fillMaxSize()
            .padding(end = UTILITY_SETTINGS_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UtilityActions.TIMER_CHOICES_MINUTES.forEach { minutes ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(UTILITY_CELL_HEIGHT)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.accentCyan.copy(alpha = 0.22f))
                    .clickable { onPick(minutes) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.utility_timer_minutes_short, minutes),
                    color = palette.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(palette.text.copy(alpha = 0.14f))
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.utility_pick_cancel),
                tint = palette.text,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
