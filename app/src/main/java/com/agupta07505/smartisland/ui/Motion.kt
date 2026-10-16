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

/** Looping float animation that rests at [initialValue] when system animations are off. */
@Composable
fun InfiniteTransition.animateFloatCalm(
    initialValue: Float,
    targetValue: Float,
    animationSpec: InfiniteRepeatableSpec<Float>,
    label: String = "FloatAnimation"
): State<Float> {
    val reduce = rememberReduceMotion()
    val animated = animateFloat(initialValue, targetValue, animationSpec, label)
    val resting = remember(initialValue) { mutableFloatStateOf(initialValue) }
    return if (reduce) resting else animated
}
