/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.ui.Dimens
import com.agupta07505.smartisland.ui.semantic

/**
 * Inset-grouped list section: an optional header above, a rounded container whose children are
 * separated by hairlines, and an optional footer below. Replaces stacks of bordered cards.
 *
 * Children are laid out vertically; a hairline is drawn between each pair, starting at
 * [dividerInset] so it aligns with row text rather than the leading glyph.
 */
@Composable
fun GroupedSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    dividerInset: Dp = Dimens.RowHorizontal,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (header != null) {
            Text(
                text = header,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = Dimens.RowHorizontal, end = Dimens.RowHorizontal, bottom = 6.dp)
                    .semantics { heading() }
            )
        }
        val separator = MaterialTheme.semantic.separator
        val rowSurface = MaterialTheme.semantic.groupedRow
        val ys = remember { mutableListOf<Float>() }
        Layout(
            content = content,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.GroupRadius))
                .background(rowSurface)
                .drawBehind {
                    val stroke = 0.5.dp.toPx().coerceAtLeast(1f)
                    ys.forEach { y ->
                        drawLine(
                            color = separator,
                            start = Offset(dividerInset.toPx(), y),
                            end = Offset(size.width, y),
                            strokeWidth = stroke
                        )
                    }
                }
        ) { measurables, constraints ->
            val childConstraints = constraints.copy(minHeight = 0)
            val placeables = measurables.map { it.measure(childConstraints) }
            val height = placeables.sumOf { it.height }
            ys.clear()
            var y = 0
            placeables.forEachIndexed { index, p ->
                if (index > 0) ys.add(y.toFloat())
                y += p.height
            }
            layout(constraints.maxWidth, height) {
                var top = 0
                placeables.forEach {
                    it.placeRelative(0, top)
                    top += it.height
                }
            }
        }
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Dimens.RowHorizontal, end = Dimens.RowHorizontal, top = 6.dp)
            )
        }
    }
}

/**
 * A single row in a [GroupedSection]. At least 44dp tall. Pass [onClick] to make the whole row the
 * touch target; [trailing] replaces the value/chevron (use it for switches and steppers).
 */
@Composable
fun GroupedRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    value: String? = null,
    showChevron: Boolean = true,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val titleColor = if (destructive) MaterialTheme.semantic.danger else MaterialTheme.colorScheme.onSurface
    val clickable = if (onClick != null && enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickable)
            .defaultMinSize(minHeight = Dimens.MinTouch)
            .padding(horizontal = Dimens.RowHorizontal, vertical = Dimens.RowVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) IconTile(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) titleColor else titleColor.copy(alpha = 0.4f)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            if (showChevron && onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Row with a switch. The entire row toggles, so the target is far larger than the thumb. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.clickable(role = Role.Switch) { onCheckedChange(!checked) } else Modifier
            )
            .defaultMinSize(minHeight = Dimens.MinTouch)
            .padding(horizontal = Dimens.RowHorizontal, vertical = Dimens.RowVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) IconTile(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // The row owns the click; the switch is presentational so TalkBack announces one control.
        Box(modifier = Modifier.clearAndSetSemantics { }) {
            SmartIslandSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}

/** Material switch restyled: white thumb, accent track when on, neutral fill track when off. */
@Composable
fun SmartIslandSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
            uncheckedBorderColor = Color.Transparent,
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.8f),
            disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            disabledCheckedBorderColor = Color.Transparent,
            disabledUncheckedThumbColor = Color.White.copy(alpha = 0.8f),
            disabledUncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            disabledUncheckedBorderColor = Color.Transparent
        )
    )
}

/** 30dp rounded glyph tile used for top-level navigation rows. One accent color, white glyph. */
@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

enum class StatusKind { Success, Warning, Danger, Neutral }

/** Status as icon + text. Color reinforces the message but is never the only signal. */
@Composable
fun StatusLabel(text: String, kind: StatusKind, modifier: Modifier = Modifier) {
    val color = when (kind) {
        StatusKind.Success -> MaterialTheme.semantic.success
        StatusKind.Warning -> MaterialTheme.semantic.warning
        StatusKind.Danger -> MaterialTheme.semantic.danger
        StatusKind.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val glyph = when (kind) {
        StatusKind.Success -> Icons.Rounded.CheckCircle
        StatusKind.Warning, StatusKind.Danger -> Icons.Rounded.ErrorOutline
        StatusKind.Neutral -> Icons.Rounded.Info
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(glyph, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

/** Spacer that separates grouped sections (24dp, per the 8dp grid). */
@Composable
fun SectionGap() {
    Spacer(Modifier.height(Dimens.SectionSpacing))
}
