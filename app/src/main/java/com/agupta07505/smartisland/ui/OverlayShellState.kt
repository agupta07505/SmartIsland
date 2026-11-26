/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.agupta07505.smartisland.data.IdleDotGeometry
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.BubbleOrigin
import com.agupta07505.smartisland.model.EdgeCamera
import com.agupta07505.smartisland.model.EdgePillLayoutMath
import com.agupta07505.smartisland.model.EdgePillMotion
import com.agupta07505.smartisland.model.EdgePillRules
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.NotchTopEdge
import com.agupta07505.smartisland.model.PillPhase
import com.agupta07505.smartisland.util.AnimationSpeed
import kotlin.math.abs

internal class ShellEdge(
    val topRoundness: () -> Float,
    val centreDp: ((Float) -> Float)?,
    val side: ShellSide?,
    val motion: (() -> EdgePillMotion)?
)

@Composable
internal fun rememberShellEdge(
    notchMode: Boolean,
    layout: IslandLayout,
    wiring: IslandWiring,
    ui: IslandInteractionState,
    edgeCamera: EdgeCamera?,
    edgeLayoutOn: Boolean,
    edgeShellSurface: Boolean,
    edgePresence: Animatable<Float, AnimationVector1D>,
    edgeDensity: Float
): ShellEdge {
    val collapsingNow = { !wiring.view.expanded && (!ui.pillOpen.active || ui.pillOpen.closing) }
    val topRoundness: () -> Float = {
        if (notchMode && !edgeLayoutOn) {
            // A cutout tap swap keeps the island expanded, so its shells follow their own open fraction down to the
            // notch edge
            NotchTopEdge.roundness(layout.expandProgress,
                ui.swapRealSlowOpenOrNull(wiring.view.expanded, swapWidthDamping(false)), collapsingNow(),
                layout.expandProgressY)
        } else 1f
    }
    val shellLayoutNow by rememberUpdatedState(layout)
    val edgeCentreDp: ((Float) -> Float)? = remember(edgeCamera, edgeLayoutOn) {
        if (edgeLayoutOn && edgeCamera != null) {
            { w: Float -> EdgePillLayoutMath.cardPlacement(edgeCamera, w).cardCentreOffsetDp }
        } else null
    }
    val edgeShellSide = remember(edgeCamera, edgeLayoutOn) {
        val t = edgeCamera?.tuning
        if (edgeLayoutOn && edgeCamera != null && t != null && EdgePillRules.flatCameraSide(t.flatEdge, t.xOffsetDp)) {
            ShellSide(edgeCamera.onLeft) { NotchTopEdge.flat(shellLayoutNow.expandProgress,
                ui.swapRealSlowOpenOrNull(wiring.view.expanded, swapWidthDamping(true)), collapsingNow(),
                shellLayoutNow.expandProgressY) }
        } else null
    }

    val edgeShellMotion: (() -> EdgePillMotion)? = remember(edgeShellSurface, edgeCamera?.onLeft, edgeCamera?.tuning,
        edgeCamera?.pillWidthDp) {
        if (edgeShellSurface && edgeCamera != null) {
            {
                val slidePx = EdgePillRules.exitSlidePx(
                    edgeCamera.onLeft,
                    EdgePillRules.exitTravelDp(edgeCamera.pillWidthDp, edgeCamera.tuning.xOffsetDp) * edgeDensity,
                    edgePresence.value
                )
                EdgePillRules.pillMotion(
                    cameraOnLeft = edgeCamera.onLeft,
                    acrossPx = ui.dragOffset,
                    alongPx = ui.pillDragOffsetX,
                    arm = ui.pillArm.value,
                    dead = ui.deadZone.value,
                    pull = ui.swipeDownPull,
                    pulseScale = 1f + PILL_PULSE_WIDTH_FRACTION * shellLayoutNow.pillPulse,
                    flat = EdgePillRules.flatCameraSide(
                        edgeCamera.tuning.flatEdge, edgeCamera.tuning.xOffsetDp
                    ),
                    widthPx = edgeCamera.pillWidthDp * edgeDensity
                ).let { m ->
                    EdgePillMotion(
                        m.translationX + slidePx, m.translationY, m.scaleX * ui.edgeMergeScaleX,
                            m.scaleY * ui.edgeMergeScaleY, m.originX
                    )
                }
            }
        } else null
    }
    return ShellEdge(topRoundness, edgeCentreDp, edgeShellSide, edgeShellMotion)
}

internal class ShellOpacity(val mediaShell: Boolean, val opacity: State<Float>)

