/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import kotlin.math.roundToInt

/**
 * Label with the live value right-aligned on the same line (Rule_Book 11.3), above a slider flanked by 44dp
 * decrement/increment buttons.
 * The buttons give an exact, non-drag way to adjust (Switch Access, TalkBack, precision).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderSettingItem(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    suffix: String = "dp",
    step: Float = 1f
) {
    val valueText = "${value.roundToInt()} $suffix"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.RowHorizontal, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    onValueChange((value - step).coerceIn(range.start, range.endInclusive))
                    onValueChangeFinished?.invoke()
                },
                modifier = Modifier.size(Dimens.MinTouch)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = stringResource(R.string.action_decrease),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Rule_Book 11.3: accent-filled track and a pure circular thumb.
            val colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = range,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "$label, $valueText" },
                colors = colors,
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .shadow(elevation = 2.dp, shape = CircleShape)
                            .background(Color.White, CircleShape)
                    )
                },
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        modifier = Modifier.height(4.dp),
                        colors = colors,
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp,
                        trackInsideCornerSize = 2.dp
                    )
                }
            )
            IconButton(
                onClick = {
                    onValueChange((value + step).coerceIn(range.start, range.endInclusive))
                    onValueChangeFinished?.invoke()
                },
                modifier = Modifier.size(Dimens.MinTouch)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.action_increase),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
