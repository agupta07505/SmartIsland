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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.agupta07505.smartisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.ui.search.searchAnchor
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.MergeStyle
import kotlinx.coroutines.launch

/**
 * Layout tab: how fast the island opens, closes and settles. Follows the Portrait / Landscape switch above it, so
 * each orientation keeps its own speed.
 */
@Composable
fun AnimationSpeedSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    landscape: Boolean
) {
    val scope = rememberCoroutineScope()
    val selected = if (landscape) settings.animSpeedLandscape else settings.animSpeedPortrait
    val options = listOf(
        AnimationSpeed.ID_RELAXED to stringResource(R.string.anim_speed_relaxed),
        AnimationSpeed.ID_NORMAL to stringResource(R.string.anim_speed_normal),
        AnimationSpeed.ID_SNAPPY to stringResource(R.string.anim_speed_snappy)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .searchAnchor("layout.anim_speed"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    if (landscape) R.string.anim_speed_title_landscape else R.string.anim_speed_title_portrait
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.anim_speed_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                options.forEach { (id, label) ->
                    val isSelected = id == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable {
                                scope.launch {
                                    if (landscape) repository.setAnimSpeedLandscape(id)
                                    else repository.setAnimSpeedPortrait(id)
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Layout tab: how the small round cutout joins the main one when it merges. Follows the Portrait / Landscape switch
 * above it, so each orientation keeps its own choice.
 */
@Composable
fun MergeStyleSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    landscape: Boolean
) {
    val scope = rememberCoroutineScope()
    val selected = settings.mergeStyleKind(landscape).id
    val options = listOf(
        MergeStyle.ID_SLIDE to stringResource(R.string.merge_style_slide),
        MergeStyle.ID_BUBBLE to stringResource(R.string.merge_style_bubble)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .searchAnchor("layout.merge_style"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    if (landscape) R.string.merge_style_title_landscape else R.string.merge_style_title_portrait
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.merge_style_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                options.forEach { (id, label) ->
                    val isSelected = id == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable {
                                scope.launch {
                                    if (landscape) repository.setMergeStyleLandscape(id)
                                    else repository.setMergeStyle(id)
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
