/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.provider.Settings
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalContext

/**
 * True when the person has turned system animations off (Remove animations / animator scale 0).
 * Decorative looping motion must stop in that case; it must never be the only carrier of meaning.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Whether the screen is on (Rule_Book 4.3, 5). The overlay provides it from the service's ACTION_SCREEN_ON /
 * ACTION_SCREEN_OFF; outside the overlay (the settings app) it is always true. Looping decorations and
 * once-a-second tickers stop while it is false and pick up again, recomputed from the clock, when it turns on.
 */
val LocalScreenOn = compositionLocalOf { true }

/**
 * Looping float animation that rests at [initialValue] when system animations are off or the screen is off.
 * The looping animation then leaves the transition altogether, so nothing ticks; it restarts from
 * [initialValue] when allowed again.
 */
@Composable
fun InfiniteTransition.animateFloatCalm(
    initialValue: Float,
    targetValue: Float,
    animationSpec: InfiniteRepeatableSpec<Float>,
    label: String = "FloatAnimation"
): State<Float> {
    val calm = rememberReduceMotion() || !LocalScreenOn.current
    if (calm) {
        return remember(initialValue) { mutableFloatStateOf(initialValue) }
    }
    return animateFloat(initialValue, targetValue, animationSpec, label)
}
