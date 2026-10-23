/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout

/**
 * Composes and measures [content] whether or not it is shown. While [placed] returns false the
 * content is measured but never placed, so it is not drawn and cannot be touched (an unplaced node is skipped by drawing
 * and by hit testing), yet its measured sizes (the card's height) are known before the tap that opens the island.
 *
 * It measures the way the `Box` it replaces did: the incoming constraints with the minimums dropped, content at (0, 0),
 * its own size clamped to the constraints. [placed] is read while placing, so a change re-places without recomposing.
 * [modifier] is applied to this layout node, so `requiredWidth` / `graphicsLayer` act on it exactly as they did on the Box.
 */
@Composable
internal fun PrewarmedCard(
    placed: () -> Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = subcompose(Unit, content).map { it.measure(loose) }
        val width = (placeables.maxOfOrNull { it.width } ?: 0).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (placeables.maxOfOrNull { it.height } ?: 0).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            if (placed()) placeables.forEach { it.place(0, 0) }
        }
    }
}

/**
 * True while the card is really on screen (the island is open or still closing). A pre-composed card that waits
 * collapsed reads false, so its endless loops (the media position, the wave) can pause.
 */
internal val LocalCardShown = compositionLocalOf { true }

/** Whether the screen is on (interactive). Used to pre-compose the media card only while someone can tap it. */
@Composable
internal fun rememberScreenInteractive(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    var on by remember {
        mutableStateOf(
            (context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager)?.isInteractive ?: true
        )
    }
    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, intent: android.content.Intent?) {
                when (intent?.action) {
                    android.content.Intent.ACTION_SCREEN_ON -> on = true
                    android.content.Intent.ACTION_SCREEN_OFF -> on = false
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_SCREEN_ON)
            addAction(android.content.Intent.ACTION_SCREEN_OFF)
        }
        androidx.core.content.ContextCompat.registerReceiver(
            context, receiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return on
}

