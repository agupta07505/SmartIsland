/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation/action row kept for existing call sites. Renders a [GroupedRow]; the tint parameters
 * are accepted for source compatibility but the row uses the single accent glyph tile.
 */
@Composable
fun ClickableRowItem(
    label: String,
    icon: ImageVector,
    subtitle: String? = null,
    value: String? = null,
    @Suppress("UNUSED_PARAMETER") iconTint: Color = MaterialTheme.colorScheme.primary,
    @Suppress("UNUSED_PARAMETER") iconBg: Color = iconTint,
    onClick: () -> Unit
) {
    GroupedRow(
        title = label,
        subtitle = subtitle,
        icon = icon,
        value = value,
        showChevron = value == null,
        onClick = onClick
    )
}
