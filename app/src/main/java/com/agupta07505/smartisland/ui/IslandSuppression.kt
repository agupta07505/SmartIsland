/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/** How small the island shrinks while it fades out for immersive mode. */
internal const val SUPPRESSED_SCALE = 0.6f
internal const val SUPPRESSION_FADE_MS = 220

/**
 * SUPPRESSED (Rule_Book 10.1): the island scales down toward the camera and fades to zero; it comes back the
 * same way. Reduce Motion snaps. Touches are dropped by the window (FLAG_NOT_TOUCHABLE), not here.
 */
@Composable
fun Modifier.suppressedFade(suppressed: Boolean): Modifier {
    val reduceMotion = rememberReduceMotion()
    val progress by animateFloatAsState(
        targetValue = if (suppressed) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(SUPPRESSION_FADE_MS),
        label = "suppressedFade"
    )
    if (progress == 0f) return this
    return graphicsLayer {
        val scale = 1f - (1f - SUPPRESSED_SCALE) * progress
        scaleX = scale
        scaleY = scale
        alpha = 1f - progress
        transformOrigin = TransformOrigin(0.5f, 0f)
    }
}
