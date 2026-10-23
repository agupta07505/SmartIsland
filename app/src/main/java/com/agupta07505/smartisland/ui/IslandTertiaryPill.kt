/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings

@Composable
internal fun TertiaryPill(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    secondary: IslandSecondary,
    switchScaleAnim: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    cornerLockY: () -> Dp,
    screenCenter: Dp,
    onPageSelected: (Int) -> Unit,
    cb: IslandCallbacks
) {
    if (!settings.enableNotchMode && secondary.tertiaryShown && secondary.tertiaryNotification != null) {
        Box(
            modifier = Modifier
                .absoluteOffset {
                    IntOffset(
                        (secondary.expandedCompactX - screenCenter + layout.miniPillWidth / 2f).roundToPx(),
                        cornerLockY().roundToPx()
                    )
                }
                .width(layout.miniPillWidth)
                .height(layout.circleSize)
                .graphicsLayer {
                    alpha = secondary.tertiaryAlpha
                    scaleX = secondary.tertiaryScale * switchScaleAnim.value
                    scaleY = secondary.tertiaryScale * switchScaleAnim.value
                }
                .then(
                    if (settings.enableShadow && settings.shadowElevation > 0f) {
                        Modifier.shadow(
                            elevation = (settings.shadowElevation * 0.85f).dp,
                            shape = RoundedCornerShape(settings.cornerRadius.dp),
                            clip = false,
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                    } else Modifier
                )
                .clip(RoundedCornerShape(settings.cornerRadius.dp))
                .background(layout.pillBackgroundColor.copy(alpha = settings.opacity))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (secondary.tertiaryIndex >= 0) {
                        onPageSelected(secondary.tertiaryIndex)
                    }
                    if (!cb.currentExpanded) {
                        cb.currentOnToggle()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            IslandCollapsedContent(
                mode = secondary.tertiaryNotification.mode,
                notification = secondary.tertiaryNotification,
                collapsedAlpha = 1f,
                settings = settings
            )
        }
    }
}