@Composable
internal fun rememberShellOpacity(
    expanded: Boolean,
    activeMode: IslandMode,
    settings: SmartIslandSettings,
    layoutSettings: SmartIslandSettings,
    layout: IslandLayout,
    pillExit: PillExitState,
    landscapeDot: IdleDotGeometry?,
    animSpeedNow: AnimationSpeed
): ShellOpacity {
    // the shell's opacity fades between the shared value and the media value
    val mediaShell = expanded && activeMode == IslandMode.Music
    val mediaShellOpacity = animateFloatAsState(
        targetValue = if (mediaShell) settings.mediaOpacity else layoutSettings.opacity,
        // When the pill has left and the island takes the idle settings
        animationSpec = if (pillExit.phase == PillPhase.Settling && landscapeDot != null) {
            snap()
        } else {
            tween(
                durationMillis = animSpeedNow.durationMs(MEDIA_SHELL_OPACITY_FADE_MS)
            )
        },
        label = "mediaShellOpacity"
    )
    // Tells the pill exit when the shell has really reached the idle settings
    SideEffect {
        pillExit.dotCoversHandover = landscapeDot != null
        pillExit.shellSettled = {
            val wantWidth = if (layout.isHiding && layout.hidingAlpha <= 0.01f) 0f else settings.width
            val wantHeight = if (layout.isHiding && layout.hidingAlpha <= 0.01f) 0f else settings.height
            abs(layout.safeWidth.value - wantWidth) < SHELL_SETTLED_DP &&
                abs(layout.safeHeight.value - wantHeight) < SHELL_SETTLED_DP &&
                abs(mediaShellOpacity.value - layoutSettings.opacity) < SHELL_SETTLED_OPACITY
        }
    }
    return ShellOpacity(mediaShell, mediaShellOpacity)
}

internal class PillFades(val contentFade: Modifier, val edgeShellAlpha: State<Float>)

@Composable
internal fun rememberPillFades(
    ui: IslandInteractionState,
    expanded: Boolean,
    edgeExiting: Boolean,
    edgeActive: Boolean,
    edgeShellSurface: Boolean
): PillFades {
    val pillHiddenByIsland = ui.widgetIslandShown && !expanded
    val pillContentAlpha = animateFloatAsState(
        targetValue = if (pillHiddenByIsland) 0f else 1f,
        animationSpec = if (pillHiddenByIsland) snap() else tween(120),
        label = "pillContentAlpha"
    )
    val pillContentFade = Modifier.graphicsLayer { alpha = pillContentAlpha.value }
    // Edge layout: the shell is the visible island while cards are on it
    val edgeShellAlpha = animateFloatAsState(
        targetValue = if (edgeExiting || (edgeActive && !edgeShellSurface)) 0f else 1f,
        animationSpec = if (edgeExiting) {
            snap()
        } else if (edgeActive && !edgeShellSurface) {
            tween(EDGE_SHELL_FADE_MS)
        } else {
            snap()
        },
        label = "edgeShellAlpha"
    )
    return PillFades(pillContentFade, edgeShellAlpha)
}

@NonRestartableComposable
@Composable
internal fun BoxScope.CollapsedShellLayers(
    layout: IslandLayout,
    collapsedContentFade: CollapsedContentFade,
    activeMode: IslandMode,
    activeNotification: IslandNotification?,
    settings: SmartIslandSettings,
    ui: IslandInteractionState,
    animSpeedNow: AnimationSpeed,
    idleSuppressed: Boolean,
    pillContentFade: Modifier,
    edgeLayoutOn: Boolean,
    bubbleShell: Boolean,
    isLandscape: Boolean
) {
    if (bubbleShell && activeNotification != null) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(layout.circleSize)
                .graphicsLayer {
                    alpha = BubbleOrigin.contentAlpha(layout.expandProgress)
                },
            contentAlignment = Alignment.Center
        ) {
            SecondaryBubbleContent(notification = activeNotification, settings = settings)
        }
    }
    if (layout.collapsedShown && !edgeLayoutOn && !bubbleShell) {
        CollapsedContentLayer(
            layout = layout,
            fade = collapsedContentFade,
            activeMode = activeMode,
            settings = settings,
            ui = ui,
            animSpeed = animSpeedNow,
            idleSuppressed = idleSuppressed,
            modifier = Modifier.align(Alignment.TopCenter).then(pillContentFade)
        )
        // the clock widget's tick on the full hour
        val tickCanHold = isLandscape && !settings.enableNotchMode
        HourChimeLayer(settings, ui, animSpeedNow, Modifier.align(Alignment.TopCenter).then(pillContentFade),
            cameraBand = layout.cameraBand, slotSide = layout.slotSide, canHold = tickCanHold)
        DayMomentLayer(settings, ui, animSpeedNow, Modifier.align(Alignment.TopCenter).then(pillContentFade),
            cameraBand = layout.cameraBand, slotSide = layout.slotSide, canHold = tickCanHold)
        WeatherTickLayer(settings, ui, animSpeedNow, Modifier.align(Alignment.TopCenter).then(pillContentFade),
            cameraBand = layout.cameraBand, slotSide = layout.slotSide, canHold = tickCanHold)
    }
}

internal fun shellAtRest(ui: IslandInteractionState): Boolean =
    ui.cardDragX == 0f && ui.dragOffset == 0f && ui.swipeDownPull == 0f && ui.handlePull == 0f &&
        ui.pendingCardEnterSign == 0 && ui.cardPop.value >= 1f && ui.cardLeanFromPx == 0f && !ui.stepDrag.active &&
        !ui.pillOpen.active

private const val SHELL_SETTLED_DP = 0.6f
internal const val EDGE_SHELL_GONE_DP = 1.5f
private const val EDGE_SHELL_FADE_MS = 120
private const val SHELL_SETTLED_OPACITY = 0.02f

private const val MEDIA_SHELL_OPACITY_FADE_MS = 200
