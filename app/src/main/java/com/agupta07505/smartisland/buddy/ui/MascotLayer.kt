/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.ui

import android.content.Context
import android.os.PowerManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.buddy.model.MascotLook
import com.agupta07505.smartisland.buddy.model.MascotMood
import com.agupta07505.smartisland.buddy.model.MascotTrack
import com.agupta07505.smartisland.buddy.runtime.MascotBlink
import com.agupta07505.smartisland.buddy.runtime.MascotController
import com.agupta07505.smartisland.ui.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import java.util.Calendar

/** A reaction published longer ago than this is stale (for example, one that arrived while hidden). */
private const val FRESH_REACTION_SEC = 1.5

/**
 * The buddy inside the closed pill. It is a pure drawing layer: no input of its own (the pill's one
 * gesture handler owns every touch) and hidden from accessibility, since it conveys nothing.
 *
 * [active] must be false whenever the pill is expanded, hidden or showing something else; then no
 * animation runs at all, so an idle resting buddy costs nothing between blinks. While active it
 * blinks, grows sleepy after a long rest, and plays the reactions [controller] publishes.
 */
@Composable
fun MascotLayer(
    look: MascotLook,
    track: MascotTrack,
    active: Boolean,
    controller: MascotController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val reduceMotion = rememberReduceMotion()
    val pose = remember { MascotPose() }

    // The controller only plays a reaction while the buddy is at rest on a visible pill.
    LaunchedEffect(active) { controller.setAtRest(active) }
    DisposableEffect(controller) { onDispose { controller.setAtRest(false) } }

    // Reactions. Each one cuts the last short and leaves no motion behind.
    val hopPx = track.hop(0.28f) * density
    LaunchedEffect(controller, reduceMotion, hopPx) {
        var seen = controller.reaction.value?.id
        controller.reaction.collectLatest { event ->
            if (event == null || event.id == seen) return@collectLatest
            seen = event.id
            if (controller.now() - event.startSec > FRESH_REACTION_SEC) return@collectLatest
            pose.react(event.reaction, hopPx, reduceMotion)
        }
    }

    // Blinks, and a doze after a long rest. Nothing runs while it sleeps or is not at rest.
    LaunchedEffect(active, reduceMotion, pose.baseMood) {
        pose.blink.snapTo(1f)
        if (!active || !pose.baseMood.expression.blinks) return@LaunchedEffect
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        var blinks = 0
        while (isActive) {
            delay(MascotBlink.nextDelayMs(lowPower = power?.isPowerSaveMode == true))
            pose.blink(double = MascotBlink.isDouble(), reduceMotion = reduceMotion)
            blinks++
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (pose.baseMood == MascotMood.Idle && blinks >= MascotBlink.blinksBeforeSleep(hour)) {
                pose.setBaseMood(MascotMood.Sleepy, reduceMotion)
                return@LaunchedEffect
            }
        }
    }

    val size = track.size.dp
    // The box keeps headroom for the robot's antenna; the canvas centres the figure itself, so the box
    // is placed by its middle on the track's baseline.
    Box(modifier = modifier.fillMaxSize().clearAndSetSemantics { }) {
        Box(
            modifier = Modifier.offset(
                x = (track.rest - track.size / 2f).dp,
                y = (track.baseline - track.size / 2f).dp
            )
        ) {
            MascotCanvas(look = look, pose = pose, size = size)
        }
    }
}
