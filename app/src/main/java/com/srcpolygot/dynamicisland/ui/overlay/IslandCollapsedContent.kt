/*
 * Realistic Dynamic Island Fork
 * Collapsed state: pure black pill that hides the punch-hole
 * Supports up to 3 simultaneous activities (iPhone 18 Pro Max style)
 */

package com.srcpolygot.dynamicisland.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.srcpolygot.dynamicisland.domain.model.ActiveIslandState
import com.srcpolygot.dynamicisland.domain.model.IslandMode

/**
 * The collapsed Dynamic Island.
 * Pure black by default so it fully covers the camera cutout on devices like iQOO Z9x.
 */
@Composable
fun IslandCollapsedContent(
    state: ActiveIslandState,
    widthDp: Float,
    heightDp: Float,
    cornerRadiusDp: Float,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(cornerRadiusDp.dp)

    Box(
        modifier = modifier
            .width(widthDp.dp)
            .height(heightDp.dp)
            .clip(shape)
            .background(Color.Black), // Pure black to hide punch-hole
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            // Primary activity indicator (left side of pill)
            state.primary?.let { primary ->
                ActivityDot(mode = primary.mode)
            }

            // Secondary companion (if present)
            state.secondary?.let { secondary ->
                Spacer(modifier = Modifier.width(6.dp))
                ActivityDot(mode = secondary.mode, size = 10.dp)
            }

            // Tertiary companion (iPhone 18 Pro Max style – 3rd activity)
            state.tertiary?.let { tertiary ->
                Spacer(modifier = Modifier.width(6.dp))
                ActivityDot(mode = tertiary.mode, size = 10.dp)
            }
        }
    }
}

@Composable
private fun ActivityDot(
    mode: IslandMode,
    size: androidx.compose.ui.unit.Dp = 12.dp
) {
    val color = when (mode) {
        IslandMode.Music -> Color(0xFF1DB954)          // Spotify green-ish
        IslandMode.IncomingCall, IslandMode.OngoingCall -> Color(0xFF34C759)
        IslandMode.Timer, IslandMode.Stopwatch -> Color(0xFFFF9500)
        IslandMode.Navigation -> Color(0xFF007AFF)
        IslandMode.Charging, IslandMode.Battery -> Color(0xFF30D158)
        IslandMode.CameraInUse -> Color(0xFFFF3B30)   // Privacy red
        IslandMode.MicrophoneInUse -> Color(0xFFFF3B30)
        IslandMode.Flashlight -> Color(0xFFFFCC00)
        IslandMode.ScreenRecording -> Color(0xFFFF2D55)
        else -> Color.White.copy(alpha = 0.85f)
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}
