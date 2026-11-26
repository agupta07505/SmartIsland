/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import com.agupta07505.smartisland.model.PulseScroll
import kotlinx.coroutines.delay

/** The pulse's text reports here how long it needs to scroll through once (ms from now), see [PulseScroll]. */
internal val LocalPulseScrollNeed = staticCompositionLocalOf<(Long) -> Unit> { {} }

private class ScrollOnce {
    var overflowPx by mutableIntStateOf(0)
    val offsetPx = Animatable(0f)
}

/**
 * A single-line text wider than its room scrolls to its end once and rests there (rules and speed:
 * model/PulseWidth.kt). Where [basicMarquee] loops forever, this tells the pulse how long the pass takes
 */
@Composable
internal fun Modifier.pulseScrollOnce(text: String): Modifier {
    val need = LocalPulseScrollNeed.current
    val density = LocalDensity.current.density
    val scroll = remember(text) { ScrollOnce() }
    val overflowPx = scroll.overflowPx
    LaunchedEffect(text, overflowPx) {
        scroll.offsetPx.snapTo(0f)
        if (overflowPx <= 0) return@LaunchedEffect
        val overflowDp = overflowPx / density
        delay(PulseScroll.START_DELAY_MS)
        // The pulse could close mid-scroll. Request time for the rest of the scroll and the pause at the end.
        need(PulseScroll.remainingMs(overflowDp))
        scroll.offsetPx.animateTo(
            overflowPx.toFloat(),
            tween(PulseScroll.scrollMs(overflowDp).toInt(), easing = LinearEasing)
        )
    }
    return clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
        val width = placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val overflow = (placeable.width - width).coerceAtLeast(0)
        if (scroll.overflowPx != overflow) scroll.overflowPx = overflow
        val rtl = layoutDirection == LayoutDirection.Rtl
        layout(width, placeable.height) {
            placeable.placeWithLayer(if (rtl) width - placeable.width else 0, 0) {
                translationX = if (rtl) scroll.offsetPx.value else -scroll.offsetPx.value
            }
        }
    }
}
