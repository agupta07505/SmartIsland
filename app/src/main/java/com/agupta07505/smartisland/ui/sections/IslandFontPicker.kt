/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.ui.IslandFontOptions
import com.agupta07505.smartisland.ui.forgetCustomFontFamily
import com.agupta07505.smartisland.ui.islandFontFamily
import com.agupta07505.smartisland.util.CustomFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pick the font of the island's text. Each row shows its own name in its own font. Under the built-in fonts the user
 * can add a font from a .ttf or .otf file; added fonts can be removed again.
 */
@Composable
internal fun IslandFontPicker(
    selected: String,
    onSelected: (String) -> Unit,
    /** Another title and description than the island font's (the idle glyph font uses the same picker). */
    title: String? = null,
    description: String? = null,
    /** A first row that stands for "no font of its own" (selected = ""): label and note. Null = no such row. */
    inheritRow: Pair<String, String>? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var customFonts by remember { mutableStateOf(CustomFonts.list(context)) }

    val addLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { CustomFonts.import(context, uri) }
            when (result) {
                is CustomFonts.ImportResult.Added -> {
                    customFonts = CustomFonts.list(context)
                    onSelected(result.id)
                    Toast.makeText(context, context.getString(R.string.island_font_added), Toast.LENGTH_SHORT).show()
                }
                CustomFonts.ImportResult.TooLarge ->
                    Toast.makeText(context, context.getString(R.string.island_font_too_large), Toast.LENGTH_LONG).show()
                CustomFonts.ImportResult.NotAFont ->
                    Toast.makeText(context, context.getString(R.string.island_font_not_a_font), Toast.LENGTH_LONG).show()
                CustomFonts.ImportResult.Failed ->
                    Toast.makeText(context, context.getString(R.string.island_font_import_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title ?: stringResource(R.string.island_font_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = description ?: stringResource(R.string.island_font_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp)
        )
        inheritRow?.let { (label, note) ->
            FontRow(
                label = label,
                note = note,
                family = androidx.compose.ui.text.font.FontFamily.Default,
                isSelected = selected.isEmpty(),
                onClick = { onSelected("") }
            )
        }
        IslandFontOptions.forEach { option ->
            FontRow(
                label = option.label,
                note = option.note,
                family = option.family,
                isSelected = selected.isNotEmpty() && option.id == selected,
                onClick = { onSelected(option.id) }
            )
        }

        if (customFonts.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            Text(
                text = stringResource(R.string.island_font_yours),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            customFonts.forEach { fileName ->
                val id = CustomFonts.idFor(fileName)
                val family = remember(id) { islandFontFamily(context, id) }
                FontRow(
                    label = CustomFonts.labelOf(fileName),
                    note = fileName.substringAfterLast('.').uppercase(),
                    family = family,
                    isSelected = id == selected,
                    onClick = { onSelected(id) },
                    onRemove = {
                        val wasSelected = id == selected
                        CustomFonts.delete(context, id)
                        forgetCustomFontFamily(id)
                        customFonts = CustomFonts.list(context)
                        if (wasSelected) onSelected(if (inheritRow != null) "" else SmartIslandSettings.Default.islandFont)
                    }
                )
            }
        }

        OutlinedButton(
            onClick = { addLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
            Text(
                text = stringResource(R.string.island_font_add),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            text = stringResource(R.string.island_font_add_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
        )
    }
}

@Composable
private fun FontRow(
    label: String,
    note: String,
    family: androidx.compose.ui.text.font.FontFamily,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(start = 16.dp, end = if (onRemove != null) 4.dp else 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = family,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.island_font_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
