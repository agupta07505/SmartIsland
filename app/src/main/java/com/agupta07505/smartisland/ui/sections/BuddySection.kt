/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.buddy.model.MascotPalette
import com.agupta07505.smartisland.buddy.model.MascotShape
import com.agupta07505.smartisland.buddy.model.MascotSide
import com.agupta07505.smartisland.buddy.model.MascotStyle
import com.agupta07505.smartisland.buddy.model.MascotTrack
import com.agupta07505.smartisland.buddy.runtime.MascotController
import com.agupta07505.smartisland.buddy.runtime.MascotMoment
import com.agupta07505.smartisland.buddy.ui.MascotLayer
import com.agupta07505.smartisland.ui.components.GroupedRow
import android.os.SystemClock
import com.agupta07505.smartisland.buddy.ui.toColor
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.Dimens
import com.agupta07505.smartisland.ui.PillLayout
import com.agupta07505.smartisland.ui.components.GroupedSection
import com.agupta07505.smartisland.ui.components.SegmentedControl
import com.agupta07505.smartisland.ui.components.SwitchRow
import com.agupta07505.smartisland.ui.semantic
import kotlinx.coroutines.launch

/** Settings > Appearance > Buddy: a live preview, an on/off switch and the buddy's look. */
@Composable
fun BuddySection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    val look = settings.buddyLook
    // The preview has its own controller: it always rests, and "Try It" plays reactions on it directly.
    val previewController = remember { MascotController { SystemClock.elapsedRealtime() / 1000.0 } }
    val styleLabels = mapOf(
        MascotStyle.Minimal to stringResource(R.string.buddy_style_minimal),
        MascotStyle.Robot to stringResource(R.string.buddy_style_robot)
    )
    val shapeLabels = mapOf(
        MascotShape.Ball to stringResource(R.string.buddy_shape_ball),
        MascotShape.Egg to stringResource(R.string.buddy_shape_egg),
        MascotShape.Squircle to stringResource(R.string.buddy_shape_squircle),
        MascotShape.Pill to stringResource(R.string.buddy_shape_pill)
    )
    val paletteLabels = mapOf(
        MascotPalette.Pearl to stringResource(R.string.buddy_color_pearl),
        MascotPalette.Lemon to stringResource(R.string.buddy_color_lemon),
        MascotPalette.Peach to stringResource(R.string.buddy_color_peach),
        MascotPalette.Rose to stringResource(R.string.buddy_color_rose),
        MascotPalette.Lilac to stringResource(R.string.buddy_color_lilac),
        MascotPalette.Sky to stringResource(R.string.buddy_color_sky),
        MascotPalette.Mint to stringResource(R.string.buddy_color_mint)
    )
    val previewDescription = stringResource(
        R.string.buddy_preview_description,
        styleLabels.getValue(look.style),
        shapeLabels.getValue(look.shape),
        paletteLabels.getValue(look.palette)
    )

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)) {
        GroupedSection {
            BuddyPreview(
                look = look,
                side = settings.buddySideValue,
                contentDescription = previewDescription,
                controller = previewController
            )
        }

        GroupedSection(
            footer = if (settings.buddyEnabled && settings.hideWhenIdle) stringResource(R.string.buddy_hide_when_idle_note)
            else stringResource(R.string.buddy_description)
        ) {
            SwitchRow(
                title = stringResource(R.string.buddy_show),
                checked = settings.buddyEnabled,
                onCheckedChange = { scope.launch { repository.setBuddyEnabled(it) } }
            )
        }

        GroupedSection {
            ControlRow(title = stringResource(R.string.buddy_style)) {
                SegmentedControl(
                    options = MascotStyle.entries.map { it to styleLabels.getValue(it) },
                    selected = look.style,
                    onSelect = { scope.launch { repository.setBuddyStyle(it.id) } }
                )
            }
            ControlRow(title = stringResource(R.string.buddy_shape)) {
                SegmentedControl(
                    options = MascotShape.entries.map { it to shapeLabels.getValue(it) },
                    selected = look.shape,
                    onSelect = { scope.launch { repository.setBuddyShape(it.id) } }
                )
            }
            ControlRow(title = stringResource(R.string.buddy_color), value = paletteLabels.getValue(look.palette)) {
                SwatchRow(
                    selected = look.palette,
                    labels = paletteLabels,
                    onSelect = { scope.launch { repository.setBuddyPalette(it.id) } }
                )
            }
            ControlRow(title = stringResource(R.string.buddy_side)) {
                SegmentedControl(
                    options = listOf(
                        MascotSide.Left to stringResource(R.string.buddy_side_left),
                        MascotSide.Right to stringResource(R.string.buddy_side_right)
                    ),
                    selected = settings.buddySideValue,
                    onSelect = { scope.launch { repository.setBuddySide(it.id) } }
                )
            }
        }

        GroupedSection(footer = stringResource(R.string.buddy_react_desc)) {
            SwitchRow(
                title = stringResource(R.string.buddy_react_title),
                checked = settings.buddyReacts,
                onCheckedChange = { scope.launch { repository.setBuddyReacts(it) } }
            )
        }

        GroupedSection(
            header = stringResource(R.string.buddy_try_header),
            footer = stringResource(R.string.buddy_try_footer)
        ) {
            listOf(
                R.string.buddy_try_wake to MascotMoment.Unlock,
                R.string.buddy_try_charging to MascotMoment.Charging,
                R.string.buddy_try_download to MascotMoment.DownloadFinished,
                R.string.buddy_try_music to MascotMoment.Music,
                R.string.buddy_try_timer to MascotMoment.TimeIsUp
            ).forEach { (label, moment) ->
                GroupedRow(
                    title = stringResource(label),
                    showChevron = false,
                    onClick = { moment.reaction?.let { previewController.demo(it) } }
                )
            }
        }
    }
}

