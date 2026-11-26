/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.SwapTouchGuard
import com.agupta07505.smartisland.model.TapSwap

@Composable
internal fun TertiaryPill(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    secondary: IslandSecondary,
    switchScaleAnim: Animatable<Float, AnimationVector1D>,
    cornerLockY: () -> Dp,
    screenCenter: Dp,
    onPageSelected: (Int) -> Unit,
    wiring: IslandWiring,
    ui: IslandInteractionState
) {
    if (secondary.shown.tertiary.value && secondary.cards.tertiaryNotification != null) {
        val sideCamera = layout.cameraSide != CameraSide.Centre && layout.sideZone != null
        val pillShape = if (settings.enableNotchMode) {
            RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = settings.cornerRadius.dp,
                bottomStart = settings.cornerRadius.dp)
        } else RoundedCornerShape(settings.cornerRadius.dp)
        val cutoutBounds = remember { arrayOfNulls<Rect>(1) }
        val downTap = remember { SwapTouchGuard() }
        Box(
            modifier = Modifier
                .absoluteOffset {
                    IntOffset(
                        (secondary.offsets.expandedCompactX - screenCenter + layout.miniPillWidth / 2f).roundToPx(),
                        cornerLockY().roundToPx()
                    )
                }
                .width(layout.miniPillWidth)
                .height(layout.circleSize)
                .graphicsLayer {
                    alpha = secondary.anims.tertiaryAlpha.value * ui.cutoutSwapAlpha(TapSwap.CUTOUT_PILL_SPOT)
                    scaleX = secondary.anims.tertiaryScale.value * switchScaleAnim.value
                    scaleY = secondary.anims.tertiaryScale.value * switchScaleAnim.value
                }
                .then(
                    if (settings.enableShadow && settings.shadowElevation > 0f) {
                        Modifier.shadow(
                            elevation = (settings.shadowElevation * 0.85f).dp,
                            shape = pillShape,
                            clip = false,
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                    } else Modifier
                )
                .onGloballyPositioned { coords ->
                    if (!ui.tapSwapActive) cutoutBounds[0] = coords.boundsInWindow()
                }
                .clip(pillShape)
                .background(layout.pillBackgroundColor.copy(alpha = settings.opacity))
                .cutoutTapOnDown(
                    guard = downTap,
                    enabled = { wiring.view.expanded && !ui.tapSwapActive && secondary.cards.tertiaryIndex >= 0 }
                ) {
                    if (turnAroundOnCutoutTap(ui, wiring, secondary.cards.tertiaryIndex, onPageSelected)) return@cutoutTapOnDown
                    ui.markCutout(TapSwap.CUTOUT_PILL_SPOT) { cutoutBounds[0] }
                    onPageSelected(secondary.cards.tertiaryIndex)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (downTap.claimClick()) return@clickable
                    if (turnAroundOnCutoutTap(ui, wiring, secondary.cards.tertiaryIndex, onPageSelected)) return@clickable
                    if (secondary.cards.tertiaryIndex >= 0) {
                        ui.markCutout(TapSwap.CUTOUT_PILL_SPOT) { cutoutBounds[0] }
                        onPageSelected(secondary.cards.tertiaryIndex)
                    }
                    if (!wiring.view.expanded) {
                        wiring.actions.expand.toggle()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            IslandCollapsedContent(
                mode = secondary.cards.tertiaryNotification.mode,
                notification = secondary.cards.tertiaryNotification,
                collapsedAlpha = 1f,
                settings = settings,
                pillWidthDp = if (sideCamera) settings.width else 0f,
                cameraGapDp = if (sideCamera) layout.cameraGapDp else 0f,
                cameraCentreInPillDp = if (sideCamera) layout.cameraCentreInPillDp else null,
                cameraSide = layout.cameraSide,
                sidePlan = if (sideCamera) layout.sideZone?.plan else null
            )
        }
    }
}
