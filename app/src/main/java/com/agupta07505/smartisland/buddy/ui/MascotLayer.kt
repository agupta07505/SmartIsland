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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.buddy.model.MascotLook
import com.agupta07505.smartisland.buddy.model.MascotTrack
import com.agupta07505.smartisland.buddy.runtime.MascotBlink
import com.agupta07505.smartisland.ui.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * The buddy inside the closed pill. It is a pure drawing layer: no input of its own (the pill's one
 * gesture handler owns every touch) and hidden from accessibility, since it conveys nothing.
 *
 * [active] must be false whenever the pill is expanded, hidden or showing something else; then no
 * animation runs at all, so an idle resting buddy costs nothing between blinks.
 */
@Composable
fun MascotLayer(
    look: MascotLook,
    track: MascotTrack,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reduceMotion = rememberReduceMotion()
    val pose = remember { MascotPose() }

    LaunchedEffect(active, reduceMotion) {
        pose.reset()
        if (!active) return@LaunchedEffect
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        while (isActive) {
            delay(MascotBlink.nextDelayMs(lowPower = power?.isPowerSaveMode == true))
            pose.blink(double = MascotBlink.isDouble(), reduceMotion = reduceMotion)
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
