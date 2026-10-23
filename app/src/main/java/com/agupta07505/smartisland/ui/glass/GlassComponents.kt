/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

// Drop-in replacements for Material 3's Card and Switch: the screens import these, so every card picks up the glass
// edge and the glass switch colours.

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (LocalLiquidGlass.current) {
        androidx.compose.material3.Card(
            modifier = modifier.glassEdge(shape, glass = true, dark = LocalGlassDark.current),
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = null,
            content = content
        )
    } else {
        androidx.compose.material3.Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content
        )
    }
}

@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier.glassEdge(shape, glass = LocalLiquidGlass.current, dark = LocalGlassDark.current),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = if (LocalLiquidGlass.current) null else border,
        interactionSource = interactionSource,
        content = content
    )
}

/** Switch colours of the glass theme: a white thumb on the accent track, and a faint glass track when off. */
@Composable
fun glassSwitchColors(): SwitchColors =
    if (LocalLiquidGlass.current) {
        SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White.copy(alpha = 0.96f),
            uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
            uncheckedBorderColor = MaterialTheme.colorScheme.outline
        )
    } else {
        SwitchDefaults.colors()
    }

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = glassSwitchColors(),
    interactionSource: MutableInteractionSource? = null
) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = thumbContent,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}
