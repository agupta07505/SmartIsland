/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.agupta07505.smartisland.model.IslandNotification

internal const val CARD_SWAP_FADE_MS = 220

internal fun cardSwapFades(previousKey: String?, newKey: String?, swipeEntering: Boolean): Boolean =
    previousKey != null && newKey != null && previousKey != newKey && !swipeEntering

internal fun swapIncomingAlpha(progress: Float): Float = progress.coerceIn(0f, 1f)

internal fun swapOutgoingAlpha(progress: Float): Float = 1f - progress.coerceIn(0f, 1f)

internal class CardSwap(
    private val outgoingCard: IslandNotification?,
    private val crossingState: State<Boolean>,
    val progress: () -> Float
) {
    val crossing: Boolean get() = crossingState.value
    val outgoing: IslandNotification? get() = if (crossing) outgoingCard else null
}

@Composable
internal fun rememberCardSwap(
    current: IslandNotification?,
    swipeEntering: Boolean,
    durationMs: Int = CARD_SWAP_FADE_MS
): CardSwap {
    val last = remember { arrayOfNulls<IslandNotification>(1) }
    val now = remember { arrayOfNulls<IslandNotification>(1) }
    val fades = remember { booleanArrayOf(false) }
    val key = current?.key
    if (now[0]?.key != key) {
        last[0] = now[0]
        fades[0] = cardSwapFades(now[0]?.key, key, swipeEntering)
        now[0] = current
    }
    val anim = remember { Animatable(1f) }
    var startedKey by remember { mutableStateOf(key) }
    LaunchedEffect(key) {
        if (!fades[0]) {
            anim.snapTo(1f)
            startedKey = key
            return@LaunchedEffect
        }
        anim.snapTo(0f)
        startedKey = key
        anim.animateTo(1f, tween(durationMillis = durationMs, easing = FastOutSlowInEasing))
    }
    val crossing = remember(key) {
        derivedStateOf { fades[0] && (startedKey != key || anim.value < 1f) }
    }
    return CardSwap(last[0], crossing) { if (startedKey != key) 0f else anim.value }
}
