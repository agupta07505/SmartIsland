/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.EdgePillLayout
import com.agupta07505.smartisland.model.EdgePillLayoutMath
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.TapSwap
import com.agupta07505.smartisland.util.AnimationSpeed

/**
 * Edge pill's wide pulse for an event that arrives while a card is open: the pill's own length, glyph, rotated text
 * and progress line, drawn at the collapsed pill's place over the card. It replaces the pill under it (pill fades out
 * as the pulse fades in) and counts as the cutout for touches, a tap swaps the open island to the pulsing card like
 * a tap on the edge pill does ([onTap] gets the cutout's id and bounds). [layout] is the settled pill's, the one the
 * pulse grows from
 */
@Composable
internal fun EdgeOverCardPill(
    layout: EdgePillLayout,
    settings: SmartIslandSettings,
    card: IslandNotification?,
    redacted: Boolean,
    animSpeed: AnimationSpeed,
    ui: IslandInteractionState,
    modifier: Modifier = Modifier,
    onHold: (Boolean) -> Unit = {},
    onTap: (cutoutId: Int, bounds: () -> Rect?) -> Unit = { _, _ -> }
) {
    val shape = rememberEdgePulseShape(card, layout, redacted)
    val growth = rememberEdgePulseGrowth(shape, animSpeed)
    val fade = rememberPulsePeakFade(card, animSpeed, fadeInMs = OVER_CARD_FADE_IN_MS, fadeOutMs = OVER_CARD_FADE_OUT_MS)
    SideEffect {
        ui.pulseCoverOf = { id -> if (id == TapSwap.CUTOUT_EDGE_PILL) pulseCoverCurve(fade.alphaNow) else 0f }
    }
    DisposableEffect(ui) { onDispose { ui.pulseCoverOf = NoPulseCover } }
    val bounds = remember { arrayOfNulls<Rect>(1) }
    // Thicker by the line's height too; drawn over the strip, so the card beside it stays where it is
    val lineExtra = rememberPulseLineExtra(card, animSpeed)
    val growthShape = growth.shape
    val drawCard = fade.card
    if (growthShape == null || drawCard == null || fade.alphaNow <= 0f) return
    val grown = growth.grownLayout(layout, EdgePillLayoutMath.holeCentreYDp(layout), hasSecondary = false)
        ?: return
    val drawLayout = EdgePillLayoutMath.thickened(grown, lineExtra.value)
    val pillShape = remember(layout.camera) { edgePillShapeFor(layout.camera) }
    val opacity = layout.camera.tuning.opacity.coerceIn(0f, 1f)
    Box(modifier.fillMaxSize().graphicsLayer { alpha = fade.alphaNow * ui.swapHideAlpha(TapSwap.CUTOUT_EDGE_PILL) }) {
        Box(
            Modifier.align(Alignment.TopStart)
                .offset(x = drawLayout.pill.left.dp, y = drawLayout.pill.top.dp)
                .size(drawLayout.pill.width.dp, drawLayout.pill.height.dp)
                .onGloballyPositioned { coords -> if (!ui.tapSwapActive) bounds[0] = coords.boundsInWindow() }
                .pulseTapTarget(onHold) { onTap(TapSwap.CUTOUT_EDGE_PILL) { bounds[0] } }
                .clip(pillShape)
                .drawBehind { drawRect(Color.Black.copy(alpha = opacity)) }
        ) {
            EdgePulsePeakLayer(
                card = drawCard,
                settings = settings,
                shape = growthShape,
                holeFromTopDp = EdgePillLayoutMath.holeCentreYDp(layout) - drawLayout.pill.top,
                holeLengthDp = layout.camera.holeLengthDp,
                cornerDp = layout.camera.tuning.cornerRadiusDp,
                cameraOnLeft = layout.camera.onLeft,
                redacted = redacted,
                animSpeed = animSpeed,
                lineExtraDp = lineExtra.value
            )
        }
    }
}
