/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.agupta07505.smartisland.util.runCatchingLogged

/** Composes and measures [content] whether or not it is shown. */
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

internal val LocalCardShown = compositionLocalOf { true }

internal val LocalExpandedAlpha = compositionLocalOf<(() -> Float)?> { null }

internal val LocalSwapActionsAlpha = compositionLocalOf<(() -> Float)?> { null }

@Composable
internal fun rememberScreenInteractive(): Boolean {
    val context = LocalContext.current
    var on by remember {
        mutableStateOf(
            (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive ?: true
        )
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> on = true
                    Intent.ACTION_SCREEN_OFF -> on = false
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(
            context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { runCatchingLogged("PrewarmedCard", "Could not unregister the prewarm receiver") { context.unregisterReceiver(receiver) } }
    }
    return on
}
