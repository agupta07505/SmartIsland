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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.FoldPosture
import com.agupta07505.smartisland.util.FoldDetector

enum class LayoutMode(val label: String, val posture: FoldPosture?, val landscape: Boolean) {
    Portrait("Portrait", null, false),
    Landscape("Landscape", null, true),
    FoldedPortrait("Folded portrait", FoldPosture.FoldedPortrait, false),
    FoldedLandscape("Folded landscape", FoldPosture.FoldedLandscape, true),
    UnfoldedPortrait("Unfolded portrait", FoldPosture.UnfoldedPortrait, false),
    UnfoldedLandscape("Unfolded landscape", FoldPosture.UnfoldedLandscape, true);

    companion object {
        val Normal: List<LayoutMode> = listOf(Portrait, Landscape)
        val Fold: List<LayoutMode> = listOf(FoldedPortrait, FoldedLandscape, UnfoldedPortrait, UnfoldedLandscape)

        /** Entries of the switch: the 2 normal ones. */
        fun listFor(foldableMode: Boolean): List<LayoutMode> = if (foldableMode) Fold else Normal

        fun foldMode(unfolded: Boolean, landscape: Boolean): LayoutMode = when {
            unfolded && landscape -> UnfoldedLandscape
            unfolded -> UnfoldedPortrait
            landscape -> FoldedLandscape
            else -> FoldedPortrait
        }

        /** [selected] made valid for the current switch. */
        fun resolve(selected: LayoutMode, foldableMode: Boolean, unfoldedNow: Boolean): LayoutMode {
            val isFoldMode = selected.posture != null
            return when {
                foldableMode == isFoldMode -> selected
                foldableMode -> foldMode(unfoldedNow, selected.landscape)
                selected.landscape -> Landscape
                else -> Portrait
            }
        }
    }
}

data class EditingProfile(
    val mode: LayoutMode,
    val foldable: Boolean,
    val foldableMode: Boolean,
    val repository: SmartIslandSettingsRepository,
    val settings: SmartIslandSettings
)

/** Foldable mode as the screens see it. */
@Composable
fun rememberEditingProfile(
    root: SmartIslandSettingsRepository,
    rootSettings: SmartIslandSettings,
    selected: LayoutMode
): EditingProfile {
    val context = LocalContext.current
    val emulating by FoldDetector.emulationFlow(context).collectAsStateWithLifecycle()
    val foldable = remember(emulating) { FoldDetector.isFoldable(context) }
    val foldableMode = foldable
    val configuration = LocalConfiguration.current
    val unfoldedNow = FoldDetector
        .postureFor(context, configuration.screenWidthDp.toFloat(), configuration.screenHeightDp.toFloat())
        .unfolded
    val mode = LayoutMode.resolve(selected, foldableMode, unfoldedNow)
    val repository = remember(root, mode) { root.forPosture(mode.posture) }
    val settings by repository.settings.collectAsStateWithLifecycle(initialValue = rootSettings)
    return EditingProfile(mode, foldable, foldableMode, repository, settings)
}

/** Segmented switch at the top of the Home and Layout tabs. */
@Composable
fun LayoutModeTabs(
    selected: LayoutMode,
    foldableMode: Boolean,
    onSelected: (LayoutMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = LayoutMode.listFor(foldableMode)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        modes.forEach { mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelected(mode) }
                    .padding(vertical = if (foldableMode) 8.dp else 10.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    style = if (foldableMode) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
