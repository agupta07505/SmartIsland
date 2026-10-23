/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.agupta07505.smartisland.ui.glass.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.FontMix
import com.agupta07505.smartisland.model.FontSlot
import com.agupta07505.smartisland.ui.IslandFontOptions
import com.agupta07505.smartisland.ui.islandFontFamily
import com.agupta07505.smartisland.util.CustomFonts

/** Mix fonts (model/FontMix.kt): a switch plus three font slots, Light, Regular and Medium. */
@Composable
internal fun IslandFontMixSection(
    settings: SmartIslandSettings,
    onChange: (enabled: Boolean, light: String, regular: String, medium: String) -> Unit
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<FontSlot?>(null) }

    fun update(enabled: Boolean = settings.fontMixEnabled, slot: FontSlot? = null, id: String = "") {
        fun idFor(s: FontSlot) = if (s == slot) id else FontMix.idOf(s, settings.fontMixLight, settings.fontMixRegular, settings.fontMixMedium)
        onChange(enabled, idFor(FontSlot.Light), idFor(FontSlot.Regular), idFor(FontSlot.Medium))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.font_mix_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.font_mix_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Switch(checked = settings.fontMixEnabled, onCheckedChange = { update(enabled = it) })
        }

        if (settings.fontMixEnabled) {
            FontSlot.entries.forEach { slot ->
                val id = FontMix.idOf(slot, settings.fontMixLight, settings.fontMixRegular, settings.fontMixMedium)
                val effective = FontMix.effectiveId(id, settings.islandFont)
                val family = remember(effective) { islandFontFamily(context, effective) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editing = slot }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = slotTitle(slot),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = slotNote(slot),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // The sample is drawn with the slot's own font at the weight the island asks for it.
                        Text(
                            text = fontName(id),
                            fontFamily = family,
                            fontWeight = FontWeight(slot.representativeWeight),
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = stringResource(R.string.font_mix_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp)
            )
        }
    }

    editing?.let { slot ->
        val current = FontMix.idOf(slot, settings.fontMixLight, settings.fontMixRegular, settings.fontMixMedium)
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(slotTitle(slot)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    IslandFontPicker(
                        selected = current,
                        onSelected = { update(slot = slot, id = it) },
                        title = slotTitle(slot),
                        description = slotNote(slot),
                        inheritRow = stringResource(R.string.font_mix_same) to stringResource(R.string.font_mix_same_note)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { editing = null }) { Text(stringResource(R.string.font_mix_done)) }
            }
        )
    }
}

@Composable
private fun slotTitle(slot: FontSlot): String = when (slot) {
    FontSlot.Light -> stringResource(R.string.font_mix_light)
    FontSlot.Regular -> stringResource(R.string.font_mix_regular)
    FontSlot.Medium -> stringResource(R.string.font_mix_medium)
}

@Composable
private fun slotNote(slot: FontSlot): String = when (slot) {
    FontSlot.Light -> stringResource(R.string.font_mix_light_note)
    FontSlot.Regular -> stringResource(R.string.font_mix_regular_note)
    FontSlot.Medium -> stringResource(R.string.font_mix_medium_note)
}

@Composable
private fun fontName(id: String): String {
    if (id.isEmpty()) return stringResource(R.string.font_mix_same)
    IslandFontOptions.firstOrNull { it.id == id }?.let { return it.label }
    return CustomFonts.fileNameOf(id)?.let { CustomFonts.labelOf(it) } ?: stringResource(R.string.font_mix_same)
}