/** A row that holds a title (and optional current value) above a full-width control. */
@Composable
private fun ControlRow(
    title: String,
    value: String? = null,
    control: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.RowHorizontal, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        control()
    }
}

/**
 * Seven color swatches. The selected one carries a ring and a checkmark (not color alone) and each is
 * labelled for TalkBack by its name. Slots share the width equally and are at least 44dp tall.
 */
@Composable
private fun SwatchRow(
    selected: MascotPalette,
    labels: Map<MascotPalette, String>,
    onSelect: (MascotPalette) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MascotPalette.entries.forEach { palette ->
            val isSelected = palette == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.MinTouch)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(palette) })
                    .semantics { contentDescription = labels.getValue(palette) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(palette.light.toColor(), palette.shade.toColor())))
                        .border(
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else BorderStroke(1.dp, MaterialTheme.semantic.separator),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = com.agupta07505.smartisland.buddy.model.MascotColor.Ink.toColor(),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * The island at 2.5x with the buddy resting beside a camera hole. Decorative: described once for
 * screen readers, never focusable. It always plays (blinks) so changes show immediately, even while
 * the switch is off.
 */
@Composable
private fun BuddyPreview(
    look: com.agupta07505.smartisland.buddy.model.MascotLook,
    side: MascotSide,
    contentDescription: String,
    controller: MascotController
) {
    val scale = 2.5f
    // A typical island: 112 x 34dp around a 30dp camera hole.
    val track = remember(side) {
        MascotTrack.forPill(
            pillWidthDp = 112f,
            pillHeightDp = 34f,
            sizeDp = PillLayout.buddySizeDp(34f),
            holeWidthDp = 30f,
            holeCenterDp = 56f,
            side = side
        ).scaled(scale)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(track.width.dp)
                .height(track.height.dp)
                .clip(RoundedCornerShape((track.height / 2f).dp))
                .background(Color.Black)
        ) {
            val hole = track.hidden
            if (hole != null) {
                Box(
                    modifier = Modifier
                        .padding(start = hole.start.dp, top = ((track.height - (hole.endInclusive - hole.start)) / 2f).dp)
                        .size((hole.endInclusive - hole.start).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0B0B0D))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                )
            }
            MascotLayer(look = look, track = track, active = true, controller = controller)
        }
    }
}
