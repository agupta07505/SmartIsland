/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.util.CameraCutoutDetector
import kotlinx.coroutines.launch

@Composable
fun PositionsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Interactive Live Notch Alignment Visualizer & 1-Tap Auto Align
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Camera Cutout Calibration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Auto-detect your device camera hole or fine-tune pill placement",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Interactive Mini Simulation Screen Top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Status bar guide line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(Color.Black.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "9:41",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape))
                                Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape))
                            }
                        }
                    }

                    // Simulated Camera Cutout Dot
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .size(14.dp)
                            .background(Color(0xFF0F172A), CircleShape)
                            .border(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), CircleShape)
                    )

                    // Positioned Preview Island Pill (scaled down for canvas)
                    val scaleFactor = 0.75f
                    val animatedWidth by animateDpAsState(
                        targetValue = (settings.width * scaleFactor).dp,
                        animationSpec = tween(150),
                        label = "pillWidth"
                    )
                    val animatedHeight by animateDpAsState(
                        targetValue = (settings.height * scaleFactor).dp,
                        animationSpec = tween(150),
                        label = "pillHeight"
                    )
                    val animatedXOffset by animateDpAsState(
                        targetValue = (settings.xOffset * scaleFactor).dp,
                        animationSpec = tween(150),
                        label = "pillX"
                    )
                    val animatedYOffset by animateDpAsState(
                        targetValue = (settings.yOffset * scaleFactor).dp,
                        animationSpec = tween(150),
                        label = "pillY"
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = animatedXOffset, y = animatedYOffset)
                            .size(width = animatedWidth, height = animatedHeight)
                            .then(
                                if (settings.enableShadow) {
                                    Modifier.shadow(8.dp, RoundedCornerShape((settings.cornerRadius * scaleFactor).dp))
                                } else Modifier
                            )
                            .clip(RoundedCornerShape((settings.cornerRadius * scaleFactor).dp))
                            .background(Color.Black)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape((settings.cornerRadius * scaleFactor).dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF38BDF8), CircleShape)
                            )
                            Text(
                                text = "Smart Island",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 1-Tap Auto Align Button
                Button(
                    onClick = {
                        val detected = CameraCutoutDetector.detect(context)
                        scope.launch {
                            repository.setPosition(
                                width = detected.widthDp,
                                height = detected.heightDp,
                                xOffset = detected.xOffsetDp,
                                yOffset = detected.yOffsetDp
                            )
                        }
                        if (detected.hasHardwareCutout) {
                            Toast.makeText(
                                context,
                                "Hardware camera cutout detected! Aligned perfectly.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Status bar height calibrated to center pill.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "1-Tap Auto-Align with Camera Cutout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Presets Segment Buttons
                Text(
                    text = "Quick Notch Presets",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetButton(
                        label = "Center Hole",
                        onClick = {
                            scope.launch {
                                repository.setPosition(width = 112f, height = 34f, xOffset = 0f, yOffset = 12f)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PresetButton(
                        label = "Left Corner",
                        onClick = {
                            scope.launch {
                                repository.setPosition(width = 112f, height = 34f, xOffset = -90f, yOffset = 12f)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PresetButton(
                        label = "Right Corner",
                        onClick = {
                            scope.launch {
                                repository.setPosition(width = 112f, height = 34f, xOffset = 90f, yOffset = 12f)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PresetButton(
                        label = "Wide Island",
                        onClick = {
                            scope.launch {
                                repository.setPosition(width = 145f, height = 38f, xOffset = 0f, yOffset = 14f)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Card 2: Precision Dimensions & Offsets
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "Precision Sizing & Position",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Fine-tune dimension and offset millimeters for pixel-perfect notch wrapping",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                SliderSettingItem(
                    label = "Island Width",
                    value = settings.width,
                    range = SmartIslandSettings.MIN_WIDTH..SmartIslandSettings.MAX_WIDTH,
                    onValueChange = { scope.launch { repository.setWidth(it) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                SliderSettingItem(
                    label = "Island Height",
                    value = settings.height,
                    range = SmartIslandSettings.MIN_HEIGHT..SmartIslandSettings.MAX_HEIGHT,
                    onValueChange = { scope.launch { repository.setHeight(it) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                SliderSettingItem(
                    label = "Horizontal (X) Offset",
                    value = settings.xOffset,
                    range = SmartIslandSettings.MIN_X_OFFSET..SmartIslandSettings.MAX_X_OFFSET,
                    onValueChange = { scope.launch { repository.setXOffset(it) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                SliderSettingItem(
                    label = "Vertical (Y) Offset",
                    value = settings.yOffset,
                    range = SmartIslandSettings.MIN_Y_OFFSET..SmartIslandSettings.MAX_Y_OFFSET,
                    onValueChange = { scope.launch { repository.setYOffset(it) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                SliderSettingItem(
                    label = "Corner Radius",
                    value = settings.cornerRadius,
                    range = SmartIslandSettings.MIN_CORNER_RADIUS..SmartIslandSettings.MAX_CORNER_RADIUS,
                    onValueChange = { scope.launch { repository.setCornerRadius(it) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Island Drop Shadow",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Add a soft ambient drop shadow for a floating depth effect",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = settings.enableShadow,
                        onCheckedChange = { checked ->
                            scope.launch { repository.setEnableShadow(checked) }
                        }
                    )
                }

                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch { repository.resetPosition() }
                        Toast.makeText(context, "Position reset to defaults", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to Factory Position", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PresetButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
